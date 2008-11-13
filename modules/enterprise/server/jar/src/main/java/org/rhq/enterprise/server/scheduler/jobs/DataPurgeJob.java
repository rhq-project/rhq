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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;

import org.rhq.enterprise.server.legacy.common.shared.HQConstants;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementCompressionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDataManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.TimingVoodoo;

/**
 * This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't want multiple data purge jobs performing the data purge at the same time.
 */
public class DataPurgeJob implements StatefulJob {
    private static final Log LOG = LogFactory.getLog(DataPurgeJob.class);

    private static long HOUR = 60 * 60 * 1000L;

    /**
     * Schedules a purge job to trigger right now. This will not block - it schedules the job to trigger but immediately
     * returns. This method will ensure that no two data purge jobs will execute at the same time (Quartz will ensure
     * this since {@link DataPurgeJob} is an implementation of {@link StatefulJob}).
     *
     * @throws Exception if failed to schedule the data purge for immediate execution
     */
    public static void purgeNow() throws Exception {
        // there should always be a DataPurgeJob defined with a job name as the same as this class' name
        // let's trigger that job now.  this ensures the job is only ever run once, never concurrently
        // note that you can't call this method again until the data purge job finished; otherwise,
        // you'll get an exception saying there is already a trigger defined - this is what we want, you
        // shouldn't ask for more than one data purge job to execute now - you have to wait for it to finish.
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        SimpleTrigger trigger = new SimpleTrigger("DataPurgeJobNow", DataPurgeJob.class.getName());
        trigger.setJobName(DataPurgeJob.class.getName());
        trigger.setJobGroup(DataPurgeJob.class.getName());
        scheduler.scheduleJob(trigger);
    }

    /**
     * Public interface for quartz
     */
    public void execute(JobExecutionContext context) throws JobExecutionException {
        long start = System.currentTimeMillis();
        LOG.info("Data Purge Job STARTING");
        compressData();
        calculateAutoBaselines();
        LOG.info("Data Purge Job FINISHED [" + (System.currentTimeMillis() - start) + "]ms");
    }

    private void calculateAutoBaselines() {
        try {
            LOG.info("Initiating the calculation of auto-baselines");
            LookupUtil.getMeasurementBaselineManager().calculateAutoBaselines();
        } catch (Exception e) {
            LOG.error("Failed to auto-calculate baselines. Cause: " + e);
        }
    }

    private void compressData() {
        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        MeasurementCompressionManagerLocal compressionManager = LookupUtil.getMeasurementCompressionManager();
        MeasurementDataManagerLocal measurementDataManager = LookupUtil.getMeasurementDataManager();
        AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

        // COMPRESS MEASUREMENT DATA
        long timeStart = System.currentTimeMillis();
        LOG.info("Measurement data compression starting at " + new Date(timeStart));

        try {
            compressionManager.compressData();
        } catch (Exception e) {
            LOG.error("Unable to compress measurement data: " + e, e);
        }

        long timeEnd = System.currentTimeMillis();
        LOG.info("Measurement data compression completed in [" + (timeEnd - timeStart) + "]ms");

        Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration();

        // PURGE OLD TRAIT DATA
        timeStart = System.currentTimeMillis();
        LOG.info("Trait data purge starting at " + new Date(timeStart));
        int traitsPurged = 0;

        try {
            long threshold;
            String traitPurgeThresholdStr = systemConfig.getProperty(HQConstants.TraitPurge);
            if (traitPurgeThresholdStr == null) {
                threshold = timeStart - (1000L * 60 * 60 * 24 * 365);
                LOG.debug("No purge traits threshold found - will purge traits older than one year");
            } else {
                threshold = timeStart - Long.parseLong(traitPurgeThresholdStr);
            }

            LOG.info("Purging traits that are older than " + new Date(threshold));
            traitsPurged = measurementDataManager.purgeTraits(threshold);
        } catch (Exception e) {
            LOG.error("Unable to purge trait data: " + e, e);
        }

        timeEnd = System.currentTimeMillis();
        LOG.info("Traits data purged [" + traitsPurged + "] - completed in [" + (timeEnd - timeStart) + "]ms");

        // PURGE OLD AVAILABILITY DATA
        timeStart = System.currentTimeMillis();
        LOG.info("Availability data purge starting at " + new Date(timeStart));
        int availsPurged = 0;

        try {
            long threshold;
            String availPurgeThresholdStr = systemConfig.getProperty(HQConstants.AvailabilityPurge);
            if (availPurgeThresholdStr == null) {
                threshold = timeStart - (1000L * 60 * 60 * 24 * 365);
                LOG.debug("No purge avails threshold found - will purge availabilities older than one year");
            } else {
                threshold = timeStart - Long.parseLong(availPurgeThresholdStr);
            }
            LOG.info("Purging availablities that are older than " + new Date(threshold));
            availsPurged = availabilityManager.purgeAvailabilities(threshold);
        } catch (Exception e) {
            LOG.error("Unable to purge availability data: " + e, e);
        }

        timeEnd = System.currentTimeMillis();
        LOG.info("Availability data purged [" + availsPurged + "] - completed in [" + (timeEnd - timeStart) + "]ms");

        // Once compression finishes, we check to see if database maintenance
        // should be performed.  This is defaulted to 1 hour, so it should
        // always run unless changed by the user.  This is only a safeguard,
        // as usually an ANALYZE only takes a fraction of what a full VACUUM
        // takes.
        //
        // VACUUM will occur every day at midnight.

        String dataMaintenance = systemConfig.getProperty(HQConstants.DataMaintenance);
        if (dataMaintenance == null) {
            LOG.error("No data maintenance interval found - will not perform db maintenance");
            return;
        }

        long maintInterval = Long.parseLong(dataMaintenance);

        // At midnight we always perform a VACUUM, otherwise we check to see if it is time to
        // perform normal database maintenance. (On postgres we just rebuild indices using an ANALYZE)
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) == 0) {
            LOG.info("Performing database maintenance (VACUUM ANALYZE)");
            timeStart = System.currentTimeMillis();

            systemManager.vacuum(LookupUtil.getSubjectManager().getOverlord());

            String reindexStr = systemConfig.getProperty(HQConstants.DataReindex);
            boolean reindexNightly = Boolean.valueOf(reindexStr);
            if (reindexNightly) {
                LOG.info("Re-indexing data tables...");
                systemManager.reindex(LookupUtil.getSubjectManager().getOverlord());
            }

            LOG.info("Database maintenance (VACUUM ANALYZE) completed in [" + (System.currentTimeMillis() - timeStart)
                + "]ms");
        } else if (TimingVoodoo.roundDownTime(timeStart, HOUR) == TimingVoodoo.roundDownTime(timeStart, maintInterval)) {
            LOG.info("Performing database maintenance (ANALYZE)");
            timeStart = System.currentTimeMillis();
            systemManager.analyze(LookupUtil.getSubjectManager().getOverlord());
            LOG
                .info("Database maintenance (ANALYZE) completed in [" + (System.currentTimeMillis() - timeStart)
                    + "]ms");
        } else {
            LOG.debug("Not performing any database maintenance now");
        }
    }
}