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

import java.util.HashSet;
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
 * An Action that modifies a specific role.
 */
public class EditAction extends BaseAction {
    /**
     * Edit the role with the attributes specified in the given <code>RoleForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditAction.class.getName());

        RoleForm editForm = (RoleForm) form;
        Integer roleId = editForm.getR();

        ActionForward forward = checkSubmit(request, mapping, form, Constants.ROLE_PARAM, roleId);

        if (forward != null) {
            return forward;
        }

        log.trace("getting role [" + roleId + "]");

        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Role role = roleManager.getRoleById(roleId);
        role.setName(editForm.getName());
        role.setDescription(editForm.getDescription());
        role.setPermissions(new HashSet<Permission>(editForm.getPermissions()));

        log.trace("saving role [" + roleId + "]");

        try {
            roleManager.updateRole(RequestUtils.getSubject(request), role);
        } catch (Exception ex) {
            log.debug("role update failed:", ex);
            RequestUtils.setError(request, Constants.ERR_ROLE_CREATION);
            return returnFailure(request, mapping);
        }

        RequestUtils.setConfirmation(request, "admin.role.confirm.Edit");
        return returnSuccess(request, mapping, Constants.ROLE_PARAM, roleId);
    }
}