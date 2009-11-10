package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.configuration.PropertySimple

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
//    Configuration config = new Configuration()
//    properties.each { key, value -> config.put(new PropertySimple(key, value)) }
//    return config
    return loadRawConfigurations()
  }

  Configuration loadRawConfigurations() {
    Configuration config = new Configuration()
    config.addRawConfiguration(new RawConfiguration(path: rawConfig1.absolutePath, contents: rawConfig1.readBytes()))

    return config
  }

  void mergeRawConfiguration(Configuration configuration, RawConfiguration rawConfiguration) {
  }

  void mergeStructuredConfiguration(RawConfiguration rawConfiguration, Configuration configuration) {
    def buffer = new String(rawConfiguration.contents)
    buffer.splitEachLine("=") { tokens -> configuration.put(new PropertySimple(tokens[0], tokens[1]))  }
  }

}