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
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A <code>BaseDispatchAction</code> that sets up role admin portals.
 */
public class RoleAdminPortalAction extends BaseDispatchAction {
    private static final String TITLE_LIST = "admin.role.ListRolesTitle";

    private static final String PORTLET_LIST = ".admin.role.List";

    private static final String TITLE_ADD_USERS = "admin.role.AddRoleUsersTitle";

    private static final String PORTLET_ADD_USERS = ".admin.role.AddUsers";

    private static final String TITLE_ADD_GROUPS = "admin.role.AddRoleGroupsTitle";
    private static final String PORTLET_ADD_GROUPS = ".admin.role.AddGroups";

    private static final String TITLE_EDIT = "admin.role.EditRoleTitle";

    private static final String PORTLET_EDIT = ".admin.role.Edit";

    private static final String TITLE_NEW = "admin.role.NewRoleTitle";

    private static final String PORTLET_NEW = ".admin.role.New";

    private static final String TITLE_VIEW = "admin.role.ViewRoleTitle";

    private static final String PORTLET_VIEW = ".admin.role.View";

    private static final String TITLE_CHANGE_OWNER = "admin.role.ChangeRoleOwnerTitle";
    private static final String PORTLET_CHANGE_OWNER = ".admin.role.ChangeOwner";

    protected static final Log log = LogFactory.getLog(RoleAdminPortalAction.class.getName());

    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty(Constants.MODE_LIST, "listRoles");
        keyMethodMap.setProperty(Constants.MODE_ADD_USERS, "addRoleUsers");
        keyMethodMap.setProperty(Constants.MODE_ADD_GROUPS, "addRoleGroups");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editRole");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newRole");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewRole");
        keyMethodMap.setProperty(Constants.MODE_CHANGE_OWNER, "changeRoleOwner");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward listRoles(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setReturnPath(request, mapping, Constants.MODE_LIST);

        Portal portal = Portal.createPortal(TITLE_LIST, PORTLET_LIST);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addRoleUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setRole(request);

        Portal portal = Portal.createPortal(TITLE_ADD_USERS, PORTLET_ADD_USERS);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addRoleGroups(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setRole(request);

        Portal portal = Portal.createPortal(TITLE_ADD_GROUPS, PORTLET_ADD_GROUPS);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editRole(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setRole(request);

        // can't edit system roles
        Role role = (Role) request.getAttribute(Constants.ROLE_ATTR);
        if (role.getFsystem()) {
            RequestUtils.setError(request, "admin.role.error.EditPermission");
            throw new PermissionException();
        }

        Portal portal = Portal.createPortal(TITLE_EDIT, PORTLET_EDIT);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward newRole(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewRole(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setRole(request);
        setReturnPath(request, mapping, Constants.MODE_VIEW);

        Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward changeRoleOwner(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setRole(request);

        Portal portal = Portal.createPortal(TITLE_CHANGE_OWNER, PORTLET_CHANGE_OWNER);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    /**
     * Set the role for the current action.
     *
     * @param request The request to get the session to store the returnPath into.
     */
    protected void setRole(HttpServletRequest request) throws Exception {
        Integer roleId = RequestUtils.getRoleId(request);
        ServletContext ctx = getServlet().getServletContext();

        if (log.isTraceEnabled()) {
            log.trace("finding role [" + roleId + "]");
        }

        RoleManagerLocal roleManager = LookupUtil.getRoleManager();
        Role role = roleManager.findRoleById(roleId);

        request.setAttribute(Constants.ROLE_ATTR, role);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, role.getName());
    }

    /**
     * Set the return path for the current action, including the mode and (if necessary) role id request parameters.
     *
     * @param request The request to get the session to store the return path into.
     * @param mapping The ActionMapping to get the return path from.
     * @param mode    The name of the current display mode.
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, String mode) throws Exception {
        HashMap params = new HashMap();
        params.put(Constants.MODE_PARAM, mode);
        try {
            params.put(Constants.ROLE_PARAM, RequestUtils.getRoleId(request));
        } catch (ParameterNotFoundException e) {
            ; // not in a specific role's context
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}