/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.enterprise.server.measurement;

import static java.util.Arrays.asList;
import static org.rhq.core.domain.measurement.DataType.MEASUREMENT;
import static org.rhq.core.domain.measurement.NumericType.DYNAMIC;
import static org.rhq.core.domain.resource.ResourceCategory.SERVER;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;

import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceCriteria;
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
import org.rhq.enterprise.server.cassandra.SessionManagerBean;
import org.rhq.enterprise.server.drift.DriftServerPluginService;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.ResourceTreeHelper;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateType;
import org.rhq.server.metrics.domain.MetricsTable;
import org.rhq.test.AssertUtils;

/**
 * @author John Sanda
 */
public class MeasurementDataManagerBeanTest extends AbstractEJB3Test {

    //private final Log log = LogFactory.getLog(MeasurementDataManagerBeanTest.class);

    private static final boolean ENABLED = true;

    //private final long SECOND = 1000;

    //private final long MINUTE = 60 * SECOND;

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

    private Subject overlord;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private MeasurementDataManagerLocal dataManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private SessionManagerBean cassandraSessionManager;

    private MetricsDAO metricsDAO;

    @Override
    protected void beforeMethod() throws Exception {
        overlord = subjectManager.getOverlord();

        metricsDAO = cassandraSessionManager.getMetricsDAO();

        // MeasurementDataManagerUtility looks up config settings from SystemManagerBean.
        // SystemManagerBean.getDriftServerPluginManager method requires drift server plugin.
        DriftServerPluginService driftServerPluginService = new DriftServerPluginService(getTempDir());
        prepareCustomServerPluginService(driftServerPluginService);
        driftServerPluginService.masterConfig.getPluginDirectory().mkdirs();

        createInventory();
        insertDummyReport();
    }

    @Override
    protected void afterMethod() throws Exception {
        purgeDB();

        unprepareServerPluginService();
    }

    @Test(enabled = ENABLED)
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

        dataManager.mergeMeasurementReport(report);

