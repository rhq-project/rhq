/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;

@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class TestBean implements TestLocal {
    private final Log log = LogFactory.getLog(TestBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    private DatabaseType databaseType;

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

    @Override
    public void enableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(entityManager, ManagementFactory.getPlatformMBeanServer(), true);
    }

    @Override
    public void disableHibernateStatistics() {
        PersistenceUtility.enableHibernateStatistics(entityManager, ManagementFactory.getPlatformMBeanServer(), false);
    }

    @Override
    public Map<String, Long> getMeasurementTableStats() {
        String qTrue = this.databaseType.getBooleanValue(true);

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
            + "  where ms.enabled = " + qTrue + " and md.data_type=0) as enabledMetricSchedules," //
            + "(select count(ms.id) from rhq_measurement_sched ms" //
            + "   join rhq_measurement_def md on ms.definition = md.id" //
            + "  where ms.enabled = " + qTrue + " and md.data_type=1) as enabledTraitSchedules," //
            + "(select count(ms.id) from rhq_measurement_sched ms" //
            + "   join rhq_measurement_def md on ms.definition = md.id" //
            + "  where ms.enabled = " + qTrue + " and md.data_type=3) as enabledCalltimeSchedules";

        String querySuffix = ";";
        if (DatabaseTypeFactory.isOracle(this.databaseType)) {
            querySuffix = " from dual";
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
}