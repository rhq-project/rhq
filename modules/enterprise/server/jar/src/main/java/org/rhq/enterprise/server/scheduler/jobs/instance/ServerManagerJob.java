package org.rhq.enterprise.server.scheduler.jobs.instance;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.enterprise.server.cluster.instance.ServerManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ServerManagerJob implements Job {

    public void execute(JobExecutionContext arg0) throws JobExecutionException {
        ServerManagerLocal serverManager = LookupUtil.getServerManager();

        serverManager.establishCurrentServerMode();
        serverManager.updateMtimeToCurrentTime(serverManager.getServer());
    }

}
