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
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupUpdateException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that adds Resources to a Group in the BizApp. This is first created with
 * AddGroupResourcesFormPrepareAction, which creates the list of pending Resources to add to the group. Heavily based
 * on:
 *
 * @see org.rhq.enterprise.gui.legacy.action.resource.group.inventory.AddGroupResourcesFormPrepareAction
 */
public class AddGroupResourcesAction extends BaseAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Add roles to the user specified in the given <code>AddGroupResourcesForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddGroupResourcesAction.class);
        HttpSession session = request.getSession();

        AddGroupResourcesForm addForm = (AddGroupResourcesForm) form;

        Integer groupId = addForm.getGroupId();
        String category = addForm.getCategory();

        Map<String, Object> forwardParams = new HashMap<String, Object>(2);
        forwardParams.put(HubConstants.PARAM_GROUP_ID, groupId);
        forwardParams.put(HubConstants.PARAM_GROUP_CATEGORY, category);

        try {
            ActionForward forward = checkSubmit(request, mapping, form, forwardParams);
            if (forward != null) {
                BaseValidatorForm spiderForm = (BaseValidatorForm) form;

                if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                    log.trace("removing pending/removed resources list");
                    SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);
                } else if (spiderForm.isAddClicked()) {
                    log.trace("adding to pending resources list");
                    SessionUtils.addToList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm
                        .getAvailableResources());
                } else if (spiderForm.isRemoveClicked()) {
                    log.trace("removing from pending resources list");
                    SessionUtils.removeFromList(session, Constants.PENDING_RESOURCES_SES_ATTR, addForm
                        .getPendingResources());
                }

                return forward;
            }

            ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

            Subject user = RequestUtils.getSubject(request);

            log.trace("getting pending resource list");
            List<String> pendingResourceIdStrings = SessionUtils.getListAsListStr(request.getSession(),
                Constants.PENDING_RESOURCES_SES_ATTR);

            // Bug#7258; don't do anything if the user does not add any
            // members to the group.
            if (pendingResourceIdStrings.size() == 0) {
                return returnSuccess(request, mapping, forwardParams);
            }

            Integer[] pendingResourceIds = new Integer[pendingResourceIdStrings.size()];
            for (int i = 0; i < pendingResourceIds.length; i++) {
                pendingResourceIds[i] = Integer.valueOf(pendingResourceIdStrings.get(i));
            }

            groupManager.addResourcesToGroup(user, groupId, pendingResourceIds);

            // but how does this get removed when the user
            // navigates away instead of concluding this workflow
            log.trace("removing pending user list");
            SessionUtils.removeList(session, Constants.PENDING_RESOURCES_SES_ATTR);

            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.AddResources");

            return returnSuccess(request, mapping, forwardParams);
        } catch (ResourceGroupUpdateException rgue) {
            log.debug("group update failed:", rgue);
            RequestUtils.setError(request, "resource.group.inventory.error.GroupUpdateError", rgue.getMessage());
            return returnFailure(request, mapping, forwardParams);
        } catch (ResourceGroupNotFoundException raee) {
            log.debug("group update failed:", raee);
            RequestUtils.setError(request, "resource.group.inventory.error.GroupNotFound");
            return returnFailure(request, mapping, forwardParams);
        }
    }
}