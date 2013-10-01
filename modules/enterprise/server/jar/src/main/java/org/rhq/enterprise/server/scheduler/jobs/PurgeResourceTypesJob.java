/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class PurgeResourceTypesJob extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(PurgeResourceTypesJob.class);

    private static class DeletionStats {
        int numDeleted;
        long deletionTime;

        @Override
        public String toString() {
            return PurgeResourceTypesJob.class.getSimpleName() + ": deleted " + numDeleted + " in " + deletionTime + " ms";
        }
    }

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        DeletionStats stats = new DeletionStats();

        long startTotalTime = System.currentTimeMillis();
        List<ResourceType> deletedTypes = inventoryMgr.getDeletedTypes();
        for (ResourceType deletedType : deletedTypes) {
            if (inventoryMgr.isReadyForPermanentRemoval(deletedType)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Permanently removing " + deletedType);
                }
                long startTime = System.currentTimeMillis();
                inventoryMgr.purgeDeletedResourceType(deletedType);
                long endTime = System.currentTimeMillis();
                stats.numDeleted++;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Deleted " + deletedType + " in " + (endTime - startTime) + " ms");
                }
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(deletedType + " has been deleted but is not yet ready for permanent removal.");
                }
            }
        }
        long stopTotalTime = System.currentTimeMillis();
        stats.deletionTime = stopTotalTime - startTotalTime;
        LOG.debug(stats);
    }


}
