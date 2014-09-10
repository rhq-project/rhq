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

package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Collections;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * This implements {@link org.quartz.StatefulJob} (as opposed to {@link org.quartz.Job}) because we do not need nor want
 * this job triggered concurrently. That is, we don't want multiple data calc jobs performing the data calc at the
 * same time.
 *
 * The work done in this job used to be performed as part of {@link DataPurgeJob} but now, especially since the metric
 * storage and aggregation work is performed against the cassandra storage cluster, and not the RDB, it's been split
 * away to be executed independently.  Moreover, there is really no reason to wait for all of the DB maintenance and
 * unrelated data purge before performing out metric calculations. Also, see [BZ 1125439].
 *
 * @author Jay Shaughnessy
 */
public class DataCalcJob extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(DataCalcJob.class);

    /**
     * Schedules a calc job to trigger right now. This will not block - it schedules the job to trigger but immediately
     * returns. This method will ensure that no two data calc jobs will execute at the same time (Quartz will ensure
     * this since {@link DataCalcJob} is an implementation of {@link org.quartz.StatefulJob}).
     *
     * @throws Exception if failed to schedule the data calc for immediate execution
     */
    public static void calcNow() throws Exception {
        // there should always be a DataCalcJob defined with a job name as the same as this class' name
        // let's trigger that job now.  this ensures the job is only ever run once, never concurrently
        // note that you can't call this method again until the data calc job finished; otherwise,
        // you'll get an exception saying there is already a trigger defined - this is what we want, you
        // shouldn't ask for more than one data calc job to execute now - you have to wait for it to finish.
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        SimpleTrigger trigger = new SimpleTrigger("DataCalcJobNow", DataCalcJob.class.getName());
        trigger.setJobName(DataCalcJob.class.getName());
        trigger.setJobGroup(DataCalcJob.class.getName());
        scheduler.scheduleJob(trigger);
    }

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        long timeStart = System.currentTimeMillis();
        LOG.info("Data Calc Job STARTING");

        try {
            Iterable<AggregateNumericMetric> oneHourAggregates = compressMeasurementData();
            calculateAutoBaselines(LookupUtil.getMeasurementBaselineManager());
            calculateOOBs(oneHourAggregates);
        } catch (Exception e) {
            LOG.error("Data Calc Job FAILED TO COMPLETE. Cause: " + e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Data Calc Job FINISHED [" + duration + "]ms");
        }
    }

    private Iterable<AggregateNumericMetric> compressMeasurementData() {
        long timeStart = System.currentTimeMillis();
        LOG.info("Measurement data compression starting at " + new Date(timeStart));

        try {
            StorageClientManager storageClientManager = LookupUtil.getStorageClientManager();
            MetricsServer metricsServer = storageClientManager.getMetricsServer();
            return metricsServer.calculateAggregates();
        } catch (Exception e) {
            LOG.error("Failed to compress measurement data. Cause: " + e, e);
            return Collections.emptyList();
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Measurement data compression completed in [" + duration + "]ms");
        }
    }

    private void calculateAutoBaselines(MeasurementBaselineManagerLocal measurementBaselineManager) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Auto-calculation of baselines starting at " + new Date(timeStart));

        try {
            measurementBaselineManager.calculateAutoBaselines();
        } catch (Exception e) {
            LOG.error("Failed to auto-calculate baselines. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Auto-calculation of baselines completed in [" + duration + "]ms");
        }
    }

    /**
     * Calculate the OOB values for the last hour.
     * This also removes out-dated ones due to recalculated baselines.
     */
    public void calculateOOBs(Iterable<AggregateNumericMetric> oneHourAggregates) {
        LOG.info("Auto-calculation of OOBs starting");

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        MeasurementOOBManagerLocal manager = LookupUtil.getOOBManager();
        PurgeManagerLocal purgeManager = LookupUtil.getPurgeManager();
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();

        // purge OOBs whose baseline just got recalculated
        // For now just assume that our system is fast, so a cutoff of 30mins is ok,
        // as the calculate baseline job runs hourly
        long cutOff = System.currentTimeMillis() - (30L * 60L * 1000L);

        long timeStart = System.currentTimeMillis();

        purgeManager.removeOutdatedOOBs(cutOff);

        // clean up
        systemManager.vacuum(overlord, new String[] { "RHQ_MEASUREMENT_OOB" });

        // Now calculate the fresh OOBs
        manager.computeOOBsForLastHour(overlord, oneHourAggregates);

        long duration = System.currentTimeMillis() - timeStart;

        LOG.info("Auto-calculation of OOBs completed in [" + duration + "]ms");
    }
}
