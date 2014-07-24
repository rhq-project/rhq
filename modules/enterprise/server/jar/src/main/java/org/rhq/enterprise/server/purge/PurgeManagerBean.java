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

package org.rhq.enterprise.server.purge;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;

/**
 * A manager for purge. It's mostly used from the {@link org.rhq.enterprise.server.scheduler.jobs.DataPurgeJob}.<br>
 * <br>
 * Purge operations should happen in (relatively) small, transaction-bounded chunks. This is to avoid transaction
 * timeouts as a single delete query might fully lock a table for quite some time, thus preventing data coming from
 * agent reports from being inserted.
 *
 * @author Thomas Segismont
 */
@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class PurgeManagerBean implements PurgeManagerLocal {
    private static final Log LOG = LogFactory.getLog(PurgeManagerBean.class);

    @Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
    private DataSource dataSource;

    @Resource
    private UserTransaction userTransaction;

    @Override
    public int purgeAvailabilities(long oldest) {
        AvailabilityPurge availabilityPurge = new AvailabilityPurge(dataSource, userTransaction, oldest);
        long startTime = System.currentTimeMillis();
        int deleted = availabilityPurge.execute();
        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedAvailabilities(deleted);
        return deleted;
    }

    @Override
    public int purgeTraits(long oldest) {
        MeasurementDataTraitPurge traitPurge = new MeasurementDataTraitPurge(dataSource, userTransaction, oldest);
        long startTime = System.currentTimeMillis();
        int deleted = traitPurge.execute();
        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedMeasurementTraits(deleted);
        return deleted;
    }

    @Override
    public int purgeEventData(long deleteUpToTime) {
        EventDataPurge eventDataPurge = new EventDataPurge(dataSource, userTransaction, deleteUpToTime);
        long startTime = System.currentTimeMillis();
        int deleted = eventDataPurge.execute();
        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedEvents(deleted);
        return deleted;
    }

    @Override
    public int purgeCallTimeData(long deleteUpToTime) {
        // NOTE: We do not purge unreferenced rows from RHQ_CALLTIME_DATA_KEY, because this can cause issues
        //       (see http://jira.jboss.com/jira/browse/JBNADM-1606). Once we limit the number of keys per
        //       resource at insertion time (see http://jira.jboss.com/jira/browse/JBNADM-2618), the key
        //       table will not require truncation.
        CallTimeDataValuePurge callTimeDataValuePurge = new CallTimeDataValuePurge(dataSource, userTransaction,
            deleteUpToTime);
        long startTime = System.currentTimeMillis();
        int deletedRowCount = callTimeDataValuePurge.execute();
        MeasurementMonitor.getMBean().incrementPurgeTime(System.currentTimeMillis() - startTime);
        MeasurementMonitor.getMBean().setPurgedCallTimeData(deletedRowCount);
        return deletedRowCount;
    }

    @Override
    public int deleteAlerts(long beginTime, long endTime) {
        long totalTime = 0;

        AlertConditionLogPurge conditionLogPurge = new AlertConditionLogPurge(dataSource, userTransaction, beginTime,
            endTime);
        long start = System.currentTimeMillis();
        int conditionsDeleted = conditionLogPurge.execute();
        long end = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted [" + conditionsDeleted + "] alert condition logs in [" + (end - start) + "]ms");
        }
        totalTime += (end - start);

        AlertNotificationLogPurge notificationLogPurge = new AlertNotificationLogPurge(dataSource, userTransaction,
            beginTime, endTime);
        start = System.currentTimeMillis();
        int deletedNotifications = notificationLogPurge.execute();
        end = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted [" + deletedNotifications + "] alert notifications in [" + (end - start) + "]ms");
        }
        totalTime += (end - start);

        AlertPurge alertPurge = new AlertPurge(dataSource, userTransaction, beginTime, endTime);
        start = System.currentTimeMillis();
        int deletedAlerts = alertPurge.execute();
        end = System.currentTimeMillis();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted [" + deletedAlerts + "] alerts in [" + (end - start) + "]ms");
        }
        totalTime += (end - start);

        MeasurementMonitor.getMBean().incrementPurgeTime(totalTime);
        MeasurementMonitor.getMBean().setPurgedAlerts(deletedAlerts);
        MeasurementMonitor.getMBean().setPurgedAlertConditions(conditionsDeleted);
        MeasurementMonitor.getMBean().setPurgedAlertNotifications(deletedNotifications);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleted [" + (deletedAlerts + conditionsDeleted + deletedNotifications) + "] "
                + "alert audit records in [" + (totalTime) + "]ms");
        }

        return deletedAlerts;
    }

    @Override
    public void removeOutdatedOOBs(long cutoffTime) {
        MeasurementOOBPurge measurementOOBPurge = new MeasurementOOBPurge(dataSource, userTransaction, cutoffTime);
        int count = measurementOOBPurge.execute();
        LOG.info("Removed [" + count + "] outdated OOBs");
    }
}
