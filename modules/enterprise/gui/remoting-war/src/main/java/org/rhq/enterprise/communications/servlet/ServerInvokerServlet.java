/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.enterprise.communications.servlet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;
import org.jboss.remoting.transport.servlet.ServletServerInvokerMBean;

/**
 * Extends JBoss/Remoting 2's servlet that receives the inital http request for the ServletServerInvoker.
 * This extension finds remoting ServletServerInvoker using our own mechanism.  We need to do this since
 * Remoting 2.4+ changed the ObjectName such that we can't statically define it in the web.xml.
 * The new ObjectName that Remoting uses is:
 * jboss.remoting:service=invoker,transport=servlet,host=myhost,port=7080,rhq.communications.connector.rhqtype=server
 * but we can't assume users won't configure RHQ with a different port and we don't know at build time
 * host name.
 */
public class ServerInvokerServlet extends org.jboss.remoting.transport.servlet.web.ServerInvokerServlet {
    private static final long serialVersionUID = 1L;

    private static Logger log = Logger.getLogger(ServerInvokerServlet.class);

    private ServletConfig servletConfig;
    private AtomicInteger evilLogMessageCount = new AtomicInteger(0);
    private final AtomicBoolean alreadyInitialized = new AtomicBoolean(false);

    @Override
    public void init(ServletConfig config) throws ServletException {
        // purposefully do not call init(config) - we can't have the superclass try to get
        // the servletInvoker yet and risk getting that dreaded and evil log message!
        super.init();
        this.servletConfig = config;
    }

    @Override
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        java.io.IOException {

        // find the servlet invoker now - catch exceptions and avoid logging so we don't scare people
        if (initServletInvokerNow()) {
            super.processRequest(request, response);
        } else {
            // tell the client we can't process the request
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Server is not ready yet");
        }
    }

    /**
     * Performs the "real" init so the JBoss/Remoting superclass loads the servlet invoker.
     * This returns true if we can keep going; false if an error occurred and we can't process the current request.
     */
    private boolean initServletInvokerNow() {
        try {
            synchronized (alreadyInitialized) {
                if (!alreadyInitialized.get()) {
                    super.init(this.servletConfig);
                    alreadyInitialized.set(true);
                }
            }
            return true;
        } catch (Exception e) {
            // only log a message (at debug level) if we see this lots of times, but only every 10th time
            int msgCount = evilLogMessageCount.incrementAndGet();
            if ((msgCount % 10 == 0) && (msgCount >= 100)) {
                log.debug(e);
            }
            return false;
        }
    }

    /**
     * Returns the servlet server invoker. In our implementation, this always returns
     * the MBean.
     *
     * @param config the servlet configuration
     * @return the servlet server invoker using the "invokerName" to query to find the MBean
     * @throws ServletException
     */
    protected ServletServerInvokerMBean getInvokerFromInvokerName(ServletConfig config) throws ServletException {
        // get the invokerName initial parameter as defined in web.xml
        String invokerNameString = config.getInitParameter("invokerName");
        if (invokerNameString == null) {
            throw new ServletException("RHQ's use of this Servlet requires invokerName init parameter");
        }

        // the invoker name should be a query ObjectName that should find the 1 MBean we are looking for
        ObjectName invokerObjectNameQuery = null;
        try {
            invokerObjectNameQuery = new ObjectName(invokerNameString);
            log("invokerObjectNameQuery=" + invokerObjectNameQuery);
        } catch (MalformedObjectNameException e) {
            throw new ServletException("Failed to build invokerObjectNameQuery", e);
        }

        // Lookup the MBeanServer
        String mbeanServerId = config.getInitParameter("mbeanServer");
        MBeanServer mbeanServer = getMBeanServer(mbeanServerId);
        if (mbeanServer == null) {
            throw new ServletException("Failed to locate the MBeanServer");
        }

        Set<ObjectName> mbeans = mbeanServer.queryNames(invokerObjectNameQuery, null);
        if (mbeans.isEmpty()) {
            throw new ServletException("Could not find the remoting servlet invoker: " + invokerObjectNameQuery
                + " - need to wait for remoting to be initialized later");
        }
        if (mbeans.size() > 1) {
            throw new ServletException("Found more than one remoting servlet invoker at [" + invokerObjectNameQuery
                + "]=" + mbeans);
        }

        ObjectName theInvokerObjectName = mbeans.iterator().next();
        log("Found RHQ remoting servlet: " + theInvokerObjectName);

        return (ServletServerInvokerMBean) MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
            theInvokerObjectName, ServletServerInvokerMBean.class, false);
    }
}
