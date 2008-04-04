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

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 */
public class NewGroupAction extends BaseAction {
    private Log log = LogFactory.getLog(NewGroupAction.class);

    /**
     * Create the group with the attributes specified in the given <code>GroupForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        GroupForm newForm = (GroupForm) form;

        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        Subject subject = RequestUtils.getSubject(request);
        Integer resourceTypeId = newForm.getResourceTypeId();
        String newGroupName = newForm.getName();
        GroupCategory category = null;
        try {
            category = newForm.getGroupCategory();
        } catch (Exception e) {
            RequestUtils.setError(request, "resource.group.inventory.error.GroupTypeIsRequired");
            return returnFailure(request, mapping);
        }

        ResourceTypeManagerLocal resourceTypeManager = LookupUtil.getResourceTypeManager();
        ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

        ResourceGroup newGroup = new ResourceGroup(newGroupName);
        try {
            if (category == GroupCategory.COMPATIBLE) {
                ResourceType groupResourceType = resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
                newGroup.setResourceType(groupResourceType);
            }
        } catch (ResourceTypeNotFoundException ex) {
            log.debug("group created failed: ", ex);
            RequestUtils.setError(request, "resource.group.inventory.error.ResourceTypeIdNotFound");
            return returnFailure(request, mapping);
        }

        String location = newForm.getLocation();
        String description = newForm.getDescription();
        boolean recursive = newForm.isRecursive();

        try {
            newGroup.setDescription(description);
            newGroup.setLocation(location);
            newGroup.setRecursive(recursive);

            log.trace("creating group [" + newForm.getName() + "]" + " with attributes " + newForm);

            // ctime, mtime, and modifiedBy should all be persistence hooks
            resourceGroupManager.createResourceGroup(subject, newGroup);
        } catch (ResourceGroupAlreadyExistsException ex) {
            log.debug("group creation failed: ", ex);
            RequestUtils.setError(request, "resource.group.inventory.error.DuplicateGroupName");
            return returnFailure(request, mapping);
        }

        // Check for resources
        Integer groupId = newGroup.getId();
        Integer[] newResourceIds = newForm.getResourceIds();
        if ((newResourceIds != null) && (newResourceIds.length > 0)) {
            // Clean up after ourselves first
            HttpSession session = request.getSession();
            session.removeAttribute(Constants.RESOURCE_IDS_ATTR);
            session.removeAttribute(Constants.RESOURCE_TYPE_ATTR);

            // Now add the new entities to group
            resourceGroupManager.addResourcesToGroup(subject, groupId, newResourceIds);
        }

        RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.CreateGroup", newGroupName);

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(HubConstants.PARAM_GROUP_ID, groupId);
        forwardParams.put(HubConstants.PARAM_GROUP_CATEGORY, category.name());

        return returnNew(request, mapping, forwardParams);
    }
}