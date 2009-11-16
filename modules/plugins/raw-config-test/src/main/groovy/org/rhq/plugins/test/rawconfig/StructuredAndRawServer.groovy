package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.configuration.PropertySimple
import org.apache.commons.configuration.PropertiesConfiguration

class StructuredAndRawServer implements ResourceComponent, ResourceConfigurationFacet {

  File rawConfigDir

  File rawConfig1

  File rawConfig2

  File rawConfig3

  void start(ResourceContext context) {
    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    rawConfig1 = new File(rawConfigDir, "structured-and-raw-test-1.txt")
    rawConfig2 = new File(rawConfigDir, "structured-and-raw-test-2.txt")
    rawConfig3 = new File(rawConfigDir, "structured-and-raw-test-3.txt")

    createRawConfigDir()
    createConfigFile(rawConfig1, ["x": "1", "y": "2", "z": "3"])
    createConfigFile(rawConfig2, ["username": "rhqadmin", "password": "rhqadmin"])
    createConfigFile(rawConfig3, ["rhq.server.hostname": "localhost", "rhq.server.port": "7080"])
  }

  def createRawConfigDir() {
    rawConfigDir.mkdirs()
  }

  def createConfigFile(File rawConfig, Map properties) {
    if (rawConfig.exists()) {
      return null
    }

    addProperties(rawConfig, properties)
  }

  def addProperties(File rawConfig, Map properties) {
    rawConfig.createNewFile()
    rawConfig.withWriter {writer ->
      properties.each {key, value -> writer.writeLine("${key}=${value}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    def config = new Configuration()

    loadStructuredForRawConfig1(config)
    loadStructuredForRawConfig2(config)
    loadStructuredForRawConfig3(config)

    return config
  }

  def loadStructuredForRawConfig1(Configuration config) {
    def propertiesConfig = new PropertiesConfiguration(rawConfig1)

    config.put(new PropertySimple("x", propertiesConfig.getString("x")))
    config.put(new PropertySimple("y", propertiesConfig.getString("y")))
    config.put(new PropertySimple("z", propertiesConfig.getString("z")))
  }

  def loadStructuredForRawConfig2(Configuration config) {
    def propertiesConfig = new PropertiesConfiguration(rawConfig2)

    config.put(new PropertySimple("username", propertiesConfig.getString("username")))
    config.put(new PropertySimple("password", propertiesConfig.getString("password")))
  }

  def loadStructuredForRawConfig3(Configuration config) {
    def propertiesConfig = new PropertiesConfiguration(rawConfig3)

    config.put(new PropertySimple("rhq.server.hostname", propertiesConfig.getString("rhq.server.hostname")))
    config.put(new PropertySimple("rhq.server.port", propertiesConfig.getString("rhq.server.port")))
  }

  Set<RawConfiguration> loadRawConfigurations() {
    def rawConfigs = new HashSet<RawConfiguration>()
    rawConfigs.add(new RawConfiguration(path: rawConfig1.absolutePath, contents: rawConfig1.readBytes()))
    rawConfigs.add(new RawConfiguration(path: rawConfig2.absolutePath, contents: rawConfig2.readBytes()))
    rawConfigs.add(new RawConfiguration(path: rawConfig3.absolutePath, contents: rawConfig3.readBytes()))

    return rawConfigs
  }

  void mergeRawConfiguration(Configuration configuration, RawConfiguration rawConfiguration) {
    def rawPropertiesConfig = loadRawPropertiesConfiguration(rawConfiguration)
    def propertyNames = getPropertyNames(rawConfiguration)

    propertyNames.each { propertyName ->
      def property = configuration.get(propertyName)
      rawPropertiesConfig.setProperty(propertyName, property.stringValue)
    }

//    def x = configuration.get("x")
//    def y = configuration.get("y")
//    def z = configuration.get("z")
//
//    rawPropertiesConfig.setProperty("x", x.stringValue)
//    rawPropertiesConfig.setProperty("y", y.stringValue)
//    rawPropertiesConfig.setProperty("z", z.stringValue)

    def stream = new ByteArrayOutputStream()
    rawPropertiesConfig.save(stream)
    rawConfiguration.contents = stream.toByteArray()    
  }

  def loadRawPropertiesConfiguration(rawConfig) {
    def stream = new ByteArrayInputStream(rawConfig.contents)
    def rawPropertiesConfig = new PropertiesConfiguration()
    rawPropertiesConfig.load(stream)

    return rawPropertiesConfig
  }

  void mergeStructuredConfiguration(RawConfiguration rawConfiguration, Configuration configuration) {
    def rawPropertiesConfig = loadRawPropertiesConfiguration(rawConfiguration)
    def propertyNames = getPropertyNames(rawConfiguration)

    propertyNames.each { name -> configuration.put(new PropertySimple(name, rawPropertiesConfig.getString(name))) }

//    configuration.put(new PropertySimple("x", rawPropertiesConfig.getString("x")))
//    configuration.put(new PropertySimple("y", rawPropertiesConfig.getString("y")))
//    configuration.put(new PropertySimple("z", rawPropertiesConfig.getString("z")))
  }

  def getPropertyNames(rawConfig) {
    if (rawConfig == rawConfig1) {
      return ["x", "y", "z"]
    }
    else if (rawConfig == rawConfig2) {
      return ["username", "password"]
    }
    else if (rawConfig == rawConfig3) {
      return ["rhq.server.hostname", "rhq.server.port"]
    }
    else {
      throw new RuntimeException("$rawConfig is not a recongnized raw config file")
    }
  }

  void validateRawConfiguration(RawConfiguration rawConfig) {
  }

  void validateStructuredConfiguration(Configuration config) {
  }

  void persistStructuredConfiguration(Configuration config) {
  }

  void persistRawConfiguration(RawConfiguration rawConfig) {    
  }

}