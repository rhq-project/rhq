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

package org.rhq.enterprise.server.scheduler.jobs;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityDurationComposite;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Jay Shaughnessy
 */
public class AlertAvailabilityDurationJob extends AbstractStatefulJob {
    private static final Log LOG = LogFactory.getLog(AlertAvailabilityDurationJob.class);

    public static final String DATAMAP_CONDITION_ID = "alertConditionId";
    public static final String DATAMAP_DURATION = "duration";
    public static final String DATAMAP_OPERATOR = "alertConditionOperator";
    public static final String DATAMAP_RESOURCE_ID = "resourceId";

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getTrigger().getJobDataMap();
        int conditionId = Integer.valueOf(jobDataMap.getString(DATAMAP_CONDITION_ID));
        int resourceId = Integer.valueOf(jobDataMap.getString(DATAMAP_RESOURCE_ID));
        long duration = Long.valueOf(jobDataMap.getString(DATAMAP_DURATION));
        AlertConditionOperator operator = AlertConditionOperator.valueOf(jobDataMap.getString(DATAMAP_OPERATOR));

        // get the current resource availability
        ResourceAvailability resourceAvail = LookupUtil.getResourceAvailabilityManager().getLatestAvailability(
            resourceId);

        // Although unlikely, it's possible the resource has actually gone away while we waited out the duration period.
        // If we can't find any resource avail assume the resource is gone and just end the job.
        if (null == resourceAvail) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AlertAvailabilityDurationJob: Assuming resource has been uninventoried [" + resourceId + "]");
            }

            return;
        }

        AvailabilityType availType = resourceAvail.getAvailabilityType();

        // Question? Do we care whether the avail type has been the same for the entire duration (meaning we would need
        // to go and find the most recent Availability record)?  Or do we only care if it is currently at the avail
        // type that matters? For now we'll go with the latter approach and ignore whether the avail actually
        // changed during the specified duration.

        boolean checkConditions = false;
        switch (operator) {
        case AVAIL_DURATION_DOWN:
            checkConditions = (AvailabilityType.DOWN == availType);
            break;
        case AVAIL_DURATION_NOT_UP:
            checkConditions = (AvailabilityType.UP != availType);
            break;
        }

        // the call to checkConditions will probably result in an alert, as the actual condition satisfaction was
        // just done. but we need to actually hook into the alerting chassis to ensure any other conditions are
        // still satisfied and to make sure all the alert processing is performed.
        if (checkConditions) {
            AvailabilityDurationComposite composite = new AvailabilityDurationComposite(conditionId, operator,
                resourceId, availType, duration);
            AlertConditionCacheStats stats = LookupUtil.getAlertConditionCacheManager().checkConditions(composite);

            if (LOG.isDebugEnabled()) {
                LOG.debug("AlertAvailabilityDurationJob: " + stats.toString());
            }
        }
    }
}
