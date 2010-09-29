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

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public abstract class AbstractGWTServiceImpl extends RemoteServiceServlet {

    private ThreadLocal<Subject> sessionSubject = new ThreadLocal<Subject>();

    protected Subject getSessionSubject() {
        return sessionSubject.get();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sid = req.getHeader(UserSessionManager.SESSION_NAME);
        Subject subject = null;
        if (sid != null) {
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            try {
                subject = subjectManager.getSubjectBySessionId(Integer.parseInt(sid));
            } catch (Exception e) {
                throw new RuntimeException("Failed to validate session", e);
            }
        }
        sessionSubject.set(subject);

        long id = HibernatePerformanceMonitor.get().start();
        super.service(req, resp);
        HibernatePerformanceMonitor.get().stop(id, "GWT Service Request");
    }

}
