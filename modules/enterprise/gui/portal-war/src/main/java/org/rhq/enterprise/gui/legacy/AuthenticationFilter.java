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
package org.rhq.enterprise.gui.legacy;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SessionTimeoutException;
import org.rhq.enterprise.server.license.License;
import org.rhq.enterprise.server.system.LicenseException;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public final class AuthenticationFilter extends BaseFilter {
    private static Log log = LogFactory.getLog(AuthenticationFilter.class.getName());

    private FilterConfig filterConfig;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        //if a session does not already exist this call will create one
        HttpSession session = request.getSession();

        /* check if the user object is in the session.
         * if not then the user is not validated and should be forwarded to the login page
         */
        WebUser webUser = SessionUtils.getWebUser(session);

        if (webUser != null) {
            try {
                // the web user exists, so update our SessionManager's session last-access-time
                Subject subject = webUser.getSubject();

                if (subject == null) {
                    throw new SessionNotFoundException("Web user not associated with a subject");
                }

                SessionManager.getInstance().getSubject(subject.getSessionId());
            } catch (SessionNotFoundException snfe) {
                session.removeAttribute(Constants.USER_PARAM);
                session.removeAttribute(Constants.WEBUSER_SES_ATTR);
                webUser = null;
            } catch (SessionTimeoutException ste) {
                session.removeAttribute(Constants.USER_PARAM);
                session.removeAttribute(Constants.WEBUSER_SES_ATTR);
                webUser = null;
            }
        }

        if (webUser == null) {
            String path = request.getServletPath();

            if ("/Login.do".equals(path) || "/j_security_check.do".equals(path)) {
                chain.doFilter(request, response);
            } else {
                //copy the url and request parameters so that the user can be
                // forwarded to the originally requested page after authorization
                Map parameters = request.getParameterMap();
                if (!parameters.isEmpty()) {
                    Map newMap = new HashMap();
                    for (Object keyObj : parameters.keySet()) {
                        String key = (String) keyObj;
                        newMap.put(key, request.getParameter(key));
                    }

                    session.setAttribute(Constants.LOGON_URL_PARAMETERS, newMap);
                }

                session.setAttribute(Constants.LOGON_URL_KEY, request.getServletPath());
                response.setStatus(401);
                response.sendRedirect(request.getContextPath() + "/Login.do");
            }
        } else {
            // Now check if the license is up and running
            if (request.getRequestURI().indexOf("/admin/license") < 0) {
                SystemManagerLocal systemManager = LookupUtil.getSystemManager();
                License license = null;
                try {
                    license = systemManager.getLicense();
                } catch (LicenseException le) {
                    /*
                     * license will still be null because the exception is thrown out of azboss.getLicense()
                     * LicenseAdmin.do will take care of showing the appropriate exception by delegating to
                     * LicenseController
                     */
                }

                if ((license == null) || (license.getLicenseExpiration() < System.currentTimeMillis())) {
                    response.sendRedirect(request.getContextPath() + "/admin/license/LicenseAdmin.do?mode=view");
                }
            }

            try {
                chain.doFilter(request, response);
            } catch (IOException e) {
                if (request != null) {
                    log.warn("Caught IO Exception from client " + request.getRemoteAddr() + ": " + e.getMessage());
                } else {
                    log.debug("Caught IO Exception: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        super.init(filterConfig);
        this.filterConfig = filterConfig;
    }
}