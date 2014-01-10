/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.rest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.resteasy.util.Base64;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Authentication filter for the rest stuff
 * @author Heiko W. Rupp
 */
public class AuthFilter implements javax.servlet.Filter {

    Log log = LogFactory.getLog("AuthFilter");

    public void destroy() {
    }

    public void doFilter(javax.servlet.ServletRequest req, javax.servlet.ServletResponse resp,
                         javax.servlet.FilterChain chain) throws javax.servlet.ServletException, IOException {

        HttpServletRequest hreq = (HttpServletRequest) req;

        HttpSession session = hreq.getSession();

        String authHeader = hreq.getHeader("authorization");

        if(hreq.getHeader("Access-Control-Request-Method") != null &&
            hreq.getMethod() != null &&
            hreq.getMethod().equals("OPTIONS")){

            chain.doFilter(req, resp);
            return;
        }

        if (authHeader==null || authHeader.isEmpty()) {
            log.warn("Client sent no authorization header");
            ((HttpServletResponse) resp).sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        authHeader = authHeader.substring(5);
        byte[] bytes = Base64.decode(authHeader);
        String userPass = new String(bytes);
        String username = userPass.substring(0,userPass.indexOf(":"));
        String password = userPass.substring(userPass.indexOf(":")+1);

        SubjectManagerLocal sm = LookupUtil.getSubjectManager();
        Subject subject = sm.checkAuthentication(username, password);

        if (subject==null) {

            ((HttpServletResponse) resp).sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        else {
            session.setAttribute("subject",subject); // TODO how to inject to the business methods?
            log.debug("User '" + username + "' has passed");
            chain.doFilter(req, resp);
        }
    }

    public void init(javax.servlet.FilterConfig config) throws javax.servlet.ServletException {

    }

}