        List<MeasurementDataNumericHighLowComposite> actualData = findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicSchedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals("Expected to get back 60 data points.", buckets.getNumDataPoints(), actualData.size());

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0), divide((1.1 + 2.2 + 3.3), 3), 3.3, 1.1);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59), divide((4.4 + 5.5 + 6.6), 3), 6.6, 4.4);
        MeasurementDataNumericHighLowComposite expectedBucket29Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(29), Double.NaN, Double.NaN, Double.NaN);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0), 0.0001D);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59), 0.0001D);
        assertPropertiesMatch("The data for bucket 29 does not match the expected values.", expectedBucket29Data,
            actualData.get(29));
    }

    static double divide(double dividend, int divisor) {
        return new BigDecimal(Double.toString(dividend)).divide(new BigDecimal(Integer.toString(divisor)),
            MathContext.DECIMAL64).doubleValue();
    }

    @Test(enabled = true)
    public void getRawAggregate() {
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

        dataManager.mergeMeasurementReport(report);

        MeasurementAggregate actual = dataManager.getAggregate(overlord, dynamicSchedule.getId(),
            beginTime.getMillis(), endTime.getMillis());

        MeasurementAggregate expected = new MeasurementAggregate(1.1, divide((1.1 + 2.2 + 3.3 + 4.4 + 5.5 + 6.6), 6),
            6.6);

        AssertUtils.assertPropertiesMatch("Aggregate does not match", expected, actual, 0.0001D);
    }

    @Test(enabled = ENABLED)
    public void find1HourNumericData() throws Exception {
        DateTime now = new DateTime();
        DateTime beginTime = now.minusDays(11);
        DateTime endTime = now;

        // results in an interval or bucket size of 4.4 hours
        Buckets buckets = new Buckets(beginTime, endTime);

        List<AggregateTestData> data = asList(new AggregateTestData(buckets.get(0), dynamicSchedule.getId(), 2.0, 3.0,
            1.0),
            new AggregateTestData(buckets.get(0) + Hours.ONE.toStandardDuration().getMillis(), dynamicSchedule.getId(),
                5.0, 6.0, 4.0), new AggregateTestData(buckets.get(0) + Hours.TWO.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 3.0, 3.0, 3.0),

            new AggregateTestData(buckets.get(59), dynamicSchedule.getId(), 5.0, 9.0, 2.0), new AggregateTestData(
                buckets.get(59) + Hours.ONE.toStandardDuration().getMillis(), dynamicSchedule.getId(), 5.0, 6.0, 4.0),
            new AggregateTestData(buckets.get(59) + Hours.TWO.toStandardDuration().getMillis(),
                dynamicSchedule.getId(), 3.0, 3.0, 3.0));

        insert1HourData(data);

        List<MeasurementDataNumericHighLowComposite> actualData = findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicSchedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals("Expected to get back 60 data points.", buckets.getNumDataPoints(), actualData.size());

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0), divide((2.0 + 5.0 + 3.0), 3), 6.0, 1.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59), divide((5.0 + 5.0 + 3.0), 3), 9.0, 2.0);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0), 0.0001D);
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59), 0.0001D);
    }

    private void createInventory() throws Exception {
        purgeDB();
        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                resourceType = new ResourceType(RESOURCE_TYPE, PLUGIN, SERVER, null);
                em.persist(resourceType);

                agent = new Agent(AGENT_NAME, "localhost", 9999, "", "randomToken");
                em.persist(agent);

                dynamicMeasuremenDef = new MeasurementDefinition(resourceType, DYNAMIC_DEF_NAME);
                dynamicMeasuremenDef.setDefaultOn(true);
                dynamicMeasuremenDef.setDataType(MEASUREMENT);
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

    private void purgeDB() {
        purgeMetricsTables();

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                ResourceCriteria c = new ResourceCriteria();
                c.addFilterInventoryStatus(null);
                c.addFilterResourceKey(RESOURCE_KEY);
                c.fetchSchedules(true);
                List<Resource> r = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), c);

                // Note that the order of deletes is important due to FK
                // constraints.
                if (!r.isEmpty()) {
                    assertTrue("Should be only 1 resource", r.size() == 1);
                    Resource doomedResource = r.get(0);
                    deleteMeasurementSchedules(doomedResource);
                    deleteResource(doomedResource);
                }
                deleteAgent();
                deleteDynamicMeasurementDef();
                deleteResourceType();
            }
        });
    }

    private void deleteDynamicMeasurementDef() {
        em.createQuery("delete from MeasurementDefinition " + "where dataType = :dataType and " + "name = :name")
            .setParameter("dataType", MEASUREMENT).setParameter("name", DYNAMIC_DEF_NAME).executeUpdate();
    }

    private void deleteAgent() {
        em.createQuery("delete from Agent where name = :name").setParameter("name", AGENT_NAME).executeUpdate();
    }

    private void deleteResourceType() {
        em.createQuery("delete from ResourceType where name = :name and plugin = :plugin")
            .setParameter("name", RESOURCE_TYPE).setParameter("plugin", PLUGIN).executeUpdate();
    }

    private void deleteResource(Resource doomedResource) {
        ResourceTreeHelper.deleteResource(em, doomedResource);
        em.flush();
    }

    private void deleteMeasurementSchedules(Resource doomedResource) {
        for (MeasurementSchedule ms : doomedResource.getSchedules()) {
            int i = em.createQuery("delete from MeasurementSchedule where id = :msId").setParameter("msId", ms.getId())
                .executeUpdate();
            em.flush();
            System.out.println("Deleted [" + i + "] schedules with id [" + ms.getId() + "]");
        }
    }

    private void insertDummyReport() {
        // we insert the dummy report due to https://bugzilla.redhat.com/show_bug.cgi?id=822240
        DateTime now = new DateTime();
        MeasurementReport dummyReport = new MeasurementReport();
        dummyReport.addData(new MeasurementDataNumeric(now.getMillis(), -1, 0.0));

        dataManager.mergeMeasurementReport(dummyReport);
    }

    private void purgeMetricsTables() {
        try {
            Session session = cassandraSessionManager.getSession();

            session.execute("TRUNCATE " + MetricsTable.RAW);
            session.execute("TRUNCATE " + MetricsTable.ONE_HOUR);
            session.execute("TRUNCATE " + MetricsTable.SIX_HOUR);
            session.execute("TRUNCATE " + MetricsTable.TWENTY_FOUR_HOUR);
            session.execute("TRUNCATE " + MetricsTable.INDEX);
        } catch (NoHostAvailableException e) {
            throw new RuntimeException("An error occurred while purging metrics tables", e);
        }
    }

    private void insert1HourData(List<AggregateTestData> data) {
        List<AggregateNumericMetric> metrics = new ArrayList<AggregateNumericMetric>(data.size());
        for (AggregateTestData datum : data) {
            metricsDAO.insertOneHourData(datum.getScheduleId(), datum.getTimestamp(), AggregateType.MIN,
                datum.getMin());
            metricsDAO.insertOneHourData(datum.getScheduleId(), datum.getTimestamp(), AggregateType.AVG,
                datum.getAvg());
            metricsDAO.insertOneHourData(datum.getScheduleId(), datum.getTimestamp(), AggregateType.MAX,
                datum.getMax());
        }
    }

    private List<MeasurementDataNumericHighLowComposite> findDataForContext(Subject subject, EntityContext context,
        MeasurementSchedule schedule, long beginTime, long endTime) {
        List<List<MeasurementDataNumericHighLowComposite>> data = dataManager.findDataForContext(subject, context,
            schedule.getDefinition().getId(), beginTime, endTime, 60);

        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        return data.get(0);
    }

}
