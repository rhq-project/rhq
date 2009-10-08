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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves data to facilitate display of the form for adding groups to a role.
 */
public class AddResourceGroupsFormPrepareAction extends TilesAction {
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AddResourceGroupsFormPrepareAction.class.getName());

        Subject whoami = RequestUtils.getSubject(request);
        AddResourceGroupsForm addForm = (AddResourceGroupsForm) form;
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

        /* pending groups are those on the right side of the "add
         * to list" widget- awaiting association with the rolewhen the form's "ok" button is clicked. */
        Integer[] pendingGroupIds = SessionUtils.getList(request.getSession(), Constants.PENDING_RESGRPS_SES_ATTR);

        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        log.trace("getting pending groups for role [" + roleId + ")");
        PageList<ResourceGroup> pendingGroups = groupManager.findResourceGroupByIds(whoami, ArrayUtils
            .unwrapArray(pendingGroupIds), pcp);

        request.setAttribute(Constants.PENDING_RESGRPS_ATTR, pendingGroups);
        request.setAttribute(Constants.NUM_PENDING_RESGRPS_ATTR, new Integer(pendingGroups.getTotalSize()));

        /* available groups are all groups in the system that are not
         * associated with the role and are not pending
         */
        log.trace("getting available groups for role [" + roleId + "]");
        PageList<ResourceGroup> availableGroups = groupManager.findAvailableResourceGroupsForRole(whoami, roleId,
            ArrayUtils.unwrapArray(pendingGroupIds), pca);

        request.setAttribute(Constants.AVAIL_RESGRPS_ATTR, availableGroups);
        request.setAttribute(Constants.NUM_AVAIL_RESGRPS_ATTR, new Integer(availableGroups.getTotalSize()));

        return null;
    }
}