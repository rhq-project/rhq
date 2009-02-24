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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementDataPK;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;

/**
 * Manager bean for Out-of-Bound measurements.
 *
 * @author Heiko W. Rupp
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementOOBManagerBean implements MeasurementOOBManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementOOBManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;

    /**
     * Compute oobs from the values in the 1h measurement table that just got added.
     * For the total result, this is an incremental computation. The idea is that
     * it gets run *directly* after the 1h compression (and the baseline recalculation too)
     * @param subject Subject of the caller
     * @param begin Start time of the 1h entries to look at
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void computeOOBsFromHourBeginingAt(Subject subject, long begin) {

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            log.info("Calculating OOBs for hour " + new Date(begin));
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof PostgresqlDatabaseType)
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_QUERY_POSTGRES);
            else if (dbType instanceof OracleDatabaseType)
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_QUERY_ORACLE);
            else
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);

            stmt.setLong(1, begin);
            stmt.setLong(2, begin);
            stmt.setLong(3, begin);
            long t0 = System.currentTimeMillis();
            int count = stmt.executeUpdate();
            long t1 = System.currentTimeMillis();
            log.info("Done calculating OOBs. [" + count + "] new entries in [" + (t1 - t0) + "] ms");
        } catch (SQLException e) {
            log.error(e);
        } catch (Exception e) {
            log.error(e);
        } finally {
            JDBCUtil.safeClose(conn, stmt, null);
        }
    }

    /**
     * Computes the OOBs for the last hour.
     * This is done by getting the latest timestamp of the 1h table and invoking
     * #computeOOBsFromHourBeginingAt
     * @param subject Caller
     */
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
     * Remove old OOB entries from the database
     * @param subject Subject of the caller
     * @param end oldest value to keep
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void removeOldOOBs(Subject subject, long end) {

        if (log.isDebugEnabled())
            log.debug("Removing OOBs older than " + new Date(end));
        Query q = entityManager.createQuery("DELETE FROM MeasurementOOB mo WHERE mo.id.timestamp < :time");
        q.setParameter("time", end);
        long t0 = System.currentTimeMillis();
        int count = q.executeUpdate();
        long t1 = System.currentTimeMillis();
        log.info("Removed [" + count + "] old OOB entries in [" + (t1 - t0) + "] ms");
    }

    /**
     * Remove OOBs for schedules that had their baselines calculated after
     * a certain cutoff point. This is used to get rid of outdated OOB data for
     * baselines that got recalculated, as the new baselines will be 'big' enough for
     * what have been OOBs before and we don't have any baseline history.
     * @param subject The caller
     * @param cutoffTime The reference time to determine new baselines
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void removeOutdatedOObs(Subject subject, long cutoffTime) {

        Query q = entityManager.createNamedQuery(MeasurementOOB.DELETE_OUTDATED);
        q.setParameter("cutOff", cutoffTime);
        int count = q.executeUpdate();
        log.info("Removed [" + count + "] outdated OOBs");
    }

    /**
     * Remove all OOB data for the passed schedule
     * @param subject Caller
     * @param sched the schedule for which we want to clean out the data
     */
    public void removeOOBsForSchedule(Subject subject, MeasurementSchedule sched) {
        Query q = entityManager.createQuery("DELETE FROM MeasurementOOB o WHERE o.id.scheduleId = :id");
        q.setParameter("id", sched.getId());
        q.executeUpdate();
    }

    /**
     * Return OOB Composites that contain all information about the OOBs in a given time as aggregates.
     * @param subject The caller
     * @param end end time we are interested in
     * @param pc PageControl to do pagination
     * @return List of schedules with the corresponing oob aggregates
     */
    public PageList<MeasurementOOBComposite> getSchedulesWithOOBs(Subject subject, long end, PageControl pc) {

        pc.initDefaultOrderingField("max(o.oobFactor)", PageOrdering.DESC);

        long begin = end - (3L * 86400L * 1000L);

        Query queryCount = entityManager.createNamedQuery(MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE_COUNT);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE, pc);
        queryCount.setParameter("begin", begin);
        queryCount.setParameter("end", end);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        query.setParameter("resourceId", null);

        List<MeasurementOOBComposite> results = query.getResultList();
        long totalCount = queryCount.getResultList().size();

        if (!results.isEmpty()) {
            //  add 24h and 48h factors
            Map<Integer, MeasurementOOBComposite> map = new HashMap<Integer, MeasurementOOBComposite>(results.size());
            List<Integer> scheduleIds = new ArrayList<Integer>(results.size());
            List<MeasurementDataPK> pks = new ArrayList<MeasurementDataPK>(results.size());
            Map<MeasurementDataPK, MeasurementOOBComposite> map2 = new HashMap<MeasurementDataPK, MeasurementOOBComposite>();
            for (MeasurementOOBComposite comp : results) {
                scheduleIds.add(comp.getScheduleId());
                map.put(comp.getScheduleId(), comp);
                MeasurementDataPK key = new MeasurementDataPK(comp.getTimestamp(), comp.getScheduleId());
                map2.put(key, comp);
                pks.add(key);

            }

            //  add outlier data
            List<MeasurementDataNumeric1H> datas = getOneHourDataForPKs(pks);
            for (MeasurementDataNumeric1H data : datas) {
                MeasurementDataPK pk = new MeasurementDataPK(data.getTimestamp(), data.getScheduleId());
                MeasurementOOBComposite comp = map2.get(pk);
                comp.setData(data);
                comp.calculateOutlier();
            }

            begin = end - (2L * 86400L * 1000L);

            Query q = entityManager.createNamedQuery(MeasurementOOB.GET_FACTOR_FOR_SCHEDULES);
            q.setParameter("schedules", scheduleIds);
            q.setParameter("begin", begin);
            q.setParameter("end", end);
            List<Object[]> ret = q.getResultList();

            for (Object[] objs : ret) {
                Integer id = (Integer) objs[0];
                Integer fac = (Integer) objs[1];
                Double avg = (Double) objs[2];
                map.get(id).setFactor48(fac.intValue());
                map.get(id).setAvg48(avg.intValue());
            }

            begin = end - (2L * 86400L * 1000L);

            q = entityManager.createNamedQuery(MeasurementOOB.GET_FACTOR_FOR_SCHEDULES);
            q.setParameter("schedules", scheduleIds);
            q.setParameter("begin", begin);
            q.setParameter("end", end);
            ret = q.getResultList();

            for (Object[] objs : ret) {
                Integer id = (Integer) objs[0];
                Integer fac = (Integer) objs[1];
                Double avg = (Double) objs[2];
                map.get(id).setFactor24(fac.intValue());
                map.get(id).setAvg24(avg.intValue());
            }
        }

        return new PageList<MeasurementOOBComposite>(results, (int) totalCount, pc);
    }

    /**
     * Returns the highest n OOBs for the passed resource id within the last 72h
     * @param subject caller
     * @param end end time
     * @param resourceId the resource we are interested in
     * @param n max number of entries wanted
     * @return
     */
    public PageList<MeasurementOOBComposite> getHighestNOOBsForResource(Subject subject, long end, int resourceId, int n) {

        PageControl pc = new PageControl(0, n);
        pc.initDefaultOrderingField("o.oobFactor", PageOrdering.DESC);
        pc.addDefaultOrderingField("o.id.timestamp", PageOrdering.DESC);

        long begin = end - (3L * 86400L * 1000L);
        if (begin < 0)
            begin = 0;

        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            MeasurementOOB.GET_HIGHEST_FACTORS_FOR_RESOURCE, pc);
        query.setParameter("begin", begin);
        query.setParameter("end", end);
        query.setParameter("resourceId", resourceId);

        List<MeasurementOOBComposite> results = query.getResultList();

        if (!results.isEmpty()) {
            // we have the n OOBs, so lets fetch the MeasurementData for those
            List<MeasurementDataPK> pks = new ArrayList<MeasurementDataPK>(results.size());
            Map<MeasurementDataPK, MeasurementOOBComposite> map = new HashMap<MeasurementDataPK, MeasurementOOBComposite>();
            for (MeasurementOOBComposite comp : results) {
                MeasurementDataPK key = new MeasurementDataPK(comp.getTimestamp(), comp.getScheduleId());
                pks.add(key);
                map.put(key, comp);
            }
            // compute and add the outlier data
            List<MeasurementDataNumeric1H> datas = getOneHourDataForPKs(pks);
            for (MeasurementDataNumeric1H data : datas) {
                MeasurementDataPK pk = new MeasurementDataPK(data.getTimestamp(), data.getScheduleId());
                MeasurementOOBComposite comp = map.get(pk);
                comp.setData(data);
                comp.calculateOutlier();
            }
        }
        // return the result
        PageList<MeasurementOOBComposite> result = new PageList<MeasurementOOBComposite>(results, n, pc);

        return result;

    }

    /**
     * Return the 1h numeric data for the passed primary keys (schedule, timestamp)
     * @param pks Primary keys to look up
     * @return List of 1h data
     */
    private List<MeasurementDataNumeric1H> getOneHourDataForPKs(List<MeasurementDataPK> pks) {

        Query q = entityManager.createQuery("SELECT data FROM MeasurementDataNumeric1H data WHERE data.id IN (:pks)");
        q.setParameter("pks", pks);
        List<MeasurementDataNumeric1H> res = q.getResultList();

        return res;
    }

}
