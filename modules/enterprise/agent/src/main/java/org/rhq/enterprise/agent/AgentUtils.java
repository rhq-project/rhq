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

package org.rhq.enterprise.agent;

import java.util.Map;

import org.jboss.remoting.InvokerLocator;

import org.rhq.core.domain.cloud.composite.FailoverListComposite.ServerEntry;

/**
 * Just a place where utility methods are placed for use by the agent
 * and its related classes.
 * 
 * @author John Mazzitelli
 */
public abstract class AgentUtils {
    /**
     * Given a server string with an agent's configuration (used to obtain defaults), this will
     * return an object that contains information that can be used to communicate with the server
     * (such as its transport, hostname, port, etc). If the <code>agentConfig</code> is not
     * provided (i.e. <code>null</code>), it will be ignored - but if the server string does not
     * contain all the necessary information, this method will throw an exception.
     * 
     * The purpose of this method is to allow users to provide a simpler server hostname string
     * and just have the agent configuration provide the rest of the information.
     * But it also allows a user to provide a full server URI in case the server in question
     * is configured differently than the one the agent is, by default, going to talk to (for
     * example, if by default the agent talks to its server unencrypted but the server string
     * identifies a server that requires SSL to be communicated with - in that case, the transports
     * are different).
     * 
     * @param agentConfig the configuration of the agent (may be <code>null</code>)
     * @param server identifies the server; this may be a full server endpoint URI that has all the
     *               information this method needs, or it may just be a hostname string in which
     *               case the agent configuration will be used to determine the rest of the server
     *               endpoint information (such as the port the server is listening on, the transport
     *               to be used and the transport parameters)
     * @return the server endpoint information that can be used to talk to the server
     * @throws IllegalArgumentException if <code>agentConfig</code> is <code>null</code>
     *                                  and <code>server</code> is only a hostname string and as such
     *                                  does not provide everything this method needs. This exception
     *                                  may also be throw if server is a full, but invalid, URI.
     */
    @SuppressWarnings("unchecked")
    public static ServerEndpoint getServerEndpoint(AgentConfiguration agentConfig, String server) {

        ServerEndpoint serverEndpoint = new ServerEndpoint();

        // hostnames never have a ":" - if there is a ":", this is therefore a full endpoint URL
        if (server.indexOf(':') > -1) {
            try {
                InvokerLocator endpointUrl = new InvokerLocator(server);
                String host = endpointUrl.getHost();
                int port = endpointUrl.getPort();
                serverEndpoint.namePort = new ServerEntry(host, port, port);
                serverEndpoint.transport = endpointUrl.getProtocol();
                String path = endpointUrl.getPath();
                Map<Object, Object> parameters = endpointUrl.getParameters();
                serverEndpoint.transportParams = "/" + ((path != null) ? path : "");
                if (parameters != null && parameters.size() > 0) {
                    serverEndpoint.transportParams += "?";
                    boolean needAmp = false;
                    for (Map.Entry<Object, Object> configEntry : parameters.entrySet()) {
                        if (needAmp) {
                            serverEndpoint.transportParams += "&";
                        }
                        serverEndpoint.transportParams += configEntry.getKey().toString() + "="
                            + configEntry.getValue().toString();
                        needAmp = true;
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            if (agentConfig != null) {
                int currentServerPort = agentConfig.getServerBindPort();
                String currentServerTransport = agentConfig.getServerTransport();
                String currentServerTransportParams = agentConfig.getServerTransportParams();

                serverEndpoint.namePort = new ServerEntry(server, currentServerPort, currentServerPort);
                serverEndpoint.transport = currentServerTransport;
                serverEndpoint.transportParams = currentServerTransportParams;
            } else {
                throw new IllegalArgumentException("agentConfig==null, server==[" + server + ']');
            }
        }

        return serverEndpoint;
    }

    /**
     * A very simple object that encapsulates server endpoint information that can be used
     * to connect to a server.  This includes a {@link ServerEntry} object that consists
     * of the server hostname/IP and the port you can use to talk to it over the given
     * transport (note that two ports in the ServerEntry will be the same).
     * 
     * @author John Mazzitelli
     *
     */
    public static class ServerEndpoint {
        public ServerEntry namePort;
        public String transport;
        public String transportParams;

        @Override
        public String toString() {
            return transport + "://" + namePort.address + ":" + namePort.port + transportParams;
        }
    }
}
