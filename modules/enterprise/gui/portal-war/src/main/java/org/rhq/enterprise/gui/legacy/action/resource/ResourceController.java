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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.inventory.resource.ResourceUIBean;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.MessageConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An abstract subclass of <code>BaseDispatchAction</code> that provides common methods for resource controller actions.
 *
 * @author Ian Springer
 */
public abstract class ResourceController extends BaseDispatchAction {
    private final Log log = LogFactory.getLog(ResourceController.class);

    protected void setResource(HttpServletRequest request) throws Exception {
        this.setResource(request, false);
    }

    protected void setResource(HttpServletRequest request, boolean config) throws Exception {
        try {
            Subject subject = WebUtility.getSubject(request);
            Integer resourceTypeId = WebUtility.getOptionalIntRequestParameter(request,
                ParamConstants.RESOURCE_TYPE_ID_PARAM, -1);
            int groupId = WebUtility.getOptionalIntRequestParameter(request, AttrConstants.GROUP_ID, -1);
            int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
            String[] r = request.getParameterValues("r");
            String[] resourceIds = request.getParameterValues("resourceIds");

            // TODO rewrite the selection using WebUtility.getMetricsDisplayMode()
            if ((resourceTypeId > 0) && (parent > 0)) // autogroup
            {
                ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
                ResourceType resourceType = resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
                request.setAttribute(AttrConstants.RESOURCE_TYPE_ATTR, resourceType);
                request.setAttribute(AttrConstants.TITLE_PARAM_ATTR, resourceType.getName());
                request.setAttribute("parent", parent);
                request.setAttribute(ParamConstants.RESOURCE_TYPE_ID_PARAM, resourceTypeId);
                if (log.isDebugEnabled()) {
                    log.debug("Autogroup p=" + parent + ", ct=" + resourceTypeId);
                }
            } else if (groupId > 0) // compat (or mixed) group
            {
                ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();
                ResourceGroup group = resourceGroupManager.getResourceGroupById(subject, groupId, null);
                request.setAttribute(AttrConstants.GROUP_ID, groupId);
                request.setAttribute(AttrConstants.TITLE_PARAM_ATTR, group.getName());
                // TODO more ?
            } else if ((resourceTypeId > 0) && (parent == -1)) // MeasurementDefinition
            {
                ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
                ResourceType resourceType = resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
                request.setAttribute(AttrConstants.RESOURCE_TYPE_ATTR, resourceType);
                request.setAttribute(ParamConstants.RESOURCE_TYPE_ID_PARAM, resourceTypeId);
            } else if ((r != null) && (r.length > 0)) // multiple scathered resources
            {
                log.trace("Multiple resources not handled yet"); // TODO what do we do here?
            } else if ((resourceIds != null) && (resourceIds.length > 0)) {
                log.trace("Multiple resources not yet handled"); // TODO what to we do here?
            } else // single resource
            {
                Integer resourceId = WebUtility.getRequiredIntRequestParameter(request,
                    ParamConstants.RESOURCE_ID_PARAM);
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
                Resource resource = resourceManager.getResourceById(subject, resourceId);
                ResourceUIBean resourceUIBean = new ResourceUIBean(resource, subject);
                request.setAttribute(AttrConstants.RESOURCE_ATTR, resource);
                request.setAttribute(AttrConstants.RESOURCE_ID_ATTR, resourceId);
                request.setAttribute(AttrConstants.TITLE_PARAM_ATTR, resource.getName());
                request.setAttribute(AttrConstants.PERFORMANCE_SUPPORTED_ATTR, resourceUIBean.getFacets().isCallTime());
            }
        } catch (ResourceNotFoundException e) {
            RequestUtils.setError(request, MessageConstants.ERR_RESOURCE_NOT_FOUND);
        }
    }

    protected void fetchReturnPathParams(HttpServletRequest request, Map params) {
        int groupId = WebUtility.getOptionalIntRequestParameter(request, AttrConstants.GROUP_ID, -1);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        int ctype = WebUtility.getOptionalIntRequestParameter(request, "ctype", -1); // TODO JBNADM-2630
        String[] resourceIds = request.getParameterValues("resourceIds");

        if (groupId > 0) // compat group
        {
            params.put(ParamConstants.GROUP_ID_PARAM, groupId);
            String category = request.getParameter(HubConstants.PARAM_GROUP_CATEGORY);
            if ((category == null) || category.equals("")) {
                category = "COMPATIBLE";
            }

            params.put(HubConstants.PARAM_GROUP_CATEGORY, category);
        } else if ((parent > 0) && ((type > 0) || (ctype > 0))) // autogroup
        {
            params.put("parent", parent);
            if (type > 0) {
                params.put("type", type);
            } else {
                params.put("type", ctype);
            }
        } else if ((resourceIds != null) && (resourceIds.length > 0)) {
            params.put("resourceIds", resourceIds);
        } else {
            params.put(ParamConstants.RESOURCE_ID_PARAM, WebUtility.getResourceId(request));
        }

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(ParamConstants.MODE_PARAM);
        params.put(ParamConstants.MODE_PARAM, mode);
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
    @Override
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        this.fetchReturnPathParams(request, params);
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }

    @Override
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping) throws Exception {
        setReturnPath(request, mapping, new HashMap());
    }

    /**
     * This method sets the current location for the nav map
     */
    protected void setNavMapLocation(HttpServletRequest request, ActionMapping mapping, String currLoc)
        throws Exception {
        Map<String, String> params = new HashMap<String, String>();

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(ParamConstants.MODE_PARAM);
        params.put(ParamConstants.MODE_PARAM, mode);

        String newUrl = ActionUtils.changeUrl(currLoc, params);

        request.setAttribute(AttrConstants.CURR_RES_LOCATION_MODE, mode);
        request.setAttribute(AttrConstants.CURR_RES_LOCATION_TYPE, currLoc);
        request.setAttribute(AttrConstants.CURR_RES_LOCATION_TAG, newUrl);
    }
}