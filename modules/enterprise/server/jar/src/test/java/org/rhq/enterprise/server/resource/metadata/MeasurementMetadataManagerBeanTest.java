package org.rhq.enterprise.server.resource.metadata;

import java.util.List;

import org.testng.annotations.Test;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.test.AssertUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;

public class MeasurementMetadataManagerBeanTest extends MetadataBeanTest {

    @Test(groups = {"plugin.metadata", "Metrics.NewPlugin"})
    void registerMetricsPlugin() throws Exception {
        createPlugin("metric-test-plugin", "1.0", "plugin_v1.xml");
    }

    @Test(groups = {"plugin.metadata", "Metrics.NewPlugin"}, dependsOnMethods = {"registerMetricsPlugin"})
    public void persistNewMetrics() throws Exception {
        assertResourceTypeAssociationEquals(
            "MetricServer1",
            "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions",
            asList("metric1", "metric2", "metric3")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.NewPlugin"}, dependsOnMethods = {"persistNewMetrics"})
    public void persistNewTraitDefinitionProperties() {
        MeasurementDefinition traitDef = loadMeasurementDef("metric1", "MetricServer1");

        MeasurementDefinition expected = new  MeasurementDefinition("metric1", MeasurementCategory.AVAILABILITY,
            MeasurementUnits.MILLISECONDS, DataType.TRAIT, NumericType.DYNAMIC, true, 30000, DisplayType.SUMMARY);
        expected.setDescription("Metric 1");
        expected.setDisplayName("metric1");
        expected.setDisplayOrder(1);

        AssertUtils.assertPropertiesMatch(
            "Failed to persist properties for a trait metric definition",
            expected,
            traitDef,
            asList("id", "resourceType")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.NewPlugin"}, dependsOnMethods = {"persistNewMetrics"})
    public void persistNewNumericMeasurementDef() {
        MeasurementDefinition measurementDef = loadMeasurementDef("metric2", "MetricServer1");

        MeasurementDefinition expected = new MeasurementDefinition("metric2", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MEGABYTES, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL);
        expected.setRawNumericType(null);
        expected.setDescription("Metric 2");
        expected.setDisplayName("metric2");
        expected.setDisplayOrder(2);

        AssertUtils.assertPropertiesMatch(
            "Failed to persist properties for numeric metric definition",
            expected,
            measurementDef,
            asList("id", "resourceType")
        );

        MeasurementDefinition perMinuteDef = loadMeasurementDef("metric2", "MetricServer1", "metric2 per Minute");

        expected = new MeasurementDefinition(measurementDef);
        expected.setDisplayName("metric2 per Minute");
        expected.setDisplayOrder(3);
        expected.setDefaultOn(true);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setRawNumericType(measurementDef.getNumericType());

        AssertUtils.assertPropertiesMatch(
            "Failed to create and persist per minute metric definition for numeric metric definition",
            expected,
            perMinuteDef,
            asList("id", "resourceType")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.NewPlugin"}, dependsOnMethods = {"persistNewMetrics"})
    public void persistNewCallTimeDef() {
        MeasurementDefinition calltimeDef = loadMeasurementDef("metric3", "MetricServer1");

        MeasurementDefinition expected = new  MeasurementDefinition("metric3", MeasurementCategory.THROUGHPUT,
            MeasurementUnits.MILLISECONDS, DataType.CALLTIME, true, 30000, DisplayType.DETAIL);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setDestinationType("myMethod");
        expected.setDescription("Metric 3");
        expected.setDisplayName("metric3");
        expected.setDisplayOrder(4);

        AssertUtils.assertPropertiesMatch(
            "Failed to create calltime metric definition",
            expected,
            calltimeDef,
            asList("id", "resourceType")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.UpgradePlugin"}, dependsOnGroups = {"Metrics.NewPlugin"})
    public void upgradeMetricsPlugin() throws Exception {
        createPlugin("metric-test-plugin", "2.0", "plugin_v2.xml");
    }

    @Test(groups = {"plugin.metadata", "Metrics.UpradePlugin"}, dependsOnMethods = {"upgradeMetricsPlugin"})
    public void addNewMetricDef() throws Exception {
        assertResourceTypeAssociationEquals(
            "MetricServer2",
            "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions",
            asList("metric1")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.UpradePlugin"}, dependsOnMethods = {"upgradeMetricsPlugin"})
    public void changeTraitDefToMeasurementDef() {
        MeasurementDefinition measurementDef = loadMeasurementDef("metric1", "MetricServer3");

        MeasurementDefinition expected = new  MeasurementDefinition("metric1", MeasurementCategory.PERFORMANCE,
            MeasurementUnits.MILLISECONDS, DataType.MEASUREMENT, NumericType.TRENDSUP, false, 30000, DisplayType.DETAIL);
        expected.setRawNumericType(null);
        expected.setDefaultInterval(30000);
        expected.setDescription("Metric One");
        expected.setDisplayName("metric1");
        expected.setDisplayOrder(1);

        AssertUtils.assertPropertiesMatch(
           "Failed to change trait definition to a measurement defintion",
            expected,
            measurementDef,
            asList("id", "resourceType")
        );

        MeasurementDefinition perMinuteDef = loadMeasurementDef("metric1", "MetricServer3", "metric1 per Minute");
        expected.setDisplayName("metric1 per Minute");
        expected.setDisplayOrder(2);
        expected.setDefaultInterval(60000);
        expected.setDefaultOn(false);
        expected.setNumericType(NumericType.DYNAMIC);
        expected.setRawNumericType(measurementDef.getNumericType());

        AssertUtils.assertPropertiesMatch(
            "Failed to create and persist per minute metric definition for updated metric definition",
            expected,
            perMinuteDef,
            asList("id", "resourceType")
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.UpradePlugin"}, dependsOnMethods = {"upgradeMetricsPlugin"})
    public void deleteMetricDefThatHasBeenRemovedFromResourceType() throws Exception {
        assertResourceTypeAssociationEquals(
            "MetricServer4",
            "MeasurementMetadataManagerBeanTestPlugin",
            "metricDefinitions",
            EMPTY_LIST
        );
    }

    @Test(groups = {"plugin.metadata", "Metrics.UpradePlugin"}, dependsOnMethods = {"upgradeMetricsPlugin"})
    public void deleteMetricDefsForResourceTypeThatIsRemoved() {
        List metricDefs = getEntityManager().createQuery(
            "from  MeasurementDefinition m where m.name = :metric1Name or name = :metric2Name")
            .setParameter("metric1Name", "MetricServer5.metric1")
            .setParameter("metric2Name", "MetricServer5.metric3")
            .getResultList();

        assertEquals("Failed to delete metric definitions", 0, metricDefs.size());
    }

    MeasurementDefinition loadMeasurementDef(String name, String resourceType) {
        return loadMeasurementDef(name, resourceType, name);
    }

    MeasurementDefinition loadMeasurementDef(String name, String resourceType, String displayName) {
        return (MeasurementDefinition) getEntityManager().createQuery(
            "from  MeasurementDefinition m " +
            "where m.name = :name and " +
            "m.displayName = :displayName and " +
            "m.resourceType.name = :resourceType")
            .setParameter("name", name)
            .setParameter("displayName", displayName)
            .setParameter("resourceType", resourceType)
            .getSingleResult();
    }

}
