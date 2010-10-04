/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public abstract class AbstractGWTServiceImpl extends RemoteServiceServlet {

    private static final long serialVersionUID = 1L;

    private ThreadLocal<Subject> sessionSubject = new ThreadLocal<Subject>();

    protected Subject getSessionSubject() {
        return sessionSubject.get();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sid = req.getHeader(UserSessionManager.SESSION_NAME);
        if (sid != null) {
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            try {
                Subject subject = subjectManager.getSubjectBySessionId(Integer.parseInt(sid));
                sessionSubject.set(subject);
            } catch (Exception e) {
                Log.error("Failed to validate request: sessionId was '" + sid + "', requestURL=" + req.getRequestURL());
            }
        } else {
            Log.error("Failed to validate request: sessionId missing, requestURL=" + req.getRequestURL());
        }

        long id = HibernatePerformanceMonitor.get().start();
        super.service(req, resp);
        HibernatePerformanceMonitor.get().stop(id, "GWT Service Request");
    }
}
