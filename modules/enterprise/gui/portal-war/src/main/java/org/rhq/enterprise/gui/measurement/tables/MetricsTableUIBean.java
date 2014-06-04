package org.rhq.enterprise.gui.measurement.tables;

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
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementChartsManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This class supports the UI needs of Resource metrics.  Depending on the context various metric summary processing takes place
 * returning the appropriate metric summaries.  This class can be extended to provide context-specific UI Bean classes, or
 * encapsulated in UI beans that must extend other classes, such as PagedDataTableUIBean.
 *
 * @author jay shaughnessy
 */
public class MetricsTableUIBean {

    private final Log log = LogFactory.getLog(MetricsTableUIBean.class);

    protected MeasurementChartsManagerLocal chartManager = LookupUtil.getMeasurementChartsManager();
    protected MeasurementDefinitionManagerLocal definitionManager = LookupUtil.getMeasurementDefinitionManager();
    protected MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
    protected ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

    public static final String MANAGED_BEAN_NAME = "MetricsTableUIBean";

    protected EntityContext context;
    protected ResourceGroup resourceGroup;

    private List<MetricDisplaySummary> metricSummaries = null;
    private boolean valuesPresent = false;

    public MetricsTableUIBean() {
        context = WebUtility.getEntityContext();
    }

    public EntityContext getContext() {
        return this.context;
    }

    public List<MetricDisplaySummary> getMetricSummaries() {
        if (null != metricSummaries) {
            return metricSummaries;
        }

        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences range = preferences.getMetricRangePreferences();

        if (context.type == EntityContext.Type.Resource) {
            //null -> don't filter, we want everything, false -> not only enabled
            List<MeasurementSchedule> measurementSchedules = scheduleManager.findSchedulesForResourceAndType(
                user.getSubject(), context.resourceId, DataType.MEASUREMENT, null, true);

            int[] scheduleIds = new int[measurementSchedules.size()];
            int i = 0;
            for (MeasurementSchedule sched : measurementSchedules) {
                scheduleIds[i++] = sched.getId();
            }

            metricSummaries = chartManager.getMetricDisplaySummariesForResource(user.getSubject(), context.resourceId,
                scheduleIds, range.begin, range.end);

        } else if (context.type == EntityContext.Type.ResourceGroup) {
            List<MeasurementDefinition> measurementDefinitions = definitionManager
                .findMeasurementDefinitionsByResourceType(user.getSubject(), getResourceGroup(user).getResourceType()
                    .getId(), DataType.MEASUREMENT, null);

            int[] defIds = new int[measurementDefinitions.size()];
            int i = 0;
            for (MeasurementDefinition def : measurementDefinitions) {
                defIds[i++] = def.getId();
            }

            metricSummaries = chartManager.getMetricDisplaySummariesForCompatibleGroup(user.getSubject(), context,
                defIds, range.begin, range.end, true);

        } else if (context.type == EntityContext.Type.AutoGroup) {
            List<MeasurementDefinition> measurementDefinitions = definitionManager
                .findMeasurementDefinitionsByResourceType(user.getSubject(), context.getResourceTypeId(),
                    DataType.MEASUREMENT, null);

            int[] defIds = new int[measurementDefinitions.size()];
            int i = 0;
            for (MeasurementDefinition def : measurementDefinitions) {
                defIds[i++] = def.getId();
            }

            metricSummaries = chartManager.getMetricDisplaySummariesForAutoGroup(user.getSubject(),
                context.getParentResourceId(), context.getResourceTypeId(), defIds, range.begin, range.end, true);

        } else {
            log.error(context.getUnknownContextMessage());
            // return an empty list in this unlikely scenario
            return new ArrayList<MetricDisplaySummary>();
        }

        for (MetricDisplaySummary summary : metricSummaries) {
            MonitorUtils.formatSimpleMetrics(summary, FacesContext.getCurrentInstance().getExternalContext()
                .getRequestLocale());
            if (summary.getValuesPresent())
                valuesPresent = true;
        }

        return metricSummaries;
    }

    /**
     * Return it the summaries have values present.
     * Value is only valid *after* a call to #getMetricSummaries()
     * @return
     */
    public boolean isValuesPresent() {
        return valuesPresent;
    }

    public ResourceGroup getResourceGroup(WebUser user) {
        if (null == resourceGroup) {
            resourceGroup = resourceGroupManager.getResourceGroupById(user.getSubject(), context.groupId,
                GroupCategory.COMPATIBLE);
        }

        return resourceGroup;
    }

    public int[] getResourceGroupMemberIds(WebUser user) {
        List<Resource> resources = resourceGroupManager.findResourcesForResourceGroup(user.getSubject(),
            context.groupId, GroupCategory.COMPATIBLE);

        int[] resourceIds = new int[resources.size()];
        int i = 0;
        for (Resource res : resources) {
            resourceIds[i] = res.getId();
            i++;
        }

        return resourceIds;
    }

}
