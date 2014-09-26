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
package org.rhq.enterprise.gui.authentication;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Authenticates the web user's credentials and establishes his identity.
 */
public class AuthenticateUserAction extends TilesAction {
    private static final String URL_REGISTER = "/admin/user/UserAdmin.do?mode=register";
    private static final String URL_DASHBOARD = "/";

    /**
     * @see TilesAction#execute(ActionMapping, ActionForm, HttpServletRequest, HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        Log log = LogFactory.getLog(AuthenticateUserAction.class.getName());

        HttpSession session = request.getSession(true);
        LogonForm logonForm = (LogonForm) form;
        ServletContext ctx = getServlet().getServletContext();

        WebUser webUser = null;
        Map<String, Boolean> userGlobalPermissionsMap = new HashMap<String, Boolean>();
        boolean needsRegistration = false;

        try {
            // authenticate the credentials
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            Subject subject = subjectManager.loginLocal(logonForm.getJ_username(), logonForm.getJ_password());
            Integer sessionId = subject.getSessionId(); // this is the RHQ session ID, not related to the HTTP session

            log.debug("Logged in as [" + logonForm.getJ_username() + "] with session id [" + sessionId + "]");

            boolean hasPrincipal = true;
            if (subject.getId() == 0) {
                // Subject with a ID of 0 means the subject wasn't in the database but the login succeeded.
                // This means the login method detected that LDAP authenticated the user and just gave us a dummy subject.
                // Set the needs-registration flag so we can eventually steer the user to the LDAP registration workflow.
                needsRegistration = true;
            }

            if (!needsRegistration) {
                subject = subjectManager.loadUserConfiguration(subject.getId());
                subject.setSessionId(sessionId); // put the transient data back into our new subject

                if (subject.getUserConfiguration() == null) {
                    subject.setUserConfiguration((Configuration) ctx.getAttribute(Constants.DEF_USER_PREFS));
                    subject = subjectManager.updateSubject(subject, subject);
                    subject.setSessionId(sessionId); // put the transient data back into our new subject
                }

                // look up the user's permissions
                Set<Permission> all_permissions = LookupUtil.getAuthorizationManager().getExplicitGlobalPermissions(
                    subject);

                for (Permission permission : all_permissions) {
                    userGlobalPermissionsMap.put(permission.toString(), Boolean.TRUE);
                }
            }

            webUser = new WebUser(subject, hasPrincipal);
        } catch (Exception e) {
            String msg = e.getMessage().toLowerCase();
            if ((msg.indexOf("username") >= 0) || (msg.indexOf("password") >= 0)) {
                request.setAttribute(Constants.LOGON_STATUS, "login.info.bad");
            } else {
                log.error("Could not log into the web application", e);
                request.setAttribute(Constants.LOGON_STATUS, "login.bad.backend");
            }

            return (mapping.findForward("bad"));
        }

        // compute the post-login destination
        ActionForward af;
        if (needsRegistration) {
            // Since we are authenticating the user with LDAP and the user has never logged in before,
            // that user has no subject record yet. We need to send him through the LDAP registration workflow.
            log.debug("LDAP registration required for user [" + logonForm.getJ_username() + "]");
            af = new ActionForward(URL_REGISTER);
        } else {
            // if the user's session timed out, we "bookmarked" the url that he was going to
            // so that we can send him there after login. otherwise, he gets the dashboard.
            String url = getBookmarkedUrl(session);
            if ((url == null) || url.equals("/Logout.do")) {
                url = URL_DASHBOARD;
            }
            if (url.toLowerCase().indexOf("ajax") != -1) {
                // we can't return to a URL that was a partial page request
                // because the view no longer exists, and will blow up.
                // instead, redirect back to the last saved URL
                url = webUser.getWebPreferences().getLastVisitedURL(2);
                log.info("Bypassing partial-page with " + url);
            }

            af = new ActionForward(url);
        }

        af.setRedirect(true);

        // now that we've constructed a forward to the bookmarked url,
        // if any, forget the old session and start a new one,
        // setting the web user to show that we're logged in
        session.invalidate();
        session = request.getSession(true);
        SessionUtils.setWebUser(session, webUser);
        session.setAttribute(Constants.USER_OPERATIONS_ATTR, userGlobalPermissionsMap);

        if (needsRegistration) {
            // will be cleaned out during registration
            session.setAttribute(Constants.PASSWORD_SES_ATTR, logonForm.getJ_password());
        }

        return af;
    }

    /**
     * Return the "bookmarked" url saved when we discovered the user's session had timed out, or null if there is no
     * bookmarked url.
     *
     * @param  session
     *
     * @return the URL the user should be forwarded to after successfully logging on
     */
    private String getBookmarkedUrl(HttpSession session) {
        String val = (String) session.getAttribute(Constants.LOGON_URL_KEY);
        if ((val == null) || (val.length() == 0)) {
            return null;
        }

        StringBuffer url = new StringBuffer(val);

        Map parameters = (Map) session.getAttribute(Constants.LOGON_URL_PARAMETERS);
        if ((parameters != null) && !parameters.isEmpty()) {
            String sep = "?";
            for (Iterator i = parameters.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                String value = (String) parameters.get(key);
                url.append(sep).append(key).append("=").append(value);

                if (sep.equals("?")) {
                    sep = "&";
                }
            }
        }

        return url.toString();
    }

    /**
     * This returns <code>true</code> if the RHQ Server has been configured to allow for authentication via LDAP. <code>
     * false</code> if only database authentication is available.
     *
     * @param  context
     *
     * @return <code>true</code> if LDAP authentication is allowed
     *
     * @throws Exception
     */
    public static boolean usingLDAPAuthentication(ServletContext context) throws Exception {
        String provider = (String) context.getAttribute(Constants.JAAS_PROVIDER_CTX_ATTR);

        if (provider == null) {
            SystemSettings systemSettings = LookupUtil.getSystemManager().getUnmaskedSystemSettings(true);
            provider = systemSettings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);
            context.setAttribute(Constants.JAAS_PROVIDER_CTX_ATTR, provider);
        }

        return (provider != null) ? Boolean.valueOf(provider) : false;
    }
}
