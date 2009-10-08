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
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RemoveAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(RemoveAction.class.getName());

        RemoveForm rmForm = (RemoveForm) form;

        ActionForward forward = checkSubmit(request, mapping, form);
        if (forward != null) {
            return forward;
        }

        Integer[] roles = rmForm.getRoles();
        if ((roles == null) || (roles.length < 1)) {
            log.trace("no roles specified in request");
            return returnFailure(request, mapping);
        }

        Subject whoami = RequestUtils.getSubject(request);

        for (int i = 0; i < roles.length; i++) {
            log.trace("removing role [" + roles[i] + "]");
        }

        try {
            RoleManagerLocal roleManager = LookupUtil.getRoleManager();
            roleManager.deleteRoles(whoami, roles);
        } catch (Exception e) {
            log.trace("failed to remove roles");
            RequestUtils.setError(request, "admin.role.error.RemoveRolePermission");
            return returnFailure(request, mapping);
        }

        RequestUtils.setConfirmation(request, "admin.role.confirm.Remove");
        return returnSuccess(request, mapping);
    }
}