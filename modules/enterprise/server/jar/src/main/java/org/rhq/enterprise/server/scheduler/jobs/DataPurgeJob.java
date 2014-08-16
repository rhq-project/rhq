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

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SimpleTrigger;

import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.AlertConditionManagerLocal;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.drift.DriftManagerLocal;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.purge.PurgeManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.TimingVoodoo;

/**
 * This implements {@link org.quartz.StatefulJob} (as opposed to {@link org.quartz.Job}) because we do not need nor want
 * this job triggered concurrently. That is, we don't want multiple data purge jobs performing the data purge at the
 * same time.
 *
 * Note, some of the work previously performed in this job has been moved to {@link DataCalcJob}.
 */
public class DataPurgeJob extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(DataPurgeJob.class);

    private static final long HOUR = 60 * 60 * 1000L;

    /**
     * Schedules a purge job to trigger right now. This will not block - it schedules the job to trigger but immediately
     * returns. This method will ensure that no two data purge jobs will execute at the same time (Quartz will ensure
     * this since {@link DataPurgeJob} is an implementation of {@link org.quartz.StatefulJob}).
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
            purgeEverything(systemConfig);
            performDatabaseMaintenance(LookupUtil.getSystemManager(), systemConfig);
        } catch (Exception e) {
            LOG.error("Data Purge Job FAILED TO COMPLETE. Cause: " + e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Data Purge Job FINISHED [" + duration + "]ms");
        }
    }

    private void purgeEverything(Properties systemConfig) {
        PurgeManagerLocal purgeManager = LookupUtil.getPurgeManager();
        purgeCallTimeData(purgeManager, systemConfig);
        purgeEventData(purgeManager, systemConfig);
        purgeAlertData(purgeManager, systemConfig);
        purgeUnusedAlertDefinitions(LookupUtil.getAlertDefinitionManager());
        purgeOrphanedAlertConditions(LookupUtil.getAlertConditionManager());
        purgeOrphanedAlertNotifications(LookupUtil.getAlertNotificationManager());
        purgeMeasurementTraitData(purgeManager, systemConfig);
        purgeAvailabilityData(purgeManager, systemConfig);
        purgeOrphanedDriftFiles(LookupUtil.getDriftManager(), systemConfig);
        purgeOperationHistoryData(LookupUtil.getOperationManager(), systemConfig);
    }

    private void purgeMeasurementTraitData(PurgeManagerLocal purgeManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Trait data purge starting at " + new Date(timeStart));
        int traitsPurged = 0;

        try {
            long threshold;
            String traitPurgeThresholdStr = systemConfig.getProperty(RHQConstants.TraitPurge);
            if (traitPurgeThresholdStr == null) {
                threshold = timeStart - (1000L * 60 * 60 * 24 * 365);
                LOG.debug("No purge traits threshold found - will purge traits older than one year");
            } else {
                threshold = timeStart - Long.parseLong(traitPurgeThresholdStr);
            }

            LOG.info("Purging traits that are older than " + new Date(threshold));
            traitsPurged = purgeManager.purgeTraits(threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge trait data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Traits data purged [" + traitsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeOperationHistoryData(OperationManagerLocal operationManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        int purgeCount = 0;

        try {
            String purgeThresholdStr = systemConfig.getProperty(RHQConstants.OperationHistoryPurge, "0");
            long purgeThreshold = Long.parseLong(purgeThresholdStr);
            if (purgeThreshold <= 0) {
                LOG.info("Operation History threshold set to 0, skipping purge of operation history data.");
                return;
            }

            LOG.info("Operation History data purge starting at " + new Date(timeStart));
            long threshold = timeStart - purgeThreshold;

            Date purgeBeforeTime = new Date(threshold);
            LOG.info("Purging operation history older than " + purgeBeforeTime);
            purgeCount = operationManager.purgeOperationHistory(purgeBeforeTime);

        } catch (Exception e) {
            LOG.error("Failed to purge operation history data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Operation history data purged [" + purgeCount + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeAvailabilityData(PurgeManagerLocal purgeManager, Properties systemConfig) {
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
            availsPurged = purgeManager.purgeAvailabilities(threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge availability data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Availability data purged [" + availsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeCallTimeData(PurgeManagerLocal purgeManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Measurement calltime data purge starting at " + new Date(timeStart));
        int calltimePurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.RtDataPurge));
            LOG.info("Purging calltime data that is older than " + new Date(threshold));
            calltimePurged = purgeManager.purgeCallTimeData(threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge calltime data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Calltime purged [" + calltimePurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeEventData(PurgeManagerLocal purgeManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Event data purge starting at " + new Date(timeStart));
        int eventsPurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.EventPurge));
            LOG.info("Purging event data older than " + new Date(threshold));
            eventsPurged = purgeManager.purgeEventData(threshold);
        } catch (Exception e) {
            LOG.error("Failed to purge event data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Event data purged [" + eventsPurged + "] - completed in [" + duration + "]ms");
        }
    }

    private void purgeAlertData(PurgeManagerLocal purgeManager, Properties systemConfig) {
        long timeStart = System.currentTimeMillis();
        LOG.info("Alert data purge starting at " + new Date(timeStart));
        int alertsPurged = 0;

        try {
            long threshold = timeStart - Long.parseLong(systemConfig.getProperty(RHQConstants.AlertPurge));
            LOG.info("Purging alert data older than " + new Date(threshold));
            alertsPurged = purgeManager.deleteAlerts(0, threshold);
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
}
