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

    config.put(new PropertySimple("foo", propertiesConfig.getString("foo")))
    config.put(new PropertySimple("bar", propertiesConfig.getString("bar")))
    config.put(new PropertySimple("bam", propertiesConfig.getString("bam")))

    return config
  }

  Set<RawConfiguration> loadRawConfigurations() {
    null
  }

  RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
    null
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
