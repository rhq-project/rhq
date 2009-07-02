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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data to facilitate display of the <em>AddUserRoles</em> form. The purpose of this is to add
 * Roles to a given user.
 */
public class AddUserRolesFormPrepareAction extends WorkflowPrepareAction {
    @Override
    @SuppressWarnings("deprecation")
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddUserRolesFormPrepareAction.class.getName());

        AddUserRolesForm addForm = (AddUserRolesForm) form;
        Integer userId = addForm.getU();

        if (userId == null) {
            userId = RequestUtils.getUserId(request);
        }

        PageControl pca = WebUtility.getPageControl(request, "a");
        PageControl pcp = WebUtility.getPageControl(request, "p");

        log.trace("available page control: " + pca);
        log.trace("pending page control: " + pcp);
        log.trace("getting user [" + userId + "]");

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();

        Subject user = subjectManager.getSubjectById(userId);
        if (user == null) {
            throw new NullPointerException("User [" + userId + "] is not known");
        }

        request.setAttribute(Constants.USER_ATTR, user);
        addForm.setU(user.getId());

        /* pending roles are those on the right side of the "add
         * to list" widget- awaiting association with the userwhen the form's "ok" button is clicked. */
        Integer[] pendingRoleIds = SessionUtils.getList(request.getSession(), Constants.PENDING_ROLES_SES_ATTR);

        log.trace("getting pending roles for user [" + userId + "]");
        PageList<Role> pendingRoles = roleManager.getRolesById(pendingRoleIds, pcp);
        request.setAttribute(Constants.PENDING_ROLES_ATTR, pendingRoles);

        /*
         * available roles are all roles in the system that are not associated with the user and are not pending
         */
        log.trace("getting available roles for user [" + userId + "]");
        PageList<Role> availableRoles = roleManager.getAvailableRolesForSubject(RequestUtils.getSubject(request),
            userId, pendingRoleIds, pca);
        request.setAttribute(Constants.AVAIL_ROLES_ATTR, availableRoles);

        return null;
    }
}