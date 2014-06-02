/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> that retrieves data from the backend to facilitate display of the <em>Resource Metrics</em>
 * pages.
 *
 * <p/>This is used for the "Metric Data" sub sub tab on the monioring pages.
 */
public class ResourceMetricsFormPrepareAction extends MetricsDisplayFormPrepareAction {
    protected static Log log = LogFactory.getLog(ResourceMetricsFormPrepareAction.class);

    // ---------------------------------------------------- Protected Methods

    /**
     * Do we show the baseline column on this page? The answer is no (for now).
     */
    @Override
    protected Boolean getShowBaseline() {
        return Boolean.FALSE;
    }

    /**
     * Get from the Bizapp the set of metric summaries for the specified entity that will be displayed on the page.
     * Returns a <code>Map</code> keyed by metric category.
     *
     * @param  request     the http request
     * @param  resourceIds the entity id of the currently viewed resource
     * @param  begin       the time (in milliseconds since the epoch) that begins the timeframe for which the metrics
     *                     are summarized
     * @param  end         the time (in milliseconds since the epoch) that ends the timeframe for which the metrics are
     *                     summarized
     *
     * @return Map keyed on the category (String), values are List's of MetricDisplaySummary beans
     */
    @Override
    protected List<MetricDisplaySummary> getMetrics(HttpServletRequest request, int[] resourceIds, long filters,
        String keyword, Long begin, Long end) throws Exception {
        Subject subject = WebUtility.getSubject(request);

        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        int myGroupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);

        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        MeasurementChartsManagerLocal chartsManager = LookupUtil.getMeasurementChartsManager();

        if (log.isTraceEnabled()) {
            log.trace("finding metric summaries for resource [" + Arrays.toString(resourceIds) + "] for range " + begin
                + ":" + end + " filters value: " + filters + " and keyword: " + keyword);
        }

        List<MetricDisplaySummary> metricSummaries = null;

        //  GH: Why are we only getting the first one? --> single resource case
        if (myGroupId > 0) {
            ResourceGroupManagerLocal resGrpMgr = LookupUtil.getResourceGroupManager();
            int[] definitionIds = resGrpMgr.findDefinitionsForCompatibleGroup(subject, myGroupId, false);
            metricSummaries = chartsManager.getMetricDisplaySummariesForCompatibleGroup(subject,
                EntityContext.forGroup(myGroupId),
                definitionIds, begin, end, false);
        } else if ((parent > 0) && (type > 0)) {
            ResourceGroupManagerLocal resGrpMgr = LookupUtil.getResourceGroupManager();
            int[] definitionIds = resGrpMgr.findDefinitionsForAutoGroup(subject, parent, type, false);
            metricSummaries = chartsManager.getMetricDisplaySummariesForAutoGroup(subject, parent, type, definitionIds,
                begin, end, false);
        } else if ((resourceIds != null) && (resourceIds.length > 0)) {
            int resourceId = resourceIds[0];

            List<MeasurementSchedule> scheds = scheduleManager.findSchedulesForResourceAndType(subject,
                resourceId, null, null, false); //null -> don't filter, we want everything, false -> not only enabled

            int metricOrTraitCount = 0;
            for (MeasurementSchedule sched : scheds) {
                if ((sched.getDefinition().getDataType() == DataType.MEASUREMENT)
                    || (sched.getDefinition().getDataType() == DataType.TRAIT)) {
                    // We only want to display numeric metrics and traits on the Visibility and Metric Data subtabs.
                    metricOrTraitCount++;
                }
            }

            int[] scheduleIds = new int[metricOrTraitCount];
            int index = 0;
            for (MeasurementSchedule sched : scheds) {
                if ((sched.getDefinition().getDataType() == DataType.MEASUREMENT)
                    || (sched.getDefinition().getDataType() == DataType.TRAIT)) {
                    // We only want to display numeric metrics and traits on the Visibility and Metric Data subtabs.
                    scheduleIds[index++] = sched.getId();
                }
            }

            metricSummaries = chartsManager.getMetricDisplaySummariesForResource(subject, resourceId, scheduleIds,
                begin, end);
        } else {
            throw new IllegalArgumentException("Unknown operation mode");
        }

        /*
         * Loop over the summaries to see if they have data. If all are empty, then just return an empty list.
         */
        boolean dataPresent = false;
        for (MetricDisplaySummary sum : metricSummaries) {
            if (sum.getValuesPresent() == true) {
                dataPresent = true;
                break;
            }
        }

        if (!dataPresent) {
            metricSummaries = new ArrayList<MetricDisplaySummary>();
        }

        return metricSummaries;
    }
}