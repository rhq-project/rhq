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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.MetricsDisplayMode;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Modifies either the measurement schedules for a resource instance or the default metric schedules for a resource
 * type.
 *
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class ConfigMetricsAction extends BaseAction {
    private final Log log = LogFactory.getLog(ConfigMetricsAction.class.getName());

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        ActionForward forward;
        log.trace("modify-metric-schedules action");

        Subject subject = WebUtility.getSubject(request);
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();

        Map<String, Integer> returnRequestParams = new HashMap<String, Integer>(2);

        // NOTE: This action will be passed either a resourceTypeId OR a resourceId OR a groupId or ...
        int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int resourceTypeId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_TYPE_ID_PARAM,
            -1);

        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);

        // Find out what we want to configure
        MetricsDisplayMode mode = WebUtility.getMetricsDisplayMode(request);

        // put the return params in the request
        switch (mode) {
        case RESOURCE_DEFAULT: {
            returnRequestParams.put(ParamConstants.RESOURCE_TYPE_ID_PARAM, resourceTypeId);
            break;
        }

        case RESOURCE: {
            returnRequestParams.put(ParamConstants.RESOURCE_ID_PARAM, resourceId);
            break;
        }

        case COMPGROUP: {
            returnRequestParams.put(ParamConstants.GROUP_ID_PARAM, groupId);

            break;
        }

        case AUTOGROUP: {
            returnRequestParams.put(ParamConstants.RESOURCE_TYPE_ID_PARAM, resourceTypeId);
            returnRequestParams.put("parent", parent);
            break;
        }
        }

        forward = checkSubmit(request, mapping, form, returnRequestParams);
        MonitoringConfigForm monitoringConfigForm = (MonitoringConfigForm) form;
        int[] measurementDefinitionIdsToUpdate = monitoringConfigForm.getMids();

        // Don't make any back-end call if user has not selected any metrics.
        if (measurementDefinitionIdsToUpdate.length == 0) {
            return (forward != null) ? forward : returnSuccess(request, mapping, returnRequestParams);
        }

        /*
         * Do the real show. First see if we need to disable the collection. If not, change the collection interval
         * further down
         */
        if (forward != null) {
            if (monitoringConfigForm.isRemoveClicked()) {
                switch (mode) {
                case RESOURCE_DEFAULT: {
                    scheduleManager.disableDefaultCollectionForMeasurementDefinitions(subject,
                        measurementDefinitionIdsToUpdate);
                    break;
                }

                case RESOURCE: {
                    scheduleManager.disableMeasurementSchedules(subject, measurementDefinitionIdsToUpdate, resourceId);
                    break;
                }

                case COMPGROUP: {
                    scheduleManager.disableMeasurementSchedulesForCompatGroup(subject,
                        measurementDefinitionIdsToUpdate, groupId);
                    break;
                }

                case AUTOGROUP: {
                    scheduleManager.disableMeasurementSchedulesForAutoGroup(subject, measurementDefinitionIdsToUpdate,
                        parent, resourceTypeId);
                    break;
                }
                }

                RequestUtils.setConfirmation(request,
                    "resource.common.monitor.visibility.config.RemoveMetrics.Confirmation");
            }

            return forward;
        }

        // If we've fallen through to here, it's a request to update the collection interval...
        long newCollectionInterval = monitoringConfigForm.getIntervalTime();
        switch (mode) {
        case RESOURCE_DEFAULT: {
            scheduleManager.updateDefaultCollectionIntervalForMeasurementDefinitions(subject,
                measurementDefinitionIdsToUpdate, newCollectionInterval, monitoringConfigForm
                    .getSchedulesShouldChange());
            break;
        }

        case RESOURCE: {
            scheduleManager.updateMeasurementSchedules(subject, measurementDefinitionIdsToUpdate, resourceId,
                newCollectionInterval);
            break;
        }

        case COMPGROUP: {
            scheduleManager.updateMeasurementSchedulesForCompatGroup(subject, measurementDefinitionIdsToUpdate,
                groupId, newCollectionInterval);
            break;
        }

        case AUTOGROUP: {
            scheduleManager.updateMeasurementSchedulesForAutoGroup(subject, measurementDefinitionIdsToUpdate, parent,
                resourceTypeId, newCollectionInterval);
            break;
        }
        }

        RequestUtils.setConfirmation(request, "resource.common.monitor.visibility.config.ConfigMetrics.Confirmation");

        return returnSuccess(request, mapping, returnRequestParams);
    }
}