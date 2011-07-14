package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import java.util.List;

public class PurgeResourceTypesJob extends AbstractStatefulJob {

    private static class DeletionStats {
        int numDeleted;
        long deletionTime;

        @Override
        public String toString() {
            return PurgeResourceTypesJob.class.getSimpleName() + ": deleted " + numDeleted + " in " + deletionTime + " ms";
        }
    }

    private static final Log log = LogFactory.getLog(PurgeResourceTypesJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();
        DeletionStats stats = new DeletionStats();

        long startTotalTime = System.currentTimeMillis();
        List<ResourceType> deletedTypes = inventoryMgr.getDeletedTypes();
        for (ResourceType deletedType : deletedTypes) {
            if (inventoryMgr.isReadyForPermanentRemoval(deletedType)) {
                log.debug("Permanently removing " + deletedType);
                long startTime = System.currentTimeMillis();
                inventoryMgr.purgeDeletedResourceType(deletedType);
                long endTime = System.currentTimeMillis();
                stats.numDeleted++;
                log.debug("Deleted " + deletedType + " in " + (endTime - startTime) + " ms");
            }
            else {
                log.debug(deletedType + " has been deleted but is not yet ready for permanent removal.");
            }
        }
        long stopTotalTime = System.currentTimeMillis();
        stats.deletionTime = stopTotalTime - startTotalTime;
        log.debug(stats);
    }


}
