/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.plugins.metrics;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.measurement.NumericType.DYNAMIC;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.DateTimeService;
import org.rhq.enterprise.server.measurement.MetricsManagerLocal;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.metrics.AggregateTestData;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.metrics.MetricsServerPluginTestDelegate;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;
import org.rhq.test.TransactionCallback;

/**
 * @author John Sanda
 */
public class MetricsServerPluginTest extends AbstractEJB3Test {

    private final long SECOND = 1000;

    private final long MINUTE = 60 * SECOND;

    private final String RESOURCE_TYPE = getClass().getName() + "_TYPE";

    private final String PLUGIN = getClass().getName() + "_PLUGIN";

    private final String AGENT_NAME = getClass().getName() + "_AGENT";

    private final String DYNAMIC_DEF_NAME = getClass().getName() + "_DYNAMIC";

    private final String RESOURCE_KEY = getClass().getName() + "_RESOURCE_KEY";

    private final String RESOURCE_NAME = getClass().getName() + "_NAME";

    private final String RESOURCE_UUID = getClass().getSimpleName() + "_UUID";

    private ResourceType resourceType;

    private Agent agent;

    private MeasurementDefinition dynamicMeasuremenDef;

    private Resource resource;

    private MeasurementSchedule dynamicSchedule;

    private MetricsServerPluginService metricsServerPluginService;

    private Subject overlord;

    private MetricsManagerLocal metricsManager;

    private MetricsServerPluginTestDelegate testDelegate;

    @BeforeClass
    public void prepareMetricsServer() throws Exception {
        initMetricsServer();
        testDelegate = getTestDelegate();
    }

    @BeforeMethod
    public void prepareTests() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        overlord = subjectManager.getOverlord();

        metricsManager = LookupUtil.getMetricsManager();

