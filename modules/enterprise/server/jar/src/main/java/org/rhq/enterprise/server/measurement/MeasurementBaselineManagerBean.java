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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * A manager for {@link MeasurementBaseline}s.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Joseph Marques
 */
@Stateless
public class MeasurementBaselineManagerBean implements MeasurementBaselineManagerLocal {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @EJB
    private StatusManagerLocal agentStatusManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private MeasurementDataManagerLocal dataManager;
    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;
    @EJB
    private MeasurementBaselineManagerLocal measurementBaselineManager; // self
    @EJB
    private MeasurementOOBManagerLocal oobManager;
    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    private SubjectManagerLocal subjectManager;

    private final Log log = LogFactory.getLog(MeasurementBaselineManagerBean.class);

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void calculateAutoBaselines() {
        Properties conf = systemManager.getSystemConfiguration();

        // frequency is how often the baselines are recalculated
        // data set is how far back for a particular scheduled measurement is included in the baseline calcs
        // frequency of 3 days and data set of 10 days means "every 3 days, recalculate baselines automatically.
        // For each scheduled measurement, take their last 10 days worth of data and use that data set
        // as the portion that will be used to get the min/max/average".
        String baselineFrequencyString = conf.getProperty(RHQConstants.BaselineFrequency);
        String baselineDataSetString = conf.getProperty(RHQConstants.BaselineDataSet);
        String baselineLastCalcTimeString = conf.getProperty(RHQConstants.BaselineLastCalculationTime);

        log.debug("Found baseline defaults: " + "frequency=" + baselineFrequencyString + " dataset="
            + baselineDataSetString + " last-calc-time=" + baselineLastCalcTimeString);

        // see if baseline auto-calculations is disabled; if so, return immediately and do nothing
        long frequency = Long.parseLong(baselineFrequencyString);
        if (frequency <= 0) {
            log.debug("System was configured to never auto-calculate baselines, so not calculating them now");
            return;
        }

        // see if its time to auto-calculate the baselines; if not, return immediately and do nothing
        long now = System.currentTimeMillis();
        long lastCalcTime = Long.parseLong(baselineLastCalcTimeString);
        long timeToNextCalc = now - (lastCalcTime + frequency);

        if (timeToNextCalc < 0) {
            long minutes = (-timeToNextCalc) / (1000L * 60);
            long hours = minutes / 60L;
            if (hours > 2) {
                log.debug("Not time yet to auto-calculate baselines - [" + hours + "] hours more to go");
            } else {
                log.debug("Not time yet to auto-calculate baselines - [" + minutes + "] minutes more to go");
            }

            return;
        }

        // Its time to auto-calculate the baselines again.
        // Determine how much data we need to calculate baselines for by determining the oldest and youngest
        // measurement data to include in the calculations.
        long dataSet = Long.parseLong(baselineDataSetString);
        long endTime = now;
        long startTime = endTime - dataSet;

        long computeTime = measurementBaselineManager.calculateAutoBaselines(startTime, endTime);

        // everything was calculated successfully, remember this time
        conf = systemManager.getSystemConfiguration(); // reload the config in case it was changed since we started
        conf.setProperty(RHQConstants.BaselineLastCalculationTime, String.valueOf(computeTime));
        systemManager.setSystemConfiguration(subjectManager.getOverlord(), conf, true);

        log.info("Auto-calculation of baselines done. Next scheduled for " + new Date(computeTime + frequency));
    }

