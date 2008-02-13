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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.AuthzConstants;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * An Action that retrieves a role from the BizApp to facility display of the <em>Change Role Owner</em> form.
 */
public class ChangeOwnerFormPrepareAction extends WorkflowPrepareAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve the full <code>List</code> of <code>AuthzSubjectValue</code> objects representing all users in the
     * database excluding the owner of the role identified by the request parameter <code>Constants.ROLE_PARAM</code>
     * and store that list in in the <code>Constants.ALL_USERS_ATTR</code> request attribute. Also store the <code>
     * RoleValue</code> itself in the <code>Constants.ROLE_ATTR</code> request attribute.
     */
    @Override
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(ChangeOwnerFormPrepareAction.class.getName());

        ChangeOwnerForm changeForm = (ChangeOwnerForm) form;
        Integer roleId = changeForm.getR();

        if (roleId == null) {
            roleId = RequestUtils.getRoleId(request);
        }

        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }

        if (AuthzConstants.authzResourceGroupId.intValue() == roleId.intValue()) {
            throw new PermissionException("can't change super user role");
        }

        changeForm.setR(role.getId());

        //        Integer sessionId = RequestUtils.getSessionId(request);
        //        PageControl pc = RequestUtils.getPageControl(request);
        ServletContext ctx = getServlet().getServletContext();
        //        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);

        log.trace("getting all users");

        //        PageList allUsers = boss.getAllSubjects(sessionId, pc);
        //
        //        // remove the role's owner from the list of users
        //        ArrayList owner = new ArrayList();
        //        //owner.add(role.getOwner());
        //        List users = BizappUtils.grepSubjects(allUsers, owner);
        //
        //        request.setAttribute(Constants.ALL_USERS_ATTR, users);
        //        // we want to use the (allUsers.totalSize - 1) here
        //        // instead of users.size so that the paging controls work
        //        // (PR 4774)
        //        request.setAttribute( Constants.NUM_USERS_ATTR,
        //                              new Integer(allUsers.getTotalSize() - 1) );
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return null;
    }
}