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
        String scheduleSubQuery = (String) jobDataMap.get(MeasurementScheduleManagerBean.SCHEDULE_SUBQUERY);
        EntityContext entityContext = new EntityContext(
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_RESOURCEID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_GROUPID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_PARENT_RESOURCEID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_RESOURCETYPEID))
        );

        scheduleManager.notifyAgentsOfScheduleUpdates(entityContext, scheduleSubQuery);
    }
}
