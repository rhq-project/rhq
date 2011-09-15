package org.rhq.enterprise.server.measurement;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.enterprise.server.scheduler.jobs.AbstractStatefulJob;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.core.domain.common.EntityContext;

/**
 * Job that asynchronously notifies agents of schedule updates.
 */
public class NotifyAgentsOfScheduleUpdatesJob extends AbstractStatefulJob {
    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();

        JobDataMap jobDataMap = context.getMergedJobDataMap();
        String scheduleSubQuery = (String) jobDataMap.get("ScheduleSubQuery");
        EntityContext entityContext = new EntityContext(
            Integer.parseInt((String) jobDataMap.get("EntityContext.resourceId")),
            Integer.parseInt((String) jobDataMap.get("EntityContext.groupId")),
            Integer.parseInt((String) jobDataMap.get("EntityContext.parentResourceId")),
            Integer.parseInt((String) jobDataMap.get("EntityContext.resourceTypeId"))
        );

        scheduleManager.notifyAgentsOfScheduleUpdates(entityContext, scheduleSubQuery);
    }
}
