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

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Action that allows a user to edit his own password or an admin to edit anyone's password.
 */
public class EditPasswordAction extends BaseAction {
    /**
     * @see BaseAction#execute(org.apache.struts.action.ActionMapping,org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(NewAction.class.getName());

        EditPasswordForm pForm = (EditPasswordForm) form;
        ActionForward forward = checkSubmit(request, mapping, form, ParamConstants.USER_PARAM, pForm.getId());

        if (forward != null) {
            return forward;
        }

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        Subject subject = WebUtility.getSubject(request);
        int subjectSession = subject.getSessionId();

        Subject userToBeModified = subjectManager.getSubjectById(pForm.getId());

        String userName = userToBeModified.getName();
        log.debug("Editing password for user [" + userName + "]");

        boolean admin = LookupUtil.getAuthorizationManager().hasGlobalPermission(subject, Permission.MANAGE_SECURITY);
        boolean isSameUser = subject.getName().equals(userName);

        // if this user cannot administer other user's passwords, make sure he gave the old password as confirmation
        if (!admin) {
            try {
                int dummySession = subjectManager.loginLocal(userName, pForm.getCurrentPassword()).getSessionId();
                subjectManager.logout(dummySession);

                // The above killed our session for subject if subject == userToBeModified
                if (isSameUser) {
                    subject = subjectManager.loginLocal(userName, pForm.getCurrentPassword());
                }
            } catch (LoginException e) {
                RequestUtils.setError(request, "admin.user.error.WrongPassword", "currentPassword");
                return returnFailure(request, mapping, ParamConstants.USER_PARAM, pForm.getId());
            }
        }

        String newPassword = pForm.getNewPassword();
        subjectManager.changePassword(subject, userName, newPassword);

        // when we have arrived here, the password is changed.
        // If this was a change of our own password, we need to re-login now
        if (isSameUser) {
            subjectManager.logout(subject.getSessionId());
        }

        return returnSuccess(request, mapping, ParamConstants.USER_PARAM,

        pForm.getId());
    }
}