/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import com.datastax.driver.core.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.H2DatabaseType;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.db.SQLServerDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.util.QueryUtility;
import org.rhq.server.metrics.MetricsDAO;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.StorageResultSetFuture;
import org.rhq.server.metrics.domain.AggregateNumericMetric;
import org.rhq.server.metrics.domain.AggregateNumericMetricMapper;
import org.rhq.server.metrics.domain.Bucket;

/**
 * Manager bean for Out-of-Bound measurements.
 *
 * @author Heiko W. Rupp
 */
@Stateless
public class MeasurementOOBManagerBean implements MeasurementOOBManagerLocal {

    private static final int BATCH_SIZE = 500;
    private final Log log = LogFactory.getLog(MeasurementOOBManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource rhqDs;

    @EJB
    AuthorizationManagerLocal authMangager;

    @EJB
    MeasurementOOBManagerLocal oobManager;

    @EJB
    StorageClientManager storageManager;

    /**
     * Compute oobs from the values in the 1h measurement table that just got added.
     * For the total result, this is an incremental computation. The idea is that
     * it gets run *directly* after the 1h compression (and the baseline recalculation too).
     *
     * Algorithm is as follows:
     * <ul>
     * <li> insert new values in tmp table
     * <li> update real table with max (tmp table, real table)
     * <li> insert items from tmp table that were not in real table
     * <li> tuncate tmp table
     * </ul>
     *
     * @param subject Subject of the caller
     * @param begin Start time of the 1h entries to look at
     */
    @Override
    public void computeOOBsFromHourBeginingAt(Subject subject, long begin) {

        Connection conn = null;
        PreparedStatement stmt = null;
        List<Long> timings = new ArrayList<Long>();

        try {
            log.info("Calculating OOBs for hour starting at " + new Date(begin));
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDefaultDatabaseType();

            long t0 = System.currentTimeMillis();
            long tstart = t0;

            // first truncate tmp table
            log.debug("Truncating tmp table");
            //            stmt = conn.prepareStatement(MeasurementOOB.TRUNCATE_TMP_TABLE);
            stmt = conn.prepareStatement("DELETE FROM RHQ_MEASUREMENT_OOB_TMP");
            stmt.executeUpdate();
            long t1 = System.currentTimeMillis();
            timings.add((t1 - t0));
            log.debug("Truncating the tmp table done");

            String theQuery;

            // Compute the OOBs and put them in the tmp table
            if (dbType instanceof PostgresqlDatabaseType || dbType instanceof H2DatabaseType)
                theQuery = MeasurementOOB.INSERT_QUERY.replace("%TRUE%", "true");
            else if (dbType instanceof OracleDatabaseType || dbType instanceof SQLServerDatabaseType)
                theQuery = MeasurementOOB.INSERT_QUERY.replace("%TRUE%", "1");
            else
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);

            stmt = conn.prepareStatement(theQuery);

            stmt.setLong(1, begin);
            stmt.setLong(2, begin);
            stmt.setLong(3, begin);
            int count = stmt.executeUpdate();
            t1 = System.currentTimeMillis();
            log.debug("Calculation of OOBs done");
            timings.add((t1 - t0));
            t0 = t1;

            // Update the real table from the tmp table
            if (dbType instanceof PostgresqlDatabaseType) {
                stmt = conn.prepareStatement(MeasurementOOB.UPDATE_MASTER_POSTGRES);
                stmt.executeUpdate();
                t1 = System.currentTimeMillis();
                timings.add((t1 - t0));
                log.debug("Update of master table done");
                t0 = t1;

                // Insert missing ones
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_NEW_ONES);
                stmt.executeUpdate();
                t1 = System.currentTimeMillis();
                timings.add((t1 - t0));
                log.debug("Insert of new oobs done");
            } else if (dbType instanceof OracleDatabaseType) {
                stmt = conn.prepareStatement(MeasurementOOB.MERGE_TABLES_ORACLE);
                stmt.executeUpdate();
                t1 = System.currentTimeMillis();
                timings.add((t1 - t0));
                log.debug("Merge of master table done");
            } else if (dbType instanceof H2DatabaseType || dbType instanceof SQLServerDatabaseType) {
                stmt = conn.prepareStatement(MeasurementOOB.UPDATE_MASTER_GENERIC);
                stmt.executeUpdate();
                t1 = System.currentTimeMillis();
                timings.add((t1 - t0));
                log.debug("Update of master table done");
                t0 = t1;

                // Insert missing ones
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_NEW_ONES);
                stmt.executeUpdate();
                t1 = System.currentTimeMillis();
                timings.add((t1 - t0));
                log.debug("Insert of new oobs done");
            } else
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);

            log.info("Done calculating OOBs. [" + count + "] entries in [" + (t1 - tstart) + "] ms (" + timings + ")");
        } catch (SQLException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
    public void computeOOBsForLastHour(Subject subject, Iterable<AggregateNumericMetric> metrics) {
        log.info("Computing OOBs");
        int count = 0;
        long startTime = System.currentTimeMillis();

        try {
            Iterator<AggregateNumericMetric> iterator = metrics.iterator();
            while (iterator.hasNext()) {

                List<Integer> scheduleIds = new ArrayList<Integer>(BATCH_SIZE);
                List<AggregateNumericMetric> metricList = new ArrayList<AggregateNumericMetric>(BATCH_SIZE);

                int i = 0;
                do {
                    AggregateNumericMetric aggregate = iterator.next();
                    scheduleIds.add(aggregate.getScheduleId());
                    metricList.add(aggregate);
                    i++;
                } while (i < BATCH_SIZE && iterator.hasNext());

                Query q  = entityManager.createNamedQuery(MeasurementBaseline.QUERY_BY_SCHEDULE_IDS);
                q.setParameter("scheduleIds",scheduleIds);
                List<MeasurementBaseline> tmpList = q.getResultList();

                // put the result in a HashMap to speed up query later
                Map<Integer,MeasurementBaseline> baselineMap = new HashMap<Integer, MeasurementBaseline>(tmpList.size());
                for (MeasurementBaseline baseline : tmpList) {
                    baselineMap.put(baseline.getScheduleId(),baseline);
                }

                for (AggregateNumericMetric metric : metricList) {
                    MeasurementBaseline baseline = baselineMap.get(metric.getScheduleId());
                    if (baseline==null)
                        continue;

                    try {
                        count += oobManager.calculateOOB(metric,baseline);
                    } catch (Exception e) {
                        log.error("An error occurred while calculating OOBs for " + metric, e);
                        throw new RuntimeException(e);
                    }

                }
            }
        } finally {
            long endTime = System.currentTimeMillis();
            if (log.isInfoEnabled()) {
                log.info("Finished calculating " + count + " OOBs in " + (endTime - startTime) + " ms");
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public int calculateOOB(AggregateNumericMetric metric,MeasurementBaseline baseline) {
        if (baseline==null) {
            return 0;
        }

        Long upperDelta = null;
        Long lowerDelta = null;

        if (isPastUpperBound(baseline, metric)) {
            upperDelta =
                Math.round(((metric.getMax() - baseline.getMax()) / (baseline.getMax() - baseline.getMin())) * 100);
        }

        if (isPastLowerBound(baseline, metric)) {
            lowerDelta =
                Math.round(((baseline.getMin() - metric.getMin()) / (baseline.getMax() - baseline.getMin())) * 100);
        }

        Integer oobFactor;
        if (upperDelta != null && lowerDelta == null) {
            oobFactor = upperDelta.intValue();
        } else if (upperDelta == null && lowerDelta != null) {
            oobFactor = lowerDelta.intValue();
        } else if (upperDelta != null && lowerDelta != null) {
            if (upperDelta > lowerDelta) {
                oobFactor = upperDelta.intValue();
            } else {
                oobFactor = lowerDelta.intValue();
            }
        } else { // both are null
            oobFactor = null;
        }

        if (oobFactor != null) {
            MeasurementOOB oob = new MeasurementOOB();
            oob.setScheduleId(metric.getScheduleId());
            oob.setTimestamp(metric.getTimestamp());
            oob.setOobFactor(oobFactor);

            if (log.isDebugEnabled()) {
                log.debug("Generated OOB " + oob + " for 1 hr metric " + metric + " with baseline " + baseline);
            }

            entityManager.merge(oob);
            return 1;
        }

        return 0;
    }

    private boolean isPastUpperBound(MeasurementBaseline baseline, AggregateNumericMetric metric) {
        return metric.getAvg() > baseline.getMax() &&
            (baseline.getMax() - baseline.getMin() > 0.1) &&
            (metric.getMax() - baseline.getMax() > 0);
    }

    private boolean isPastLowerBound(MeasurementBaseline baseline, AggregateNumericMetric metric) {
        return metric.getAvg() < baseline.getMax() &&
            (baseline.getMax() - baseline.getMin() > 0.1) &&
            (baseline.getMin() - metric.getMin()) > 0;
    }

    /**
     * Computes the OOBs for the last hour.
     * This is done by getting the latest timestamp of the 1h table and invoking
     * #computeOOBsFromHourBeginingAt
     * @param subject Caller
     */
    @Override
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void computeOOBsFromLastHour(Subject subject) {

        Query q = entityManager.createNamedQuery(MeasurementDataNumeric1H.GET_MAX_TIMESTAMP);
        Object res = q.getSingleResult();
        if (res == null) {
            if (log.isDebugEnabled())
                log.debug("No data yet in 1h table, nothing to do");
            return; // no data in that table yet - nothing to do.
        }
        long timeStamp = (Long) res;

        // check if we did this already (because the server did not get data for > 1h
        q = entityManager.createNamedQuery(MeasurementOOB.COUNT_FOR_DATE);
        q.setParameter("timestamp", timeStamp);
        Long count = (Long) q.getSingleResult();

        if (count == 0)
            computeOOBsFromHourBeginingAt(subject, timeStamp);
        else
            log.info("Calculation of OOBs already done for hour " + new Date(timeStamp));

    }

    /**
     * Resets the OOB data for the passed schedule
     * @param subject Caller
     * @param sched the schedule for which we want to clean out the data
     */
    @Override
    public void removeOOBsForSchedule(Subject subject, MeasurementSchedule sched) {
        Query q = entityManager.createNamedQuery(MeasurementOOB.DELETE_FOR_SCHEDULE);
        q.setParameter("id", sched.getId());
        q.executeUpdate();
    }

    @Override
    public void removeOOBsForGroupAndDefinition(Subject subject, int resourceGroupId, int measurementDefinitionId) {
        Query query = entityManager.createNamedQuery(MeasurementOOB.DELETE_FOR_GROUP_AND_DEFINITION);
        query.setParameter("groupId", resourceGroupId);
        query.setParameter("definitionId", measurementDefinitionId);
        query.executeUpdate();
    }

    /**
     * Return OOB Composites that contain all information about the OOBs in a given time as aggregates.
     * @param subject The caller
     * @param metricNameFilter a schedule name to filter for
     * @param resourceNameFilter a resource name to filter for
     * @param parentNameFilter a parent resource name to filter for   @return List of schedules with the corresponing oob aggregates
     * @param pc PageControl to do pagination
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<MeasurementOOBComposite> getSchedulesWithOOBs(Subject subject, String metricNameFilter,
        String resourceNameFilter, String parentNameFilter, PageControl pc) {

        pc.initDefaultOrderingField("o.oobFactor", PageOrdering.DESC);

        boolean isAdmin = authMangager.isOverlord(subject) || authMangager.isSystemSuperuser(subject);

        String queryName = isAdmin ? MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_ADMIN
            : MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE;

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName, "sched.id");
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        metricNameFilter = QueryUtility.formatSearchParameter(metricNameFilter);
        resourceNameFilter = QueryUtility.formatSearchParameter(resourceNameFilter);
        parentNameFilter = QueryUtility.formatSearchParameter(parentNameFilter);

        query.setParameter("metricName", metricNameFilter);
        queryCount.setParameter("metricName", metricNameFilter);
        query.setParameter("resourceName", resourceNameFilter);
        queryCount.setParameter("resourceName", resourceNameFilter);
        query.setParameter("parentName", parentNameFilter);
        queryCount.setParameter("parentName", parentNameFilter);
        query.setParameter("escapeChar", QueryUtility.getEscapeCharacter());
        queryCount.setParameter("escapeChar", QueryUtility.getEscapeCharacter());

        if (!isAdmin) {
            query.setParameter("subjectId", subject.getId());
            queryCount.setParameter("subjectId", subject.getId());
        }

        List<MeasurementOOBComposite> results = query.getResultList();
        long totalCount = (Long) queryCount.getSingleResult();

        if (!results.isEmpty()) {

            List<MeasurementDataPK> pks = new ArrayList<MeasurementDataPK>(results.size());
            Map<MeasurementDataPK, MeasurementOOBComposite> map = new HashMap<MeasurementDataPK, MeasurementOOBComposite>();
            for (MeasurementOOBComposite comp : results) {
                MeasurementDataPK key = new MeasurementDataPK(comp.getTimestamp(), comp.getScheduleId());
                map.put(key, comp);
                pks.add(key);

            }

            //  add outlier data
            List<AggregateNumericMetric> datas = getOneHourDataForPKs(pks);
            for (AggregateNumericMetric data : datas) {
                MeasurementDataPK pk = new MeasurementDataPK(data.getTimestamp(), data.getScheduleId());
                MeasurementOOBComposite comp = map.get(pk);
                comp.setDataMin(data.getMin());
                comp.setDataMax(data.getMax());

                comp.calculateOutlier();
            }

        }

        return new PageList<MeasurementOOBComposite>(results, (int) totalCount, pc);
    }

    /**
     * Returns the highest n OOBs for the passed resource id
     * @param subject caller
     * @param resourceId the resource we are interested in
     * @param n max number of entries wanted
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<MeasurementOOBComposite> getHighestNOOBsForResource(Subject subject, int resourceId, int n) {

        if (!authMangager.canViewResource(subject, resourceId)) {
            return new PageList<MeasurementOOBComposite>();
        }

        PageControl pc = new PageControl(0, n);
        pc.addDefaultOrderingField("sched.id");
        pc.addDefaultOrderingField("o.oobFactor", PageOrdering.DESC);

        String queryName = MeasurementOOB.GET_HIGHEST_FACTORS_FOR_RESOURCE;
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, queryName);
        query.setParameter("resourceId", resourceId);
        countQuery.setParameter("resourceId", resourceId);

        List<MeasurementOOBComposite> results = query.getResultList();

        if (!results.isEmpty()) {
            // we have the n OOBs, so lets fetch the MeasurementData for those
            List<MeasurementDataPK> pks = new ArrayList<MeasurementDataPK>(results.size());
            Map<MeasurementDataPK, MeasurementOOBComposite> map = new HashMap<MeasurementDataPK, MeasurementOOBComposite>();
            for (MeasurementOOBComposite comp : results) {
                int schedule = comp.getScheduleId();
                MeasurementDataPK key = new MeasurementDataPK(comp.getTimestamp(), schedule);
                pks.add(key);
                map.put(key, comp);
            }
            // compute and add the outlier data
            List<AggregateNumericMetric> datas = getOneHourDataForPKs(pks);
            for (AggregateNumericMetric data : datas) {
                MeasurementDataPK pk = new MeasurementDataPK(data.getTimestamp(), data.getScheduleId());
                MeasurementOOBComposite comp = map.get(pk);
                comp.setDataMin(data.getMin());
                comp.setDataMax(data.getMax());

                comp.calculateOutlier();
            }
        }
        // return the result
        long totalCount = (Long) countQuery.getSingleResult();
        PageList<MeasurementOOBComposite> result = new PageList<MeasurementOOBComposite>(results, (int) totalCount, pc);

        return result;
    }

    /**
     * Returns the highest n OOBs for the passed group id
     * @param subject caller
     * @param groupId the group we are interested in
     * @param n max number of entries wanted
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public PageList<MeasurementOOBComposite> getHighestNOOBsForGroup(Subject subject, int groupId, int n) {

        if (!authMangager.canViewResource(subject, groupId)) {
            return new PageList<MeasurementOOBComposite>();
        }

        PageControl pc = new PageControl(0, n);
        pc.addDefaultOrderingField("sched.id");
        pc.addDefaultOrderingField("o.oobFactor", PageOrdering.DESC);

        String queryName = MeasurementOOB.GET_HIGHEST_FACTORS_FOR_GROUP;
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);
        Query countQuery = PersistenceUtility.createCountQuery(entityManager, queryName);
        query.setParameter("groupId", groupId);
        countQuery.setParameter("groupId", groupId);

        List<MeasurementOOBComposite> results = query.getResultList();

        if (!results.isEmpty()) {
            // we have the n OOBs, so lets fetch the MeasurementData for those
            List<MeasurementDataPK> pks = new ArrayList<MeasurementDataPK>(results.size());
            Map<MeasurementDataPK, MeasurementOOBComposite> map = new HashMap<MeasurementDataPK, MeasurementOOBComposite>();
            for (MeasurementOOBComposite comp : results) {
                int schedule = comp.getScheduleId();
                MeasurementDataPK key = new MeasurementDataPK(comp.getTimestamp(), schedule);
                pks.add(key);
                map.put(key, comp);
            }
            // compute and add the outlier data
            List<AggregateNumericMetric> datas = getOneHourDataForPKs(pks);
            for (AggregateNumericMetric data : datas) {
                MeasurementDataPK pk = new MeasurementDataPK(data.getTimestamp(), data.getScheduleId());
                MeasurementOOBComposite comp = map.get(pk);
                comp.setDataMin(data.getMin());
                comp.setDataMax(data.getMax());

                comp.calculateOutlier();
            }
        }
        // return the result
        long totalCount = (Long) countQuery.getSingleResult();
        PageList<MeasurementOOBComposite> result = new PageList<MeasurementOOBComposite>(results, (int) totalCount, pc);

        return result;
    }

    /**
     * Return the 1h numeric data for the passed primary keys (schedule, timestamp)
     * @param pks Primary keys to look up
     * @return List of 1h data
     */
    private List<AggregateNumericMetric> getOneHourDataForPKs(List<MeasurementDataPK> pks) {
        MetricsDAO metricsDAO = storageManager.getMetricsDAO();

        List<StorageResultSetFuture> futureResults = new ArrayList<StorageResultSetFuture>();
        for (MeasurementDataPK pk : pks) {
            // technically we could get more than 1 row but in practice this is not going to be the case.
            // there are never going to be 2 measurements for a single schedule persisted within 1 millisecond.
            StorageResultSetFuture f = metricsDAO
                .findAggregateMetricsAsync(pk.getScheduleId(), Bucket.ONE_HOUR, pk.getTimestamp(), pk.getTimestamp() + 1);
            futureResults.add(f);
        }

        AggregateNumericMetricMapper mapper = new AggregateNumericMetricMapper();

        List<AggregateNumericMetric> result = new ArrayList<AggregateNumericMetric>(pks.size());
        for (StorageResultSetFuture f : futureResults) {
            ResultSet r = f.get();

            // this should always get exactly 1 row
            if (!r.isExhausted()) {
                AggregateNumericMetric aggregate = mapper.mapOne(r);
                result.add(aggregate);
            }
        }

        return result;
    }

}
