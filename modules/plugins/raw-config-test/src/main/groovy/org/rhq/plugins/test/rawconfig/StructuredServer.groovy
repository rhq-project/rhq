package org.rhq.plugins.test.rawconfig

import org.rhq.core.pluginapi.configuration.ResourceConfigurationFacet
import org.rhq.core.pluginapi.inventory.ResourceComponent
import org.rhq.core.pluginapi.inventory.ResourceContext
import org.rhq.core.domain.measurement.AvailabilityType
import org.rhq.core.domain.configuration.PropertySimple
import org.rhq.core.domain.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration
import org.rhq.core.domain.configuration.RawConfiguration
import groovy.util.AntBuilder

class StructuredServer implements ResourceComponent, ResourceConfigurationFacet {

  ResourceContext resourceContext

  File configDir

  File structuredConfigFile

  def ant = new AntBuilder()

  void start(ResourceContext context) {
    resourceContext = context

    configDir = new File("${System.getProperty('java.io.tmpdir')}/raw-config-test")
    structuredConfigFile = new File(configDir, "structured-test-1.txt")

    createRawConfigDir()
    createConfigFile()
  }

  def createRawConfigDir() {
    configDir.mkdirs()
  }

  def createConfigFile() {
    if (structuredConfigFile.exists()) {
      return null
    }

    def properties = ["foo": "1", "bar": "2", "bam": "3"]

    structuredConfigFile.createNewFile()
    structuredConfigFile.withWriter { writer ->
      properties.each { key, value -> writer.writeLine("${key}=${value}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    def propertiesConfig = new PropertiesConfiguration(structuredConfigFile)
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
    def failValidation = resourceContext.pluginConfiguration.getSimple("failUpdate")
    if (failValidation.getBooleanValue()) {
      throw new RuntimeException("Validation failed for $configuration");
    }

    ant.copy(file: structuredConfigFile.absolutePath, tofile: "${structuredConfigFile.absolutePath}.orig")
    ant.delete(file: structuredConfigFile)

    def file = new File(structuredConfigFile.path)
    file.createNewFile()

    def propertiesConfig = new PropertiesConfiguration(structuredConfigFile)

    configuration.properties.each { propertiesConfig.setProperty(it.name, it.stringValue)  }
    propertiesConfig.save(file) 
  }

  void persistRawConfiguration(RawConfiguration rawConfiguration) {
  }

  void validateStructuredConfiguration(Configuration configuration) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failValidation")
    if (failValidation.getBooleanValue()) {
      throw new RuntimeException("Validation failed for $configuration");
    }
  }

  void validateRawConfiguration(RawConfiguration rawConfiguration) {
  }

}
