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
package org.rhq.coregui.server.gwt;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 */
public abstract class AbstractGWTServiceImpl extends RemoteServiceServlet {

    private static final long serialVersionUID = 1L;
    private Log log = LogFactory.getLog(this.getClass());

    private ThreadLocal<Subject> sessionSubject = new ThreadLocal<Subject>();
    private ThreadLocal<String> rpcMethod = new ThreadLocal<String>();

    protected Subject getSessionSubject() {
        return sessionSubject.get();
    }

    protected Log getLog() {
        return log;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (log.isTraceEnabled()) {
            printHeaders(req);
        }

        boolean continueProcessing = true;
        String sid = req.getHeader("RHQ-Session");
        if (sid != null) {
            SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
            try {
                Subject subject = subjectManager.getSubjectBySessionId(Integer.parseInt(sid));
                sessionSubject.set(subject);
            } catch (Exception e) {
                log.trace("Failed to validate request: sessionId was '" + sid + "', requestURL=" + req.getRequestURL());
                continueProcessing = false;
            }
        } else {
            log.debug("Failed to validate request: sessionId missing, requestURL=" + req.getRequestURL());
            continueProcessing = false;
        }

        if (continueProcessing) {
            // TODO: only execute this if the session lookup was successful, otherwise fail in some deterministic fashion
            //       alter callback handlers to capture expected failure and retry (at least once)
            //      to add resilience to gwt service calls
            long id = HibernatePerformanceMonitor.get().start();
            super.service(req, resp);
            HibernatePerformanceMonitor.get().stop(id, "GWT:" + rpcMethod.get());
        }
    }

    @Override
    protected void onAfterRequestDeserialized(RPCRequest rpcRequest) {
        super.onAfterRequestDeserialized(rpcRequest);

        Method rpcMethod = rpcRequest.getMethod();
        String className = rpcMethod.getDeclaringClass().getSimpleName();
        String methodName = rpcMethod.getName();

        this.rpcMethod.set(className + "." + methodName);
    }

    /**
     * Our GWT Service implementations should call this whenever it needs to send an exception to the GWT client.
     * @param t the server side exception that needs to be thrown
     * @returns a RuntimeException that is the real exception that should be thrown to the GWT client
     */
    protected RuntimeException getExceptionToThrowToClient(Throwable t) throws RuntimeException {
        return getExceptionToThrowToClient(t, null);
    }

    /**
     * Our GWT Service implementations should call this whenever it needs to send an exception to the GWT client.
     * @param t the server side exception that needs to be thrown
     * @param message an extra message to put in the returned exception
     * @returns a RuntimeException that is the real exception that should be thrown to the GWT client
     */
    protected RuntimeException getExceptionToThrowToClient(Throwable t, String message) throws RuntimeException {
        // this id is so the user can correlate this exception in the server log with the client message in the browser
        StringBuilder id = new StringBuilder("[");
        id.append(System.currentTimeMillis());
        if (message != null) {
            id.append(" ").append(message);
        }
        id.append("] ");

        // log the exception server-side
        log.warn("Sending exception to client: " + id.toString(), t);

        // cannot assume gwt client has our exception classes, only send the messages in a generic runtime exception
        return new RuntimeException(id.toString() + ThrowableUtil.getAllMessages(t));
    }

    @SuppressWarnings("unchecked")
    private void printHeaders(HttpServletRequest req) {
        // TODO: figure out why SESSION_NAME header and other GWT-specific headers are missing occasionally
        //       seems to only happen on polling for recent alerts when there is no user activity for a few minutes
        Enumeration<String> headerNames = req.getHeaderNames();
        log.trace(req.getRequestURL().toString());
        while (headerNames.hasMoreElements()) {
            log.trace("   " + headerNames.nextElement());
        }
    }
}
