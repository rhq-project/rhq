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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.exception.UnscheduleException;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AsyncResourceDeleteJob extends AbstractStatefulJob {

    private final Log log = LogFactory.getLog(AsyncResourceDeleteJob.class);

    Subject overlord = LookupUtil.getSubjectManager().getOverlord();
    ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

    private class AsyncDeletionStats {
        int deletedSuccessfully = 0;
        int deletedWithFailure = 0;
        long deletionTime;

        public String toString() {
            return "Async resource deletion - " + deletedSuccessfully + " successful, " + deletedWithFailure
                + " failed, took [" + deletionTime + "] ms";
        }
    }

    @Override
    public void executeJobCode(JobExecutionContext arg0) throws JobExecutionException {
        List<Integer> toBeRemovedIds = resourceManager.findResourcesMarkedForAsyncDeletion(overlord);

        AsyncDeletionStats stats = new AsyncDeletionStats();
        for (Integer doomedResourceId : toBeRemovedIds) {
            try {
                // do not recurse
                uninventoryResource(overlord, doomedResourceId, stats, false);
            } catch (Throwable t) {
                log.debug("Simple asynchronous deletion of resource[id=" + doomedResourceId + "] failed, "
                    + "trying more robust yet expensive removal method, cause: " + ThrowableUtil.getAllMessages(t));
                try {
                    // try more robust yet expensive recursive delete
                    uninventoryResource(overlord, doomedResourceId, stats, true);
                } catch (Throwable tt) {
                    log.debug("Error during asynchronous deletion of resource[id=" + doomedResourceId + "], cause: "
                        + ThrowableUtil.getAllMessages(tt));
                    stats.deletedWithFailure++;
                }
            }
        }

        if (stats.deletedSuccessfully > 0 || stats.deletedWithFailure > 0) {
            log.info(stats);
        }
    }

    // return true if successful
    private void uninventoryResource(Subject overlord, Integer doomedResourceId, AsyncDeletionStats stats,
        boolean recurse) {
        if (recurse) {
            List<Integer> doomedChildrenIds = resourceManager.findChildrenResourceIds(doomedResourceId, null);
            for (Integer nextDoomedChildId : doomedChildrenIds) {
                uninventoryResource(overlord, nextDoomedChildId, stats, recurse);
            }
        }
        log.debug("Before " + (recurse ? "(recursive)" : "") + " asynchronous deletion of resource[id="
            + doomedResourceId + "]");
        long startTime = System.currentTimeMillis();

        unscheduleJobs(overlord, doomedResourceId);
        resourceManager.uninventoryResourceAsyncWork(overlord, doomedResourceId);
        stats.deletedSuccessfully++;

        long endTime = System.currentTimeMillis();
        log.debug("After " + (recurse ? "(recursive)" : "") + " asynchronous deletion of resource[id="
            + doomedResourceId + "] took [" + (endTime - startTime) + "]ms");

        stats.deletionTime += (endTime - startTime);
    }

    private void unscheduleJobs(Subject overlord, Integer resourceId) {
        log.debug("Unscheduling jobs for resource[id=" + resourceId + "]");
        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        try {
            List<ResourceOperationSchedule> schedules = operationManager.findScheduledResourceOperations(overlord,
                resourceId);

            for (ResourceOperationSchedule schedule : schedules) {
                try {
                    /*
                     * unscheduleResourceOperation already takes care of ignoring requests to delete unknown schedules,
                     * which would happen if the following sequence occurs:
                     *
                     * - a user tries to delete a resource, gets the list of resource operation schedules - just then, one
                     * or more of the schedules completes it's last scheduled firing, and is removed - then we try to
                     * unschedule it here, except that the jobid will no longer be known
                     */
                    operationManager.unscheduleResourceOperation(overlord, schedule.getJobId().toString(), resourceId);
                } catch (UnscheduleException ise) {
                    log.warn("Failed to unschedule job [" + schedule + "] for a resource being deleted [" + resourceId
                        + "]", ise);
                }
            }
        } catch (Throwable t) {
            log.warn("Failed to get jobs for a resource being deleted [" + resourceId
                + "]; will not attempt to unschedule anything", t);
        }
    }
}
