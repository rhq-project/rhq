package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.domain.configuration.PropertySimple
import org.rhq.core.domain.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration
import org.rhq.core.domain.configuration.RawConfiguration

class StructuredServer implements ResourceComponent, ResourceConfigurationFacet {

  File rawConfigDir

  File rawConfig1

  void start(ResourceContext context) {
    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    rawConfig1 = new File(rawConfigDir, "structured-test-1.txt")

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

    def properties = ["foo": "1", "bar": "2", "bam": "3"]

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
    def propertiesConfig = new PropertiesConfiguration(rawConfig1)
    def config = new Configuration()

    config.put(new PropertySimple("x", propertiesConfig.getString("x")))
    config.put(new PropertySimple("y", propertiesConfig.getString("y")))
    config.put(new PropertySimple("z", propertiesConfig.getString("z")))

    return config
  }

  Set<RawConfiguration> loadRawConfigurations() {
    null
  }

  void mergeRawConfiguration(Configuration from, RawConfiguration to) {
  }

  void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {
  }

  void persistStructuredConfiguration(Configuration configuration) {
  }

  void persistRawConfiguration(RawConfiguration rawConfiguration) {
  }

  void validateStructuredConfiguration(Configuration configuration) {
  }

  void validateRawConfiguration(RawConfiguration rawConfiguration) {
  }

}
