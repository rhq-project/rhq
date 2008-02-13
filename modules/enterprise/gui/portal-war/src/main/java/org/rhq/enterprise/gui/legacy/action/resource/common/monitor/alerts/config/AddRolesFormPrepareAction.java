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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

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
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data from the BizApp to facilitate display of the form to add roles to AlertDefinition.
 */
public class AddRolesFormPrepareAction extends TilesAction {
    private Log log = LogFactory.getLog(AddRolesFormPrepareAction.class);

    @Override
    @SuppressWarnings("deprecation")
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        AddRolesForm addForm = (AddRolesForm) form;
        Subject subject = RequestUtils.getSubject(request);

        PageControl pcp = WebUtility.getPageControl(request, "p");
        PageControl pca = WebUtility.getPageControl(request, "a");

        log.trace("available page control: " + pca);
        log.trace("pending page control: " + pcp);

        RoleManagerLocal roleManager = LookupUtil.getRoleManager();

        /*
         * pending users are those on the right side of the "add to list" widget - awaiting association with the Alert
         * Definition when the form's "ok" button is clicked.
         */
        Integer[] pendingIds = SessionUtils.getList(request.getSession(), Constants.PENDING_ROLES_SES_ATTR);

        log.trace("getting pending roles for alert definition [" + addForm.getAd() + "]");
        PageList<Role> pendingUsers = roleManager.getRolesById(pendingIds, pcp);
        request.setAttribute(Constants.PENDING_ROLES_ATTR, pendingUsers);

        /*
         * available users are all users in the system that are _not_ associated with the definition and are not pending
         */
        log.trace("getting available roles for alert definition [" + addForm.getAd() + "]");
        PageList<Role> availableUsers = roleManager.getAvailableRolesForAlertDefinition(subject, addForm.getAd(),
            pendingIds, pca);
        request.setAttribute(Constants.AVAIL_ROLES_ATTR, availableUsers);

        return null;
    }
}