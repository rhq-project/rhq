package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.server.inventory.InventoryManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import java.util.List;

public class PurgeResourceTypes extends AbstractStatefulJob {

    private static final Log log = LogFactory.getLog(PurgeResourceTypes.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        InventoryManagerLocal inventoryMgr = LookupUtil.getInventoryManager();

        List<ResourceType> deletedTypes = inventoryMgr.getDeletedTypes();
        for (ResourceType deletedType : deletedTypes) {
            if (inventoryMgr.isReadyForPermanentRemoval(deletedType)) {
                log.debug("Permanently removing " + deletedType);
                inventoryMgr.purgeDeletedResourceType(deletedType);
            }
            else {
                log.debug(deletedType + " has been deleted but is not yet ready for permanent removal.");
            }
        }
    }


}
