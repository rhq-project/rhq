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
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class AsyncResourceDeleteJob extends AbstractStatefulJob {

    private final Log log = LogFactory.getLog(AsyncResourceDeleteJob.class);

    @Override
    public void executeJobCode(JobExecutionContext arg0) throws JobExecutionException {

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        int deletedSuccessfully = 0;
        int deletedWithFailure = 0;
        long time = 0;
        List<Integer> toBeRemovedIds = resourceManager.findResourcesMarkedForAsyncDeletion(overlord);
        for (Integer doomedResourceId : toBeRemovedIds) {
            try {
                log.debug("Before asynchronous deletion of resource[id=" + doomedResourceId + "]");
                long startTime = System.currentTimeMillis();
                resourceManager.deleteSingleResourceInNewTransaction(overlord, doomedResourceId);
                long endTime = System.currentTimeMillis();
                time += (endTime - startTime);
                log.debug("After asynchronous deletion of resource[id=" + doomedResourceId + "], took ["
                    + (endTime - startTime) + "]ms");
                deletedSuccessfully++;
            } catch (Throwable t) {
                log.debug("Error during asynchronous deletion of resource[id=" + doomedResourceId + "]", t);
                deletedWithFailure++;
            }
        }

        if (deletedSuccessfully > 0 || deletedWithFailure > 0) {
            log.info("Async resource deletion - " + deletedSuccessfully + " successful, " + deletedWithFailure
                + " failed, took [" + time + "]ms");
        }
    }
}
