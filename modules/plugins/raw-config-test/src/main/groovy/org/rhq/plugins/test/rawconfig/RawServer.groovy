package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.inventory.ResourceContext

class RawServer implements ResourceComponent, ResourceConfigurationFacet {

  File rawConfigDir

  File rawConfig1

  void start(ResourceContext context) {
    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    rawConfig1 = new File(rawConfigDir, "raw-test-1.txt")

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

    def properties = ["a": 1, "b": 2, "c": "3"]

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
    null
  }

  Set<RawConfiguration> loadRawConfigurations() {
    def rawConfigs = new HashSet()
    rawConfigs.add(new RawConfiguration(path: rawConfig1.absolutePath, contents: rawConfig1.readBytes()))

    return rawConfigs
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
