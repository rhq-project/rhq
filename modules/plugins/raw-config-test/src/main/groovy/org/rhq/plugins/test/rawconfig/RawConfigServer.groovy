package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.configuration.PropertySimple

class RawConfigServer implements ResourceComponent, ResourceConfigurationFacet {

  def properties = ["x": 1, "y": 2, "z": "3"]

  File rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")

  void start(ResourceContext context) {
    createRawConfigDir()
    createConfigFile()
  }

  def createRawConfigDir() {
    rawConfigDir.mkdirs()
  }

  def createConfigFile() {
    def configFile = new File(rawConfigDir, "rawconfig-test-1.txt")

    if (configFile.exists()) {
      return null
    }

    configFile.createNewFile()
    configFile.withWriter { writer ->
      properties.each { key, value -> writer.writeLine("${key}=${value}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    Configuration config = new Configuration()
    properties.each { key, value -> config.put(new PropertySimple(key, value)) }
    return config
  }

  Configuration loadRawConfigurations() {
    return null;
  }

  void mergeRawConfiguration(Configuration configuration, RawConfiguration rawConfiguration) {
  }

  void mergeStructuredConfiguration(RawConfiguration rawConfiguration, Configuration configuration) {
  }

}