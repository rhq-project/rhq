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
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A <code>BaseAction</code> subclass that removes associated users from a role.
 */
public class RemoveUsersAction extends BaseAction {
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(RemoveUsersAction.class.getName());

        RemoveUsersForm rmForm = (RemoveUsersForm) form;
        Integer roleId = rmForm.getR();

        Integer[] users = rmForm.getUsers();
        log.trace("removing users " + users + "] from role [" + roleId + "]");

        Integer[] role_array = new Integer[] { roleId };
        RoleManagerLocal roleManager = LookupUtil.getRoleManager();

        for (Integer user : users) {
            roleManager.removeRolesFromSubject(RequestUtils.getSubject(request), user, role_array);
        }

        RequestUtils.setConfirmation(request, "admin.role.confirm.RemoveUsers");
        return returnSuccess(request, mapping, Constants.ROLE_PARAM, roleId);
    }
}