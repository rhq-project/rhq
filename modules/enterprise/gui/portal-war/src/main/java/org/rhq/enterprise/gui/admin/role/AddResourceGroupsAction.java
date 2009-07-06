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
package org.rhq.enterprise.gui.admin.role;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that adds resource groups for a role.
 */
public class AddResourceGroupsAction extends BaseAction {
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddResourceGroupsAction.class.getName());
        HttpSession session = request.getSession();

        AddResourceGroupsForm addForm = (AddResourceGroupsForm) form;
        Integer roleId = addForm.getR();

        ActionForward forward = checkSubmit(request, mapping, form, Constants.ROLE_PARAM, roleId);
        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                log.debug("removing pending group list");
                SessionUtils.removeList(session, Constants.PENDING_RESGRPS_SES_ATTR);
            } else if (spiderForm.isAddClicked()) {
                log.debug("adding to pending group list");
                SessionUtils.addToList(session, Constants.PENDING_RESGRPS_SES_ATTR, addForm.getAvailableGroups());
            } else if (spiderForm.isRemoveClicked()) {
                log.debug("removing from pending group list");
                SessionUtils.removeFromList(session, Constants.PENDING_RESGRPS_SES_ATTR, addForm.getPendingGroups());
            }

            return forward;
        }

        log.debug("getting pending group list");
        int[] pendingGroupIds = ArrayUtils.unwrapArray(SessionUtils
            .getList(session, Constants.PENDING_RESGRPS_SES_ATTR));
        for (int i = 0; i < pendingGroupIds.length; i++) {
            log.debug("adding group [" + pendingGroupIds[i] + "] for role [" + roleId + "]");
        }

        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        roleManager.addResourceGroupsToRole(RequestUtils.getSubject(request), roleId, pendingGroupIds);

        log.debug("removing pending group list");
        SessionUtils.removeList(session, Constants.PENDING_RESGRPS_SES_ATTR);

        RequestUtils.setConfirmation(request, "admin.role.confirm.AddResourceGroups");
        return returnSuccess(request, mapping, Constants.ROLE_PARAM, roleId);
    }
}