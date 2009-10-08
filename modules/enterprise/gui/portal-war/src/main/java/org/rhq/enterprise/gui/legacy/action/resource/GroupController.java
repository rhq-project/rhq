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
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An abstract subclass of <code>BaseDispatchAction</code> that provides common methods for resource controller actions.
 */
public abstract class GroupController extends BaseDispatchAction {
    protected final Log log = LogFactory.getLog(ResourceController.class);

    protected void setResourceGroup(HttpServletRequest request) throws Exception {
        this.setResourceGroup(request, false);
    }

    protected void setResourceGroup(HttpServletRequest request, boolean config) throws Exception {
        Integer groupId = null;
        try {
            try {
                groupId = RequestUtils.getGroupId(request);
            } catch (ParameterNotFoundException e) {
                // not a problem, this can be null
            }

            Subject subject = RequestUtils.getSubject(request);
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();

            // auto-group support (of plats?)
            // request.setAttribute(Constants.CONTROL_ENABLED_ATTR, Boolean.FALSE);
            // request.setAttribute(Constants.PERFORMANCE_SUPPORTED_ATTR, Boolean.FALSE);

            //            AppdefEntityTypeID aetid = null;
            //            if (null == groupId/* || groupId instanceof AppdefEntityTypeID*/) {
            //                // this can happen if we're an auto-group of platforms
            //
            //                try {
            //                    if (groupId != null)
            //                        aetid = (AppdefEntityTypeID) groupId;
            //                    else
            //                        aetid = new AppdefEntityTypeID(
            //                            RequestUtils.getStringParameter(
            //                                request, Constants.APPDEF_RES_TYPE_ID));
            //
            ///*                    AppdefResourceTypeValue resourceTypeVal =
            //                        appdefBoss.findResourceTypeById(sessionId.intValue(),
            //                                                        aetid);*/
            //                    ResourceType resourceType = resourceTypeManager.getResourceTypeById(subject, aetid.getId());
            //
            //                    //request.setAttribute(Constants.RESOURCE_TYPE_ATTR, resourceTypeVal);
            //                    request.setAttribute(Constants.RESOURCE_TYPE_ATTR, resourceType);
            //                    // Set the title parameters
            //                    //request.setAttribute(Constants.TITLE_PARAM_ATTR, resourceTypeVal.getName());
            //                    request.setAttribute(Constants.TITLE_PARAM_ATTR, resourceType.getName());
            //                } catch (Exception e) {
            //                    // if this param isnt found, leave aetid null
            //                }
            //            } else {
            log.trace("finding group [" + groupId + "]");

            ResourceGroup group = groupManager.getResourceGroupById(subject, groupId, null);
            GroupCategory category = group.getGroupCategory();

            log.trace("finding most recent modifier for resource [" + groupId + "]");
            Subject modifier = group.getModifiedBy();

            // TODO: jmarques - how are we going to do control on ResourceGroups?
            boolean isControllable = LookupUtil.getOperationManager().isGroupOperationSupported(subject, group.getId());

            RequestUtils.setResourceGroup(request, group);
            request.setAttribute(AttrConstants.RESOURCE_MODIFIER_ATTR, modifier);
            request.setAttribute(AttrConstants.TITLE_PARAM_ATTR, group.getName());
            request.setAttribute("category", category.name());
            request.setAttribute("groupId", groupId);
            request.setAttribute(AttrConstants.CONTROL_ENABLED_ATTR, new Boolean(isControllable));

            // }
        } catch (ResourceGroupNotFoundException e) {
            RequestUtils.setError(request, Constants.ERR_RESOURCE_GROUP_NOT_FOUND);
        }
    }

    protected void fetchReturnPathParams(HttpServletRequest request, Map params) {
        Integer groupId = RequestUtils.getGroupId(request);
        GroupCategory gc = RequestUtils.getGroupCategory(request);
        String category;
        if (gc != null)
            category = gc.name();
        else {
            category = GroupCategory.COMPATIBLE.name(); // Set a default
            log.warn("fetchReturnPathParam: No category was given, assuming compatible");
        }
        params.put(HubConstants.PARAM_GROUP_ID, groupId);
        params.put(HubConstants.PARAM_GROUP_CATEGORY, category);

        try {
            // TODO use "type" uniformly
            params.put(ParamConstants.CHILD_RESOURCE_TYPE_ID_PARAM, WebUtility.getChildResourceTypeId(request));
        } catch (ParameterNotFoundException pnfe) {
            // that's ok!
        }

        try { // Deprecated
            Integer autogrouptype = RequestUtils.getAutogroupResourceTypeId(request);
            params.put(Constants.AUTOGROUP_TYPE_ID_PARAM, autogrouptype);
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
     * @param     request The current controller's request.
     * @param     mapping The current controller's mapping that contains the input.
     *
     * @exception ParameterNotFoundException if the type or id are not found
     * @exception ServletException           If there is not input defined for this form
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
        HashMap parms = new HashMap();

        // sets the returnPath to match the mode we're in.
        String mode = request.getParameter(Constants.MODE_PARAM);
        parms.put(Constants.MODE_PARAM, mode);

        String newUrl = ActionUtils.changeUrl(currLoc, parms);

        request.setAttribute(AttrConstants.CURR_RES_LOCATION_MODE, new String(mode));
        request.setAttribute(AttrConstants.CURR_RES_LOCATION_TYPE, new String(currLoc));
        request.setAttribute(AttrConstants.CURR_RES_LOCATION_TAG, newUrl);
    }
}