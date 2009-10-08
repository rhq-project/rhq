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
package org.rhq.enterprise.server.core.comm;

import java.io.InputStream;
import java.io.OutputStream;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.mx.util.MBeanServerLocator;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;

/**
 * Provides static utilities to obtain and work with the server bootstrap service. This service provides access to the
 * server-side comm services used to communicate with agents.
 *
 * @author John Mazzitelli
 */
public class ServerCommunicationsServiceUtil {
    /**
     * Prevents instantiation.
     */
    private ServerCommunicationsServiceUtil() {
    }

    /**
     * Logger
     */
    private static final Log LOG = LogFactory.getLog(ServerCommunicationsServiceUtil.class);

    /**
     * This will obtain a proxy to the server bootstrap service - this is the service that provides the communications
     * services used to talk to agents.
     *
     * @return bootstrap proxy
     *
     * @throws WrappedRemotingException if the bootstrap service is not available
     */
    public static ServerCommunicationsServiceMBean getService() {
        ServerCommunicationsServiceMBean serverBootstrapService;
        try {
            MBeanServer mbean_server = MBeanServerLocator.locateJBoss();
            serverBootstrapService = (ServerCommunicationsServiceMBean) MBeanServerInvocationHandler.newProxyInstance(
                mbean_server, ServerCommunicationsServiceMBean.OBJECT_NAME, ServerCommunicationsServiceMBean.class,
                false);
        } catch (Exception e) {
            LOG.error("Could not find server comm service - agent communications are not allowed at this time");
            throw new WrappedRemotingException(new Exception(
                "Cannot get server comm service; unable to communicate with agents", e));
        }

        return serverBootstrapService;
    }

    /**
     * Given an input stream, this method prepares it to be remoted. It installs the proper server-side components in
     * order to allow this input stream to be accessible via a remote client (such as an agent).
     *
     * @param  in the stream to remote
     *
     * @return the input stream wrapped in the object that provides the remoting capabilities
     *
     * @throws Exception if ununable to make the given input stream remotely accessible for some reason
     */
    public static RemoteInputStream remoteInputStream(InputStream in) throws Exception {
        ServiceContainer sc = getService().getServiceContainer();
        return new RemoteInputStream(in, sc);
    }

    /**
     * Given an output stream, this method prepares it to be remoted. It installs the proper server-side components in
     * order to allow this output stream to be accessible via a remote client (such as an agent).
     *
     * @param  in the stream to remote
     *
     * @return the output stream wrapped in the object that provides the remoting capabilities
     *
     * @throws Exception if ununable to make the given output stream remotely accessible for some reason
     */
    public static RemoteOutputStream remoteInputStream(OutputStream out) throws Exception {
        ServiceContainer sc = getService().getServiceContainer();
        return new RemoteOutputStream(out, sc);
    }
}