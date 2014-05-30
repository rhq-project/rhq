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

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.criteria.AvailabilityCriteria;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.alert.engine.model.AvailabilityDurationComposite;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This was once a quartz job but now supports an EJB Timer.  It could have been merged into the EJB class with the
 * Timer code but I left it here since it calls out the fact that it is still a scheduled job.
 *
 * @author Jay Shaughnessy
 */
public class AlertAvailabilityDurationJob {
    private static final Log LOG = LogFactory.getLog(AlertAvailabilityDurationJob.class);

    public static final String DATAMAP_CONDITION_ID = "alertConditionId";
    public static final String DATAMAP_DURATION = "duration";
    public static final String DATAMAP_OPERATOR = "alertConditionOperator";
    public static final String DATAMAP_RESOURCE_ID = "resourceId";
    public static final String DATAMAP_START_TIME = "startTime";

    static public void execute(Map<String, String> infoMap) throws Exception {
        int conditionId = Integer.valueOf(infoMap.get(DATAMAP_CONDITION_ID));
        int resourceId = Integer.valueOf(infoMap.get(DATAMAP_RESOURCE_ID));
        long duration = Long.valueOf(infoMap.get(DATAMAP_DURATION)); // in seconds
        long durationStart = Long.valueOf(infoMap.get(DATAMAP_START_TIME)); // in milliseconds
        AlertConditionOperator operator = AlertConditionOperator.valueOf(infoMap.get(DATAMAP_OPERATOR));

        // get the availabilities for the duration period, one consistent duration will indicate a duration condition
        AvailabilityCriteria criteria = new AvailabilityCriteria();
        criteria.addFilterResourceId(resourceId);
        long durationEnd = durationStart + (duration * 1000);
        criteria.addFilterInterval((durationStart + 1), (durationEnd - 1)); // reduced 1ms to fake exclusive interval filter.
        criteria.addSortStartTime(PageOrdering.ASC);
        List<Availability> avails = LookupUtil.getAvailabilityManager().findAvailabilityByCriteria(
            LookupUtil.getSubjectManager().getOverlord(), criteria);

        // Although unlikely, it's possible the resource has actually gone away while we waited out the duration period.
        // If we can't find any resource avail assume the resource is gone and just end the job.
        if (avails.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AlertAvailabilityDurationJob: No alert. Assuming resource has been uninventoried ["
                    + resourceId + "]");
            }

            return;
        }

        // If there are multiple duration records for the duration period then the avail did not stay constant.
        // Therefore, the alert should not fire as the semantics are "goes down and stays down".
        if (avails.size() > 1) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("AlertAvailabilityDurationJob: No alert. Resource avail for [" + resourceId
                    + "] has fluctuated. " + avails);
            }

            return;
        }

        // At this point we should be able to just checkConditions because if there is only one avail record for the
        // duration period it means nothing has changed.  But, we'll perform a sanity check just to ensure the avail
        // type is what we think it should be...

        Availability avail = avails.get(0);
        AvailabilityType availType = avail.getAvailabilityType();

        boolean checkConditions = false;
        switch (operator) {
        case AVAIL_DURATION_DOWN:
            checkConditions = (AvailabilityType.DOWN == availType);
            break;
        case AVAIL_DURATION_NOT_UP:
            checkConditions = (AvailabilityType.UP != availType);
            break;
        default:
            LOG.error("AlertAvailabilityDurationJob: unexpected operator [" + operator.name() + "]");
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
        } else {
            LOG.warn("AlertAvailabilityDurationJob: unexpected availability for resource [" + resourceId + "]. "
                + avail);
        }
    }

}
