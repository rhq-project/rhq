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
package org.rhq.enterprise.server.measurement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;

import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;

/**
 * The manager for call-time metric data.
 *
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class CallTimeDataManagerBean implements CallTimeDataManagerLocal, CallTimeDataManagerRemote {
    private static final String DATA_VALUE_TABLE_NAME = "RHQ_CALLTIME_DATA_VALUE";
    private static final String DATA_KEY_TABLE_NAME = "RHQ_CALLTIME_DATA_KEY";

    private static final String CALLTIME_KEY_INSERT_STATEMENT = "INSERT INTO " + DATA_KEY_TABLE_NAME
        + "(id, schedule_id, call_destination) " + "SELECT %s, ?, ? FROM RHQ_numbers WHERE i = 42 "
        + "AND NOT EXISTS (SELECT * FROM " + DATA_KEY_TABLE_NAME + " WHERE schedule_id = ? AND call_destination = ?)";

    private static final String CALLTIME_KEY_INSERT_STATEMENT_AUTOINC = "INSERT INTO " + DATA_KEY_TABLE_NAME
        + "(schedule_id, call_destination) " + "SELECT ?, ? FROM RHQ_numbers WHERE i = 42 "
        + "AND NOT EXISTS (SELECT * FROM " + DATA_KEY_TABLE_NAME + " WHERE schedule_id = ? AND call_destination = ?)";

    private static final String CALLTIME_VALUE_DELETE_SUPERCEDED_STATEMENT = "DELETE FROM " + DATA_VALUE_TABLE_NAME
        + " WHERE key_id = " + "(SELECT id FROM " + DATA_KEY_TABLE_NAME
        + " WHERE schedule_id = ? AND call_destination = ?) AND begin_time = ?";

    private static final String CALLTIME_VALUE_INSERT_STATEMENT = "INSERT INTO " + DATA_VALUE_TABLE_NAME
        + "(id, key_id, begin_time, end_time, minimum, maximum, total, count) "
        + "SELECT %s, key.id, ?, ?, ?, ?, ?, ? FROM RHQ_numbers num, RHQ_calltime_data_key key WHERE num.i = 42 "
        + "AND key.id = (SELECT id FROM " + DATA_KEY_TABLE_NAME + " WHERE schedule_id = ? AND call_destination = ?)";

    private static final String CALLTIME_VALUE_INSERT_STATEMENT_AUTOINC = "INSERT INTO " + DATA_VALUE_TABLE_NAME
        + "(key_id, begin_time, end_time, minimum, maximum, total, count) "
        + "SELECT key.id, ?, ?, ?, ?, ?, ? FROM RHQ_numbers num, RHQ_calltime_data_key key WHERE num.i = 42 "
        + "AND key.id = (SELECT id FROM " + DATA_KEY_TABLE_NAME + " WHERE schedule_id = ? AND call_destination = ?)";

    private static final String CALLTIME_VALUE_PURGE_STATEMENT = "DELETE FROM " + DATA_VALUE_TABLE_NAME
        + " WHERE end_time < ?";

    private final Log log = LogFactory.getLog(CallTimeDataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource
    private SessionContext sessionContext;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addCallTimeData(@NotNull Set<CallTimeData> callTimeDataSet) {
        if (callTimeDataSet.isEmpty()) {
            return;
        }

        log.debug("Persisting call-time data for " + callTimeDataSet.size() + " schedules...");
        DataSource ds = (DataSource) this.sessionContext.lookup("RHQ_DS");
        Connection conn = null;
        try {
            conn = ds.getConnection();

            // First make sure a single row exists in the key table for each reported call destination.
            insertCallTimeDataKeys(callTimeDataSet, conn);

            // Delete any existing rows that have the same key and begin time as the data about to be inserted.
            deleteRedundantCallTimeDataValues(callTimeDataSet, conn);

            // Finally, add the stats themselves to the value table.
            insertCallTimeDataValues(callTimeDataSet, conn);
        } catch (SQLException e) {
            logSQLException(e);
        } catch (Throwable t) {
            log.error("Failed to persist call-time data.", t);
        } finally {
            JDBCUtil.safeClose(conn);
        }
    }

    @SuppressWarnings("unchecked")
    public PageList<CallTimeDataComposite> getCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pageControl) throws FetchException {
        pageControl.initDefaultOrderingField("SUM(value.total)/SUM(value.count)", PageOrdering.DESC); // only set if no ordering yet specified
        pageControl.addDefaultOrderingField("key.callDestination", PageOrdering.ASC); // add this to sort, if not already specified

        // TODO: authz check

        String query = CallTimeDataValue.QUERY_FIND_COMPOSITES_FOR_RESOURCE;

        Query queryWithOrderBy = PersistenceUtility.createQueryWithOrderBy(entityManager, query, pageControl);
        Query queryCount = PersistenceUtility.createCountQuery(this.entityManager, query);

        queryWithOrderBy.setParameter("scheduleId", scheduleId);
        queryWithOrderBy.setParameter("beginTime", beginTime);
        queryWithOrderBy.setParameter("endTime", endTime);

        List<CallTimeDataComposite> results = queryWithOrderBy.getResultList();

        queryCount.setParameter("scheduleId", scheduleId);
        queryCount.setParameter("beginTime", beginTime);
        queryCount.setParameter("endTime", endTime);

        // Because of the use of the GROUP BY clause, the result list count will be returned as
        // the number of rows, rather than as a single number.
        long count = queryCount.getResultList().size();

        return new PageList<CallTimeDataComposite>(results, (int) count, pageControl);
    }

    public PageList<CallTimeDataComposite> getCallTimeDataForCompatibleGroup(Subject subject, int groupId,
        int measurementDefinitionId, long beginTime, long endTime, PageControl pageControl) {
        // TODO
        return null;
    }

    public PageList<CallTimeDataComposite> getCallTimeDataForAutoGroup(Subject subject, int parentResourceId,
        int childResourceTypeId, int measurementDefinitionId, long beginTime, long endTime, PageControl pageControl) {
        // TODO
        return null;
    }

    /**
     * Deletes call-time data older than the specified time.
     *
     * @param deleteUpToTime call-time data older than this time will be deleted
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(6 * 60 * 60)
    public int purgeCallTimeData(Date deleteUpToTime) throws SQLException {
        // NOTE: Apparently, Hibernate does not support DML JPQL queries, so we're stuck using JDBC.
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = ((DataSource) this.sessionContext.lookup(RHQConstants.DATASOURCE_JNDI_NAME)).getConnection();

            // Purge old rows from RHQ_CALLTIME_DATA_VALUE.
            stmt = conn.prepareStatement(CALLTIME_VALUE_PURGE_STATEMENT);
            stmt.setLong(1, deleteUpToTime.getTime());

            long startTime = System.currentTimeMillis();
            int deletedRowCount = stmt.executeUpdate();
            MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
            MeasurementMonitor.getMBean().setPurgedCallTimeData(deletedRowCount);
            return deletedRowCount;

            // NOTE: We do not purge unreferenced rows from RHQ_CALLTIME_DATA_KEY, because this can cause issues
            //       (see http://jira.jboss.com/jira/browse/JBNADM-1606). Once we limit the number of keys per
            //       resource at insertion time (see http://jira.jboss.com/jira/browse/JBNADM-2618), the key
            //       table will not require truncation.
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }
    }

    private void insertCallTimeDataKeys(Set<CallTimeData> callTimeDataSet, Connection conn) throws Exception {

        int[] results;
        String insertKeySql;
        PreparedStatement ps = null;

        try {
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);
            if (dbType instanceof PostgresqlDatabaseType || dbType instanceof OracleDatabaseType
                || dbType instanceof H2DatabaseType) {
                String keyNextvalSql = JDBCUtil.getNextValSql(conn, "RHQ_calltime_data_key");
                insertKeySql = String.format(CALLTIME_KEY_INSERT_STATEMENT, keyNextvalSql);
            } else if (dbType instanceof SQLServerDatabaseType) {
                insertKeySql = CALLTIME_KEY_INSERT_STATEMENT_AUTOINC;
            } else {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);
            }

            ps = conn.prepareStatement(insertKeySql);
            for (CallTimeData callTimeData : callTimeDataSet) {
                ps.setInt(1, callTimeData.getScheduleId());
                ps.setInt(3, callTimeData.getScheduleId());
                Set<String> callDestinations = callTimeData.getValues().keySet();
                for (String callDestination : callDestinations) {
                    ps.setString(2, callDestination);
                    ps.setString(4, callDestination);
                    ps.addBatch();
                }
            }

            results = ps.executeBatch();
        } finally {
            JDBCUtil.safeClose(ps);
        }

        int insertedRowCount = 0;
        for (int i = 0; i < results.length; i++) {
            if (((results[i] < 0) || (results[i] > 1)) && (results[i] != -2)) // oracle returns -2 because it can't count updated rows
            {
                throw new MeasurementStorageException("Failed to insert call-time data key rows - result ["
                    + results[i] + "] for batch command [" + i + "] is less than 0 or greater than 1.");
            }

            insertedRowCount += results[i];
        }

        log.debug("Inserted new call-time data key rows for " + ((insertedRowCount >= 0) ? insertedRowCount : "?")
            + " out of " + results.length + " reported key-value pairs.");
    }

    private void deleteRedundantCallTimeDataValues(Set<CallTimeData> callTimeDataSet, Connection conn)
        throws SQLException {
        PreparedStatement ps = null;
        int[] results;
        try {
            ps = conn.prepareStatement(CALLTIME_VALUE_DELETE_SUPERCEDED_STATEMENT);
            for (CallTimeData callTimeData : callTimeDataSet) {
                ps.setInt(1, callTimeData.getScheduleId());
                Set<String> callDestinations = callTimeData.getValues().keySet();
                for (String callDestination : callDestinations) {
                    ps.setString(2, callDestination);
                    CallTimeDataValue value = callTimeData.getValues().get(callDestination);
                    ps.setLong(3, value.getBeginTime());
                    ps.addBatch();
                }
            }

            results = ps.executeBatch();
        } finally {
            JDBCUtil.safeClose(ps);
        }

        int deletedRowCount = 0;
        for (int i = 0; i < results.length; i++) {
            if ((results[i] < 0) && (results[i] != -2)) // oracle returns -2 because it can't count updated rows
            {
                throw new MeasurementStorageException("Failed to delete redundant call-time data rows - result ["
                    + results[i] + "] for batch command [" + i + "] is less than 0.");
            }

            deletedRowCount += results[i];
        }

        log
            .debug("Deleted "
                + ((deletedRowCount >= 0) ? deletedRowCount : "?")
                + " redundant call-time data value rows that were superceded by data in the measurement report currently being processed.");
    }

    private void insertCallTimeDataValues(Set<CallTimeData> callTimeDataSet, Connection conn) throws Exception {
        int[] results;
        String insertValueSql;
        PreparedStatement ps = null;

        try {
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);
            if (dbType instanceof PostgresqlDatabaseType || dbType instanceof OracleDatabaseType
                || dbType instanceof H2DatabaseType) {
                String valueNextvalSql = JDBCUtil.getNextValSql(conn, "RHQ_calltime_data_value");
                insertValueSql = String.format(CALLTIME_VALUE_INSERT_STATEMENT, valueNextvalSql);
            } else if (dbType instanceof SQLServerDatabaseType) {
                insertValueSql = CALLTIME_VALUE_INSERT_STATEMENT_AUTOINC;
            } else {
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);
            }

            ps = conn.prepareStatement(insertValueSql);
            for (CallTimeData callTimeData : callTimeDataSet) {
                ps.setInt(7, callTimeData.getScheduleId());
                Set<String> callDestinations = callTimeData.getValues().keySet();
                for (String callDestination : callDestinations) {
                    CallTimeDataValue callTimeDataValue = callTimeData.getValues().get(callDestination);
                    ps.setLong(1, callTimeDataValue.getBeginTime());
                    ps.setLong(2, callTimeDataValue.getEndTime());
                    ps.setDouble(3, callTimeDataValue.getMinimum());
                    ps.setDouble(4, callTimeDataValue.getMaximum());
                    ps.setDouble(5, callTimeDataValue.getTotal());
                    ps.setLong(6, callTimeDataValue.getCount());
                    ps.setString(8, callDestination);
                    ps.addBatch();
                }
            }

            results = ps.executeBatch();
        } finally {
            JDBCUtil.safeClose(ps);
        }

        int insertedRowCount = 0;
        for (int i = 0; i < results.length; i++) {
            if ((results[i] != 1) && (results[i] != -2)) // Oracle likes to return -2 becuase it doesn't track batch update counts
            {
                throw new MeasurementStorageException("Failed to insert call-time data value rows - result ["
                    + results[i] + "] for batch command [" + i + "] does not equal 1.");
            }

            insertedRowCount += results[i];
        }

        log.debug("Inserted " + ((insertedRowCount >= 0) ? insertedRowCount : "?") + " call-time data value rows.");
    }

    private void logSQLException(SQLException e) {
        SQLException mainException = e;
        StringBuilder causes = new StringBuilder();
        int i = 1;
        while ((e = e.getNextException()) != null) {
            causes.append(i++).append("\n\t").append(e);
        }

        log.error("Failed to persist call-time data - causes: " + causes, mainException);
    }
}