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

import java.util.ArrayList;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.measurement.MeasurementBaseline;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.storage.StorageClientManagerBean;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.server.metrics.MetricsBaselineCalculator;

/**
 * A manager for {@link MeasurementBaseline}s.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 * @author Joseph Marques
 */
@Stateless
public class MeasurementBaselineManagerBean implements MeasurementBaselineManagerLocal,
    MeasurementBaselineManagerRemote {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

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
    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private StorageClientManagerBean sessionManager;

    private final Log log = LogFactory.getLog(MeasurementBaselineManagerBean.class);

    private static final int BASELINE_PROCESSING_LIMIT = 100;

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void calculateAutoBaselines() {
        Properties conf = systemManager.getSystemConfiguration(subjectManager.getOverlord());

        // frequency is how often the baselines are recalculated
        // data set is how far back for a particular scheduled measurement is included in the baseline calcs
        // frequency of 3 days and data set of 10 days means "every 3 days, recalculate baselines automatically.
        // For each scheduled measurement, take their last 10 days worth of data and use that data set
        // as the portion that will be used to get the min/max/average".
        String baselineFrequencyString = conf.getProperty(RHQConstants.BaselineFrequency);
        String baselineDataSetString = conf.getProperty(RHQConstants.BaselineDataSet);

        log.debug("Found baseline defaults: " + "frequency=" + baselineFrequencyString + " dataset="
            + baselineDataSetString);

        // Its time to auto-calculate the baselines again.
        // Determine how much data we need to calculate baselines for by determining the oldest and youngest
        // measurement data to include in the calculations.
        long amountOfData = Long.parseLong(baselineDataSetString);
        long baselineFrequency = Long.parseLong(baselineFrequencyString);
        if (baselineFrequency==0) {
            log.info("Baseline frequency is set to 0 - not recomputing baselines. Go to Admin->System settings to change this.");
            return;
        }
        long baselinesOlderThanTime = System.currentTimeMillis() - baselineFrequency;

        measurementBaselineManager.calculateAutoBaselines(amountOfData, baselinesOlderThanTime);

        // everything was calculated successfully, remember this time
        conf = systemManager.getSystemConfiguration(subjectManager.getOverlord()); // reload the config in case it was changed since we started
        try {
            systemManager.setSystemConfiguration(subjectManager.getOverlord(), conf, true);
        } catch (Exception e) {
            log.error("Failed to remember the time when we just calc'ed baselines - it may recalculate again soon.", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public long calculateAutoBaselines(long amountOfData, long baselinesOlderThanTime) {
        try {
            log.info("Calculating auto baselines");
            log.info("Deleting baselines computations older than " + new Date(baselinesOlderThanTime));
            log.info("Inserting new baselines using last " + (amountOfData / (24 * 60 * 60 * 1000L))
                + " days of 1H data");
            long now = System.currentTimeMillis();
            long computeTime = now;

            log.debug("computeTime = " + computeTime);

            int deleted = measurementBaselineManager._calculateAutoBaselinesDELETE(baselinesOlderThanTime);
            log.info("Removed [" + deleted + "] old baselines - they will now be recalculated ("
                + (System.currentTimeMillis() - now) + ")ms");

            now = System.currentTimeMillis();
                /*
                 * each call is done in a separate xtn of at most 100K inserted rows; this helps to keep the xtn
                 * shorter to avoid timeouts in scenarios where baseline calculations bunch together. the idea was that
                 * by basing a batch of baseline calculations off of the import time of the resource into inventory,
                 * that the total work would naturally be staggered throughout the day. in practice, this didn't always
                 * work as intended for one of several reasons:
                 *
                 *   1) all servers in the cloud were down for a few days (maybe a slow product upgrade, maybe a cold
                 *      data center relocation)
                 *   2) issues with running the job itself, if quartz had locking issues under severe load and somehow
                 *      this job wasn't get executed for a few hours / days
                 *   3) the user tended to import all new resources / platforms at the same time of day, thus bypassing
                 *      the implicit optimization of trying to stagger the calculations by resource commit time
                 *
                 * 2/18/2010 NOTE: Limits weren't / aren't actually achieving the affect we want.  The baseline query
                 * follows the general form of "insert into...select from <big query> having <subquery> limit X".
                 * In this case, the limit was reducing the number of rows inserted, but it was still taking the full
                 * cost of calculating everything that should have been inserted.  The limit was intended as a cheap
                 * method of chunking or partitioning the work, but wasn't properly chunking the expensive
                 * part - the "big query".  What we actually want to do is come of with a strategy that lessens the
                 * amount of data we need to select, thereby reducing the amount of time it takes to calculate the
                 * insertion list.
                 *
                 * One proposed strategy for this would be to chunk on the scheduleId.  So if there were, say,
                 * 5M scheduleIds in the systems, we might take 500K of them at a time and then execute the
                 * baseline insertion job 10 times against a much smaller set of data each time.  But the
                 * complication here is how to calculate precise groups of 500K schedules at a time, and then
                 * walk that chunked list.
                 *
                 * Another strategy would be to divy things up by resource type. Since a measurementSchedule is
                 * linked to a measurementDefinition which is linked to a resourceType, we could very easily chunk
                 * the insertion based off the schedules that belong to each resourceType.  This would create
                 * one insert statement for each type of resource in system.  The complication here, however,
                 * is that you may have millions of resources of one type, but hardly any resources of another.
                 * So there's still a chance that some insertions proceed slowly (in the worst case).
                 *
                 * In any event, an appropriate chunking solution needs to be found, and that partitioning strategy
                 * needs to replace the limits in the query today.
                 */
            List<Integer> schedulesWithoutBaselines = measurementBaselineManager.getSchedulesWithoutBaselines();

            List<Integer> accumulator = new ArrayList<Integer>();
            for (Integer value : schedulesWithoutBaselines) {
                accumulator.add(value);
                if (accumulator.size() == BASELINE_PROCESSING_LIMIT) {
                    measurementBaselineManager.calculateBaselines(accumulator, now, amountOfData);
                    accumulator.clear();
                }
            }
            if (!accumulator.isEmpty()) {
                measurementBaselineManager.calculateBaselines(accumulator, now, amountOfData);
                accumulator.clear();
            }

            log.info("Calculated and inserted [" + schedulesWithoutBaselines.size() + "] new baselines. ("
                + (System.currentTimeMillis() - now) + ")ms");

            MeasurementMonitor.getMBean().incrementBaselineCalculationTime(System.currentTimeMillis() - computeTime);

            agentStatusManager.updateByAutoBaselineCalculationJob();

            return computeTime;
        } catch (Exception e) {
            log.error("Failed to auto-calculate baselines", e);
            throw new RuntimeException("Auto-calculation failure", e);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public int _calculateAutoBaselinesDELETE(long olderThanTime) throws Exception {
        Query query = entityManager.createNamedQuery(MeasurementBaseline.QUERY_DELETE_BY_COMPUTE_TIME);
        query.setParameter("timestamp", olderThanTime);
        int rowsAffected = query.executeUpdate();
        return rowsAffected;
    }

    @SuppressWarnings("unchecked")
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Integer> getSchedulesWithoutBaselines() {
        final String sql =
            "SELECT s.id FROM rhq_measurement_sched s INNER JOIN rhq_measurement_def d ON s.definition = d.id " +
            "LEFT JOIN rhq_measurement_bline b ON s.id = b.schedule_id WHERE s.enabled = true AND b.schedule_id IS NULL AND d.numeric_type = 0";
        Query query = this.entityManager.createNativeQuery(sql);

        return query.getResultList();
    }

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void calculateBaselines(List<Integer> schedules, long olderThan, long amountOfData) {
        long endTime = olderThan;
        long startTime = endTime - amountOfData;

        log.debug("Computing baselines for " + schedules.size() + " schedules");
        MetricsBaselineCalculator baselineCalculator = new MetricsBaselineCalculator(sessionManager.getMetricsDAO());
        long calcStartTime = System.currentTimeMillis();
        List<MeasurementBaseline> results = baselineCalculator.calculateBaselines(schedules, startTime, endTime);
        long calcEndTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug("Finished computing " + results.size() + " new baselines in " + (calcEndTime - calcStartTime)
                + " ms");
        }

        log.debug("Persisting baselines calculations");
        long saveStartTime = System.currentTimeMillis();

        measurementBaselineManager.saveNewBaselines(results);

        long saveEndTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Finished persisting " + results.size() + " baselines in " + (saveEndTime - saveStartTime)
                + " ms");
        }
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void saveNewBaselines(List<MeasurementBaseline> baselines) {
        for (MeasurementBaseline baseline : baselines) {
            MeasurementSchedule schedule = new MeasurementSchedule();
            schedule.setId(baseline.getScheduleId());
            baseline.setSchedule(schedule);
            this.entityManager.merge(baseline);
        }
    }

    /**
     * If the measurement baselines for the corresponding resources are the same, that value will be returned;
     * otherwise null will be returned
     */
    public MeasurementBaseline getBaselineIfEqual(Subject subject, int groupId, int definitionId) {
        Query query = entityManager.createQuery("" //
            + "SELECT MIN(mb.baselineMin),  MAX(mb.baselineMin), " //
            + "       MIN(mb.baselineMean), MAX(mb.baselineMean), " //
            + "       MIN(mb.baselineMax),  MAX(mb.baselineMax), " //
            + "       COUNT(mb.id) " //
            + "  FROM MeasurementBaseline mb " //
            + "  JOIN mb.schedule ms " //
            + "  JOIN ms.resource res " //
            + "  JOIN res.implicitGroups rg " //
            + " WHERE rg.id = :groupId " //
            + "   AND ms.definition.id = :definitionId ");
        query.setParameter("groupId", groupId);
        query.setParameter("definitionId", definitionId);
        Object[] results = (Object[]) query.getSingleResult();

        MeasurementBaseline baseline = new MeasurementBaseline();
        if ((Long) results[6] == 0) {
            // no baselines calculated yet, return null to indicate that
            return null;
        }

        // there was at least one baseline, so one or more of min/mean/max might be non-null
        if (results[0] == null || results[1] == null) {
            baseline.setMin(null);
        } else if (Math.abs((Double) results[0] - (Double) results[1]) < 1e-9) {
            baseline.setMin((Double) results[0]); // they are close enough to being equal
        } else {
            baseline.setMin(-1.0); // use negative to represent mixed, because we currently don't support graphing negs
        }
        if (results[2] == null || results[3] == null) {
            baseline.setMean(null);
        } else if (Math.abs((Double) results[2] - (Double) results[3]) < 1e-9) {
            baseline.setMean((Double) results[2]); // they are close enough to being equal
        } else {
            baseline.setMean(-1.0); // use negative to represent mixed, because we currently don't support graphing negs
        }
        if (results[4] == null || results[5] == null) {
            baseline.setMax(null);
        } else if (Math.abs((Double) results[4] - (Double) results[5]) < 1e-9) {
            baseline.setMax((Double) results[4]); // they are close enough to being equal
        } else {
            baseline.setMax(-1.0); // use negative to represent mixed, because we currently don't support graphing negs
        }
        return baseline;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public MeasurementBaseline calculateAutoBaseline(Subject subject, Integer measurementScheduleId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException {

        MeasurementBaseline result = measurementBaselineManager.calculateAutoBaselineInNewTransaction(subject,
            measurementScheduleId, startDate, endDate, save);

        if (save) {
            // note, this executes in a new transaction so the baseline must already be committed to the database
            agentStatusManager.updateByMeasurementBaseline(result.getId());
        }

        return result;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public MeasurementBaseline calculateAutoBaselineInNewTransaction(Subject subject, Integer measurementScheduleId,
        long startDate, long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException {

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
            if (save) {
                // We have changed the baseline information for the schedule, so remove the now outdated OOB info.
                oobManager.removeOOBsForSchedule(subject, sched);
            }
        } catch (DataNotAvailableException e) {
            throw new BaselineCreationException(
                "Error fetching data for baseline calculation for measurementSchedule[id=" + measurementScheduleId
                    + "]");
        }

        return baseline;
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public MeasurementBaseline calculateAutoBaseline(Subject subject, int groupId, int definitionId, long startDate,
        long endDate, boolean save) throws BaselineCreationException, MeasurementNotFoundException {

        MeasurementBaseline result = measurementBaselineManager.calculateAutoBaselineForGroupInNewTransaction(subject,
            groupId, definitionId, startDate, endDate, save);

        if (save) {
            // note, this executes in a new transaction so the baseline must already be committed to the database
            agentStatusManager.updateByMeasurementBaseline(result.getId());
        }

        return result;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public MeasurementBaseline calculateAutoBaselineForGroupInNewTransaction(Subject subject, int groupId,
        int definitionId, long startDate, long endDate, boolean save) throws BaselineCreationException,
        MeasurementNotFoundException {

        if (save && !authorizationManager.hasGroupPermission(subject, Permission.MANAGE_MEASUREMENTS, groupId)) {
            throw new PermissionException("User[" + subject.getName()
                + "] does not have permission to calculate and set baselines for group[id=" + groupId + "]");
        }

        MeasurementBaseline baseline;
        try {
            baseline = calculateBaselineForGroup(groupId, definitionId, true, startDate, endDate, save);
            if (save) {
                // We have changed the baseline information for the schedule, so remove the now outdated OOB info.
                oobManager.removeOOBsForGroupAndDefinition(subject, groupId, definitionId);
            }
        } catch (DataNotAvailableException e) {
            throw new BaselineCreationException("Error fetching data for baseline calculation for group[id=" + groupId
                + "], definition[id=" + definitionId + "]");
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

        MeasurementAggregate agg = dataManager.getAggregate(subjectManager.getOverlord(), schedule.getId(), startDate,
            endDate);

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
        }

        return baseline;
    }

    private MeasurementBaseline calculateBaselineForGroup(int groupId, int definitionId, boolean userEntered,
        long startDate, long endDate, boolean save) throws DataNotAvailableException, BaselineCreationException {

        MeasurementAggregate agg = dataManager.getAggregate(subjectManager.getOverlord(), groupId, definitionId,
            startDate, endDate);

        Subject overlord = subjectManager.getOverlord();
        List<Integer> resourceIds = resourceManager.findImplicitResourceIdsByResourceGroup(groupId);
        List<MeasurementSchedule> schedules = measurementScheduleManager.findSchedulesByResourceIdsAndDefinitionId(
            overlord, ArrayUtils.unwrapCollection(resourceIds), definitionId);

        MeasurementBaseline baseline = null;
        for (MeasurementSchedule schedule : schedules) {
            // attach the entity, so we can find the baseline
            schedule = entityManager.merge(schedule);

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
            }
        }

        // all baselines should be the same
        return baseline;
    }

    @SuppressWarnings("unchecked")
    public List<MeasurementBaseline> findBaselinesForResource(Subject subject, int resourceId) {
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User[" + subject.getName()
                + " ] does not have permission to view baselines for resource[id=" + resourceId + "]");
        }

        Query query = entityManager.createNamedQuery(MeasurementBaseline.QUERY_FIND_BY_RESOURCE);
        query.setParameter("resourceId", resourceId);
        List<MeasurementBaseline> results = query.getResultList();
        return results;
    }
}