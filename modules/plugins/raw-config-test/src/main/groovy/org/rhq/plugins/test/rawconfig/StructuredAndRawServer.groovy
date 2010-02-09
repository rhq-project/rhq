package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.configuration.PropertySimple
import org.apache.commons.configuration.PropertiesConfiguration

class StructuredAndRawServer extends ConfigurationServer implements ResourceComponent, ResourceConfigurationFacet {

  ResourceContext resourceContext

  File rawConfigDir

  File rawConfigSubdir1

  File rawConfigSubdir2

  File rawConfig1

  List rawConfig1PropertyNames = ["x", "y", "z", "structuredOnlyProperty"]

  File rawConfig2

  List rawConfig2PropertyNames = ["username", "password"]

  File rawConfig3

  List rawConfig3PropertyNames = ["rhq.server.hostname", "rhq.server.port"]

  File rawConfig4

  def ant = new AntBuilder()

  void start(ResourceContext context) {
    resourceContext = context

    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    rawConfigSubdir1 = new File(rawConfigDir, "structured-and-raw-1")
    rawConfigSubdir2 = new File(rawConfigDir, "structured-and-raw-2")

    rawConfig1 = new File(rawConfigSubdir1, "structured-and-raw-test-1.txt")
    rawConfig2 = new File(rawConfigSubdir1, "structured-and-raw-test-2.txt")
    rawConfig3 = new File(rawConfigSubdir2, "structured-and-raw-test-3.txt")
    rawConfig4 = new File(rawConfigSubdir2, "structured-and-raw-test-4.txt")

    createRawConfigDirs()
    createConfigFile(rawConfig1, ["x": "1", "y": "2", "z": "3", "structuredOnlyProperty": "a structured property"])
    createConfigFile(rawConfig2, ["username": "rhqadmin", "password": "rhqadmin"])
    createConfigFile(rawConfig3, ["rhq.server.hostname": "localhost", "rhq.server.port": "7080"])
    createConfigFile(rawConfig4, ["raw.only.x": "foo", "raw.only.y": "bar"])
  }

  def createRawConfigDirs() {
    rawConfigSubdir1.mkdirs()
    rawConfigSubdir2.mkdirs()
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
      properties.each {key, value -> writer.writeLine("${key} = ${value}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    def properties = loadSimpleProperties(rawConfig1, rawConfig1PropertyNames)
    properties.addAll(loadSimpleProperties(rawConfig2, rawConfig2PropertyNames))
    properties.addAll(loadSimpleProperties(rawConfig3, rawConfig3PropertyNames))

    def configuration = new Configuration()
    configuration.properties = properties

    return configuration
  }

  Set<RawConfiguration> loadRawConfigurations() {
    def rawConfigs = new HashSet<RawConfiguration>()
    rawConfigs.add(new RawConfiguration(path: rawConfig1.absolutePath, contents: rawConfig1.text))
    rawConfigs.add(new RawConfiguration(path: rawConfig2.absolutePath, contents: rawConfig2.text))
    rawConfigs.add(new RawConfiguration(path: rawConfig3.absolutePath, contents: rawConfig3.text))
    rawConfigs.add(new RawConfiguration(path: rawConfig4.absolutePath, contents: rawConfig4.text))

    return rawConfigs
  }

  RawConfiguration mergeRawConfiguration(Configuration configuration, RawConfiguration rawConfiguration) {
    if (rawConfiguration.path == rawConfig4.absolutePath) {
      return rawConfiguration
    }

    def rawPropertiesConfig = loadRawPropertiesConfiguration(rawConfiguration)
    def propertyNames = getPropertyNames(rawConfiguration)

    propertyNames.each { propertyName ->
      def property = configuration.get(propertyName)
      rawPropertiesConfig.setProperty(propertyName, property.stringValue)
    }

    def writer = new StringWriter()
    rawPropertiesConfig.save(writer)
    rawConfiguration.contents = writer.toString()

    return rawConfiguration
  }

  def loadRawPropertiesConfiguration(rawConfig) {
    def rawPropertiesConfig = new PropertiesConfiguration()
    rawPropertiesConfig.load(new StringReader(rawConfig.contents))

    return rawPropertiesConfig
  }

  void mergeStructuredConfiguration(RawConfiguration rawConfiguration, Configuration configuration) {
    if (rawConfiguration.path == rawConfig4.absolutePath) {
      return
    }

    def rawPropertiesConfig = loadRawPropertiesConfiguration(rawConfiguration)
    def propertyNames = getPropertyNames(rawConfiguration)

    propertyNames.each { name -> configuration.put(new PropertySimple(name, rawPropertiesConfig.getString(name))) }
  }

  def getPropertyNames(rawConfig) {
    if (rawConfig.getPath() == rawConfig1.getPath()) {
      return ["x", "y", "z"]
    }
    else if (rawConfig.getPath() == rawConfig2.getPath()) {
      return ["username", "password"]
    }
    else if (rawConfig.getPath() == rawConfig3.getPath()) {
      return ["rhq.server.hostname", "rhq.server.port"]
    }
    else {
      throw new RuntimeException("$rawConfig.getPath() is not a recongnized raw config file")
    }
  }

  void validateRawConfiguration(RawConfiguration rawConfig) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failRawValidation")
    if (failValidation.getBooleanValue()) {
      def fileNames = resourceContext.pluginConfiguration.getSimple("filesToFailValidation").stringValue.split()
      def match = fileNames.find { rawConfig.path.endsWith(it) }

      if (match) {
        throw new RuntimeException("Validation failed for ${rawConfig.path}");
      }
    }
  }

  void validateStructuredConfiguration(Configuration config) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failStructuredValidation")
    if (failValidation.getBooleanValue()) {
      throw new RuntimeException("Validation failed for $config");
    }
  }

  void persistStructuredConfiguration(Configuration config) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failStructuredUpdate")
    if (failValidation.getBooleanValue()) {
      throw new RuntimeException("Update failed for $configuration");
    }
  }

  void persistRawConfiguration(RawConfiguration rawConfiguration) {
    def failUpdate = resourceContext.pluginConfiguration.getSimple("failRawUpdate")
    if (failUpdate.getBooleanValue()) {
      def fileNames = resourceContext.pluginConfiguration.getSimple("filesToFailUpdate").stringValue.split()
      def match = fileNames.find { rawConfiguration.path.endsWith(it) }

      if (match) {
        throw new RuntimeException("Update failed for ${rawConfiguration.path}");
      }
    }

    backupAndSave(rawConfiguration)
  }

}