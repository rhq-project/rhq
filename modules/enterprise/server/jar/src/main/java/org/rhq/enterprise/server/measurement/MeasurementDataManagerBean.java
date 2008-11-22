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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.annotation.IgnoreDependency;
import org.jboss.annotation.ejb.TransactionTimeout;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayConstants;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.server.measurement.uibean.MetricDisplayValue;
import org.rhq.enterprise.server.measurement.util.MeasurementDataManagerUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;

/**
 * A manager for {@link MeasurementData}s.
 *
 * @author Heiko W. Rupp
 * @author Greg Hinkle
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementDataManagerBean implements MeasurementDataManagerLocal {
    // time_stamp, schedule_id, value, schedule_id, schedule_id, value, value, value, value
    private static final String TRAIT_INSERT_STATEMENT = "INSERT INTO RHQ_measurement_data_trait \n"
        + "  SELECT ?, ?, ?  FROM RHQ_numbers n \n"
        + "  WHERE n.i = 42 \n"
        + "    AND NOT EXISTS \n"
        + "      ( \n"
        + "      SELECT 1 \n"
        + "      FROM (SELECT dt2.value as v \n"
        + "            FROM RHQ_measurement_data_trait dt2 \n"
        + "      WHERE dt2.schedule_id = ? \n"
        + "        AND dt2.time_stamp = \n"
        + "          (SELECT max(dt3.time_stamp) FROM RHQ_measurement_data_trait dt3 WHERE dt3.schedule_id = ?))  lastValue \n"
        + "      WHERE NOT ((? is null AND lastValue.v is not null) \n"
        + "        OR (? is not null AND lastValue.v is null) \n"
        + "              OR (? is not null AND lastValue.v is not null AND ? <> lastValue.v)) \n" + "      )";

    private final Log log = LogFactory.getLog(MeasurementDataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;
    @EJB
    private AlertManagerLocal alertManager;
    @EJB
    @IgnoreDependency
    private AgentManagerLocal agentClientManager;
    @EJB
    private MeasurementProblemManagerLocal measurementProblemManager;
    @EJB
    @IgnoreDependency
    private ResourceManagerLocal resourceManager;
    @EJB
    @IgnoreDependency
    private MeasurementScheduleManagerLocal scheduleManager;
    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;
    @EJB
    private CallTimeDataManagerLocal callTimeDataManager;
    @EJB
    private MeasurementDataManagerLocal measurementDataManager;
    @EJB
    @IgnoreDependency
    private MeasurementDefinitionManagerLocal measurementDefinitionManager;

    // doing a bulk delete in here, need to be in its own tx
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @TransactionTimeout(30 * 60 * 1000)
    public int purgeTraits(long oldest) {
        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = rhqDs.getConnection();
            stmt = conn.prepareStatement(MeasurementDataTrait.NATIVE_QUERY_PURGE);
            stmt.setLong(1, oldest);
            long startTime = System.currentTimeMillis();
            int deleted = stmt.executeUpdate();
            MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
            return deleted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to purge traits older than [" + oldest + "]", e);
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void mergeMeasurementReport(MeasurementReport report) {
        long start = System.currentTimeMillis();
        // TODO GH: Deal with offset (this is only for situations where the clock doesn't match on the agent)

        this.measurementDataManager.addNumericData(report.getNumericData());
        this.measurementDataManager.addTraitData(report.getTraitData());
        this.callTimeDataManager.addCallTimeData(report.getCallTimeData());

        long time = System.currentTimeMillis() - start;
        MeasurementMonitor.getMBean().incrementMeasurementInsertTime(time);
        MeasurementMonitor.getMBean().incrementMeasurementsInserted(report.getDataCount());

        if (log.isDebugEnabled()) {
            log.debug("Measurement storage for [" + report.getDataCount() + "] took " + time + "ms");
        }
    }

    /**
     * Add metrics data to the database. Data that is passed can come from several Schedules, but needs to be of only
     * one type of MeasurementGathering. For good performance it is important that the agent sends batches as big as
     * possible (ok, perhaps not more than 100 items at a time).
     *
     * @param data the actual data points
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addNumericData(Set<MeasurementDataNumeric> data) {
        if ((data == null) || (data.size() == 0)) {
            return;
        }

        int expectedCount = data.size();

        Connection conn = null;
        DatabaseType dbType = null;

        Map<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();

        try {
            conn = rhqDs.getConnection();
            dbType = DatabaseTypeFactory.getDatabaseType(conn);

            for (MeasurementDataNumeric aData : data) {
                if (aData.getValue() == null || Double.isNaN(aData.getValue())) {
                    expectedCount--;
                    continue;
                }

                String table = MeasurementDataManagerUtility.getTable(aData.getTimestamp());

                PreparedStatement ps = statements.get(table);

                if (ps == null) {
                    String insertSql = "INSERT  /*+ APPEND */ INTO " + table
                        + "(schedule_id,time_stamp,value) VALUES(?,?,?)";
                    ps = conn.prepareStatement(insertSql);
                    statements.put(table, ps);
                }

                ps.setInt(1, aData.getScheduleId());
                ps.setLong(2, aData.getTimestamp());
                ps.setDouble(3, aData.getValue());
                ps.addBatch();
            }

            int count = 0;
            for (PreparedStatement ps : statements.values()) {
                int[] res = ps.executeBatch();
                for (int updates : res) {
                    if ((updates != 1) && (updates != -2)) // oracle returns -2 on success
                    {
                        throw new MeasurementStorageException("Unexpected batch update size [" + updates + "]");
                    }

                    count++;
                }
            }

            if (count != expectedCount) {
                throw new MeasurementStorageException("Failure to store measurement data.");
            }

            notifyAlertConditionCacheManager("mergeMeasurementReport", data.toArray(new MeasurementData[data.size()]));
        } catch (SQLException e) {
            // TODO hwr What do we do here ? Depending on driver  database ..
            log.warn("Failure saving measurement data:\n" + e.getMessage());

            if ((dbType != null) && DatabaseTypeFactory.isPostgres(dbType)) {
                SQLException next = e.getNextException();
                log.warn("  +-> \n" + next.getMessage());
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            for (PreparedStatement ps : statements.values()) {
                JDBCUtil.safeClose(ps);
            }

            JDBCUtil.safeClose(conn);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void addTraitData(Set<MeasurementDataTrait> data) {
        if ((data == null) || (data.size() == 0)) {
            return;
        }

        Connection conn = null;
        PreparedStatement ps = null;
        DatabaseType dbType = null;
        try {
            conn = rhqDs.getConnection();

            // TODO GH: Can't do this managed txn? conn.setAutoCommit(false);
            dbType = DatabaseTypeFactory.getDatabaseType(conn);

            ps = conn.prepareStatement(TRAIT_INSERT_STATEMENT);

            for (MeasurementDataTrait aData : data) {
                // time_stamp, schedule_id, value, schedule_id, schedule_id, value, value, value, value
                ps.setLong(1, aData.getTimestamp());
                ps.setInt(2, aData.getScheduleId());
                ps.setString(3, aData.getValue());
                ps.setInt(4, aData.getScheduleId());
                ps.setInt(5, aData.getScheduleId());
                ps.setString(6, aData.getValue());
                ps.setString(7, aData.getValue());
                ps.setString(8, aData.getValue());
                ps.setString(9, aData.getValue());
                ps.addBatch();
            }

            int[] res = ps.executeBatch();
            if (res.length != data.size()) {
                throw new MeasurementStorageException("Failure to store measurement trait data.");
                // It is expected that some of these batch updates didn't update anything as the previous value was the same
            }

            notifyAlertConditionCacheManager("mergeMeasurementReport", data.toArray(new MeasurementData[data.size()]));
        } catch (SQLException e) {
            // TODO hwr What do we do here ? Depending on driver  database ..
            log.warn("Failure saving measurement data:\n" + e.getMessage());

            if ((dbType != null) && DatabaseTypeFactory.isPostgres(dbType)) {
                SQLException next = e.getNextException();
                log.warn("  +-> \n" + next.getMessage());
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
            JDBCUtil.safeClose(conn, ps, null);
        }
    }

    /**
     * Get the aggregate values of the numerical values for a given schedule This can only provide aggregates for data
     * in the "live" table
     *
     * @param  sched The Schedule for which this data is
     * @param  start the start time
     * @param  end   the end time
     *
     * @return MeasurementAggregate bean with the data
     *
     * @throws MeasurementException is the Schedule does not reference numerical data
     */
    public MeasurementAggregate getAggregate(MeasurementSchedule sched, long start, long end)
        throws MeasurementException {
        if (sched.getDefinition().getDataType() != DataType.MEASUREMENT) {
            throw new MeasurementException(sched + " is not about numerical values. Can't compute aggregates");
        }

        if (start > end) {
            throw new MeasurementException("Start date " + start + " is not before " + end);
        }

        MeasurementAggregate aggregate = MeasurementDataManagerUtility.getInstance(rhqDs).getAggregateByScheduleId(
            start, end, sched.getId());
        return aggregate;
    }

    public Set<MeasurementData> getLiveData(int resourceId, Set<Integer> definitionIds) {
        Resource resource = entityManager.find(Resource.class, resourceId);
        Agent agent = resource.getAgent();

        Query q = entityManager.createNamedQuery(MeasurementDefinition.FIND_BY_IDS);
        q.setParameter("ids", definitionIds);
        List<MeasurementDefinition> definitions = q.getResultList();

        String[] names = new String[definitions.size()];
        int i = 0;
        for (MeasurementDefinition def : definitions) {
            names[i++] = def.getName();
        }

        AgentClient ac = agentClientManager.getAgentClient(agent);
        Set<MeasurementData> values = ac.getMeasurementAgentService().getRealTimeMeasurementValue(resourceId,
            DataType.MEASUREMENT, names);

        return values;
    }

    /**
     * Remove gathered Measurement for the given Schedule
     *
     * @param sched The Schedule for which to remove the data
     */
    public void removeGatheredMetricsForSchedule(MeasurementSchedule sched) {
        List<MeasurementSchedule> schedules = new ArrayList<MeasurementSchedule>(1);
        schedules.add(sched);
        removeGatheredMetricsForSchedules(schedules);
    }

    /**
     * Remove gathered Measurement for the given Schedules from the compression tables.
     *
     * @param schedules The Schedule for which to remove the data
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void removeGatheredMetricsForSchedules(List<MeasurementSchedule> schedules) {
        if (schedules.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("removeGatheredMetricsForSchedules: nothing to do");
            }
            return;
        }

        int[] values = new int[schedules.size()];
        int i = 0;
        for (MeasurementSchedule sch : schedules) {
            values[i++] = sch.getId();
        }

        List<String> tmp = Arrays.asList(MeasurementDataManagerUtility.getAllRawTables());
        List<String> tables = new ArrayList<String>(tmp);
        tables.add("RHQ_MEASUREMENT_DATA_NUM_1H");
        tables.add("RHQ_MEASUREMENT_DATA_NUM_6H");
        tables.add("RHQ_MEASUREMENT_DATA_NUM_1D");

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = rhqDs.getConnection();
            for (String table : tables) {
                if (log.isDebugEnabled())
                    log.debug("Deleting data from table " + table);
                String query = "DELETE FROM " + table + " md WHERE md.schedule_id IN ( :scheduleIds ) ";
                query = JDBCUtil.transformQueryForMultipleInParameters(query, ":scheduleIds", schedules.size());
                ps = con.prepareStatement(query);
                JDBCUtil.bindNTimes(ps, values, 1);
                ps.execute();
                JDBCUtil.safeClose(ps);
            }
        } catch (SQLException e) {
            log.warn("Could not remove metrics for schedules: " + e.getMessage());
        } finally {
            JDBCUtil.safeClose(ps);
            JDBCUtil.safeClose(con);
        }
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForResource(Subject subject, int resourceId,
        int[] measurementDefinitionIds, long beginTime, long endTime) throws MeasurementException {
        List<MetricDisplaySummary> allMeasurementData = new ArrayList<MetricDisplaySummary>(
            measurementDefinitionIds.length);
        List<Integer> scheduleIds = new ArrayList<Integer>(measurementDefinitionIds.length);
        for (int measurementDefinitionId : measurementDefinitionIds) {
            MeasurementSchedule schedule = null;
            try {
                schedule = scheduleManager.getMeasurementSchedule(subject, measurementDefinitionId, resourceId, false);
                scheduleIds.add(schedule.getId());
            } catch (MeasurementNotFoundException mnfe) {
                throw new MeasurementException(mnfe);
            }

            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, beginTime, endTime, false);
            if (summary != null) {
                summary.setUnits(schedule.getDefinition().getUnits().name());
                allMeasurementData.add(summary);
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);
        for (MetricDisplaySummary sum : allMeasurementData) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return allMeasurementData;
    }

    @SuppressWarnings("unchecked")
    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForResource(Subject subject,
        int resourceId, int[] measurementDefinitionIds, long beginTime, long endTime, int numberOfDataPoints) {
        return MeasurementDataManagerUtility.getInstance(rhqDs).getMeasurementDataForResource(beginTime, endTime,
            resourceId, measurementDefinitionIds);
    }

    /**
     * Return a List of List<MesurementDataNumeric..> where each nested list contains the data for one of the passed
     * resource ids
     */
    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForSiblingResources(Subject subject,
        int[] resourceIds, int measurementDefinitionId, long beginTime, long endTime, int numberOfDataPoints) {
        return MeasurementDataManagerUtility.getInstance(rhqDs).getMeasurementDataForSiblingResources(beginTime,
            endTime, resourceIds, measurementDefinitionId);
    }

    private List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataAggregatesForSiblingResources(
        Subject subject, int[] resourceIds, int measurementDefinitionId, long beginTime, long endTime,
        int numberOfDataPoints) {
        return MeasurementDataManagerUtility.getInstance(rhqDs).getMeasurementDataAggregatesForSiblingResources(
            beginTime, endTime, resourceIds, measurementDefinitionId);
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForCompatibleGroup(Subject subject,
        int compatibleGroupId, int measurementDefinitionId, long beginTime, long endTime, int numberOfDataPoints,
        boolean aggregateOverGroup) {
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, compatibleGroupId,
            GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();

        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources) {
            resourceIds[i] = res.getId();
            i++;
        }

        List<List<MeasurementDataNumericHighLowComposite>> ret;

        if (aggregateOverGroup) {
            ret = getMeasurementDataAggregatesForSiblingResources(subject, resourceIds, measurementDefinitionId,
                beginTime, endTime, numberOfDataPoints);
        } else {
            ret = getMeasurementDataForSiblingResources(subject, resourceIds, measurementDefinitionId, beginTime,
                endTime, numberOfDataPoints);
        }

        return ret;
    }

    public List<List<MeasurementDataNumericHighLowComposite>> getMeasurementDataForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int measurementDefinitionId, long beginTime,
        long endTime, int numberOfDataPoints, boolean aggregateOverAutoGroup) {
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, autoGroupParentResourceId,
            autoGroupChildResourceTypeId);
        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources) {
            resourceIds[i] = res.getId();
            i++;
        }

        List<List<MeasurementDataNumericHighLowComposite>> ret;
        if (aggregateOverAutoGroup) {
            ret = getMeasurementDataAggregatesForSiblingResources(subject, resourceIds, measurementDefinitionId,
                beginTime, endTime, numberOfDataPoints);
        } else {
            ret = getMeasurementDataForSiblingResources(subject, resourceIds, measurementDefinitionId, beginTime,
                endTime, numberOfDataPoints);
        }

        return ret;
    }

    public List<MetricDisplaySummary> getMetricDisplaySummariesForMetrics(Subject subject, int resourceId,
        DataType dataType, long begin, long end, boolean narrowed, boolean enabledOnly) throws MeasurementException {
        List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();

        List<MeasurementSchedule> scheds = scheduleManager.getMeasurementSchedulesForResourceAndType( // TODO only get ids
            subject, resourceId, dataType, null, enabledOnly); //null -> don't filter, we want everything

        List<Integer> scheduleIds = new ArrayList<Integer>(scheds.size());

        for (MeasurementSchedule schedule : scheds) {
            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, begin, end, narrowed);
            if (summary != null) {
                summary.setResourceId(resourceId);
                summaries.add(summary);
                scheduleIds.add(schedule.getId());
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(begin, end, scheduleIds);
        for (MetricDisplaySummary sum : summaries) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return summaries;
    }

    /**
     * Return a map of &lt;resource id, List&lt;MetricDisplaySummary&gt;&gt;, where the list contains the
     * {@link MetricDisplaySummary} for the (enabled) schedules of the resource
     *
     * @param subject        Subject of the caller
     * @param resourceTypeId ResourceTypeId of the child resources
     * @param parentId       ID of the common parent resource
     * @param resourceIds    List of primary keys of the resources we are interested in
     * @param begin          begin time
     * @param end            end time
     */
    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricDisplaySummariesForResourcesAndParent(
        Subject subject, int resourceTypeId, int parentId, List<Integer> resourceIds, long begin, long end) {
        Map<Integer, List<MetricDisplaySummary>> sumMap = new HashMap<Integer, List<MetricDisplaySummary>>();
        if ((parentId <= 0) || (resourceIds == null) || (resourceIds.size() == 0) || (end < begin)) {
            return sumMap;
        }

        /*
         * Get the schedule(ids) for the passed resources and types and stuff them in a MapMap to easier access them
         * afterwards.
         */
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_ENABLED_BY_RESOURCE_IDS_AND_RESOURCE_TYPE_ID);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setParameter("resourceTypeId", resourceTypeId);
        q.setParameter("resourceIds", resourceIds);

        // <schedId, resId, defId>
        List<Object[]> triples = q.getResultList();

        Map<Integer, Map<Integer, Integer>> resDefSchedMap = new HashMap<Integer, Map<Integer, Integer>>();

        List<Integer> scheduleIds = new ArrayList<Integer>(triples.size());
        for (Object[] triple : triples) {
            int sid = (Integer) triple[0];
            scheduleIds.add(sid);
            int res = (Integer) triple[1];
            int def = (Integer) triple[2];
            Map<Integer, Integer> defSchedMap;
            if (!resDefSchedMap.containsKey(res)) {
                defSchedMap = new HashMap<Integer, Integer>();
                resDefSchedMap.put(res, defSchedMap);
            } else {
                defSchedMap = resDefSchedMap.get(res);
            }

            defSchedMap.put(def, sid);
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(begin, end, scheduleIds);

        List<MeasurementDefinition> definitions = measurementDefinitionManager.getMeasurementDefinitionsByResourceType(
            subject, resourceTypeId, DataType.MEASUREMENT, null);
        Map<Integer, MeasurementDefinition> defMap = new HashMap<Integer, MeasurementDefinition>(definitions.size());
        for (MeasurementDefinition def : definitions) {
            defMap.put(def.getId(), def);
        }

        /*
         * Now that we have the data, loop over the data and fill in the MetricDisplaySummaries.
         */
        for (int resourceId : resourceIds) {
            List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
            if (resDefSchedMap.containsKey(resourceId)) {
                Map<Integer, Integer> defSchedMap = resDefSchedMap.get(resourceId);
                for (int defId : defSchedMap.keySet()) {
                    if (defMap.get(defId) == null) {
                        // This is not a DataType.MEASUREMENT type measurement
                        continue;
                    }

                    int sid = defSchedMap.get(defId);
                    MetricDisplaySummary mds = new MetricDisplaySummary();
                    mds.setAlertCount(alerts.get(sid));
                    mds.setBeginTimeFrame(begin);
                    mds.setEndTimeFrame(end);
                    mds.setDefinitionId(defId);
                    mds.setLabel(defMap.get(defId).getDisplayName());
                    mds.setParentId(parentId);
                    mds.setChildTypeId(resourceTypeId);
                    summaries.add(mds);
                }
            }

            sumMap.put(resourceId, summaries);
        }

        return sumMap;
    }

    /**
     * TODO hwr document me !!!
     */
    @Deprecated
    public List<MetricDisplaySummary> getMetricDisplaySummariesForSchedules(Subject subject, int resourceId,
        List<Integer> scheduleIds, long beginTime, long endTime, boolean narrowed) throws MeasurementException {
        List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
        for (int scheduleId : scheduleIds) {
            MeasurementSchedule schedule = scheduleManager.getMeasurementScheduleById(subject, scheduleId);
            if (schedule == null)
                continue;

            MetricDisplaySummary summary = getMetricDisplaySummary(schedule, beginTime, endTime, narrowed);
            if (summary != null) {
                summary.setResourceId(resourceId);
                summaries.add(summary);
            }
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);
        for (MetricDisplaySummary sum : summaries) {
            sum.setAlertCount(alerts.get(sum.getScheduleId()));
        }

        return summaries;
    }

    /**
     * get a display summary for the passed schedule
     *
     * @param narrowed if true, don't obtain the metrical values  NOTE: alertCounts need to be set by the caller.
     */
    private MetricDisplaySummary getMetricDisplaySummary(MeasurementSchedule schedule, long begin, long end,
        boolean narrowed) throws MeasurementException {
        MetricDisplaySummary summary = new MetricDisplaySummary();
        summary.setScheduleId(schedule.getId());
        summary.setBeginTimeFrame(begin);
        summary.setEndTimeFrame(end);

        MeasurementDefinition definition = schedule.getDefinition();
        summary.setDefinitionId(definition.getId());
        summary.setLabel(definition.getDisplayName());
        summary.setDescription(definition.getDescription());
        summary.setMetricSource(schedule.getResource().getName());

        List<Resource> resources = new ArrayList<Resource>(1);
        resources.add(schedule.getResource());
        //      summary.setAlertCount( alertManager.getAlertCountByMeasurementDefinitionAndResources(
        //               definition.getId(), resources, begin, end));

        switch (definition.getDataType()) {
        case MEASUREMENT: {
            MeasurementUnits units = definition.getUnits();
            summary.setUnits(units.name());

            NumericType type = definition.getNumericType();
            if (type == null) {
                throw new IllegalStateException("NumericType is null, but no traits are expected here");
            }

            int collectionType;
            switch (type) {
            case DYNAMIC: {
                collectionType = 0;
                break;
            }

            case TRENDSUP: {
                collectionType = 2;
                break;
            }

            case TRENDSDOWN: {
                collectionType = 3;
                break;
            }

            default: {
                throw new IllegalStateException("Unknown measurement type: " + type);
            }
            }

            summary.setCollectionType(collectionType);

            if (narrowed == false) {
                MeasurementAggregate compositeHighLow = getAggregate(schedule, begin, end);
                if (compositeHighLow.isEmpty()) {
                    summary.setValuesPresent(false);
                }

                Map<String, MetricDisplayValue> metricValues = new HashMap<String, MetricDisplayValue>();
                metricValues.put(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(compositeHighLow.getMin()));
                metricValues.put(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(compositeHighLow.getAvg()));
                metricValues.put(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(compositeHighLow.getMax()));
                MeasurementDataNumeric currentNumeric = getCurrentNumericForSchedule(schedule.getId());
                Double lastValue = (currentNumeric != null) ? currentNumeric.getValue() : Double.NaN;
                metricValues.put(MetricDisplayConstants.LAST_KEY, new MetricDisplayValue(lastValue));
                summary.setMetrics(metricValues);
            }

            break;
        }

        case TRAIT: {
            summary.setIsTrait(true);
            MeasurementDataTrait trait = getCurrentTraitForSchedule(schedule.getId());
            if (trait != null) {
                summary.setValue(trait.getValue());
                summary.setTimestamp(trait.getTimestamp());
            } else {
                summary.setValue("- nothing found -");
                summary.setTimestamp(System.currentTimeMillis());
            }

            break;
        }

        case CALLTIME: {
            // TODO: ignore?
            break;
        }

        default: {
            throw new IllegalStateException("Unsupported metric data type: " + definition.getDataType());
        }
        }

        return summary;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getMetricDisplaySummariesForAutoGroup(org.jboss.on.domain.auth.Subject,
     * int, int, int[], long, long)
     */
    public List<MetricDisplaySummary> getMetricDisplaySummariesForAutoGroup(Subject subject,
        int autoGroupParentResourceId, int autoGroupChildResourceTypeId, int[] measurementDefinitionIds, long begin,
        long end, boolean enabledOnly) throws MeasurementException {
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, autoGroupParentResourceId,
            autoGroupChildResourceTypeId);
        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject, resources,
            measurementDefinitionIds, begin, end, enabledOnly);
        for (MetricDisplaySummary tmp : ret) {
            tmp.setParentId(autoGroupParentResourceId);
            tmp.setChildTypeId(autoGroupChildResourceTypeId);
        }

        return ret;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getMetricDisplaySummariesForCompatibleGroup(org.jboss.on.domain.auth.Subject,
     * int, int[], long, long)
     */
    public List<MetricDisplaySummary> getMetricDisplaySummariesForCompatibleGroup(Subject subject, int groupId,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException {
        ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE);
        Set<Resource> resources = group.getExplicitResources();
        List<Resource> resList = new ArrayList<Resource>(resources.size());
        resList.addAll(resources);

        List<MetricDisplaySummary> ret = getAggregateMetricDisplaySummaries(subject, resList, measurementDefinitionIds,
            begin, end, enabledOnly);
        for (MetricDisplaySummary tmp : ret) {
            tmp.setGroupId(groupId);
        }

        return ret;
    }

    public Map<MeasurementDefinition, List<MetricDisplaySummary>> getMetricDisplaySummariesForMetricsCompare(
        Subject subject, Integer[] resourceIds, int[] definitionIds, long begin, long end) throws MeasurementException {
        // Getting all the Resource objects in one call, and caching here for the rest of this method
        PageList<Resource> resources = resourceManager.getResourceByIds(subject, resourceIds, true, PageControl
            .getUnlimitedInstance());

        // I want to only get the definition objects once for each ID, and cache here for the rest of this method
        Map<Integer, MeasurementDefinition> measurementDefinitionsMap = new HashMap<Integer, MeasurementDefinition>(
            definitionIds.length);
        List<Integer> listOfDefinitionIds = new ArrayList<Integer>();
        for (int definitionId : definitionIds) {
            if (isMetricCollecting(subject, resources, definitionId)) {
                listOfDefinitionIds.add(definitionId);
            }
        }

        Integer[] defIds = listOfDefinitionIds.toArray(new Integer[listOfDefinitionIds.size()]);

        List<MeasurementDefinition> definitions = measurementDefinitionManager.getMeasurementDefinitionsByIds(subject,
            defIds);
        for (MeasurementDefinition definition : definitions) {
            measurementDefinitionsMap.put(definition.getId(), definition);
        }

        Map<MeasurementDefinition, List<MetricDisplaySummary>> compareMetrics = new HashMap<MeasurementDefinition, List<MetricDisplaySummary>>();
        for (Resource resource : resources) {
            List<MetricDisplaySummary> resourceMDS = getMetricDisplaySummariesForResource(subject, resource.getId(),
                definitionIds, begin, end);
            for (MetricDisplaySummary summary : resourceMDS) {
                Integer definitionId = summary.getDefinitionId();
                MeasurementDefinition definition = measurementDefinitionsMap.get(definitionId);

                // This isn't a collecting metric, so move along
                if (definition == null) {
                    continue;
                }

                summary.setResource(resource);
                summary.setParent(resource.getParentResource());
                summary.setUnits(definition.getUnits().getName());
                summary.setDescription(definition.getDescription());
                summary.setLabel(definition.getDisplayName());
                summary.setMetricSource(definition.getResourceType().getName());

                List<MetricDisplaySummary> metricsForDefinition = compareMetrics.get(definition);
                if (metricsForDefinition == null) {
                    metricsForDefinition = new ArrayList<MetricDisplaySummary>();
                    compareMetrics.put(definition, metricsForDefinition);
                }

                metricsForDefinition.add(summary);
            }
        }

        return compareMetrics;
    }

    private boolean isMetricCollecting(Subject subject, List<Resource> resources, int definitionId) {
        boolean isCollecting = false;
        List<MeasurementSchedule> schedules = scheduleManager.getMeasurementSchedulesByDefinitionIdAndResources(
            subject, definitionId, resources);
        for (MeasurementSchedule schedule : schedules) {
            if (schedule.isEnabled()) {
                isCollecting = true;
                break;
            }
        }

        return isCollecting;
    }

    /**
     * Get the group display summaries for the passed resources that belong to a (auto)group
     *
     * @param  subject
     * @param  resources
     * @param  measurementDefinitionIds
     * @param  begin
     * @param  end
     * @param  enabledOnly              do we want to restrict this to enabled metrics?
     *
     * @return
     *
     * @throws MeasurementException
     */
    private List<MetricDisplaySummary> getAggregateMetricDisplaySummaries(Subject subject, List<Resource> resources,
        int[] measurementDefinitionIds, long begin, long end, boolean enabledOnly) throws MeasurementException {

        List<MetricDisplaySummary> data = new ArrayList<MetricDisplaySummary>(measurementDefinitionIds.length);

        // nothing to do, as we have no resources in that group
        if (resources.isEmpty()) {
            return data;
        }
        if (measurementDefinitionIds == null || measurementDefinitionIds.length == 0) {
            return data;
        }

        MeasurementDataManagerUtility dataUtil = MeasurementDataManagerUtility.getInstance(rhqDs);

        // Loop over the definitions, find matching schedules and create a MetricDisplaySummary for each definition
        for (int definitionId : measurementDefinitionIds) {
            int collecting = 0;
            List<MeasurementSchedule> schedules = scheduleManager.getMeasurementSchedulesByDefinitionIdAndResources(
                subject, definitionId, resources);
            int[] scheduleIds = new int[schedules.size()];
            for (int i = 0; i < schedules.size(); i++) {
                MeasurementSchedule schedule = schedules.get(i);
                if (schedule.isEnabled()) {
                    collecting++;
                }

                scheduleIds[i] = schedule.getId();
            }

            /*
             * If no metric is collecting, stop here as we have nothing to contribute
             */
            if ((collecting == 0) && enabledOnly) {
                if (log.isTraceEnabled()) {
                    log.trace("There were no schedules enabled for definition id " + definitionId);
                }

                continue;
            }

            MetricDisplaySummary summary = new MetricDisplaySummary();

            // See MetricsDisplay.jsp for wrt the next two lines.
            summary.setNumberCollecting(collecting);
            summary.setShowNumberCollecting(true);

            summary.setDefinitionId(definitionId);
            summary.setBeginTimeFrame(begin);
            summary.setAlertCount(alertManager.getAlertCountByMeasurementDefinitionAndResources(definitionId,
                resources, begin, end));
            summary.setEndTimeFrame(end);

            MeasurementDefinition definition = entityManager.find(MeasurementDefinition.class, definitionId);
            summary.setDefinitionId(definition.getId());
            summary.setUnits(definition.getUnits().getName());
            summary.setDescription(definition.getDescription());
            summary.setLabel(definition.getDisplayName());
            summary.setDescription(definition.getDescription());
            summary.setMetricSource(definition.getResourceType().getName());

            /*
             * Get the aggregate data from the backend and check if it is empty or not. If it is empty (for all members
             * of the group), skip over this metric.
             */
            MeasurementAggregate aggregate;
            if (scheduleIds == null || scheduleIds.length == 0) {
                aggregate = new MeasurementAggregate(null, null, null);
                log.warn("No metric schedules found for def=[" + definition + "] and resources [" + resources
                    + "], using empty aggregate");
            } else {
                aggregate = dataUtil.getAggregateByScheduleIds(begin, end, scheduleIds);
            }
            if (aggregate.isEmpty()) {
                if (log.isTraceEnabled()) {
                    log.trace("There was no measurement data available for schedules " + Arrays.toString(scheduleIds)
                        + " in the timeframe [" + new Date(begin) + ", " + new Date(end) + "]");
                }

                summary.setValuesPresent(false);
            }

            Map<String, MetricDisplayValue> metricValues = new HashMap<String, MetricDisplayValue>();
            metricValues.put(MetricDisplayConstants.MIN_KEY, new MetricDisplayValue(aggregate.getMin()));
            metricValues.put(MetricDisplayConstants.AVERAGE_KEY, new MetricDisplayValue(aggregate.getAvg()));
            metricValues.put(MetricDisplayConstants.MAX_KEY, new MetricDisplayValue(aggregate.getMax()));

            // TODO put the sum back on - JBNADM-2626
            //metricValues.put(MetricDisplayConstants.SUMMARY_KEY, new MetricDisplayValue(aggregate.getSum()));
            summary.setMetrics(metricValues);

            // TODO what else do we need ?

            data.add(summary);
        }

        return data;
    }

    /* (non-Javadoc)
     * @see
     * org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal#getNarrowedMetricsDisplaySummaryForCompGroup(org.jboss.on.domain.auth.Subject,
     * int)
     */
    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricsDisplaySummaryForCompGroup(Subject subject,
        ResourceGroup group, long beginTime, long endTime) {
        group = entityManager.merge(group);
        Set<Resource> resources = group.getExplicitResources();

        Map<Integer, List<MetricDisplaySummary>> resMap = getNarrowedMetricDisplaySummaryForCompatibleResources(
            subject, resources, beginTime, endTime);

        // loop over the map entries and set the group Id on each list element
        for (List<MetricDisplaySummary> summaries : resMap.values()) {
            for (MetricDisplaySummary sum : summaries) {
                sum.setGroupId(group.getId());
            }
        }

        return resMap;
    }

    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricsDisplaySummaryForAutoGroup(Subject subject,
        int parentId, int cType, long beginTime, long endTime) {
        List<Resource> resources = resourceGroupManager.getResourcesForAutoGroup(subject, parentId, cType);
        Set<Resource> resSet = new HashSet<Resource>(resources.size());

        Map<Integer, List<MetricDisplaySummary>> resMap = getNarrowedMetricDisplaySummaryForCompatibleResources(
            subject, resSet, beginTime, endTime);

        // loop over the map entries and set the group Id on each list element
        for (List<MetricDisplaySummary> summaries : resMap.values()) {
            for (MetricDisplaySummary sum : summaries) {
                sum.setChildTypeId(cType);
                sum.setParentId(parentId);
            }
        }

        return resMap;
    }

    /**
     * Get the {@link MetricDisplaySummary}s for the resources passed in, that all need to be of the same
     * {@link ResourceType}. Summaries only contain a basic selection of fields for the purpose of filling the Child
     * resource popups.
     */
    public Map<Integer, List<MetricDisplaySummary>> getNarrowedMetricDisplaySummaryForCompatibleResources(
        Subject subject, Collection<Resource> resources, long beginTime, long endTime) {
        Map<Integer, List<MetricDisplaySummary>> resMap = new HashMap<Integer, List<MetricDisplaySummary>>();

        if ((resources == null) || (resources.size() == 0)) {
            return resMap;
        }

        /*
         * Get the resource type and make sure all resources are of the same type
         */
        Iterator<Resource> it = resources.iterator();
        ResourceType type = it.next().getResourceType();
        boolean found = false;
        while (it.hasNext()) {
            ResourceType tmp = it.next().getResourceType();
            if (tmp != type) {
                found = true;
                break;
            }
        }

        if (found) {
            throw new IllegalArgumentException("Resources were of different type: " + resources);
        }

        Set<MeasurementDefinition> defs = type.getMetricDefinitions();

        // get all schedules that are collecting (=enabled)
        Query q = entityManager.createNamedQuery(MeasurementSchedule.FIND_ENABLED_BY_RESOURCES_AND_RESOURCE_TYPE);
        q.setFlushMode(FlushModeType.COMMIT);
        q.setParameter("resourceType", type);
        q.setParameter("resources", resources);
        q.setParameter("dataType", DataType.MEASUREMENT);

        // <schedId, resId, defId>
        List<Object[]> schedules = q.getResultList();

        Map<Integer, Map<Integer, Integer>> resDefSchedMap = new HashMap<Integer, Map<Integer, Integer>>();

        List<Integer> scheduleIds = new ArrayList<Integer>(schedules.size());
        for (Object[] sched : schedules) {
            int sid = (Integer) sched[0];
            scheduleIds.add(sid);
            int res = (Integer) sched[1];
            int def = (Integer) sched[2];
            Map<Integer, Integer> defSchedMap;
            if (!resDefSchedMap.containsKey(res)) {
                defSchedMap = new HashMap<Integer, Integer>();
                resDefSchedMap.put(res, defSchedMap);
            } else {
                defSchedMap = resDefSchedMap.get(res);
            }

            defSchedMap.put(def, sid);
        }

        Map<Integer, Integer> alerts = alertManager.getAlertCountForSchedules(beginTime, endTime, scheduleIds);

        /*
         * Loop over the resources and populate the map with the schedules for the definitions we have There won't be a
         * schedule for each combination, as the list above only contains schedules that are actually collecting. Also
         * if the schedule is not collecting, we don't need to add it to the result.
         */
        for (Resource res : resources) {
            List<MetricDisplaySummary> summaries = new ArrayList<MetricDisplaySummary>();
            for (MeasurementDefinition def : defs) {
                MetricDisplaySummary sum = new MetricDisplaySummary();
                sum.setDefinitionId(def.getId());
                sum.setLabel(def.getDisplayName());
                sum.setBeginTimeFrame(beginTime);
                sum.setEndTimeFrame(endTime);

                int resId = res.getId();
                if (resDefSchedMap.containsKey(resId)) {
                    Map<Integer, Integer> defSched = resDefSchedMap.get(resId);
                    if (defSched.containsKey(def.getId())) {
                        int sid = defSched.get(def.getId());
                        sum.setScheduleId(sid);
                        sum.setAlertCount(alerts.get(sid));
                        summaries.add(sum);
                    }
                }
            }

            resMap.put(res.getId(), summaries);
        }

        return resMap;
    }

    /**
     * Return the Traits for the passed resource. This method will for each trait only return the 'youngest' entry. If
     * there are no traits found for that resource, an empty list is returned. If displayType is null, no displayType is
     * honoured, else the traits will be filtered for the given displayType
     *
     * @param  resourceId  Id of the resource we are interested in
     * @param  displayType A display type for filtering or null for all traits.
     *
     * @return a List of MeasurementDataTrait
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public List<MeasurementDataTrait> getCurrentTraitsForResource(int resourceId, DisplayType displayType) {
        Query query;
        List<Object[]> qres;

        if (displayType == null) {
            //         query = entityManager.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE);
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE, new OrderingField("d.displayOrder", PageOrdering.ASC));
        } else {
            query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                MeasurementDataTrait.FIND_CURRENT_FOR_RESOURCE_AND_DISPLAY_TYPE, new OrderingField("d.displayOrder",
                    PageOrdering.ASC));
            query.setParameter("displayType", displayType);
        }

        query.setParameter("resourceId", resourceId);
        qres = query.getResultList();

        /*
         * Now that we have everything from the query (it returns a tuple <MeasurementDataTrait,DislayName> of the
         * definition), lets create the method output.
         */
        List<MeasurementDataTrait> result = new ArrayList<MeasurementDataTrait>(qres.size());
        for (Object[] objs : qres) {
            MeasurementDataTrait mdt = fillMeasurementDataTraitFromObjectArray(objs);
            result.add(mdt);
        }

        if (log.isDebugEnabled()) {
            log.debug("getCurrentTraitsForResource(" + resourceId + ") -> result is " + result);
        }

        return result;
    }

    /**
     * Helper to fill the name of the trait from the passed array into the MeasurementDataTrait object. The input is a
     * tuple [MeasurementDataTrait,String name, Short displayOrder].
     *
     * @param  objs Tuple {@link MeasurementDataTrait},String,Short
     *
     * @return {@link MeasurementDataTrait} where the name property is set. Or null if the input was null.
     */
    private MeasurementDataTrait fillMeasurementDataTraitFromObjectArray(Object[] objs) {
        if (objs == null) {
            return null;
        }

        MeasurementDataTrait mdt = (MeasurementDataTrait) objs[0];
        String name = (String) objs[1];

        mdt.setName(name);
        return mdt;
    }

    /**
     * Return the current trait value for the passed schedule
     *
     * @param  scheduleId id of a MeasurementSchedule that 'points' to a Trait
     *
     * @return One trait or null if nothing was found in the db.
     */
    @Nullable
    public MeasurementDataTrait getCurrentTraitForSchedule(int scheduleId) {
        Query q = entityManager.createNamedQuery(MeasurementDataTrait.FIND_CURRENT_FOR_SCHEDULES);
        q.setParameter("scheduleIds", Collections.singletonList(scheduleId));
        Object[] res;
        try {
            res = (Object[]) q.getSingleResult();
            MeasurementDataTrait trait = fillMeasurementDataTraitFromObjectArray(res);

            return trait;
        } catch (NoResultException nre) {
            if (log.isDebugEnabled()) {
                log.debug("No current trait data for schedule with id [" + scheduleId + "] found");
            }

            return null;
        }
    }

    @Nullable
    public MeasurementDataNumeric getCurrentNumericForSchedule(int scheduleId) {
        return MeasurementDataManagerUtility.getInstance(rhqDs).getLatestValueForSchedule(scheduleId);
    }

    /**
     * Return all known trait data for the passed schedule, defined by resourceId and definitionId
     *
     * @param  resourceId   PK of a {@link Resource}
     * @param  definitionId PK of a {@link MeasurementDefinition}
     *
     * @return a List of {@link MeasurementDataTrait} objects.
     */
    @SuppressWarnings("unchecked")
    public List<MeasurementDataTrait> getAllTraitDataForResourceAndDefinition(int resourceId, int definitionId) {
        Query q = entityManager.createNamedQuery(MeasurementDataTrait.FIND_ALL_FOR_RESOURCE_AND_DEFINITION);
        q.setParameter("resourceId", resourceId);
        q.setParameter("definitionId", definitionId);
        List<Object[]> queryResult = q.getResultList();

        List<MeasurementDataTrait> result = new ArrayList<MeasurementDataTrait>(queryResult.size());

        for (Object[] objs : queryResult) {
            MeasurementDataTrait mdt = fillMeasurementDataTraitFromObjectArray(objs);
            result.add(mdt);
        }

        return result;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, MeasurementData[] data) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(data);

        log.debug(callingMethod + ": " + stats.toString());
    }
}