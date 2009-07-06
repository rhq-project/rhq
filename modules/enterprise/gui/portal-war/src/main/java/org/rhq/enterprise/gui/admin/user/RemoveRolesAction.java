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

import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.util.LookupUtil;

public class RemoveRolesAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(RemoveAction.class.getName());

        RemoveRolesForm rmForm = (RemoveRolesForm) form;
        Integer[] roles = rmForm.getRoles();

        if ((roles == null) || (roles.length < 1)) {
            log.debug("no roles specified in request");
            return returnFailure(request, mapping);
        }

        for (int i = 0; i < roles.length; i++) {
            log.debug("removing role [" + roles[i] + "]");
        }

        int[] rolesInts = ArrayUtils.unwrapArray(roles);
        LookupUtil.getRoleManager().removeRolesFromSubject(RequestUtils.getSubject(request), rmForm.getU(), rolesInts);

        RequestUtils.setConfirmation(request, "admin.role.confirm.Remove");

        return returnSuccess(request, mapping);
    }
}