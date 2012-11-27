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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Hours;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TransactionCallback;
import org.rhq.enterprise.server.util.LookupUtil;

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

    @Override
    protected void beforeMethod() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        overlord = subjectManager.getOverlord();

        createInventory();
        insertDummyReport();
    }

    @Override
    protected void afterMethod() {
        purgeDB();
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

        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        dataManager.mergeMeasurementReport(report);

        List<MeasurementDataNumericHighLowComposite> actualData = findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicSchedule, beginTime.getMillis(),
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

    @Test(enabled = ENABLED)
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

        insert1HourData(data);

        List<MeasurementDataNumericHighLowComposite> actualData = findDataForContext(overlord,
            EntityContext.forResource(resource.getId()), dynamicSchedule, beginTime.getMillis(), endTime.getMillis());

        assertEquals("Expected to get back 60 data points.", buckets.getNumDataPoints(), actualData.size());

        MeasurementDataNumericHighLowComposite expectedBucket0Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(0), (2.0 + 5.0 + 3.0) / 3, 6.0, 1.0);
        MeasurementDataNumericHighLowComposite expectedBucket59Data = new MeasurementDataNumericHighLowComposite(
            buckets.get(59), (5.0 + 5.0 + 3.0) / 3, 9.0, 2.0);

        assertPropertiesMatch("The data for bucket 0 does not match the expected values.", expectedBucket0Data,
            actualData.get(0));
        assertPropertiesMatch("The data for bucket 59 does not match the expected values.", expectedBucket59Data,
            actualData.get(59));
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
        purgeRawData();
        purge1HourData();
        purge6HourData();
        purge24HourData();

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {

                // Note that the order of deletes is important due to FK
                // constraints.
                deleteMeasurementSchedules();
                deleteResource();
                deleteAgent();
                deleteDynamicMeasurementDef();
                deleteResourceType();
            }
        });
    }

    private void deleteDynamicMeasurementDef() {
        em.createQuery("delete from MeasurementDefinition " +
            "where dataType = :dataType and " +
            "name = :name")
            .setParameter("dataType", DYNAMIC)
            .setParameter("name", DYNAMIC_DEF_NAME)
            .executeUpdate();
    }

    private void deleteAgent() {
        em.createQuery("delete from Agent where name = :name")
            .setParameter("name", AGENT_NAME)
            .executeUpdate();
    }

    private void deleteResourceType() {
        em.createQuery("delete from ResourceType where name = :name and plugin = :plugin")
            .setParameter("name", RESOURCE_TYPE)
            .setParameter("plugin", PLUGIN)
            .executeUpdate();
    }

    private void deleteResource() {
        em.createQuery("delete from Availability").executeUpdate();

        em.createQuery("delete from Resource where resourceKey = :key and uuid = :uuid")
            .setParameter("key", RESOURCE_KEY)
            .setParameter("uuid", RESOURCE_UUID)
            .executeUpdate();
    }

    private void deleteMeasurementSchedules() {
        em.createQuery("delete from MeasurementSchedule").executeUpdate();
    }

    private void insertDummyReport() {
        // we insert the dummy report due to https://bugzilla.redhat.com/show_bug.cgi?id=822240
        DateTime now = new DateTime();
        MeasurementReport dummyReport = new MeasurementReport();
        dummyReport.addData(new MeasurementDataNumeric(now.getMillis(), -1, 0.0));

        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        dataManager.mergeMeasurementReport(dummyReport);
    }

    public void purgeRawData() {
        purgeTables(MeasurementDataManagerUtility.getAllRawTables());
    }

    public void purge1HourData() {
        purgeTables("rhq_measurement_data_num_1h");
    }

    public void purge6HourData() {
        purgeTables("rhq_measurement_data_num_6h");
    }

    public void purge24HourData() {
        purgeTables("rhq_measurement_data_num_1d");
    }

    private void purgeTables(String... tables) {
        // This method was previous implemented using EntityManager.createNativeQuery
        // and called from within a TransactionCallback. It was causing a
        // TransactionRequiredException, and I am not clear why. I suspect it is a
        // configuration issue in our testing environment, but I haven't figured it out
        // yet. For now,  raw tables are purges in their own separate JDBC transaction.
        //
        // jsanda
        Connection connection = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            for (String table : tables) {
                Statement statement = connection.createStatement();
                try {
                    statement.execute("delete from " + table);
                } finally {
                    JDBCUtil.safeClose(statement);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new RuntimeException("Failed to rollback transaction", e1);
            }
            throw new RuntimeException("Failed to purge data from " + tables, e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    private void insert1HourData(List<AggregateTestData> data) {
        Connection connection = null;

        try {
            connection = getConnection();
            connection.setAutoCommit(false);
            String sql = "insert into rhq_measurement_data_num_1h(time_stamp, schedule_id, value, minvalue, maxvalue) values(?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(sql);

            for (AggregateTestData datum : data) {
                statement.setLong(1, datum.getTimestamp());
                statement.setInt(2, datum.getScheduleId());
                statement.setDouble(3, datum.getAvg());
                statement.setDouble(4, datum.getMin());
                statement.setDouble(5, datum.getMax());

                statement.addBatch();
            }

            statement.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                throw new RuntimeException("Failed to rollback transaction", e1);
            }
            throw new RuntimeException("Failed to insert 1 hour data", e);
        } finally {
            JDBCUtil.safeClose(connection);
        }
    }

    private List<MeasurementDataNumericHighLowComposite> findDataForContext(Subject subject, EntityContext context,
        MeasurementSchedule schedule, long beginTime, long endTime) {
        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        List<List<MeasurementDataNumericHighLowComposite>> data = dataManager.findDataForContext(subject, context,
            schedule.getDefinition().getId(), beginTime, endTime, 60);

        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        return data.get(0);
    }

}
