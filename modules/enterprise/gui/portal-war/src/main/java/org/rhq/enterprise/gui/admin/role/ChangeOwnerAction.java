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
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;

/**
 * An Action that changes the owner of a specific role in the BizApp.
 */
public class ChangeOwnerAction extends BaseAction {
    // ---------------------------------------------------- Public Methods

    /**
     * Change the owner of the role to the user specified in the the given <code>ChangeOwnerForm</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(ChangeOwnerAction.class.getName());

        ChangeOwnerForm changeOwnerForm = (ChangeOwnerForm) form;
        Integer roleId = changeOwnerForm.getR();
        Integer ownerId = changeOwnerForm.getOwner();

        ActionForward forward = checkSubmit(request, mapping, form, Constants.ROLE_PARAM, roleId);
        if (forward != null) {
            return forward;
        }

        ServletContext ctx = getServlet().getServletContext();

        //        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        //        Integer sessionId = RequestUtils.getSessionId(request);
        //
        //        log.trace("getting role [" + roleId + "]");
        //        Role role = boss.findRole(sessionId, roleId);
        //
        //        log.trace("getting subject [" + ownerId + "]");
        //        Subject owner = boss.findSubject(sessionId, ownerId);
        //
        //        if(AuthzConstants.authzResourceGroupId.intValue() == roleId.intValue()){
        //            throw new PermissionException("can't change super user role");
        //        }
        //
        //        log.trace("setting owner [" + ownerId +
        //                           "] for role [" + roleId + "]");
        //        boss.changeOwnerForRole(sessionId, role, owner);
        //
        //        RequestUtils.setConfirmation(request,
        //                                     "admin.role.confirm.ChangeOwner");
        //        return returnSuccess(request, mapping, Constants.ROLE_PARAM,
        //                             roleId);
        throw new IllegalStateException("deprecated code");
    }
}