        createInventory();
        insertDummyReport();
    }

    public void createInventory() throws Exception {
        testDelegate.purgeRawData();
        testDelegate.purge1HourData();
        testDelegate.purge6HourData();
        testDelegate.purge24HourData();

        executeInTransaction(new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                EntityManager em = getEntityManager();

                // Note that the order of deletes is important due to FK
                // constraints.
                deleteMeasurementSchedules(em);
                deleteResource(em);
                deleteAgent(em);
                deleteDynamicMeasurementDef(em);
                deleteResourceType(em);

                resourceType = new ResourceType(RESOURCE_TYPE, PLUGIN, SERVER, null);
                em.persist(resourceType);

                agent = new Agent(AGENT_NAME, "localhost", 9999, "", "randomToken");
                em.persist(agent);

                dynamicMeasuremenDef = new MeasurementDefinition(resourceType, DYNAMIC_DEF_NAME);
                dynamicMeasuremenDef.setDefaultOn(true);
                dynamicMeasuremenDef.setDataType(DataType.MEASUREMENT);
                dynamicMeasuremenDef.setMeasurementType(DYNAMIC);

                em.persist(dynamicMeasuremenDef);

                resource = new Resource(RESOURCE_KEY, RESOURCE_NAME, resourceType);
                resource.setUuid(RESOURCE_UUID);
                resource.setAgent(agent);

                em.persist(resource);

                dynamicSchedule = new MeasurementSchedule(dynamicMeasuremenDef, resource);
                dynamicSchedule.setEnabled(true);
                resource.addSchedule(dynamicSchedule);

                em.persist(dynamicSchedule);
            }
        });
    }

    private MetricsServerPluginTestDelegate getTestDelegate() {
        MasterServerPluginContainer masterPC = LookupUtil.getServerPluginService().getMasterPluginContainer();
        if (masterPC == null) {
            throw new IllegalStateException(MasterServerPluginContainer.class.getSimpleName() + " is not started yet");
        }

        MetricsServerPluginContainer pc = masterPC.getPluginContainerByClass(MetricsServerPluginContainer.class);
        if (pc == null) {
            throw new IllegalStateException(MetricsServerPluginContainer.class + " has not been loaded by the " +
                masterPC.getClass() + " yet.");
        }

        MetricsServerPluginManager pluginMgr = (MetricsServerPluginManager) pc.getPluginManager();

        return pluginMgr.getTestDelegate("metrics-rhq");
    }

    private void deleteDynamicMeasurementDef(EntityManager em) {
        em.createQuery("delete from MeasurementDefinition " +
            "where dataType = :dataType and " +
            "name = :name")
            .setParameter("dataType", DYNAMIC)
            .setParameter("name", DYNAMIC_DEF_NAME)
            .executeUpdate();
    }

    private void deleteAgent(EntityManager em) {
        em.createQuery("delete from Agent where name = :name")
            .setParameter("name", AGENT_NAME)
            .executeUpdate();
    }

    private void deleteResourceType(EntityManager em) {
        em.createQuery("delete from ResourceType where name = :name and plugin = :plugin")
            .setParameter("name", RESOURCE_TYPE)
            .setParameter("plugin", PLUGIN)
            .executeUpdate();
    }

    private void deleteResource(EntityManager em) {
        em.createQuery("delete from Availability").executeUpdate();

        em.createQuery("delete from Resource where resourceKey = :key and uuid = :uuid")
            .setParameter("key", RESOURCE_KEY)
            .setParameter("uuid", RESOURCE_UUID)
            .executeUpdate();
    }

    private void deleteMeasurementSchedules(EntityManager em) {
        em.createQuery("delete from MeasurementSchedule").executeUpdate();
    }

    private void initMetricsServer() throws IOException {
        metricsServerPluginService = new MetricsServerPluginService();
        prepareCustomServerPluginService(metricsServerPluginService);
        metricsServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        String projectVersion = System.getProperty("projectVersion");

        File rhqMetricsPlugin = new File("../plugins/metrics-rhq/target/metrics-rhq-serverplugin-" + projectVersion +
            ".jar");
        assertTrue("RHQ Metrics plugins not found at " + rhqMetricsPlugin.getPath(), rhqMetricsPlugin.exists());
        FileUtils.copyFileToDirectory(rhqMetricsPlugin, metricsServerPluginService.masterConfig.getPluginDirectory());
        metricsServerPluginService.startMasterPluginContainer();
    }

    @Test
    public void insertNumericData() throws Exception {
        DateTime now = new DateTime();
        DateTime oneMinuteAgo = now.minusMinutes(1);
        DateTime twoMinutesAgo = now.minusMinutes(2);
        DateTime threeMinutesAgo = now.minusMinutes(3);

        MeasurementScheduleRequest request = new MeasurementScheduleRequest(dynamicSchedule);

        final MeasurementReport report = new MeasurementReport();
        report.addData(new MeasurementDataNumeric(threeMinutesAgo.getMillis(), request, 3.2));
        report.addData(new MeasurementDataNumeric(twoMinutesAgo.getMillis(), request, 3.9));
        report.addData(new MeasurementDataNumeric(oneMinuteAgo.getMillis(), request, 2.6));
        report.setCollectionTime(now.getMillis());

        metricsManager.mergeMeasurementReport(report);

        List<MeasurementDataNumeric> actual = metricsManager.findRawData(overlord, dynamicSchedule.getId(),
            threeMinutesAgo.minusSeconds(1).getMillis(), now.getMillis());

        List<MeasurementDataNumeric> expected = asList(
            new MeasurementDataNumeric(threeMinutesAgo.getMillis(), dynamicSchedule.getId(), 3.2),
            new MeasurementDataNumeric(twoMinutesAgo.getMillis(), dynamicSchedule.getId(), 3.9),
            new MeasurementDataNumeric(oneMinuteAgo.getMillis(), dynamicSchedule.getId(), 2.6));

        assertCollectionEqualsNoOrder(expected, actual, "Failed to insert numeric data");
    }

    @Test
    public void findRawNumericData() {
        DateTime now = new DateTime();
        DateTime beginTime = now.minusHours(4);
        DateTime endTime = now;

        Buckets buckets = new Buckets(beginTime, endTime);

        MeasurementScheduleRequest request = new MeasurementScheduleRequest(dynamicSchedule);
        MeasurementReport report = new MeasurementReport();
        report.addData(new MeasurementDataNumeric(buckets.get(0) + 10, request, 1.1));
        report.addData(new MeasurementDataNumeric(buckets.get(0) + 20, request, 2.2));
        report.addData(new MeasurementDataNumeric(buckets.get(0) + 30, request, 3.3));
        report.addData(new MeasurementDataNumeric(buckets.get(59) + 10, request, 4.4));
        report.addData(new MeasurementDataNumeric(buckets.get(59) + 20, request, 5.5));
        report.addData(new MeasurementDataNumeric(buckets.get(59) + 30, request, 6.6));

        metricsManager.mergeMeasurementReport(report);

        List<MeasurementDataNumericHighLowComposite> actualData = metricsManager.findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicMeasuremenDef.getId(), beginTime.getMillis(),
            endTime.getMillis());

        assertEquals("Expected to get back 60 data points.", buckets.getNumDataPoints(), actualData.size());

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0), (1.1 + 2.2 + 3.3) / 3, 3.3, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59), (4.4 + 5.5 + 6.6) / 3, 6.6, 4.4);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    @Test
    public void find1HourNumericData() throws Exception {
        DateTime now = new DateTime();
        DateTime beginTime = now.minusDays(11);
        DateTime endTime = now;

        // results in an interval or bucket size of 4.4 hours
        Buckets buckets = new Buckets(beginTime, endTime);

        List<AggregateTestData> data = asList(
            new AggregateTestData(buckets.get(0), dynamicSchedule.getId(), 2.0, 3.0, 1.0),
            new AggregateTestData(buckets.get(0) + Hours.ONE.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 5.0, 6.0, 4.0),
            new AggregateTestData(buckets.get(0) + Hours.TWO.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 3.0, 3.0, 3.0),

            new AggregateTestData(buckets.get(59), dynamicSchedule.getId(), 5.0, 9.0, 2.0),
            new AggregateTestData(buckets.get(59) + Hours.ONE.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 5.0, 6.0, 4.0),
            new AggregateTestData(buckets.get(59) + Hours.TWO.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 3.0, 3.0, 3.0)
        );

        testDelegate.insert1HourData(data);

        List<MeasurementDataNumericHighLowComposite> actualData = metricsManager.findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicMeasuremenDef.getId(), beginTime.getMillis(),
            endTime.getMillis());

        assertEquals("Expected to get back 60 data points.", buckets.getNumDataPoints(), actualData.size());

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0), (2.0 + 5.0 + 3.0) / 3, 5.0, 2.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59), (5.0 + 5.0 + 3.0) / 3, 5.0, 3.0);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
    }

    @Test
    public void aggregateRawDataDuring9thHour() {
        DateTime now = new DateTime();
        DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime hour9 = hour0.plusHours(9);
        DateTime hour8 = hour9.minusHours(1);

        DateTime firstMetricTime = hour8.plusMinutes(5);
        DateTime secondMetricTime = hour8.plusMinutes(10);
        DateTime thirdMetricTime = hour8.plusMinutes(15);

        MeasurementScheduleRequest request = new MeasurementScheduleRequest(dynamicSchedule);

        final MeasurementReport report = new MeasurementReport();
        report.addData(new MeasurementDataNumeric(firstMetricTime.getMillis(), request, 3.2));
        report.addData(new MeasurementDataNumeric(secondMetricTime.getMillis(), request, 3.9));
        report.addData(new MeasurementDataNumeric(thirdMetricTime.getMillis(), request, 2.6));

        report.setCollectionTime(now.getMillis());

        metricsManager.mergeMeasurementReport(report);
        metricsManager.compressPurgeAndTruncate();

        List<AggregateTestData> data = testDelegate.find1HourData(overlord, dynamicSchedule.getId(),
            hour8.getMillis(), hour9.getMillis());

        List<AggregateTestData> expected = asList(new AggregateTestData(hour8.getMillis(), dynamicSchedule.getId(),
            (3.2 + 3.9 + 2.6) / 3, 3.9, 2.6));

        assertAggregateDataEquals(data, expected, "The values for 1 hour aggregate data are wrong.");
    }

    @Test
    public void aggregate1HourDataDuring12thHourWhenThereIsNo6HourData() {
        DateTime now = new DateTime();
        DateTime hour0 = now.hourOfDay().roundFloorCopy().minusHours(now.hourOfDay().get());
        DateTime hour12 = hour0.plusHours(12);
        DateTime hour6 = hour0.plusHours(6);
        DateTime hour11 = hour0.plusHours(11);
        DateTime hour8 = hour0.plusHours(8);

        int scheduleId = dynamicSchedule.getId();

        TestDateTimeService dateTimeService = new TestDateTimeService();
        dateTimeService.setCurrentHour(hour12);
        DateTimeService.setInstance(dateTimeService);

        double min1 = 1.1;
        double avg1 = 2.2;
        double max1 = 3.3;

        List<AggregateTestData> oneHourData = asList(
            new AggregateTestData(hour11.getMillis(), scheduleId, avg1, max1, min1)
        );
        testDelegate.insert1HourData(oneHourData);

        metricsManager.compressPurgeAndTruncate();

        List<AggregateTestData> data = testDelegate.find6HourData(overlord, scheduleId, hour6.getMillis(),
            hour12.getMillis());

        List<AggregateTestData> expected = asList(new AggregateTestData(hour8.getMillis(), scheduleId, avg1, max1,
            min1));

        assertAggregateDataEquals(data, expected, "The values for 6 hour aggregate data are wrong");
    }

    private void assertAggregateDataEquals(List<AggregateTestData> actual, List<AggregateTestData> expected,
        String msg) {
        assertEquals(msg + " - The number of aggregate data is wrong.", expected.size(), actual.size());
        int i = 0;
        for (AggregateTestData expectedData : expected) {
            AggregateTestData actualData = actual.get(i++);
            AssertUtils.assertPropertiesMatch(expectedData, actualData,
                msg + " - aggregate data does not match expected values.");
        }
    }

    private void insertDummyReport() {
        // we insert the dummy report due to https://bugzilla.redhat.com/show_bug.cgi?id=822240
        DateTime now = new DateTime();
        MeasurementReport dummyReport = new MeasurementReport();
        dummyReport.addData(new MeasurementDataNumeric(now.getMillis(), -1, 0.0));

        MetricsManagerLocal metricsManager = LookupUtil.getMetricsManager();
        metricsManager.mergeMeasurementReport(dummyReport);
    }

    private static class TestDateTimeService extends DateTimeService {
        private DateTime currentHour;

        @Override
        public long getCurrentHour() {
            return currentHour.getMillis();
        }

        public void setCurrentHour(DateTime currentHour) {
            this.currentHour = currentHour;
        }
    }

}
