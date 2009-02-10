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

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseDispatchAction;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.BizappUtils;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A <code>BaseDispatchAction</code> that sets up user admin portals.
 */
public class UserAdminPortalAction extends BaseDispatchAction {
    private static final String TITLE_LIST = "admin.user.ListUsersTitle";

    private static final String PORTLET_LIST = ".admin.user.List";

    private static final String TITLE_ADD_ROLES = "admin.user.AddUserRolesTitle";

    private static final String PORTLET_ADD_ROLES = ".admin.user.UserRoles";

    private static final String TITLE_EDIT = "admin.user.EditUserTitle";

    private static final String PORTLET_EDIT = ".admin.user.Edit";

    private static final String TITLE_NEW = "admin.user.NewUserTitle";

    private static final String PORTLET_NEW = ".admin.user.New";

    private static final String TITLE_VIEW = "admin.user.ViewUserTitle";

    private static final String PORTLET_VIEW = ".admin.user.View";

    private static final String TITLE_CHANGE_PASSWORD = "admin.user.ChangeUserPasswordTitle";
    private static final String PORTLET_CHANGE_PASSWORD = ".admin.user.EditPassword";

    private static final String TITLE_REGISTER = "admin.user.RegisterUserTitle";

    private static final String PORTLET_REGISTER = ".admin.user.RegisterUser";

    protected static Log log = LogFactory.getLog(UserAdminPortalAction.class.getName());

    private static Properties keyMethodMap = new Properties();

    static {
        keyMethodMap.setProperty(Constants.MODE_LIST, "listUsers");
        keyMethodMap.setProperty(Constants.MODE_ADD_ROLES, "addUserRoles");
        keyMethodMap.setProperty(Constants.MODE_EDIT, "editUser");
        keyMethodMap.setProperty(Constants.MODE_NEW, "newUser");
        keyMethodMap.setProperty(Constants.MODE_VIEW, "viewUser");
        keyMethodMap.setProperty(Constants.MODE_EDIT_PASS, "changeUserPassword");
        keyMethodMap.setProperty(Constants.MODE_REGISTER, "registerUser");
    }

    protected Properties getKeyMethodMap() {
        return keyMethodMap;
    }

    public ActionForward listUsers(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setReturnPath(request, mapping, Constants.MODE_LIST);

        Portal portal = Portal.createPortal(TITLE_LIST, PORTLET_LIST);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward addUserRoles(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_ADD_ROLES, PORTLET_ADD_ROLES);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward editUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_EDIT, PORTLET_EDIT);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward newUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal(TITLE_NEW, PORTLET_NEW);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward viewUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setUser(request);
        setReturnPath(request, mapping, Constants.MODE_VIEW);

        Portal portal = Portal.createPortal(TITLE_VIEW, PORTLET_VIEW);
        portal.setWorkflowPortal(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward changeUserPassword(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        setUser(request);

        Portal portal = Portal.createPortal(TITLE_CHANGE_PASSWORD, PORTLET_CHANGE_PASSWORD);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    public ActionForward registerUser(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Portal portal = Portal.createPortal(TITLE_REGISTER, PORTLET_REGISTER);
        portal.setDialog(true);
        request.setAttribute(Constants.PORTAL_KEY, portal);

        return null;
    }

    /**
     * Set the user for the current action.
     *
     * @param request The request to get the session to store the returnPath into.
     */
    protected void setUser(HttpServletRequest request) throws Exception {
        Integer userId = RequestUtils.getUserId(request);
        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = RequestUtils.getSessionId(request);
        Subject user = LookupUtil.getSubjectManager().findSubjectById(userId.intValue());

        // when LDAP authentication is enabled, we may still have
        // users logging in with JDBC. the only way we can
        // distinguish these users is by checking to see
        // if they have an entry in the principals table.
        boolean hasPrincipal = LookupUtil.getSubjectManager().isUserWithPrincipal(user.getName());

        WebUser webUser = new WebUser(user);
        webUser.setHasPrincipal(hasPrincipal);

        SessionUtils.setWebUser(request.getSession(), webUser);
        request.setAttribute(Constants.TITLE_PARAM_ATTR, BizappUtils.makeSubjectFullName(user));
    }

    /**
     * Set the return path for the current action, including the mode and (if necessary) user id request parameters.
     *
     * @param request The request to get the session to store the return path into.
     * @param mapping The ActionMapping to get the return path from.
     * @param mode    The name of the current display mode.
     */
    protected void setReturnPath(HttpServletRequest request, ActionMapping mapping, String mode) throws Exception {
        HashMap params = new HashMap();
        params.put(Constants.MODE_PARAM, mode);
        try {
            params.put(Constants.USER_PARAM, RequestUtils.getUserId(request));
        } catch (ParameterNotFoundException e) {
            ; // not in a specific user's context
        }

        String returnPath = ActionUtils.findReturnPath(mapping, params);
        if (log.isTraceEnabled()) {
            log.trace("setting return path: " + returnPath);
        }

        SessionUtils.setReturnPath(request.getSession(), returnPath);
    }
}