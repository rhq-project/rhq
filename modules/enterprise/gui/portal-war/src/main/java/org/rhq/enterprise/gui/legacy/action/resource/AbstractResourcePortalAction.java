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
package org.rhq.enterprise.gui.legacy.action.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An abstract subclass of <code>BaseDispatchAction</code> that provides common methods for {@link Resource} portal
 * actions. TODO: This can be deleted after we make sure there's nothing else we want to move over to
 * ResourceController. (ips, 11/13/07)
 */
@Deprecated
public abstract class AbstractResourcePortalAction extends BaseDispatchAction {
    private final Log log = LogFactory.getLog(AbstractResourcePortalAction.class);

    protected void setResource(HttpServletRequest request) throws Exception {
        boolean requiresResourceToBeConfigured = false;
        setResource(request, requiresResourceToBeConfigured);
    }

    protected void setResource(HttpServletRequest request, boolean requiresResourceToBeConfigured) throws Exception {
        int resourceId = RequestUtils.getIntParameter(request, "id");
        WebUser webUser = RequestUtils.getWebUser(request);
        try {
            log.info("finding resource with ID " + resourceId + "...");

            Resource resource = LookupUtil.getResourceManager().getResourceById(webUser.getSubject(), resourceId);

            // TODO (ips): Check if resource has been configured successfully yet.
            //InventoryHelper helper = InventoryHelper.getHelper(null);
            //helper.isResourceConfigured(request, ctx, !config);

            RequestUtils.setResource(request, resource);
            request.setAttribute(AttrConstants.TITLE_PARAM_ATTR, resource.getName());
            request.setAttribute(AttrConstants.CONTROL_ENABLED_ATTR, supportsInvocation(resource));
            request.setAttribute(AttrConstants.PERFORMANCE_SUPPORTED_ATTR, supportsResponseTime(resource));

            // TODO (ips): Set the custom properties (or custom set of string metrics) in the request.
            //request.setAttribute("cprops", cprops);
        } catch (ResourceNotFoundException e) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_NOT_FOUND);
        }
    }

    protected void fetchReturnPathParams(HttpServletRequest request, Map params) {
        Integer resourceId = RequestUtils.getResourceId(request);
        params.put(Constants.RESOURCE_ID_PARAM, resourceId);

        try {
            //AppdefEntityTypeID ctype = RequestUtils.getChildResourceTypeId(request);
            //params.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, ctype);
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
        }

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        params.put(Constants.MODE_PARAM, mode);
    }

    /**
     * This sets the return path for a ResourceAction by appending the type and resource id to the forward url.
     *
     * @param  request The current controller's request.
     * @param  mapping The current controller's mapping that contains the input.
     *
     * @throws ParameterNotFoundException if the type or id are not found
     * @throws ServletException           If there is not input defined for this form
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        this.fetchReturnPathParams(request, params);
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }

    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping) throws Exception {
        setReturnPath(request, mapping, new HashMap());
    }

    /**
     * This method sets the current location for the nav map
     */
    protected void setNavMapLocation(HttpServletRequest request, ActionMapping mapping, String currLoc)
        throws Exception {
        Map params = new HashMap();

        // Set the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        params.put(Constants.MODE_PARAM, mode);
        String newUrl = ActionUtils.changeUrl(currLoc, params);
        request.setAttribute(Constants.CURR_RES_LOCATION_MODE, mode);
        request.setAttribute(Constants.CURR_RES_LOCATION_TYPE, currLoc);
        request.setAttribute(Constants.CURR_RES_LOCATION_TAG, newUrl);
    }

    private boolean supportsInvocation(Resource resource) {
        return true;
        //return !resource.getResourceType().getControlActionDefinitions().isEmpty(); // TODO
    }

    private boolean supportsResponseTime(Resource resource) {
        Set<MeasurementDefinition> measurementDefs = resource.getResourceType().getMetricDefinitions();
        if (measurementDefs != null) {
            for (MeasurementDefinition measurementDef : measurementDefs) {
                if (measurementDef.getDataType() == DataType.CALLTIME) {
                    return true;
                }
            }
        }

        return false;
    }
}