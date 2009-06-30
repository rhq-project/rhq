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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.exception.UpdateException;
import org.rhq.enterprise.server.resource.group.ResourceGroupAlreadyExistsException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Action which saves the general properties for a group
 */
public class EditGeneralAction extends BaseAction {
    /**
     * Create the server with the attributes specified in the given <code>GroupForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditGeneralAction.class);

        GroupForm groupForm = (GroupForm) form;

        Integer groupId = groupForm.getGroupId();
        String category = groupForm.getCategory();
        Subject subject = RequestUtils.getSubject(request);

        HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(HubConstants.PARAM_GROUP_ID, groupId);
        forwardParams.put(HubConstants.PARAM_GROUP_CATEGORY, category);

        ActionForward forward = checkSubmit(request, mapping, form, forwardParams, BaseAction.YES_RETURN_PATH);

        if (forward != null) {
            return forward;
        }

        try {
            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
            ResourceGroup group = groupManager.getResourceGroupById(subject, groupId, null);
            groupForm.updateResourceGroup(group);
            groupManager.updateResourceGroup(subject, group);

            log.trace("saving group [" + group.getName() + "]" + " with attributes " + groupForm);

            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.EditGeneralProperties");
            return returnSuccess(request, mapping, forwardParams, BaseAction.YES_RETURN_PATH);
        } catch (UpdateException ue) {
            log.debug("group update failed:", ue);
            if (ue.getCause() instanceof ResourceGroupAlreadyExistsException) {
                RequestUtils.setError(request, "resource.group.inventory.error.DuplicateGroupName", ue.getMessage());
            } else {
                RequestUtils.setError(request, "resource.group.inventory.error.GroupUpdateError", ue.getMessage());
            }
            return returnFailure(request, mapping, forwardParams);
        }
    }
}