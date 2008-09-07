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
package org.rhq.enterprise.gui.legacy.portlet.addresource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.LabelValueBean;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data from the user preferences of the form. The purpose of this is to add resources to the
 * resource health dashboard widget This implementation heavily based on:
 * org.rhq.enterprise.gui.admin.role.AddUsersRoleFormPrepareAction
 */
public class AddResourcesPrepareAction extends Action {
    private static final int DEFAULT_RESOURCE_TYPE = -1;

    @Override
    @SuppressWarnings( { "unchecked", "deprecation" })
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddResourcesPrepareAction.class);
        AddResourcesForm addForm = (AddResourcesForm) form;
        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(request);
        Subject subject = user.getSubject();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

        PageControl pcAvail = WebUtility.getPageControl(request, "a");
        PageControl pcPending = WebUtility.getPageControl(request, "p");
        log.trace("available page control: " + pcAvail);
        log.trace("pending page control: " + pcPending);

        // first, get the pending resources (those that have been added to the RHS
        // pending resources are those on the right side of the "add to list" widget that are awaiting association with the group when the form's "ok" button is clicked. */
        log.debug("check session if there are pending resources");
        List<String> pendingResourceList = (List<String>) session.getAttribute(Constants.PENDING_RESOURCES_SES_ATTR);

        if (pendingResourceList == null) {
            // if hitting the page for the first time, load resources already associated with user via preferences
            log.debug("get pending resources from user preferences");
            pendingResourceList = user.getPreferenceAsList(addForm.getKey(), "|");

            if (pendingResourceList != null) {
                // otherwise, we've been here for a while but the user paged, performed changed LHS<->RHS, etc
                log.debug("put entire list of pending resources in session");
                session.setAttribute(Constants.PENDING_RESOURCES_SES_ATTR, pendingResourceList);
            }
        }

        // get the resources, so we can display name & description in the UI
        log.debug("get page of pending resources selected by user");
        Integer[] pendingResourceArray = StringUtility.getIntegerArray(pendingResourceList);
        PageList<Resource> pendingResources = resourceManager.getResourceByIds(subject, pendingResourceArray, false,
            pcPending);

        // give 'em to the jsp page
        log.debug("put selected page of pending resources in request");
        request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pendingResources);
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR, pendingResources.getTotalSize());

        // available resources are all resources in the system that are not associated with the user and are not pending
        log.debug("determine if user wants to filter available resources");
        Integer typeIdFilter = ((addForm.getType() == null) || (addForm.getType() == DEFAULT_RESOURCE_TYPE)) ? null
            : addForm.getType();
        ResourceCategory categoryFilter = (addForm.getCategory() != null) ? ResourceCategory.valueOf(addForm
            .getCategory().toUpperCase()) : ResourceCategory.PLATFORM;

        Integer[] excludeIds = StringUtility.getIntegerArray(pendingResourceList);
        PageList<Resource> availableResources = null;

        availableResources = resourceManager.getAvailableResourcesForDashboardPortlet(subject, typeIdFilter,
            categoryFilter, excludeIds, pcAvail);

        log.debug("put selected page of available resources in request");
        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, availableResources);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR, availableResources.getTotalSize());

        log.debug("get the available resources user can filter by");
        setDropDowns(addForm, request, subject, categoryFilter);

        return null;
    }

    /*
     * groups aren't resources anymore, so we don't support them as a category filter anymore
     */
    private void setDropDowns(AddResourcesForm addForm, HttpServletRequest request, Subject subject,
        ResourceCategory category) throws Exception {
        // category drop-down
        for (ResourceCategory nextCategory : ResourceCategory.values()) {
            addForm.addFunction(new LabelValueBean(nextCategory.toString(), nextCategory.name()));
        }

        // type drop-down
        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        List<ResourceType> resourceTypes = resourceTypeManager.getUtilizedResourceTypesByCategory(subject, category,
            null);
        for (ResourceType nextType : resourceTypes) {
            addForm.addType(new LabelValueBean(nextType.getName(), String.valueOf(nextType.getId())));
        }
    }
}