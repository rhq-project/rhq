package org.rhq.enterprise.server.measurement;

import org.quartz.JobDataMap;
import org.quartz.SchedulerException;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.scheduler.jobs.AbstractStatefulJob;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.core.domain.common.EntityContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Job that asynchronously notifies agents of metric schedule updates.
 */
public class NotifyAgentsOfScheduleUpdatesJob extends AbstractStatefulJob {

    private final Log log = LogFactory.getLog(MeasurementScheduleManagerBean.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getMergedJobDataMap();
        final String triggerName = (String) jobDataMap.get(MeasurementScheduleManagerBean.TRIGGER_NAME);
        final String triggerGroupName = (String) jobDataMap.get(MeasurementScheduleManagerBean.TRIGGER_GROUP_NAME);
        SchedulerLocal scheduler = LookupUtil.getSchedulerBean();
        try {
            scheduler.unscheduleJob(triggerName, triggerGroupName);
        } catch (SchedulerException e) {
            log.error("Failed to unschedule Quartz trigger [" + triggerName + "].", e);
        }

        String scheduleSubQuery = (String) jobDataMap.get(MeasurementScheduleManagerBean.SCHEDULE_SUBQUERY);
        EntityContext entityContext = new EntityContext(
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_RESOURCEID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_GROUPID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_PARENT_RESOURCEID)),
            Integer.parseInt((String) jobDataMap.get(MeasurementScheduleManagerBean.ENTITYCONTEXT_RESOURCETYPEID))
        );
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        scheduleManager.notifyAgentsOfScheduleUpdates(entityContext, scheduleSubQuery);
    }
}
