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

import javax.servlet.FilterChain;
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
import org.rhq.enterprise.server.util.LookupUtil;

public final class AdminUserFilter extends BaseFilter {

    private static Log log = LogFactory.getLog(AdminUserFilter.class);

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
        ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        HttpSession session = request.getSession();
        if (session == null) {
            throw new IllegalStateException(AdminUserFilter.class.getSimpleName() + " must be placed after "
                + AuthenticationFilter.class.getSimpleName() + " in the filter chain");
        }

        WebUser webUser = SessionUtils.getWebUser(session);
        if (webUser == null) {
            throw new IllegalStateException(AdminUserFilter.class.getSimpleName() + " must be placed after "
                + AuthenticationFilter.class.getSimpleName() + " in the filter chain");
        }

        String path = request.getServletPath();

        if (path.toLowerCase().startsWith("/admin/test/")) {
            Subject subject = webUser.getSubject();
            boolean isAdmin = LookupUtil.getAuthorizationManager().isSystemSuperuser(subject);
            if (!isAdmin) {
                log.info("User " + subject.getName() + " attempted unauthorized access to " + path);
                response.getWriter().write("Error: only the administrator has access to this page");
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } catch (IOException e) {
            log.warn("Caught IO Exception from client " + request.getRemoteAddr() + ": " + e.getMessage());
        }
    }

}