    // this is potentially really long, don't put this method in a transaction, each individual step will be its own tx
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public long calculateAutoBaselines(long startTime, long endTime) {
        try {
            log.info("Calculating auto baselines");
            long computeTime = System.currentTimeMillis();

            log.debug("startTime = " + startTime);
            log.debug("endTime = " + endTime);
            log.debug("computeTime = " + computeTime);

            long now = System.currentTimeMillis();
            int deleted = measurementBaselineManager._calculateAutoBaselinesDELETE(startTime, endTime);
            log.info("Removed [" + deleted + "] old baselines - they will now be recalculated ("
                + (System.currentTimeMillis() - now) + ")ms");

            now = System.currentTimeMillis();
            int inserted = measurementBaselineManager._calculateAutoBaselinesINSERT(startTime, endTime, computeTime);
            log.info("Calculated and inserted [" + inserted + "] new baselines. (" + (System.currentTimeMillis() - now)
                + ")ms");

            MeasurementMonitor.getMBean().incrementBaselineCalculationTime(System.currentTimeMillis() - computeTime);

            agentStatusManager.updateByAutoBaselineCalculationJob();

            return computeTime;
        } catch (Exception e) {
            log.error("Failed to auto-calculate baselines", e);
            throw new RuntimeException("Auto-calculation failure", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    //@TransactionTimeout( 60 * 60 )
    public int _calculateAutoBaselinesDELETE(long startTime, long endTime) throws Exception {
        Connection conn = null;
        PreparedStatement deleteQuery = null;

        try {
            // delete all existing baselines that we plan on re-calculating
            // do everything via JDBC - our perf testing shows that we hit entity cache locking timeouts
            // when the entity manager performs native queries under heavy load
            conn = dataSource.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof PostgresqlDatabaseType) {
                deleteQuery = conn
                    .prepareStatement(MeasurementBaseline.NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_POSTGRES);
                deleteQuery.setLong(1, startTime);
                deleteQuery.setLong(2, endTime);
                deleteQuery.setLong(3, startTime);
            } else {
                deleteQuery = conn
                    .prepareStatement(MeasurementBaseline.NATIVE_QUERY_DELETE_EXISTING_AUTOBASELINES_ORACLE);
                deleteQuery.setLong(1, startTime);
                deleteQuery.setLong(2, endTime);
                deleteQuery.setLong(3, startTime);
            }

            int deleted = deleteQuery.executeUpdate();
            return deleted;
        } finally {
            if (deleteQuery != null) {
                try {
                    deleteQuery.close();
                } catch (Exception e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    //@TransactionTimeout( 60 * 60 )
    public int _calculateAutoBaselinesINSERT(long startTime, long endTime, long computeTime) throws Exception {
        Connection conn = null;
        PreparedStatement insertQuery = null;

        try {
            // calculate the baselines for schedules that have no baseline yet (or were just deleted)
            // do everything via JDBC - our perf testing shows that we hit entity cache locking timeouts
            // when the entity manager performs native queries under heavy load
            conn = dataSource.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof PostgresqlDatabaseType) {
                insertQuery = conn.prepareStatement(MeasurementBaseline.NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_POSTGRES);
                insertQuery.setLong(1, computeTime);
                insertQuery.setLong(2, startTime);
                insertQuery.setLong(3, endTime);
                insertQuery.setLong(4, startTime);
            } else {
                insertQuery = conn.prepareStatement(MeasurementBaseline.NATIVE_QUERY_CALC_FIRST_AUTOBASELINE_ORACLE);
                insertQuery.setLong(1, computeTime);
                insertQuery.setLong(2, startTime);
                insertQuery.setLong(3, endTime);
                insertQuery.setLong(4, startTime);
            }

            int inserted = insertQuery.executeUpdate();
            return inserted;
        } finally {
            if (insertQuery != null) {
                try {
                    insertQuery.close();
                } catch (Exception e) {
                }
            }

            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    //@TransactionTimeout( 60 * 60 )
    private int _calculateAutoBaselinesDELETE_HQL(long startTime, long endTime) throws Exception {
        Query query = entityManager.createNamedQuery(MeasurementBaseline.QUERY_DELETE_EXISTING_AUTOBASELINES);

        query.setParameter("startTime", startTime);
        query.setParameter("endTime", endTime);

        int rowsModified = query.executeUpdate();

        return rowsModified;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    //@TransactionTimeout( 60 * 60 )
    private int _calculateAutoBaselinesINSERT_HQL(long startTime, long endTime, long computeTime) throws Exception {
        Query query = entityManager.createNamedQuery(MeasurementBaseline.QUERY_CALC_FIRST_AUTOBASELINE);

        //query.setParameter("computeTime", computeTime);
        query.setParameter("startTime", startTime);
        query.setParameter("endTime", endTime);

        int rowsModified = query.executeUpdate();

        return rowsModified;
    }

    public MeasurementBaseline calculateAutoBaseline(Subject subject, Integer measurementScheduleId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException {
        MeasurementBaseline baseline;
        MeasurementSchedule sched = entityManager.find(MeasurementSchedule.class, measurementScheduleId);

        if (sched != null) {
            Resource resource = sched.getResource();

            // only check permissions if the user is attempting to save a new baseline
            if (save
                && !authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, resource
                    .getId())) {
                log.error("Cannot calculate baseline - permission denied. " + "resource=" + resource + "; user="
                    + subject + "; perm=" + Permission.MANAGE_MEASUREMENTS);
                throw new PermissionException("Cannot calculate baseline - you do not have permission on this resource");
            }
        } else {
            throw new MeasurementNotFoundException("Scheduled measurement [" + measurementScheduleId + "] not found");
        }

        try {
            baseline = calculateBaseline(sched, true, startDate, endDate, save);
            // We have changed the baseline information for the schedule, so remove the now outdated OOB info.
            oobManager.removeOOBsForSchedule(subject,sched);
        } catch (DataNotAvailableException e) {
            throw new BaselineCreationException("Error fetching data for baseline calculation: "
                + measurementScheduleId);
        }

        return baseline;
    }

    public void enableAutoBaselineCalculation(Subject subject, Integer[] resourceIds, Integer[] definitionIds) {
        // bail out early if there's nothing to do
        if ((resourceIds.length < 1) || (definitionIds.length < 1)) {
            return;
        }

        List<MeasurementBaseline> bList = getBaselinesForResourcesAndDefinitionIds(resourceIds, definitionIds);
        for (MeasurementBaseline bl : bList) {
            if (!authorizationManager.hasResourcePermission(subject, Permission.MANAGE_MEASUREMENTS, bl.getSchedule()
                .getResource().getId())) {
                throw new PermissionException("Cannot enable baseline [" + bl + "] - you do not have permission");
            }

            bl.setUserEntered(false);
        }
    }

    @SuppressWarnings("unchecked")
    public List<MeasurementBaseline> findBaselinesForResource(Resource res) {
        Query q = entityManager.createNamedQuery(MeasurementBaseline.QUERY_FIND_BY_RESOURCE);
        q.setParameter("resourceId", res.getId());
        List<MeasurementBaseline> ret = q.getResultList();
        return ret;
    }

    public MeasurementBaseline findBaselineForResourceAndMeasurementDefinition(Subject subject, Integer resourceId,
        Integer measurementDefinitionId) {
        if (!authorizationManager.canViewResource(subject, resourceId)) {
            throw new PermissionException("Cannot view the baseline for resourceId=" + resourceId
                + "] - you do not have permission");
        }

        List<MeasurementBaseline> baselines = getBaselinesForResourcesAndDefinitionIds(new Integer[] { resourceId },
            new Integer[] { measurementDefinitionId });
        if ((baselines != null) && (baselines.size() > 0)) {
            return baselines.get(0);
        }

        Subject overlord = subjectManager.getOverlord();
        try {
            MeasurementSchedule schedule = measurementScheduleManager.getMeasurementSchedule(overlord,
                measurementDefinitionId, resourceId, true);

            /*
             * Use all available data from the epoch until now to calculate the baseline (we don't need to start from
             * the epoch, because the baseline should have been auto-calculated after a few days, but it's a catch-all)
             */
            MeasurementBaseline baseline = calculateAutoBaseline(overlord, schedule.getId(), 0, System
                .currentTimeMillis(), true);

            return baseline;
        } catch (MeasurementNotFoundException mnfe) {
            log.error("Could not find measurement schedule for " + "resourceId=" + resourceId + ", "
                + "measurementDefinitionId=" + measurementDefinitionId, mnfe);
        } catch (BaselineCreationException bce) {
            log.error("Could not calculate baseline for " + "resourceId=" + resourceId + ", "
                + "measurementDefinitionId=" + measurementDefinitionId, bce);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<MeasurementBaseline> getBaselinesForResourcesAndDefinitionIds(Integer[] resourceIds,
        Integer[] definitionIds) {
        Query q = entityManager.createNamedQuery(MeasurementBaseline.QUERY_FIND_BY_RESOURCE_IDS_AND_DEF_IDS);
        q.setParameter("resourceIds", Arrays.asList(resourceIds));
        q.setParameter("definitionIds", Arrays.asList(definitionIds));
        List<MeasurementBaseline> bList = q.getResultList();
        return bList;
    }

    private MeasurementBaseline calculateBaseline(MeasurementSchedule schedule, boolean userEntered, long startDate,
        long endDate, boolean save) throws DataNotAvailableException, BaselineCreationException {
        /*
         * jmarques: 2007-10-26
         *
         * navigation from schedule to definition is safe here because the only caller to this method is
         * calculateAutoBaseline( Subject, Integer, long, long, boolean ), which uses entityManager.find, so the
         * schedule should still be attached since the transaction is propagated to this method
         */
        if (schedule.getDefinition().getNumericType() != NumericType.DYNAMIC) {
            throw new BaselineCreationException("Baseline calculation is only valid for a dynamic measurement");
        }

        MeasurementAggregate agg;

        try {
            agg = dataManager.getAggregate(schedule, startDate, endDate);
        } catch (MeasurementException e) {
            throw new DataNotAvailableException(e);
        }

        // attach the entity, so we can find the baseline
        schedule = entityManager.merge(schedule);

        MeasurementBaseline baseline = null;
        if (save && (schedule.getBaseline() != null)) {
            /*
             * If saving, make sure we're updating the existing one, if it exists
             */
            baseline = schedule.getBaseline();
        } else {
            /*
             * Otherwise, if we're not saving or if the the schedule doesn't have a current baseline, we create a new
             * baseline object
             */
            baseline = new MeasurementBaseline();

            if (save) {
                /*
                 * But, if we *are* in save mode, then set the relationship so when we merge the schedule below it
                 * persists this new baseline too
                 */
                baseline.setSchedule(schedule);
            }
        }

        baseline.setUserEntered(userEntered);
        baseline.setMean(agg.getAvg());
        baseline.setMin(agg.getMin());
        baseline.setMax(agg.getMax());

        if (save) {
            entityManager.persist(baseline);
            entityManager.merge(schedule);

            notifyAlertConditionCacheManager("calculateBaseline", baseline);
        }

        return baseline;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, MeasurementBaseline baseline) {
        agentStatusManager.updateByMeasurementBaseline(baseline.getId());

        log.debug("Invoking... " + callingMethod);
    }
}