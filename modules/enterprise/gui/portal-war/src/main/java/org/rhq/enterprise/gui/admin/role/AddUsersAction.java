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

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that adds users for a role.
 */
public class AddUsersAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddUsersAction.class.getName());
        HttpSession session = request.getSession();
        AddUsersForm addForm = (AddUsersForm) form;
        Integer roleId = addForm.getR();
        ActionForward forward = checkSubmit(request, mapping, form, Constants.ROLE_PARAM, roleId);

        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                log.trace("removing pending user list");
                SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
            } else if (spiderForm.isAddClicked()) {
                log.trace("adding to pending user list");
                SessionUtils.addToList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getAvailableUsers());
            } else if (spiderForm.isRemoveClicked()) {
                log.trace("removing from pending user list");
                SessionUtils.removeFromList(session, Constants.PENDING_USERS_SES_ATTR, addForm.getPendingUsers());
            }

            return forward;
        }

        log.debug("getting role [" + roleId + "]");

        log.debug("getting pending user list");
        Integer[] pendingUserIds = SessionUtils.getList(request.getSession(), Constants.PENDING_USERS_SES_ATTR);
        for (int i = 0; i < pendingUserIds.length; i++) {
            log.debug("adding user [" + pendingUserIds[i] + "] for role [" + roleId + "]");
        }

        try {
            LookupUtil.getRoleManager().assignSubjectsToRole(RequestUtils.getSubject(request), roleId, pendingUserIds);
        } catch (PermissionException pe) {
            RequestUtils.setError(request, "admin.role.error.StaticRole");
            return returnFailure(request, mapping, Constants.ROLE_PARAM, roleId);
        } finally {
            log.debug("removing pending user list");
            SessionUtils.removeList(session, Constants.PENDING_USERS_SES_ATTR);
        }

        RequestUtils.setConfirmation(request, "admin.role.confirm.AddUsers");
        return returnSuccess(request, mapping, Constants.ROLE_PARAM, roleId);
    }
}