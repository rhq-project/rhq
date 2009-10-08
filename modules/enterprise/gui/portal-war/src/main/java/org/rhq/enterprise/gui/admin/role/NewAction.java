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

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An <code>Action</code> subclass that creates a role.
 */
public class NewAction extends BaseAction {
    /**
     * Create the role with the attributes specified in the given <code>RoleForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(NewAction.class.getName());

        RoleForm newForm = (RoleForm) form;

        ActionForward forward = checkSubmit(request, mapping, form, true);
        if (forward != null) {
            return forward;
        }

        Role role = new Role(newForm.getName());
        role.setDescription(newForm.getDescription());
        role.setFsystem(false);

        List<String> newPermissionStrings = newForm.getPermissionsStrings();
        for (String permString : newPermissionStrings) {
            Permission p = Enum.valueOf(Permission.class, permString);
            role.addPermission(p);
        }

        log.trace("creating role [" + role.getName() + "] with attributes " + newForm);

        try {
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();
            role = roleManager.createRole(RequestUtils.getSubject(request), role);
        } catch (Exception ex) {
            log.debug("role creation failed:", ex);
            RequestUtils.setError(request, Constants.ERR_ROLE_CREATION);
            return returnFailure(request, mapping);
        }

        log.trace("new role id: [" + role.getId() + "]");

        RequestUtils.setConfirmation(request, "admin.role.confirm.Create", role.getName());
        return returnNew(request, mapping, Constants.ROLE_PARAM, role.getId());
    }
}