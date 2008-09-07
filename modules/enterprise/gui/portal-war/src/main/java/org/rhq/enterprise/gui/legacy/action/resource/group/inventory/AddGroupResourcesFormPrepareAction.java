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
package org.rhq.enterprise.gui.legacy.action.resource.group.inventory;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the <em>AddGroupResources</em> form.
 */
public class AddGroupResourcesFormPrepareAction extends Action {
    private static final String CATEGORY_ALL = "None";

    /**
     * Retrieve this data and store it in the specified request parameters:
     *
     * <ul>
     *   <li><code>GroupValue</code> object identified by <code>Constants.RESOURCE_PARAM</code> request parameter in
     *     <code>Constants.RESOURCE_ATTR</code></li>
     *   <li><code>List</code> of available <code>AppdefResourceValue</code> objects (those not already associated with
     *     the group) in <code>Constants.AVAIL_RESOURCES_ATTR</code></li>
     *   <li><code>Integer</code> number of available roles in <code>Constants.NUM_AVAIL_RESOURCES_ATTR</code></li>
     *   <li><code>List</code> of pending <code>OwnedRoleValue</code> objects (those in queue to be associated with the
     *     resource) in <code>Constants.PENDING_RESOURCES_ATTR</code></li>
     *   <li><code>Integer</code> number of pending resources in <code>Constants.NUM_PENDING_RESOURCES_ATTR</code></li>
     *   <li><code>List</code> of pending <code>AppdefResourceValue</code> ids (those in queue to be associated with the
     *     resource) in <code>Constants.PENDING_RESOURCES_SES_ATTR</code></li>
     * </ul>
     *
     * This Action edits 2 lists of Resources: pending, and available.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddGroupResourcesFormPrepareAction.class);

        Subject user = RequestUtils.getSubject(request);

        AddGroupResourcesForm addForm = (AddGroupResourcesForm) form;
        Integer groupId = addForm.getGroupId();

        PageControl pcAvail = WebUtility.getPageControl(request, "a");
        PageControl pcPending = WebUtility.getPageControl(request, "p");

        log.trace("available page control: " + pcAvail);
        log.trace("pending page control: " + pcPending);
        log.trace("getting group [" + groupId + "]");

        /*
         * pending resources are those on the right side of the "add to list" widget that are awaiting association with
         * the group when the form's "ok" button is clicked.
         */
        List<String> pendingResourceIdStrings = SessionUtils.getListAsListStr(request.getSession(),
            Constants.PENDING_RESOURCES_SES_ATTR);
        Integer[] pendingResourceIds = new Integer[pendingResourceIdStrings.size()];
        for (int i = 0, sz = pendingResourceIdStrings.size(); i < sz; i++) {
            pendingResourceIds[i] = Integer.valueOf(pendingResourceIdStrings.get(i));
        }

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

        log.trace("getting pending resources for group [" + groupId + "]");
        // pass true so that the parent is each resource is connected
        PageList<Resource> pendingResources = resourceManager.getResourceByIds(user, pendingResourceIds, true,
            pcPending);

        request.setAttribute(Constants.PENDING_RESOURCES_ATTR, pendingResources);
        request.setAttribute(Constants.NUM_PENDING_RESOURCES_ATTR, pendingResources.size());

        /*
         * available resources are all resources in the system that are not associated with the user and are not pending
         */
        log.trace("getting available resources for group [" + groupId + "]");

        String nameFilter = RequestUtils.getStringParameter(request, "nameFilter", null);
        ResourceGroup resourceGroup = resourceGroupManager.getResourceGroupById(user, groupId, null);

        PageList<Resource> availableResources = null;
        if (resourceGroup.getGroupCategory() == GroupCategory.COMPATIBLE) {
            ResourceType compatibleTypeFilter = resourceGroup.getResourceType();
            availableResources = resourceManager.getAvailableResourcesForResourceGroup(user, groupId,
                compatibleTypeFilter, null, nameFilter, pendingResourceIds, pcAvail);
        } else if (resourceGroup.getGroupCategory() == GroupCategory.MIXED) {
            ResourceCategory resourceCategory = getResourceCategory(addForm.getFilterBy());
            availableResources = resourceManager.getAvailableResourcesForResourceGroup(user, groupId, null,
                resourceCategory, nameFilter, pendingResourceIds, pcAvail);
            addForm.setAvailResourceTypes(buildResourceTypes());
        } else {
            throw new IllegalArgumentException("AddGroupResourcesFormPrepareAction " + "does not support '"
                + resourceGroup.getClass().getSimpleName() + " group type");
        }

        request.setAttribute(Constants.AVAIL_RESOURCES_ATTR, availableResources);
        request.setAttribute(Constants.NUM_AVAIL_RESOURCES_ATTR, availableResources.size());

        return null;
    }

    private ResourceCategory getResourceCategory(String categoryFilter) {
        if ((categoryFilter == null) || categoryFilter.equals(CATEGORY_ALL)) {
            return null;
        } else {
            return ResourceCategory.valueOf(categoryFilter);
        }
    }

    /**
     * builds a list of resource types (platform, server, service).
     *
     * @return a list of group types from the list
     */
    private static List<LabelValueBean> buildResourceTypes() {
        List<LabelValueBean> resourceCategoryTypes = new ArrayList<LabelValueBean>();

        resourceCategoryTypes.add(new LabelValueBean(CATEGORY_ALL, CATEGORY_ALL));

        for (ResourceCategory category : ResourceCategory.values()) {
            resourceCategoryTypes.add(new LabelValueBean(category.toString(), category.name()));
        }

        return resourceCategoryTypes;
    }
}