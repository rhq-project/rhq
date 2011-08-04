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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the <em>Add Role Users</em> form.
 */
public class AddUsersFormPrepareAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddUsersFormPrepareAction.class.getName());

        AddUsersForm addForm = (AddUsersForm) form;
        Integer roleId = addForm.getR();

        if (roleId == null) {
            roleId = RequestUtils.getRoleId(request);
        }

        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }

        addForm.setR(role.getId());

        PageControl pca = WebUtility.getPageControl(request, "a");
        PageControl pcp = WebUtility.getPageControl(request, "p");

        log.trace("available page control: " + pca);
        log.trace("pending page control: " + pcp);

        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

        /* pending users are those on the right side of the "add
         * to list" widget- awaiting association with the rolewhen the form's "ok" button is clicked. */
        Integer[] pendingUserIds = SessionUtils.getList(request.getSession(), Constants.PENDING_USERS_SES_ATTR);

        log.trace("getting pending users for role [" + roleId + "]");
        SubjectCriteria c = new SubjectCriteria();
        c.addFilterIds(pendingUserIds);
        c.addFilterFsystem(false);
        c.addFilterFactive(true);
        c.fetchRoles(true);
        c.addSortName(PageOrdering.ASC);
        PageList<Subject> pendingUsers = subjectManager.findSubjectsByCriteria(RequestUtils.getSubject(request), c);
        request.setAttribute(Constants.PENDING_USERS_ATTR, pendingUsers);

        /*
         * available users are all users in the system that are /not/ associated with the role and are not pending
         */
        log.trace("getting available users for role [" + roleId + "]");
        PageList<Subject> availableUsers = subjectManager.findAvailableSubjectsForRole(
            RequestUtils.getSubject(request), roleId, pendingUserIds, pca);
        request.setAttribute(Constants.AVAIL_USERS_ATTR, availableUsers);

        return null;
    }
}
