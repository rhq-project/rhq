package org.rhq.enterprise.server.resource.metadata;

import static java.util.Arrays.asList;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.test.AssertUtils;

public class MeasurementMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" })
    void registerMetricsPlugin() throws Exception {
        createPlugin("metric-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" }, dependsOnMethods = { "registerMetricsPlugin" })
    public void persistNewMetrics() throws Exception {
        assertResourceTypeAssociationEquals("MetricServer1", "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions", asList("metric1", "metric2", "metric3", "rhq.availability"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" }, dependsOnMethods = { "persistNewMetrics" })
    public void persistNewTraitDefinitionProperties() {
        MeasurementDefinition traitDef = loadMeasurementDef("metric1", "MetricServer1");

        MeasurementDefinition expected = new MeasurementDefinition("metric1", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.MILLISECONDS, DataType.TRAIT, NumericType.DYNAMIC, true, 30000, DisplayType.SUMMARY);
        expected.setDescription("Metric 1");
        expected.setDisplayName("metric1");
        expected.setDisplayOrder(1);

        AssertUtils.assertPropertiesMatch("Failed to persist properties for a trait metric definition", expected,
            traitDef, asList("id", "resourceType", "schedules", "alertCondition"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" }, dependsOnMethods = { "persistNewMetrics" })
    public void persistNewNumericMeasurementDef() {
        MeasurementDefinition measurementDef = loadMeasurementDef("metric2", "MetricServer1");

        MeasurementDefinition expected = new MeasurementDefinition("metric2", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MEGABYTES, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL);
        expected.setRawNumericType(null);
        expected.setDescription("Metric 2");
        expected.setDisplayName("metric2");
        expected.setDisplayOrder(2);

        AssertUtils.assertPropertiesMatch("Failed to persist properties for numeric metric definition", expected,
            measurementDef, asList("id", "resourceType", "schedules", "alertCondition"));

        MeasurementDefinition perMinuteDef = loadMeasurementDef("metric2", "MetricServer1", "metric2 per Minute");

        expected = new MeasurementDefinition(measurementDef);
        expected.setDisplayName("metric2 per Minute");
        expected.setDisplayOrder(3);
        expected.setDefaultOn(true);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setRawNumericType(measurementDef.getNumericType());

        AssertUtils.assertPropertiesMatch(
            "Failed to create and persist per minute metric definition for numeric metric definition", expected,
            perMinuteDef, asList("id", "resourceType", "schedules", "alertCondition"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" }, dependsOnMethods = { "persistNewMetrics" })
    public void persistNewCallTimeDef() {
        MeasurementDefinition calltimeDef = loadMeasurementDef("metric3", "MetricServer1");

        MeasurementDefinition expected = new MeasurementDefinition("metric3", MeasurementCategory.THROUGHPUT,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 30000, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDestinationType("myMethod");
        expected.setDescription("Metric 3");
        expected.setDisplayName("metric3");
        expected.setDisplayOrder(4);

        AssertUtils.assertPropertiesMatch("Failed to create calltime metric definition", expected, calltimeDef,
            asList("id", "resourceType", "schedules", "alertCondition"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.NewPlugin" }, dependsOnMethods = { "persistNewMetrics" })
    public void availabilityDefaultTest() {
        MeasurementDefinition serverAvailDef = loadMeasurementDef("rhq.availability", "MetricServer1",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        MeasurementDefinition expected = new MeasurementDefinition("rhq.availability",
            MeasurementCategory.AVAILABILITY, MeasurementUnits.NONE, DataType.AVAILABILITY, true,
            MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVER, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serverAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "alertCondition", "schedules"));

        MeasurementDefinition serviceAvailDef = loadMeasurementDef("rhq.availability", "MetricService1",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        expected = new MeasurementDefinition("rhq.availability", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.NONE, DataType.AVAILABILITY, false, 120000, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serviceAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "schedules", "alertCondition"));

        serviceAvailDef = loadMeasurementDef("rhq.availability", "MetricService2",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        expected = new MeasurementDefinition("rhq.availability", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.NONE, DataType.AVAILABILITY, true,
            MeasurementDefinition.AVAILABILITY_DEFAULT_PERIOD_SERVICE, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serviceAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "schedules", "alertCondition"));

    }

    @Test(groups = { "plugin.metadata", "Metrics.UpgradePlugin" }, dependsOnGroups = { "Metrics.NewPlugin" })
    public void upgradeMetricsPlugin() throws Exception {
        createPlugin("metric-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = { "plugin.metadata", "Metrics.UpradePlugin" }, dependsOnMethods = { "upgradeMetricsPlugin" })
    public void addNewMetricDef() throws Exception {
        assertResourceTypeAssociationEquals("MetricServer2", "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions", asList("metric1", "rhq.availability"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.UpradePlugin" }, dependsOnMethods = { "upgradeMetricsPlugin" })
    public void changeTraitDefToMeasurementDef() {
        MeasurementDefinition measurementDef = loadMeasurementDef("metric1", "MetricServer3");

        MeasurementDefinition expected = new MeasurementDefinition("metric1", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.MEASUREMENT, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL);
        expected.setRawNumericType(null);
        expected.setDefaultInterval(30000);
        expected.setDescription("Metric One");
        expected.setDisplayName("metric1");
        expected.setDisplayOrder(1);

        AssertUtils.assertPropertiesMatch("Failed to change trait definition to a measurement defintion", expected,
            measurementDef, asList("id", "resourceType", "schedules", "alertCondition"));

        MeasurementDefinition perMinuteDef = loadMeasurementDef("metric1", "MetricServer3", "metric1 per Minute");
        expected.setDisplayName("metric1 per Minute");
        expected.setDisplayOrder(2);
        expected.setDefaultInterval(60000);
        expected.setDefaultOn(false);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setRawNumericType(measurementDef.getNumericType());

        AssertUtils.assertPropertiesMatch(
            "Failed to create and persist per minute metric definition for updated metric definition", expected,
            perMinuteDef, asList("id", "resourceType", "schedules", "alertCondition"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.UpradePlugin" }, dependsOnMethods = { "upgradeMetricsPlugin" })
    public void deleteMetricDefThatHasBeenRemovedFromResourceType() throws Exception {
        assertResourceTypeAssociationEquals("MetricServer4", "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions", asList("rhq.availability"));
    }

    @Test(groups = { "plugin.metadata", "Metrics.UpradePlugin" }, dependsOnMethods = { "upgradeMetricsPlugin" })
    public void deleteMetricDefsForResourceTypeThatIsRemoved() {
        List metricDefs = getEntityManager()
            .createQuery("from  MeasurementDefinition m where m.name = :metric1Name or name = :metric2Name")
            .setParameter("metric1Name", "MetricServer5.metric1").setParameter("metric2Name", "MetricServer5.metric3")
            .getResultList();

        assertEquals("Failed to delete metric definitions", 0, metricDefs.size());
    }

    @Test(groups = { "plugin.metadata", "Metrics.UpradePlugin" }, dependsOnMethods = { "upgradeMetricsPlugin" })
    public void availabilityOverrideTest() {
        MeasurementDefinition serverAvailDef = loadMeasurementDef("rhq.availability", "MetricServer1",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        MeasurementDefinition expected = new MeasurementDefinition("rhq.availability",
            MeasurementCategory.AVAILABILITY, MeasurementUnits.NONE, DataType.AVAILABILITY, true, 120000,
            DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serverAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "schedules", "alertCondition"));

        MeasurementDefinition serviceAvailDef = loadMeasurementDef("rhq.availability", "MetricService1",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        // interval can not be changed by new plugin version if not at the category default
        expected = new MeasurementDefinition("rhq.availability", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.NONE, DataType.AVAILABILITY, true, 120000, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serviceAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "schedules", "alertCondition"));

        serviceAvailDef = loadMeasurementDef("rhq.availability", "MetricService2",
            MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);

        expected = new MeasurementDefinition("rhq.availability", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.NONE, DataType.AVAILABILITY, true, 480000, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDisplayName(MeasurementDefinition.AVAILABILITY_DISPLAY_NAME);
        expected.setDescription(MeasurementDefinition.AVAILABILITY_DESCRIPTION);

        AssertUtils.assertPropertiesMatch("Failed to create avail metric definition", expected, serviceAvailDef,
            asList("id", "resourceType", "destinationType", "displayOrder", "schedules", "alertCondition"));
    }

    // this needs to be the last test executed in the class, it does cleanup
    @Test(priority = 10, alwaysRun = true, dependsOnGroups = { "Metrics.UpradePlugin" })
    public void afterClassWorkTest() throws Exception {
        afterClassWork();
    }

    MeasurementDefinition loadMeasurementDef(String name, String resourceType) {
        return loadMeasurementDef(name, resourceType, name);
    }

    MeasurementDefinition loadMeasurementDef(String name, String resourceType, String displayName) {
        return (MeasurementDefinition) getEntityManager()
            .createQuery(
                "from  MeasurementDefinition m " + "where m.name = :name and " + "m.displayName = :displayName and "
                    + "m.resourceType.name = :resourceType").setParameter("name", name)
            .setParameter("displayName", displayName).setParameter("resourceType", resourceType).getSingleResult();
    }

}
