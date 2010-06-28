/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.measurement.MeasurementServerService;
import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementServerServiceImpl;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An EJB for testing the measurement subsystem - used by TestControl.jsp.
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementTestBean implements MeasurementTestLocal {
    private final Log log = LogFactory.getLog(MeasurementTestBean.class);

    private static final String FAKE_PLATFORM_NAME = "Fake Platform";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;
    private DatabaseType databaseType;

    private MeasurementServerService measurementServerService = new MeasurementServerServiceImpl();

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @PostConstruct
    public void init() {
        Connection conn = null;
        try {
            conn = rhqDs.getConnection();
            databaseType = DatabaseTypeFactory.getDatabaseType(conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    /**
     * Send a test measurement report, full of lots of fake metrics, to the server.
     */
    @SuppressWarnings("deprecation")
    public void sendTestMeasurementReport() {
        Resource res = setupFakePlatformIfNeeded();
        Set<ResourceMeasurementScheduleRequest> scheds;
        int[] resourceIds = new int[] { res.getId() };
        scheds = measurementScheduleManager.findSchedulesForResourceAndItsDescendants(resourceIds, false);

        Date now = new Date();
        MeasurementReport report = new MeasurementReport();
        for (ResourceMeasurementScheduleRequest sched : scheds) {
            for (MeasurementScheduleRequest s : sched.getMeasurementSchedules()) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(now);

                // send 10 data points per schedule
                for (int i = 0; i < 10; i++) {
                    long dat = cal.getTimeInMillis();
                    MeasurementDataNumeric mdn = new MeasurementDataNumeric(new MeasurementDataPK(dat, s
                        .getScheduleId()), (double) i);
                    report.addData(mdn);
                    cal.add(Calendar.SECOND, -1);
                }
            }
        }

        measurementServerService.mergeMeasurementReport(report);
    }

    public void addProblemResource() {
        Resource res = setupFakePlatformIfNeeded();
        Availability ava = new Availability(res, new Date(), AvailabilityType.UP);
        entityManager.persist(ava);

        if ((res.getSchedules().size() == 0) && (res.getResourceType().getMetricDefinitions().size() > 0)) {
            MeasurementDefinition def = res.getResourceType().getMetricDefinitions().iterator().next();
            MeasurementSchedule schedule = new MeasurementSchedule(def, res);
            entityManager.persist(schedule);
            res.addSchedule(schedule);
        }

        entityManager.flush();
    }

    /**
     * Set up a fake platform and a MeasurementDefinition for it for several usages. This method will check if the fake
     * platform already exists first.
     *
     * @return
     */
    private Resource setupFakePlatformIfNeeded() {
        Resource res;
        try {
            Query q = entityManager.createQuery("SELECT res FROM Resource res WHERE res.name = :name");
            q.setParameter("name", FAKE_PLATFORM_NAME);
            res = (Resource) q.getSingleResult();
        } catch (NoResultException nre) {
            ResourceType resourceType = new ResourceType("fake platform", "", ResourceCategory.PLATFORM, null);
            resourceType.setPlugin("Platforms");
            entityManager.persist(resourceType);
            MeasurementDefinition def = new MeasurementDefinition(resourceType, "Fake Definition");
            def.setUnits(MeasurementUnits.SECONDS);
            def.setResourceType(resourceType);
            entityManager.persist(def);
            resourceType.addMetricDefinition(def);
            res = new Resource("org.jboss.on.TestPlatfor", FAKE_PLATFORM_NAME, resourceType);
            entityManager.persist(res);
            entityManager.flush();
        }

        return res;
    }

    @SuppressWarnings("deprecation")
    public void setAgentCurrentlyScheduledMetrics(double value) {
        String scheduleString = "SELECT schedule " + "FROM MeasurementSchedule schedule "
            + "WHERE schedule.definition.name = 'CurrentlyScheduleMeasurements' "
            + "AND schedule.resource.name LIKE '%Agent%'";

        Query scheduleQuery = entityManager.createQuery(scheduleString);
        MeasurementSchedule schedule = (MeasurementSchedule) scheduleQuery.getSingleResult();

        MeasurementDataNumeric numericData = new MeasurementDataNumeric(new MeasurementDataPK(schedule.getId()), value);

        entityManager.persist(numericData);

        LookupUtil.getAlertConditionCacheManager().checkConditions(numericData);
    }

    public Map<String, Long> snapshotMeasurementTables() {
        String snapshotQuery = "" //
            + "select" //
            + "(select count(*) from rhq_meas_data_num_r00) as r00," //
            + "(select count(*) from rhq_meas_data_num_r01) as r01," //
            + "(select count(*) from rhq_meas_data_num_r02) as r02," //
            + "(select count(*) from rhq_meas_data_num_r03) as r03," //
            + "(select count(*) from rhq_meas_data_num_r04) as r04," //
            + "(select count(*) from rhq_meas_data_num_r05) as r05," //
            + "(select count(*) from rhq_meas_data_num_r06) as r06," //
            + "(select count(*) from rhq_meas_data_num_r07) as r07," //
            + "(select count(*) from rhq_meas_data_num_r08) as r08," //
            + "(select count(*) from rhq_meas_data_num_r09) as r09," //
            + "(select count(*) from rhq_meas_data_num_r10) as r10," //
            + "(select count(*) from rhq_meas_data_num_r11) as r11," //
            + "(select count(*) from rhq_meas_data_num_r12) as r12," //
            + "(select count(*) from rhq_meas_data_num_r13) as r13," //
            + "(select count(*) from rhq_meas_data_num_r14) as r14," //
            + "(select count(*) from rhq_measurement_data_num_1h) as oneHour," //
            + "(select count(*) from rhq_measurement_data_num_6h) as sixHour," //
            + "(select count(*) from rhq_measurement_data_num_1d) as oneDay," //
            + "(select count(*) from rhq_measurement_data_trait) as trait," //
            + "(select count(*) from rhq_measurement_bline) as bline," //
            + "(select count(*) from rhq_measurement_oob) as oob," //
            + "(select count(*) from rhq_measurement_oob_tmp) as oob_temp," //
            + "(select count(*) from rhq_calltime_data_key) as callkey," //
            + "(select count(*) from rhq_calltime_data_value) as calldata," //
            + "(select count(ms.id) from rhq_measurement_sched ms" //
            + "   join rhq_measurement_def md on ms.definition = md.id" //
            + "  where ms.enabLed = true and md.data_type=0) as enabledMetricSchedules," //
            + "(select count(ms.id) from rhq_measurement_sched ms" //
            + "   join rhq_measurement_def md on ms.definition = md.id" //
            + "  where ms.enabLed = true and md.data_type=1) as enabledTraitSchedules," //
            + "(select count(ms.id) from rhq_measurement_sched ms" //
            + "   join rhq_measurement_def md on ms.definition = md.id" //
            + "  where ms.enabLed = true and md.data_type=3) as enabledCalltimeSchedules";

        String querySuffix = ";";
        if (databaseType instanceof OracleDatabaseType) {
            querySuffix = "from dual;";
        }

        Map<String, Long> results = new LinkedHashMap<String, Long>();

        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = rhqDs.getConnection();
            ps = conn.prepareStatement(snapshotQuery + querySuffix);
            rs = ps.executeQuery();

            String[] columnNames = { "r00", "r01", "r02", "r03", "r04", "r05", "r06", "r07", "r08", "r09", "r10",
                "r11", "r12", "r13", "r14", "oneHour", "sixHour", "oneDay", "trait", "bline", "oob", "oob_temp",
                "callkey", "calldata", "enabledMetricSchedules", "enabledTraitSchedules", "enabledCalltimeSchedules" };
            if (rs.next()) {
                for (String nextColumn : columnNames) {
                    Long nextValue = rs.getLong(nextColumn);
                    results.put(nextColumn, nextValue);
                }
            }
        } catch (Throwable t) {
            log.error("Could not snapshot measurement tables", t);
        } finally {
            JDBCUtil.safeClose(conn, ps, rs);
        }

        return results;
    }

    public static void main(String[] args) {
        Map<String, Long> results = new MeasurementTestBean().snapshotMeasurementTables();
        for (Map.Entry<String, Long> nextCount : results.entrySet()) {
            String tableAlias = nextCount.getKey();
            Long tableCount = nextCount.getValue();

        }
    }
}