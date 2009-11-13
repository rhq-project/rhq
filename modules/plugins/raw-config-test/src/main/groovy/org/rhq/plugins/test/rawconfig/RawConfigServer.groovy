package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.configuration.PropertySimple
import org.apache.commons.configuration.PropertiesConfiguration

class RawConfigServer implements ResourceComponent, ResourceConfigurationFacet {

  File rawConfigDir

  File rawConfig1

  void start(ResourceContext context) {
    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    rawConfig1 = new File(rawConfigDir, "rawconfig-test-1.txt")

    createRawConfigDir()
    createConfigFile()
  }

  def createRawConfigDir() {
    rawConfigDir.mkdirs()
  }

  def createConfigFile() {
    if (rawConfig1.exists()) {
      return null
    }

    def properties = ["x": 1, "y": 2, "z": "3"]

    rawConfig1.createNewFile()
    rawConfig1.withWriter { writer ->
      properties.each { key, value -> writer.writeLine("${key}=${value}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    return loadRawConfigurations()
  }

  Configuration loadRawConfigurations() {
    Configuration config = new Configuration()
    config.addRawConfiguration(new RawConfiguration(path: rawConfig1.absolutePath, contents: rawConfig1.readBytes()))

    return config
  }

  void mergeRawConfiguration(Configuration configuration, RawConfiguration rawConfiguration) {
    def rawPropertiesConfig = loadRawPropertiesConfiguration(rawConfiguration)

    def x = configuration.get("x")
    def y = configuration.get("y")
    def z = configuration.get("z")

    rawPropertiesConfig.setProperty("x", x.stringValue)
    rawPropertiesConfig.setProperty("y", y.stringValue)
    rawPropertiesConfig.setProperty("z", z.stringValue)

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

    configuration.put(new PropertySimple("x", rawPropertiesConfig.getString("x")))
    configuration.put(new PropertySimple("y", rawPropertiesConfig.getString("y")))
    configuration.put(new PropertySimple("z", rawPropertiesConfig.getString("z")))
  }

}