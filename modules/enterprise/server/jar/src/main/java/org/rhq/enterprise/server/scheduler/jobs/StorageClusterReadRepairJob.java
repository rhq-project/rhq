package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class StorageClusterReadRepairJob extends AbstractStatefulJob {

    private Log log = LogFactory.getLog(StorageClusterReadRepairJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        log.info("Preparing to run read repair on storage cluster");
        StorageNodeManagerLocal storageNodeManager = LookupUtil.getStorageNodeManager();
        storageNodeManager.runReadRepair();
    }
}
