/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.rest;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.EJBContext;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.naming.OperationNotSupportedException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.auth.SessionException;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
public class ReportsInterceptor {

    private final Log log = LogFactory.getLog(ReportsInterceptor.class);

    @Resource
    EJBContext ejbContext;

    @EJB
    SubjectManagerLocal subjectManager;

    @AroundInvoke
    public Object setCaller(InvocationContext ctx) throws Exception {
        HttpServletRequest request = getRequest(ctx.getParameters());
        if (request == null) {
            // TODO should we throw a different exception?
            String msg = "No " + HttpServletRequest.class.getName() + " parameter was found for " +
                ctx.getTarget().getClass().getName() + "." + ctx.getMethod().getName() + ". An " +
                HttpServletRequest.class.getName() + " parameter must be specified in order to support authentication";
            log.error(msg);
            throw new OperationNotSupportedException(msg);
        }

        Subject subject = getSubject(request);
        if (subject == null) {
            throw new IllegalAccessException("Failed to validate request: could not access subject for request URL " +
                request.getRequestURL());
        }

        AbstractRestBean target = (AbstractRestBean) ctx.getTarget();
        target.caller = subject;

        return ctx.proceed();
    }

    private HttpServletRequest getRequest(Object[] params) {
        for (Object param : params) {
            if (param instanceof HttpServletRequest) {
                return (HttpServletRequest) param;
            }
        }
        return null;
    }

    private Subject getSubject(HttpServletRequest request) {
        Cookie rhqSession = getCookie(request, "RHQ_Session");
        if (rhqSession == null) {
            return null;
        }
        String sessionId = rhqSession.getValue();

        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        try {
            return subjectMgr.getSubjectBySessionId(Integer.parseInt(sessionId));
        } catch (NumberFormatException e) {
            log.warn(sessionId + " is not a valid session id.", e);
            return null;
        } catch (SessionException e) {
            log.warn("Could not get subject for session id " + sessionId, e);
            return null;
        } catch (Exception e) {
            log.error("An unexpected exception occurred while trying to access subject for session id " + sessionId,
                e);
            return null;
        }
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }
        return null;
    }
}

