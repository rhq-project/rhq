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
import java.util.Collections;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;
import org.quartz.StatefulJob;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementBaselineManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.TimingVoodoo;
import org.rhq.server.metrics.MetricsServer;
import org.rhq.server.metrics.domain.AggregateNumericMetric;

/**
 * This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't want multiple data purge jobs performing the data purge at the same time.
 */
public class DataPurgeJob extends AbstractStatefulJob {
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

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        long timeStart = System.currentTimeMillis();
        LOG.info("Data Purge Job STARTING");

        try {
            Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord());
            Iterable<AggregateNumericMetric> oneHourAggregates = compressMeasurementData();
            purgeEverything(systemConfig);
            performDatabaseMaintenance(LookupUtil.getSystemManager(), systemConfig);
            calculateAutoBaselines(LookupUtil.getMeasurementBaselineManager());
            calculateOOBs(oneHourAggregates);
        } catch (Exception e) {
            LOG.error("Data Purge Job FAILED TO COMPLETE. Cause: " + e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Data Purge Job FINISHED [" + duration + "]ms");
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

    private void purgeEverything(Properties systemConfig) {
        purgeEventData(LookupUtil.getEventManager(), systemConfig);
        purgeAlertData(LookupUtil.getAlertManager(), systemConfig);
        purgeUnusedAlertDefinitions(LookupUtil.getAlertDefinitionManager());
        purgeOrphanedAlertConditions(LookupUtil.getAlertConditionManager());
        purgeOrphanedAlertNotifications(LookupUtil.getAlertNotificationManager());
        purgeAvailabilityData(LookupUtil.getAvailabilityManager(), systemConfig);
        purgeOrphanedDriftFiles(LookupUtil.getDriftManager(), systemConfig);
    }

    private void purgeAvailabilityData(AvailabilityManagerLocal availabilityManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Availability data purge starting at " + new Date(timeStart));
        int availsPurged = 0;

        try {
            long threshold;
            String availPurgeThresholdStr = systemConfig.getProperty(RHQConstants.AvailabilityPurge);
            if (availPurgeThresholdStr == null) {
                threshold = timeStart - (1000L * 60 * 60 * 24 * 365);
                LOG.debug("No purge avails threshold found - will purge availabilities older than one year");
            } else {
                threshold = timeStart - Long.parseLong(availPurgeThresholdStr);
            }
            LOG.info("Purging availablities that are older than " + new Date(threshold));
            availsPurged = availabilityManager.purgeAvailabilities(threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge availability data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Availability data purged [" + availsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeEventData(EventManagerLocal eventManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Event data purge starting at " + new Date(timeStart));
        int eventsPurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.EventPurge));
            LOG.info("Purging event data older than " + new Date(threshold));
            eventsPurged = eventManager.purgeEventData(new Date(threshold));
        } catch (Exception e) {
            LOG.error("Failed to purge event data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Event data purged [" + eventsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeAlertData(AlertManagerLocal alertManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Alert data purge starting at " + new Date(timeStart));
        int alertsPurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.AlertPurge));
            LOG.info("Purging alert data older than " + new Date(threshold));
            alertsPurged = alertManager.deleteAlerts(0, threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge alert data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Alert data purged [" + alertsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeUnusedAlertDefinitions(AlertDefinitionManagerLocal alertDefinitionManager) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Alert definition unused purge starting at " + new Date(timeStart));
        int alertDefinitionsPurged = 0;

        try {
            alertDefinitionsPurged = alertDefinitionManager.purgeUnusedAlertDefinitions();
        } catch (Exception e) {
            LOG.error("Failed to purge alert definition data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Alert definitions purged [" + alertDefinitionsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeOrphanedAlertConditions(AlertConditionManagerLocal alertConditionManager) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Alert condition orphan purge starting at " + new Date(timeStart));
        int orphansPurged = 0;

        try {
            orphansPurged = alertConditionManager.purgeOrphanedAlertConditions();
        } catch (Exception e) {
            LOG.error("Failed to purge alert condition data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Purged [" + orphansPurged + "] orphan alert conditions - completed in [" + duration + "]ms");
        }
    }

    private void purgeOrphanedAlertNotifications(AlertNotificationManagerLocal alertNotificationManager) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Alert notification orphan purge starting at " + new Date(timeStart));
        int orphansPurged = 0;

        try {
            orphansPurged = alertNotificationManager.purgeOrphanedAlertNotifications();
        } catch (Exception e) {
            LOG.error("Failed to purge alert notification data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Purged [" + orphansPurged + "] orphan alert notifications - completed in [" + duration + "]ms");
        }
    }

    private void purgeOrphanedDriftFiles(DriftManagerLocal driftManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Drift file orphan purge starting at " + new Date(timeStart));
        int orphansPurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.DriftFilePurge));
            LOG.info("Purging orphaned drift files older than " + new Date(threshold));
            orphansPurged = driftManager.purgeOrphanedDriftFiles(LookupUtil.getSubjectManager().getOverlord(),
                threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge orphaned drift files. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Purged [" + orphansPurged + "] orphaned drift files - completed in [" + duration + "]ms");
        }
    }

    private void performDatabaseMaintenance(SystemManagerLocal systemManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Database maintenance starting at " + new Date(timeStart));

        try {
            // Once compression finishes, we need to check to see if database maintenance
            // should be performed.  This is defaulted to 1 hour, so it should
            // always run unless changed by the user.  This is only a safeguard,
            // as usually an ANALYZE only takes a fraction of what a full VACUUM
            // takes. VACUUM will occur every day at midnight.

            String dataMaintenance = systemConfig.getProperty(RHQConstants.DataMaintenance);
            if (dataMaintenance == null) {
                LOG.error("No data maintenance interval found - will not perform db maintenance");
                return;
            }
            long maintInterval = Long.parseLong(dataMaintenance);

            // At midnight we always perform a VACUUM, otherwise we check to see if it is time to
            // perform normal database maintenance. (On postgres we just rebuild indices using an ANALYZE)
            Calendar cal = Calendar.getInstance();
            if (cal.get(Calendar.HOUR_OF_DAY) == 0) {
                LOG.info("Performing daily database maintenance");
                systemManager.vacuum(LookupUtil.getSubjectManager().getOverlord());

                String reindexStr = systemConfig.getProperty(RHQConstants.DataReindex);
                boolean reindexNightly = Boolean.valueOf(reindexStr);
                if (reindexNightly) {
                    LOG.info("Re-indexing data tables");
                    systemManager.reindex(LookupUtil.getSubjectManager().getOverlord());
                } else {
                    LOG.info("Skipping re-indexing of data tables");
                }
            } else if (TimingVoodoo.roundDownTime(timeStart, HOUR) == TimingVoodoo.roundDownTime(timeStart,
                maintInterval)) {
                LOG.info("Performing hourly database maintenance");
                systemManager.analyze(LookupUtil.getSubjectManager().getOverlord());
            } else {
                LOG.debug("Not performing any database maintenance now");
            }
        } catch (Exception e) {
            LOG.error("Failed to perform database maintenance. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Database maintenance completed in [" + duration + "]ms");
        }

        return;
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
     * This also removes outdated ones due to recalculated baselines.
     */
    public void calculateOOBs(Iterable<AggregateNumericMetric> oneHourAggregates) {

        long timeStart = System.currentTimeMillis();
        LOG.info("Auto-calculation of OOBs starting");
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        MeasurementOOBManagerLocal manager = LookupUtil.getOOBManager();
        // purge oobs whose baseline just got recalculated
        // For now just assume that our system is fast, so a cutoff of 30mins is ok,
        // as the calculate baseline job runs hourly
        long cutOff = System.currentTimeMillis() - (30L * 60L * 1000L);
        manager.removeOutdatedOOBs(overlord, cutOff);

        // clean up
        LookupUtil.getSystemManager().vacuum(overlord, new String[] { "RHQ_MEASUREMENT_OOB" });

        // Now caclulate the fresh OOBs
        manager.computeOOBsForLastHour(overlord, oneHourAggregates);

        long duration = System.currentTimeMillis() - timeStart;
        LOG.info("Auto-calculation of OOBs completed in [" + duration + "]ms");
    }
}