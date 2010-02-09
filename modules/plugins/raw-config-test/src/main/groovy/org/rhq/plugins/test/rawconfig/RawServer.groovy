package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.RawConfiguration
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.pluginapi.inventory.ResourceContext
import groovy.util.AntBuilder

class RawServer extends ConfigurationServer implements ResourceComponent, ResourceConfigurationFacet {

  File rawConfigDir

  List rawConfigs = []

  void start(ResourceContext context) {
    resourceContext = context;

    def numberOfConfigFiles = 3

    ant = new AntBuilder()

    rawConfigDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")

    def index = 1

    numberOfConfigFiles.times { rawConfigs << new File(rawConfigDir, "raw-test-${it + index++}.txt") }

    createRawConfigDir()

    index = 1
    rawConfigs.each { rawConfig -> createConfigFile(rawConfig, index++) }
  }

  def createRawConfigDir() {
    rawConfigDir.mkdirs()
  }

  def createConfigFile(rawConfig, index) {
    if (rawConfig.exists()) {
      return null
    }

    def properties = ["raw${index}.a": 1, "raw${index}.b": 2, "raw${index}.c": "3"]

    rawConfig.createNewFile()
    rawConfig.withWriter { writer ->
      properties.each { key, value -> writer.writeLine("${key} = ${value}") }
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
    def rawConfigSet = new HashSet()
    rawConfigs.each { rawConfigSet.add(new RawConfiguration(path: it.absolutePath, contents: it.text)) }

    return rawConfigSet
  }

  RawConfiguration mergeRawConfiguration(Configuration from, RawConfiguration to) {
    null
  }

  void mergeStructuredConfiguration(RawConfiguration from, Configuration to) {
  }

  void persistStructuredConfiguration(Configuration configuration) {
  }

  void persistRawConfiguration(RawConfiguration rawConfiguration) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failRawUpdate")
    if (failValidation.booleanValue) {
      throw new RuntimeException("Update failed for ${rawConfiguration.path}");
    }

    pauseForRawUpdateIfDelaySet()
    backupAndSave(rawConfiguration)
  }

  void validateStructuredConfiguration(Configuration configuration) {
  }

  void validateRawConfiguration(RawConfiguration rawConfiguration) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failRawValidation")
    if (failValidation.getBooleanValue()) {
      throw new IllegalArgumentException("Validation failed for ${rawConfiguration.path}");
    }
  }

}
