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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.config;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This populates the "Monitor>Configure" and "Administration>Monitoring Defaults Configuration>Edit Metric Templates"
 * pages' request attributes.
 *
 * @author Ian Springer
 */
public class ConfigMetricsFormPrepareAction extends TilesAction {
    private final Log log = LogFactory.getLog(ConfigMetricsFormPrepareAction.class.getName());

    /**
     * Retrieve different resource metrics and store them in various request attributes. TODO: use the mode selection
     * code from ConfigMetricsAction
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.trace("Executing ConfigMetricsFormPrepareAction...");

        Subject subject = WebUtility.getSubject(request);
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        ResourceTypeManagerLocal rtManager = LookupUtil.getResourceTypeManager();

        // NOTE: This action will be passed either a resourceTypeId OR a resourceId or a groupID or type+parent
        boolean configuringDefaultSchedules = false;
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);

        // If only a type is present, we want the defaults
        if ((type > 0) && (parent == -1)) {
            configuringDefaultSchedules = true;
        }

        PageList<MeasurementScheduleComposite> measurementSchedules = null;
        PageControl pageControl = WebUtility.getPageControl(request);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);
        if (configuringDefaultSchedules || (groupId > 0) || ((type > 0) && (parent > 0))) {
            pageControl.initDefaultOrderingField("md.name"); // underlying query runs over the MeasurementDefinition
        } else {
            pageControl.initDefaultOrderingField("ms.definition.name");
        }

        // We never want pagination for the schedules table, so don't allow user to change ps to anything other than -1.
        pageControl.setPageSize(PageControl.SIZE_UNLIMITED);

        if (configuringDefaultSchedules) {
            int resourceTypeId = type;
            log.debug("Obtaining default metric schedules for resource type " + resourceTypeId + "...");
            measurementSchedules = scheduleManager.findScheduleDefaultsForResourceType(subject,
                resourceTypeId, pageControl);
            request.setAttribute(AttrConstants.MONITOR_ENABLED_ATTR, Boolean.FALSE);
            ResourceType rType = rtManager.getResourceTypeById(subject, resourceTypeId);

            request.setAttribute(AttrConstants.RESOURCE_TYPE_ATTR, rType);
            //request.setAttribute("section", resourceCategoryName);
        } else {
            int resourceId = WebUtility.getOptionalIntRequestParameter(request, "id", -1);
            if ((parent > 0) && (type > 0)) {
                request.setAttribute(AttrConstants.MONITOR_ENABLED_ATTR, true);
                measurementSchedules = scheduleManager.findSchedulesForAutoGroup(subject, parent, type,
                    pageControl);

                request.setAttribute("type", type);
                request.setAttribute("parent", parent);
                ResourceManagerLocal resMgr = LookupUtil.getResourceManager();
                Resource parentRes = resMgr.getResourceById(subject, parent);
                request.setAttribute("parentName", parentRes.getName());
            } else if (resourceId > 0) {
                boolean monitoringConfigured = isMonitoringConfigured(resourceId);
                request.setAttribute(AttrConstants.MONITOR_ENABLED_ATTR, monitoringConfigured);
                if (monitoringConfigured) {
                    log.debug("Obtaining metric schedules for resource " + resourceId + "...");
                    measurementSchedules = scheduleManager.findScheduleCompositesForResource(subject, resourceId,
                        null, pageControl);
                }
            } else if (groupId > 0) {
                boolean monitoringConfigured = true; // isMonitoringConfiguredForGroup(groupId); // TODO implement the method, see below
                request.setAttribute(AttrConstants.MONITOR_ENABLED_ATTR, true); // TODO change true -> monitoringConfigured
                if (monitoringConfigured) {
                    log.debug("Obtaining metric schedules for comp group " + groupId + "...");
                    measurementSchedules = scheduleManager.findSchedulesForCompatibleGroup(subject, groupId,
                        pageControl);
                    request.setAttribute(AttrConstants.GROUP_ID, groupId);
                }

                //adding the groupName into the request to display on config page.
                ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
                String groupName = resourceGroupManager
                    .getResourceGroupById(subject, groupId, GroupCategory.COMPATIBLE).getName();
                request.setAttribute(AttrConstants.GROUP_NAME, groupName);
            }
        }

        // if we did not find anything to work on
        if (measurementSchedules == null) {
            measurementSchedules = new PageList<MeasurementScheduleComposite>(pageControl);
        }

        request.setAttribute(AttrConstants.MEASUREMENT_SCHEDULES_ATTR, measurementSchedules);
        return null;
    }

    private boolean isMonitoringConfigured(int resourceId) {
        // TODO: This needs to be updated to use the new mechanism for determining if a resource has
        //       been configured. See JBNADM-1250. (ips, 04/10/07).
        /*InventoryHelper helper = InventoryHelper.getHelper(appdefId);
         *boolean monitoringConfigured = helper.isResourceConfigured(request, ctx, true);*/
        return true;
    }
}