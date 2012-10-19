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
import org.rhq.core.domain.configuration.PropertyMap
import org.rhq.core.domain.configuration.PropertyList

class StructuredServer extends ConfigurationServer implements ResourceComponent, ResourceConfigurationFacet {

  File configDir

  File structuredConfigFile

  def simpleProperties = ["foo", "bar", "bam"]

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

    int index = 1

    structuredConfigFile.createNewFile()
    structuredConfigFile.withWriter { writer ->
      simpleProperties.each { writer.writeLine("${it} = ${index++}") }
    }
  }

  void stop() {
  }

  AvailabilityType getAvailability() {
    AvailabilityType.UP
  }

  Configuration loadStructuredConfiguration() {
    def configuration = new Configuration()
    configuration.properties = loadSimpleProperties(structuredConfigFile, simpleProperties)


    loadComplexProperties(configuration, new PropertiesConfiguration(structuredConfigFile))

    return configuration
  }

  def loadComplexProperties(Configuration configuration, PropertiesConfiguration propertiesConfig) {
    def listProperty = new PropertyList("listProperty")
    def index = 0
    def complexProperties = propertiesConfig.subset("listProperty.${index}")

    while (!complexProperties.isEmpty()) {
      def x = new PropertySimple("listProperty.x", complexProperties.getString("x"))
      def y = new PropertySimple("listProperty.y", complexProperties.getString("y"))
      def z = new PropertySimple("listProperty.z", complexProperties.getString("z"))

      listProperty.add(new PropertyMap("listPropertyValues", x, y, z))

      complexProperties = propertiesConfig.subset("listProperty.${++index}")
    }

    configuration.put(listProperty)
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
    def failValidation = resourceContext.pluginConfiguration.getSimple("failStructuredUpdate")
    if (failValidation != null && failValidation.booleanValue) {
      throw new RuntimeException("Validation failed for $configuration");
    }

    pauseForStructuredUpdateIfDelaySet()

    ant.copy(file: structuredConfigFile.absolutePath, tofile: "${structuredConfigFile.absolutePath}.orig")
    ant.delete(file: structuredConfigFile)

    def file = new File(structuredConfigFile.path)
    file.createNewFile()

    def propertiesConfig = new PropertiesConfiguration(structuredConfigFile)

    persistSimpleProperties(configuration, propertiesConfig)
    persistComplexProperties(configuration, propertiesConfig)

    propertiesConfig.save(file) 
  }

  def persistSimpleProperties(Configuration configuration, PropertiesConfiguration propertiesConfig) {
    simpleProperties.each { propertyName ->
      def property = configuration.getSimple(propertyName)
      propertiesConfig.setProperty(property.name, property.stringValue)
    }
  }

  def persistComplexProperties(Configuration configuration, PropertiesConfiguration propertiesConfig) {
    def listProperty = configuration.getList("listProperty")

    if (listProperty == null) {
      return
    }

    for (i in 0 ..< listProperty.list.size()) {
      def mapProperty = listProperty.list[i]
      propertiesConfig.setProperty("listProperty.${i}.x", mapProperty.getSimpleValue("listProperty.x", null))
      propertiesConfig.setProperty("listProperty.${i}.y", mapProperty.getSimpleValue("listProperty.y", null))
      propertiesConfig.setProperty("listProperty.${i}.z", mapProperty.getSimpleValue("listProperty.z", null))
    }
  }

  void persistRawConfiguration(RawConfiguration rawConfiguration) {
  }

  void validateStructuredConfiguration(Configuration configuration) {
    def failValidation = resourceContext.pluginConfiguration.getSimple("failStructuredValidation")
    if (failValidation != null && failValidation.booleanValue) {
      throw new RuntimeException("Validation failed for $configuration");
    }
  }

  void validateRawConfiguration(RawConfiguration rawConfiguration) {
  }

}
