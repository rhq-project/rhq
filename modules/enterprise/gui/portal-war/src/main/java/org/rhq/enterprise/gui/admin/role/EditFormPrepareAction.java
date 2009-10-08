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

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * An Action that retrieves a specific role to facilitate display of the <em>Edit Role</em> form.
 */
public class EditFormPrepareAction extends TilesAction {
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(EditFormPrepareAction.class.getName());

        RoleForm editForm = (RoleForm) form;
        Integer roleId = editForm.getR();

        if (roleId == null) {
            roleId = RequestUtils.getRoleId(request);
        }

        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }

        editForm.setR(role.getId());
        editForm.setName(role.getName());
        editForm.setDescription(role.getDescription());
        editForm.setPermissions(new ArrayList<Permission>(role.getPermissions()));

        return null;
    }
}