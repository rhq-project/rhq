/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.measurement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
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

import org.jboss.ejb3.annotation.TransactionTimeout;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.Postgresql83DatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.CallTimeDataCriteria;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.measurement.calltime.CallTimeDataKey;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

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

    private static final String CALLTIME_VALUE_INSERT_STATEMENT = "INSERT /*+ APPEND */ INTO " + DATA_VALUE_TABLE_NAME
        + "(id, key_id, begin_time, end_time, minimum, maximum, total, count) "
        + "SELECT %s, key.id, ?, ?, ?, ?, ?, ? FROM " + DATA_KEY_TABLE_NAME
        + " key WHERE key.schedule_id = ? AND key.call_destination = ?";

    private static final String CALLTIME_VALUE_INSERT_STATEMENT_AUTOINC = "INSERT INTO " + DATA_VALUE_TABLE_NAME
        + "(key_id, begin_time, end_time, minimum, maximum, total, count) SELECT key.id, ?, ?, ?, ?, ?, ? FROM "
        + DATA_KEY_TABLE_NAME + " key WHERE key.schedule_id = ? AND key.call_destination = ?";

    private static final String CALLTIME_VALUE_PURGE_STATEMENT = "DELETE FROM " + DATA_VALUE_TABLE_NAME
        + " WHERE end_time < ?";

    private final Log log = LogFactory.getLog(CallTimeDataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private CallTimeDataManagerLocal callTimeDataManager;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addCallTimeData(@NotNull
    Set<CallTimeData> callTimeDataSet) {
        if (callTimeDataSet.isEmpty()) {
            return;
        }

        log.debug("Persisting call-time data for " + callTimeDataSet.size() + " schedules...");
        long startTime = System.currentTimeMillis();

        // First make sure a single row exists in the key table for each reported call destination.
        callTimeDataManager.insertCallTimeDataKeys(callTimeDataSet);

        // Finally, add the stats themselves to the value table.
        callTimeDataManager.insertCallTimeDataValues(callTimeDataSet);
        MeasurementMonitor.getMBean().incrementCallTimeInsertTime(System.currentTimeMillis() - startTime);

    }

    public PageList<CallTimeDataComposite> findCallTimeDataRawForResource(Subject subject, int scheduleId,
        long beginTime, long endTime, PageControl pageControl) {
        pageControl.initDefaultOrderingField("value.beginTime", PageOrdering.ASC);
        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
        int resourceId = schedule.getResource().getId();
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view call time data for measurementSchedule[id=" + scheduleId
                + "] and resource[id=" + resourceId + "]");
        }
        String query = CallTimeDataValue.QUERY_FIND_RAW_FOR_RESOURCE;

        Query queryWithOrderBy = PersistenceUtility.createQueryWithOrderBy(entityManager, query, pageControl);
        Query queryCount = PersistenceUtility.createCountQuery(this.entityManager, query);

        queryWithOrderBy.setParameter("scheduleId", scheduleId);
        queryWithOrderBy.setParameter("beginTime", beginTime);
        queryWithOrderBy.setParameter("endTime", endTime);

        queryCount.setParameter("scheduleId", scheduleId);
        queryCount.setParameter("beginTime", beginTime);
        queryCount.setParameter("endTime", endTime);

        @SuppressWarnings("unchecked")
        List<CallTimeDataComposite> results = queryWithOrderBy.getResultList();
        long count = (Long) queryCount.getSingleResult();

        return new PageList<CallTimeDataComposite>(results, (int) count, pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<CallTimeDataComposite> findCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pageControl) {
        pageControl.initDefaultOrderingField("SUM(value.total)/SUM(value.count)", PageOrdering.DESC); // only set if no ordering yet specified
        pageControl.addDefaultOrderingField("key.callDestination", PageOrdering.ASC); // add this to sort, if not already specified

        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
        int resourceId = schedule.getResource().getId();
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view call time data for measurementSchedule[id=" + scheduleId
                + "] and resource[id=" + resourceId + "]");
        }

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

    public PageList<CallTimeDataComposite> findCallTimeDataForCompatibleGroup(Subject subject, int groupId,
        long beginTime, long endTime, PageControl pageControl) {
        return findCallTimeDataForContext(subject, EntityContext.forGroup(groupId), beginTime, endTime, null,
            pageControl);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForAutoGroup(Subject subject, int parentResourceId,
        int childResourceTypeId, long beginTime, long endTime, PageControl pageControl) {
        return findCallTimeDataForContext(subject, EntityContext.forAutoGroup(parentResourceId, childResourceTypeId),
            beginTime, endTime, null, pageControl);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context,
        long beginTime, long endTime, String destination, PageControl pageControl) {

        CallTimeDataCriteria criteria = new CallTimeDataCriteria();
        criteria.addFilterBeginTime(beginTime);
        criteria.addFilterEndTime(endTime);
        if (destination != null && !destination.trim().equals("")) {
            criteria.addFilterDestination(destination);
        }

        criteria.setPageControl(pageControl);

        return findCallTimeDataForContext(subject, context, criteria);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context,
        CallTimeDataCriteria criteria) {

        PageControl pageControl = criteria.getPageControlOverrides();
        if (pageControl != null) {
            pageControl.initDefaultOrderingField("SUM(calltimedatavalue.total)/SUM(calltimedatavalue.count)",
                PageOrdering.DESC); // only set if no ordering yet specified
            pageControl.addDefaultOrderingField("calltimedatavalue.key.callDestination", PageOrdering.ASC); // add this to sort, if not already specified
        }

        if (context.type == EntityContext.Type.Resource) {
            criteria.addFilterResourceId(context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            criteria.addFilterResourceGroupId(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
            criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
        }
        criteria.setSupportsAddSortId(false);

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(subject, criteria);
        String replacementSelectList = "" //
            + " new org.rhq.core.domain.measurement.calltime.CallTimeDataComposite( " //
            + "   calltimedatavalue.key.callDestination, " //
            + "   MIN(calltimedatavalue.minimum), " //
            + "   MAX(calltimedatavalue.maximum), " //
            + "   SUM(calltimedatavalue.total), " //
            + "   SUM(calltimedatavalue.count), " //
            + "   SUM(calltimedatavalue.total) / SUM(calltimedatavalue.count) ) ";
        generator.alterProjection(replacementSelectList);
        generator.setGroupByClause("calltimedatavalue.key.callDestination");

        if (authorizationManager.isInventoryManager(subject) == false) {
            generator.setAuthorizationResourceFragment(CriteriaQueryGenerator.AuthorizationTokenType.RESOURCE,
                "key.schedule.resource", subject.getId());
        }

        //log.info(generator.getParameterReplacedQuery(false));
        //log.info(generator.getParameterReplacedQuery(true));

        CriteriaQueryRunner<CallTimeDataComposite> queryRunner = new CriteriaQueryRunner<CallTimeDataComposite>(
            criteria, generator, entityManager);
        PageList<CallTimeDataComposite> results = queryRunner.execute();
        return results;
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
            conn = rhqDs.getConnection();

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

    /*
     * internal method, do not expose to the remote API
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertCallTimeDataKeys(Set<CallTimeData> callTimeDataSet) {

        int[] results;
        String insertKeySql;
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof Postgresql83DatabaseType) {
                Statement st = null;
                try {
                    // Take advantage of async commit here
                    st = conn.createStatement();
                    st.execute("SET synchronous_commit = off");
                } finally {
                    JDBCUtil.safeClose(st);
                }
            }

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
                    // make sure the destination string is safe for storage, clip as needed
                    String safeCallDestination = dbType.getString(callDestination,
                        CallTimeDataKey.DESTINATION_MAX_LENGTH);
                    ps.setString(2, safeCallDestination);
                    ps.setString(4, safeCallDestination);
                    ps.addBatch();
                }
            }

            results = ps.executeBatch();

            int insertedRowCount = 0;
            for (int i = 0; i < results.length; i++) {
                if (((results[i] < 0) || (results[i] > 1)) && (results[i] != -2)) // oracle returns -2 because it can't count updated rows
                {
                    throw new MeasurementStorageException("Failed to insert call-time data key rows - result ["
                        + results[i] + "] for batch command [" + i + "] is less than 0 or greater than 1.");
                }

                insertedRowCount += results[i] == -2 ? 1 : results[i]; // If Oracle returns -2, just count 1 row
            }

            log.debug("Inserted new call-time data key rows for " + ((insertedRowCount >= 0) ? insertedRowCount : "?")
                + " out of " + results.length + " reported key-value pairs.");
        } catch (SQLException e) {
            logSQLException("Failed to persist call-time data keys", e);
        } catch (Throwable t) {
            log.error("Failed to persist call-time data keys", t);
        } finally {
            JDBCUtil.safeClose(conn, ps, null);
        }
    }

    /*
     * internal method, do not expose to the remote API
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void insertCallTimeDataValues(Set<CallTimeData> callTimeDataSet) {
        int[] results;
        String insertValueSql;
        PreparedStatement ps = null;
        Connection conn = null;

        try {
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof Postgresql83DatabaseType) {
                Statement st = null;
                try {
                    // Take advantage of async commit here
                    st = conn.createStatement();
                    st.execute("SET synchronous_commit = off");
                } finally {
                    JDBCUtil.safeClose(st);
                }
            }

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
                    // make sure the destination string is safe for storage, clip as needed
                    String safeCallDestination = dbType.getString(callDestination,
                        CallTimeDataKey.DESTINATION_MAX_LENGTH);
                    ps.setString(8, safeCallDestination);
                    ps.addBatch();
                }
            }

            results = ps.executeBatch();

            int insertedRowCount = 0;
            for (int i = 0; i < results.length; i++) {
                if ((results[i] != 1) && (results[i] != -2)) // Oracle likes to return -2 becuase it doesn't track batch update counts
                {
                    throw new MeasurementStorageException("Failed to insert call-time data value rows - result ["
                        + results[i] + "] for batch command [" + i + "] does not equal 1.");
                }

                insertedRowCount += results[i] == -2 ? 1 : results[i]; // If Oracle returns -2, just count 1 row;
            }

            notifyAlertConditionCacheManager("insertCallTimeDataValues",
                callTimeDataSet.toArray(new CallTimeData[callTimeDataSet.size()]));

            if (insertedRowCount > 0) {
                MeasurementMonitor.getMBean().incrementCalltimeValuesInserted(insertedRowCount);

                log.debug("Inserted " + insertedRowCount + " call-time data value rows.");
            }

        } catch (SQLException e) {
            logSQLException("Failed to persist call-time data values", e);
        } catch (Throwable t) {
            log.error("Failed to persist call-time data values", t);
        } finally {
            JDBCUtil.safeClose(conn, ps, null);
        }

    }

    private void notifyAlertConditionCacheManager(String callingMethod, CallTimeData... data) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(data);

        log.debug(callingMethod + ": " + stats.toString());
    }

    private void logSQLException(String message, SQLException e) {
        SQLException mainException = e;
        StringBuilder causes = new StringBuilder();
        int i = 1;
        while ((e = e.getNextException()) != null) {
            causes.append(i++).append("\n\t").append(e);
        }

        log.error(message + " - causes: " + causes, mainException);
    }
}