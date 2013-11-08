/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.alert.engine.model;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.scheduler.jobs.AlertAvailabilityDurationJob;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */

public final class AvailabilityDurationCacheElement extends AbstractEnumCacheElement<AvailabilityType> {

    /**
     * @param operator
     * @param operatorOption the duration, in seconds (as String)
     * @param value
     * @param conditionTriggerId this is actually the alertConditionId, renamed here.
     */
    public AvailabilityDurationCacheElement(AlertConditionOperator operator, String operatorOption,
        AvailabilityType value, int conditionTriggerId) {

        super(operator, operatorOption, value, conditionTriggerId);
    }

    /**
     * Here we check to see if an availability change for a resource should initiate an avail duration check. For
     * each relevant avail duration condition for the resource schedule a job to check the avail state after the
     * condition's duration.
     *
     * @param cacheElements
     * @param resource
     * @param providedValue
     */
    public static void checkCacheElements(List<AvailabilityDurationCacheElement> cacheElements, Resource resource,
        AvailabilityType providedValue) {
        if (null == cacheElements) {
            return; // nothing to do
        }

        for (AvailabilityDurationCacheElement cacheElement : cacheElements) {
            switch (cacheElement.getAlertConditionOperator()) {
            case AVAIL_DURATION_DOWN:
                if (AvailabilityType.DOWN == providedValue
                    && AvailabilityType.DOWN != cacheElement.getAlertConditionValue()) {

                    scheduleAvailabilityDurationCheck(cacheElement, resource);
                }
                break;
            case AVAIL_DURATION_NOT_UP:
                if (AvailabilityType.UP != providedValue
                    && AvailabilityType.UP == cacheElement.getAlertConditionValue()) {

                    scheduleAvailabilityDurationCheck(cacheElement, resource);
                }
                break;
            }

            cacheElement.setAlertConditionValue(providedValue);
        }
    }

    /**
     * Each avail duration check is performed by triggering an execution of {@link AlertAvailabilityDurationJob}.
     * Note that each of the scheduled jobs is relevant to only 1 condition evaluation.
     *
     * @param cacheElement
     * @param resource
     */
    private static void scheduleAvailabilityDurationCheck(AvailabilityDurationCacheElement cacheElement,
        Resource resource) {

        Log log = LogFactory.getLog(AvailabilityDurationCacheElement.class.getName());
        String jobName = AlertAvailabilityDurationJob.class.getName();
        String jobGroupName = AlertAvailabilityDurationJob.class.getName();
        String operator = cacheElement.getAlertConditionOperator().name();
        // must be unique amongst all possible firings, add a timestamp because the exact same condition may
        // get hit again while a timer is already in progress.
        String triggerName = operator + "-" + resource.getId() + "-"
            + System.currentTimeMillis();
        String duration = (String) cacheElement.getAlertConditionOperatorOption();
        // convert from seconds to milliseconds
        Date jobTime = new Date(System.currentTimeMillis() + (Long.valueOf(duration).longValue() * 1000));

        if (log.isDebugEnabled()) {
            log.debug("Scheduling availability duration job for [" + DateFormat.getDateTimeInstance().format(jobTime)
                + "]");
        }

        JobDataMap jobDataMap = new JobDataMap();
        // the condition id is needed to ensure we limit the future avail checking to the one relevant alert condition
        jobDataMap.put(AlertAvailabilityDurationJob.DATAMAP_CONDITION_ID,
            String.valueOf(cacheElement.getAlertConditionTriggerId()));
        jobDataMap.put(AlertAvailabilityDurationJob.DATAMAP_RESOURCE_ID, String.valueOf(resource.getId()));
        jobDataMap.put(AlertAvailabilityDurationJob.DATAMAP_OPERATOR, operator);
        jobDataMap.put(AlertAvailabilityDurationJob.DATAMAP_DURATION, duration);

        Trigger trigger = new SimpleTrigger(triggerName, jobGroupName, jobTime);
        trigger.setJobName(jobName);
        trigger.setJobGroup(jobGroupName);
        trigger.setJobDataMap(jobDataMap);
        try {
            LookupUtil.getSchedulerBean().scheduleJob(trigger);
        } catch (Throwable t) {
            log.warn(
                "Unable to schedule availability duration job for [" + resource + "] with JobData ["
                    + jobDataMap.values() + "]", t);
        }
    }

    @Override
    public boolean matches(AvailabilityType providedValue, Object... extraParams) {
        if (null == providedValue) {
            return false;
        }

        boolean result = false;

        switch (alertConditionOperator) {
        case AVAIL_DURATION_DOWN:
            result = (AvailabilityType.DOWN == providedValue);
            break;
        case AVAIL_DURATION_NOT_UP:
            result = (AvailabilityType.UP != providedValue);
            break;
        }

        alertConditionValue = providedValue;

        return result;
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        switch (operator) {
        case AVAIL_DURATION_DOWN:
        case AVAIL_DURATION_NOT_UP:
            return operator.getDefaultType();

        default:
            return AlertConditionOperator.Type.NONE;
        }
    }
}
