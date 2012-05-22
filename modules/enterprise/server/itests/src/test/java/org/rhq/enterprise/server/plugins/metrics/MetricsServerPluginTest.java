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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.jboss.ejb.plugins.cmp.jdbc.JDBCUtil;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementAggregate;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.measurement.MetricsManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
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

    @BeforeClass
    public void prepareTests() throws Exception {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        overlord = subjectManager.getOverlord();

        initMetricsServer();
        createInventory();
    }

    public void createInventory() throws Exception {
        purgeRawTables();
        purge1HourTable();

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

    private void purgeRawTables() throws SQLException {
        purgeTables(MeasurementDataManagerUtility.getAllRawTables());
    }

    private void purge1HourTable() throws SQLException {
        purgeTables("rhq_measurement_data_num_1h");
    }

    private void purgeTables(String... tables) throws SQLException {
        // This method was previous implemented using EntityManager.createNativeQuery
        // and called from within a TransactionCallback. It was causing a
        // TransactionRequiredException, and I am not clear why. I suspect it is a
        // configuration issue in our testing environment, but I haven't figured it out
        // yet. For now,  raw tables are purges in their own separate JDBC transaction.
        //
        // jsanda
        Connection connection = getConnection();

        try {
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
            connection.rollback();
            throw e;
        } finally {
            JDBCUtil.safeClose(connection);
        }
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

        MeasurementReport dummyReport = new MeasurementReport();
        dummyReport.addData(new MeasurementDataNumeric(now.getMillis(), -1, 0.0));

        MetricsManagerLocal metricsManager = LookupUtil.getMetricsManager();
        // we insert the dummy report due to https://bugzilla.redhat.com/show_bug.cgi?id=822240
        metricsManager.mergeMeasurementReport(dummyReport);
        metricsManager.mergeMeasurementReport(report);

        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();
        List<MeasurementDataNumeric> actual = dataManager.findRawData(overlord, dynamicSchedule.getId(),
            threeMinutesAgo.minusSeconds(1).getMillis(), now.getMillis());

        List<MeasurementDataNumeric> expected = asList(
            new MeasurementDataNumeric(threeMinutesAgo.getMillis(), dynamicSchedule.getId(), 3.2),
            new MeasurementDataNumeric(twoMinutesAgo.getMillis(), dynamicSchedule.getId(), 3.9),
            new MeasurementDataNumeric(oneMinuteAgo.getMillis(), dynamicSchedule.getId(), 2.6));

        AssertUtils.assertCollectionEqualsNoOrder(expected, actual, "Failed to insert numeric data");
    }

    @Test
    public void calculateAggregates() {
        DateTime now = new DateTime().hourOfDay().roundFloorCopy();
        DateTime oneHourAgo = now.minusHours(1);

        MeasurementScheduleRequest request = new MeasurementScheduleRequest(dynamicSchedule);

        final MeasurementReport report = new MeasurementReport();
        report.addData(new MeasurementDataNumeric(oneHourAgo.minusMinutes(12).getMillis(), request, 3.2));
        report.addData(new MeasurementDataNumeric(oneHourAgo.minusMinutes(10).getMillis(), request, 3.9));
        report.addData(new MeasurementDataNumeric(oneHourAgo.minusMinutes(6).getMillis(), request, 2.6));

        report.setCollectionTime(now.getMillis());

        MeasurementReport dummyReport = new MeasurementReport();
        dummyReport.addData(new MeasurementDataNumeric(now.getMillis(), -1, 0.0));

        MetricsManagerLocal metricsManager = LookupUtil.getMetricsManager();
        // we insert the dummy report due to https://bugzilla.redhat.com/show_bug.cgi?id=822240
        metricsManager.mergeMeasurementReport(dummyReport);
        metricsManager.mergeMeasurementReport(report);

        metricsManager.compressPurgeAndTruncate();

        MeasurementDataManagerLocal dataManager = LookupUtil.getMeasurementDataManager();

        MeasurementAggregate aggregate = dataManager.getAggregate(overlord, dynamicSchedule.getId(),
            oneHourAgo.minusMinutes(30).getMillis(), now.getMillis());

        assertNotNull(aggregate);
        assertEquals("Failed to calculate the min", 2.6, aggregate.getMin());  Double d;
        assertEquals("Failed to calculate the max", 3.9, aggregate.getMax());
        assertEquals("Failed to calculate the average", (3.2 + 3.9 + 2.6) / 3.0, aggregate.getAvg());
    }

}
