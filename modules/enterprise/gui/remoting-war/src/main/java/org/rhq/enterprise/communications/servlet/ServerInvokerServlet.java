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

import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

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
            throw new ServletException(
                "Could not find the remoting servlet invoker ["
                    + invokerObjectNameQuery
                    + "].  DURING SERVER STARTUP AND INITIALIZATION THIS IS NOT AN ERROR AND CAN BE IGNORED.  This may be a problem if occurring during normal server runtime.");
        }
        if (mbeans.size() > 1) {
            throw new ServletException("Found more than one remoting servlet invoker at [" + invokerObjectNameQuery
                + "]=" + mbeans);
        }

        ObjectName theInvokerObjectName = mbeans.iterator().next();
        log("Found RHQ remoting servlet: " + theInvokerObjectName);

        return MBeanServerInvocationHandler.newProxyInstance(mbeanServer,
            theInvokerObjectName, ServletServerInvokerMBean.class, false);
    }
}
