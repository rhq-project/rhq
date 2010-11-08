package org.rhq.enterprise.server.resource.metadata

import org.testng.annotations.Test
import org.rhq.core.domain.measurement.MeasurementDefinition
import org.rhq.test.AssertUtils
import org.rhq.core.domain.measurement.MeasurementCategory
import org.rhq.core.domain.measurement.MeasurementUnits
import org.rhq.core.domain.measurement.DataType
import org.rhq.core.domain.measurement.DisplayType
import org.rhq.core.domain.measurement.NumericType

class MeasurementMetadataManagerBeanTest extends MetadataTest {

  @Test(groups = ['NewPlugin'])
  void registerPlugin() {
    def pluginDescriptor =
    """
    <plugin name="MeasurementMetadataManagerBeanTestPlugin"
            displayName="MeasurementMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="MetricServer1">
        <metric displayName="metric1" property="metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
        <metric displayName="metric2" property="metric2" dataType="measurement" displayType="detail"
                description="Metric 2" category="performance" defaultInterval="30000" defaultOn="true"
                units="megabytes" measurementType="trendsup"/>
        <metric displayName="metric3" property="metric3" dataType="calltime" displayType="detail"
                description="Metric 3" category="throughput" defaultInterval="30000" defaultOn="true"
                units="milliseconds" destinationType="myMethod" />
      </server>
      <server name="MetricServer2"/>
      <server name="MetricServer3">
        <metric displayName="metric1" property="metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
      </server>
      <server name="MetricServer4">
        <metric displayName="metric1" property="metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
      </server>
      <server name="MetricServer5">
        <!--
          Need to use names for these metrics that are unique across the plugin because the resource
          type will be deleted and the metric definition cannot be looked up by resource type. We will
          instead look them up by name only.
        -->
        <metric displayName="metric1" property="MetricServer5.metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
        <metric displayName="metric3" property="MetricServer5.metric3" dataType="calltime" displayType="detail"
                description="Metric 3" category="throughput" defaultInterval="30000" defaultOn="true"
                units="milliseconds" destinationType="myMethod" />
      </server>
    </plugin>
    """

    createPlugin("metric-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['NewPlugin'], dependsOnMethods = ['registerPlugin'])
  void persistNewMetrics() {
    assertResourceTypeAssociationEquals(
        'MetricServer1',
        'MeasurementMetadataManagerBeanTestPlugin',
        'metricDefinitions',
        ['metric1', 'metric2', 'metric3']
    )
  }

  @Test(groups = ['NewPlugin'], dependsOnMethods = ['persistNewMetrics'])
  void persistNewTraitDefinitionProperties() {
    def traitDef = loadMeasurementDef('metric1', 'MetricServer1')

    MeasurementDefinition expected = new  MeasurementDefinition('metric1', MeasurementCategory.AVAILABILITY,
        MeasurementUnits.MILLISECONDS, DataType.TRAIT, NumericType.DYNAMIC, true, 30000, DisplayType.SUMMARY)
    expected.description = 'Metric 1'
    expected.displayName = 'metric1'
    expected.displayOrder = 1

    AssertUtils.assertPropertiesMatch(
        'Failed to persist properties for a trait metric definition',
        expected,
        traitDef,
        ['id', 'resourceType']
    )
  }

  @Test(groups = ['NewPlugin'], dependsOnMethods = ['persistNewMetrics'])
  void persistNewNumericMeasurementDef() {
    def measurementDef = loadMeasurementDef('metric2', 'MetricServer1')

    MeasurementDefinition expected = new MeasurementDefinition('metric2', MeasurementCategory.PERFORMANCE,
        MeasurementUnits.MEGABYTES, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL)
    expected.rawNumericType = null
    expected.description = 'Metric 2'
    expected.displayName = 'metric2'
    expected.displayOrder = 2

    AssertUtils.assertPropertiesMatch(
        'Failed to persist properties for numeric metric definition',
        expected,
        measurementDef,
        ['id', 'resourceType']
    )

    def perMinuteDef = loadMeasurementDef('metric2', 'MetricServer1', 'metric2 per Minute')

    expected = new MeasurementDefinition(measurementDef)
    expected.displayName = 'metric2 per Minute'
    expected.displayOrder = 3
    expected.defaultOn = true
    expected.numericType = NumericType.DYNAMIC
    expected.rawNumericType = measurementDef.numericType

    AssertUtils.assertPropertiesMatch(
        'Failed to create and persist per minute metric definition for numeric metric definition',
        expected,
        perMinuteDef,
        ['id', 'resourceType']
    )
  }

  @Test(groups = ['NewPlugin'], dependsOnMethods = ['persistNewMetrics'])
  void persistNewCallTimeDef() {
    def calltimeDef = loadMeasurementDef('metric3', 'MetricServer1')

    MeasurementDefinition expected = new  MeasurementDefinition('metric3', MeasurementCategory.THROUGHPUT,
        MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 30000, DisplayType.DETAIL)
    expected.numericType = NumericType.DYNAMIC
    expected.destinationType = 'myMethod'
    expected.description = 'Metric 3'
    expected.displayName = 'metric3'
    expected.displayOrder = 4

    AssertUtils.assertPropertiesMatch(
        'Failed to create calltime metric definition',
        expected,
        calltimeDef,
        ['id', 'resourceType']
    )
  }

  @Test(groups = ['UpgradePlugin'], dependsOnGroups = ['NewPlugin'])
  void upgradePlugin() {
    def pluginDescriptor =
    """
    <plugin name="MeasurementMetadataManagerBeanTestPlugin"
            displayName="MeasurementMetadataManagerBean Test Plugin"
            package="org.rhq.plugins.test"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns="urn:xmlns:rhq-plugin"
            xmlns:c="urn:xmlns:rhq-configuration">
      <server name="MetricServer1">
        <metric displayName="metric1" property="metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
        <metric displayName="metric2" property="metric2" dataType="measurement" displayType="detail"
                description="Metric 2" category="performance" defaultInterval="30000" defaultOn="true"
                units="megabytes" measurementType="trendsup"/>
        <metric displayName="metric3" property="metric3" dataType="calltime" displayType="detail"
                description="Metric 3" category="throughput" defaultInterval="30000" defaultOn="true"
                units="milliseconds" destinationType="myMethod" />
      </server>
      <server name="MetricServer2">
        <metric displayName="metric1" property="metric1" dataType="trait" displayType="summary"
                description="Metric 1" category="availability" defaultInterval="30000" defaultOn="true"
                units="milliseconds"/>
      </server>
      <server name="MetricServer3">
        <metric displayName="metric1" property="metric1" dataType="measurement" displayType="detail"
                description="Metric One" category="performance" defaultInterval="60000" defaultOn="false"
                units="milliseconds" measurementType="trendsup"/>
      </server>
      <server name="MetricServer4"/>
    </plugin>
    """

    createPlugin("metric-test-plugin", "1.0", pluginDescriptor)
  }

  @Test(groups = ['UpradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void addNewMetricDef() {
    assertResourceTypeAssociationEquals(
      'MetricServer2',
      'MeasurementMetadataManagerBeanTestPlugin',
      'metricDefinitions',
      ['metric1']
    )
  }

  @Test(groups = ['UpradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void changeTraitDefToMeasurementDef() {
    def measurementDef = loadMeasurementDef('metric1', 'MetricServer3')

    MeasurementDefinition expected = new  MeasurementDefinition('metric1', MeasurementCategory.PERFORMANCE,
        MeasurementUnits.MILLISECONDS, DataType.MEASUREMENT, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL)
    expected.rawNumericType = null
    expected.description = 'Metric One'
    expected.displayName = 'metric1'
    expected.displayOrder = 1

    AssertUtils.assertPropertiesMatch(
        'Failed to change trait definition to a measurement defintion',
        expected,
        measurementDef,
        ['id', 'resourceType']
    )

    def perMinuteDef = loadMeasurementDef('metric1', 'MetricServer3', 'metric1 per Minute')

    expected = new MeasurementDefinition(measurementDef)
    expected.displayName = 'metric1 per Minute'
    expected.displayOrder = 2
    expected.defaultInterval = 60000
    expected.defaultOn = false
    expected.numericType = NumericType.DYNAMIC
    expected.rawNumericType = measurementDef.numericType

    AssertUtils.assertPropertiesMatch(
        'Failed to create and persist per minute metric definition for updated metric definition',
        expected,
        perMinuteDef,
        ['id', 'resourceType']
    )
  }

  @Test(groups = ['UpradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deleteMetricDefThatHasBeenRemovedFromResourceType() {
    assertResourceTypeAssociationEquals(
        'MetricServer4',
        'MeasurementMetadataManagerBeanTestPlugin',
        'metricDefinitions',
        []
    )
  }

  @Test(groups = ['UpradePlugin'], dependsOnMethods = ['upgradePlugin'])
  void deleteMetricDefsForResourceTypeThatIsRemoved() {
    def metricDefs = entityManager.createQuery(
    """
    from  MeasurementDefinition m
    where m.name = :metric1Name or name = :metric2Name
    """
    ).setParameter('metric1Name', 'MetricServer5.metric1')
     .setParameter('metric2Name', 'MetricServer5.metric3')
     .getResultList()

    assertEquals('Failed to delete metric definitions', 0, metricDefs.size())
  }

  // TODO Need a test for deleting measurement defs that creates resources with measurment schedules

  MeasurementDefinition loadMeasurementDef(String name, String resourceType, displayName = null) {
    if (!displayName) {
      displayName = name
    }

    return (MeasurementDefinition) entityManager.createQuery(
    """
    from  MeasurementDefinition m
    where m.name = :name and
          m.displayName = :displayName and
          m.resourceType.name = :resourceType
    """
    ).setParameter('name', name)
     .setParameter('displayName', displayName)
     .setParameter('resourceType', resourceType)
     .getSingleResult()
  }

}
