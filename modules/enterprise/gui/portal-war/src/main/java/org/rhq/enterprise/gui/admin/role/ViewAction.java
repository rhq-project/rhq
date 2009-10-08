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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.AuthzConstants;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.WorkflowPrepareAction;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An Action that retrieves a specific role. This is executed when you view a specific role.
 */
public class ViewAction extends WorkflowPrepareAction {
    /**
     */
    public ActionForward workflow(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(ViewAction.class.getName());

        // make sure the request has selected a role
        Integer roleId = RequestUtils.getRoleId(request);
        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role == null) {
            RequestUtils.setError(request, Constants.ERR_ROLE_NOT_FOUND);
            return null;
        }

        // get the current user and its current page controls
        Subject whoami = RequestUtils.getSubject(request);
        PageControl pcu = WebUtility.getPageControl(request, "u");
        PageControl pcg = WebUtility.getPageControl(request, "g");
        log.trace("user page control: " + pcu);
        log.trace("group page control: " + pcg);

        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();

        // get all the role permissions
        Set<Permission> permissions = roleManager.getPermissions(roleId);
        Map<String, Boolean> permission_map = new HashMap<String, Boolean>();

        // global permissions - put in a map to make jsp writing easier
        for (Permission p : permissions) {
            permission_map.put(p.name(), Boolean.TRUE);
        }

        request.setAttribute(Constants.ROLE_PERMISSIONS_PARAM, permission_map);

        // get the subjects attached to the role
        log.trace("getting users for role [" + roleId + "]");
        PageList<Subject> users = roleManager.findSubjectsByRole(roleId, pcu);
        request.setAttribute(Constants.ROLE_USERS_ATTR, users);

        // get the groups attached to the role
        log.trace("getting resource groups for role [" + roleId + "]");
        PageList<ResourceGroup> groups = groupManager.findResourceGroupsForRole(whoami, roleId, pcg);
        request.setAttribute(Constants.ROLE_RESGRPS_ATTR, groups);
        if (groups == null) {
            request.setAttribute(Constants.NUM_RESGRPS_ATTR, new Integer(0));
        } else {
            request.setAttribute(Constants.NUM_RESGRPS_ATTR, new Integer(groups.getTotalSize()));
        }

        // create and initialize the remove users form
        RemoveUsersForm rmUsersForm = new RemoveUsersForm();
        rmUsersForm.setR(roleId);
        int psu = RequestUtils.getPageSize(request, "psu");
        rmUsersForm.setPs(new Integer(psu));
        request.setAttribute(Constants.ROLE_REMOVE_USERS_FORM_ATTR, rmUsersForm);

        // create and initialize the remove resource groups form
        RemoveResourceGroupsForm rmGroupsForm = new RemoveResourceGroupsForm();
        rmGroupsForm.setR(roleId);
        int psg = RequestUtils.getPageSize(request, "psg");
        rmGroupsForm.setPs(new Integer(psg));
        request.setAttribute(Constants.ROLE_REMOVE_RESOURCE_GROUPS_FORM_ATTR, rmGroupsForm);

        // TODO : do I need this?
        if (AuthzConstants.authzResourceGroupId.intValue() == roleId.intValue()) {
            context.putAttribute("superUser", "true");
        }

        return null;
    }
}