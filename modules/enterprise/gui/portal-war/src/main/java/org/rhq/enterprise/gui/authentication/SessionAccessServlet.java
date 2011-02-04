/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.auth.SessionNotFoundException;
import org.rhq.enterprise.server.auth.SessionTimeoutException;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public class SessionAccessServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpServletResponse response = (HttpServletResponse) resp;
        HttpServletRequest request = (HttpServletRequest) req;

        response.addHeader("Pragma", "no-cache");
        response.addHeader("Cache-Control", "no-cache");
        // Stronger according to blog comment below that references HTTP spec
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Cache-Control", "must-revalidate");
        // some date in the past
        response.addHeader("Expires", "Mon, 8 Aug 2006 10:00:00 GMT");

        //if a session does not already exist this call will create one
        HttpSession session = request.getSession();

        //check for web user update request from coregui
        String sessionWebUserUpdate = request.getHeader("rhq_webuser_update");

        /* 
         * check if the user object is in the session.  if not, then the user is not validated, the response output
         * will not contain the "<subjectId>:<sessionId>:<lastAccess>", which will forward the user to the login page
         */
        WebUser webUser = SessionUtils.getWebUser(session);

        if (webUser != null && webUser.getSubject() != null) {

            //if sessionWebUserUpdate header sent then request for WebUser to be updated
            if ((sessionWebUserUpdate != null) && (!sessionWebUserUpdate.trim().isEmpty())) {
                //if webUser.getSubject.getName is same as user with session id passed in
                try {
                    //attempt to retrieve Subject for the requested session update
                    Subject currentSubject = SessionManager.getInstance().getSubject(
                        Integer.valueOf(sessionWebUserUpdate));
                    if (currentSubject != null) {//located associated subject
                        //if userNames match(case insensitive) then update webUser appropriately and re-associate in session
                        if (webUser.getSubject().getName().equalsIgnoreCase(currentSubject.getName())) {
                            webUser = new WebUser(currentSubject);
                            SessionUtils.setWebUser(session, webUser);
                        }
                    }
                } catch (SessionNotFoundException snfe) {
                } catch (NumberFormatException e) {
                } catch (SessionTimeoutException e) {
                }
            }

            // the web user exists, so update our SessionManager's session last-access-time
            Subject subject = webUser.getSubject();
            try {
                SessionManager.getInstance().getSubject(subject.getSessionId());
                long lastAccess = SessionManager.getInstance().getlastAccess(subject.getSessionId());

                ServletOutputStream out = response.getOutputStream();
                String output = subject.getId() + ":" + webUser.getSessionId() + ":" + lastAccess;
                out.write(output.getBytes());
            } catch (SessionNotFoundException snfe) {
                session.removeAttribute(ParamConstants.USER_PARAM);
                SessionUtils.setWebUser(session, null);
                webUser = null;
            } catch (SessionTimeoutException ste) {
                session.removeAttribute(ParamConstants.USER_PARAM);
                SessionUtils.setWebUser(session, null);
                webUser = null;
            }
        }
    }
}
