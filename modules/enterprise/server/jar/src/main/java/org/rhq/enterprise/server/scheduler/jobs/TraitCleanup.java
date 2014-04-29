package org.rhq.enterprise.server.scheduler.jobs;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.server.metrics.TraitsDAO;

/**
 * Cleans up duplicate traits within a weekly window.
 */
public class TraitCleanup extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(TraitCleanup.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        long timeStart = System.currentTimeMillis();
        LOG.info("Trait cleanup starting");
        try {
            StorageClientManager storageClientManager = LookupUtil.getStorageClientManager();
            TraitsDAO traitsDAO = storageClientManager.getTraitsDAO();
            // Since we run once a week, clean up data in an ~7 day history window
            Calendar c = Calendar.getInstance();
            c.add(-8, Calendar.DATE);
            traitsDAO.cleanup(c.getTime());
        } catch (Exception e) {
            LOG.error("Failed to cleanup trait data. Cause: " + e, e);
        } finally {
            long duration = System.currentTimeMillis() - timeStart;
            LOG.info("Trait cleanup completed in [" + duration + "]ms");
        }
    }

}
