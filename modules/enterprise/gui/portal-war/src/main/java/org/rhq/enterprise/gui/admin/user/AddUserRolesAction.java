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
package org.rhq.enterprise.gui.admin.user;

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
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that adds Roles to a User. This is first created with AddUserRolesFormPrepareAction, which creates the list
 * of pending Roles to add to the user. Heavily based on: AddUserFormPrepareAction
 */
public class AddUserRolesAction extends BaseAction {
    /**
     * Add roles to the user specified in the given <code>AddUserRolesForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddUserRolesAction.class.getName());
        HttpSession session = request.getSession();

        AddUserRolesForm addForm = (AddUserRolesForm) form;
        Integer userId = addForm.getU();

        ActionForward forward = checkSubmit(request, mapping, form, Constants.USER_PARAM, userId);

        if (forward != null) {
            BaseValidatorForm spiderForm = (BaseValidatorForm) form;

            if (spiderForm.isCancelClicked() || spiderForm.isResetClicked()) {
                log.trace("removing pending roles list");
                SessionUtils.removeList(session, Constants.PENDING_ROLES_SES_ATTR);
            } else if (spiderForm.isAddClicked()) {
                log.trace("adding to pending roles list");
                SessionUtils.addToList(session, Constants.PENDING_ROLES_SES_ATTR, addForm.getAvailableRoles());
            } else if (spiderForm.isRemoveClicked()) {
                log.trace("removing from pending roles list");
                SessionUtils.removeFromList(session, Constants.PENDING_ROLES_SES_ATTR, addForm.getPendingRoles());
            }

            return forward;
        }

        Subject user = LookupUtil.getSubjectManager().findSubjectById(userId);
        if (user == null) {
            throw new NullPointerException("User [" + userId + "] is not known");
        }

        log.trace("getting pending role list");
        Integer[] pendingRoleIds = SessionUtils.getList(request.getSession(), Constants.PENDING_ROLES_SES_ATTR);
        for (int i = 0; i < pendingRoleIds.length; i++) {
            log.trace("adding role [" + pendingRoleIds[i] + "] for user [" + userId + "]");
        }

        LookupUtil.getRoleManager()
            .assignRolesToSubject(RequestUtils.getSubject(request), user.getId(), pendingRoleIds);

        log.trace("removing pending user list");
        SessionUtils.removeList(session, Constants.PENDING_ROLES_SES_ATTR);

        RequestUtils.setConfirmation(request, "admin.user.confirm.AddRoles");
        return returnSuccess(request, mapping, Constants.USER_PARAM, userId, false);
    }
}