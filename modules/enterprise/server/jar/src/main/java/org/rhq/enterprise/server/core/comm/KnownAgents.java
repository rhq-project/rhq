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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.remoting.InvokerLocator;
import org.rhq.core.domain.resource.Agent;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

/**
 * Maintains a list of agents by holding string representations of their remote endpoints. This class implementation is
 * thread safe. Note that this class does not know agents by their name - only by their remote endpoints!
 *
 * @author John Mazzitelli
 */
public class KnownAgents implements Serializable {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * The true set of agents keyed on just "host:port" with the values being the full endpoints. We key on just the
     * base host/port to avoid having duplicates in the list that differ only by parameter values or protocol. Access to
     * this map must be done in a thread safe way. Note that the InvokerLocator is the actual object used to remotely
     * communicate with the agent - its string representation is stored in the domain model in
     * {@link Agent#getRemoteEndpoint()}.
     */
    private final Map<String, InvokerLocator> m_agents;

    /**
     * Constructor for {@link KnownAgents}.
     */
    public KnownAgents() {
        m_agents = new HashMap<String, InvokerLocator>();
    }

    /**
     * Adds the given remote endpoint to the list of known agents. If the given endpoint does not represent an agent,
     * this method does nothing and simply returns.
     *
     * @param  endpoint the endpoint to add if it refers to an agent
     *
     * @return <code>true</code> if the endpoint was an agent and it was added; <code>false</code> if the endpoint was
     *         not an agent and nothing was added to the internal list of known agents
     */
    public boolean addAgent(InvokerLocator endpoint) {
        boolean is_agent = isAgent(endpoint);

        if (is_agent) {
            synchronized (m_agents) {
                m_agents.put(getEndpointKey(endpoint), endpoint);
            }
        }

        return is_agent;
    }

    /**
     * Adds the given remote endpoint to the list of known agents. If the given endpoint does not represent an agent,
     * this method does nothing and simply returns. The given <code>endpoint</code> is a String so you can call this
     * method by directly using a value from {@link Agent#getRemoteEndpoint()}.
     *
     * @param  endpoint the endpoint to add if it refers to an agent
     *
     * @return <code>true</code> if the endpoint was an agent and it was added; <code>false</code> if the endpoint was
     *         not an agent and nothing was added to the internal list of known agents
     *
     * @throws RuntimeException if the endpoint was malformed and invalid
     */
    public boolean addAgent(String endpoint) {
        try {
            return addAgent(new InvokerLocator(endpoint));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // this should rarely occur
        }
    }

    /**
     * Removes the given remote endpoint from the list of known agents. If the given endpoint does not already exist,
     * this method does nothing and simply returns <code>false</code>.
     *
     * @param  endpoint the endpoint to remove
     *
     * @return <code>true</code> if the agent existed and was removed; <code>false</code> otherwise
     */
    public boolean removeAgent(InvokerLocator endpoint) {
        InvokerLocator removed_agent;

        synchronized (m_agents) {
            removed_agent = m_agents.remove(getEndpointKey(endpoint));
        }

        return removed_agent != null;
    }

    /**
     * Removes the given remote endpoint from the list of known agents. If the given endpoint does not already exist,
     * this method does nothing and simply returns <code>false</code>. The given <code>endpoint</code> is a String so
     * you can call this method by directly using a value from {@link Agent#getRemoteEndpoint()}.
     *
     * @param  endpoint the endpoint to remove
     *
     * @return <code>true</code> if the agent existed and was removed; <code>false</code> otherwise
     *
     * @throws RuntimeException if the endpoint was malformed and invalid
     */
    public boolean removeAgent(String endpoint) {
        try {
            return removeAgent(new InvokerLocator(endpoint));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e); // this should rarely occur
        }
    }

    /**
     * Given a specific host and port, this will return the associated agent. Note that the host must match exactly (no
     * reverse DNS or virtual host IP matching is done with the given host and the known hosts names).
     *
     * @param  host the host of the agent to retrieve
     * @param  port the port of the agent to retrieve
     *
     * @return the agent listening on the port on the given host; <code>null</code> if no known agent exists in the list
     */
    public InvokerLocator getAgent(String host, int port) {
        synchronized (m_agents) {
            return m_agents.get(getEndpointKey(host, port));
        }
    }

    /**
     * Returns a list of all the known agents.
     *
     * @return list of all known agents' endpoints
     */
    public List<InvokerLocator> getAllAgents() {
        List<InvokerLocator> ret_list;

        synchronized (m_agents) {
            ret_list = new ArrayList<InvokerLocator>(m_agents.values());
        }

        return ret_list;
    }

    /**
     * Empties the internal list of agents.
     */
    public void removeAllAgents() {
        synchronized (m_agents) {
            m_agents.clear();
        }
    }

    /**
     * This returns <code>true</code> if the given remote endpoint is from an agent. If not, <code>false</code> is
     * returned.
     *
     * @param  locator the locator of the remote endpoint that is to be checked
     *
     * @return <code>true</code> if the locator represents an agent
     */
    private boolean isAgent(InvokerLocator locator) {
        Map parameters = locator.getParameters();

        if ((parameters == null) || (parameters.size() == 0)) {
            return false;
        }

        String rhqtype = (String) parameters.get(ServiceContainerConfigurationConstants.CONNECTOR_RHQTYPE);

        return ServiceContainerConfigurationConstants.RHQTYPE_AGENT.equals(rhqtype);
    }

    /**
     * Returns the endpoint key that includes base information of just the host and port (i.e. minus protocol along with
     * any and all query string parameters).
     *
     * @param  endpoint the full endpoint
     *
     * @return the endpoint's base information
     */
    private String getEndpointKey(InvokerLocator endpoint) {
        return getEndpointKey(endpoint.getHost(), endpoint.getPort());
    }

    /**
     * Returns the endpoint key based on the given host and port.
     *
     * @param  host the agent's host
     * @param  port the agent's port
     *
     * @return the endpoint's base information
     */
    private String getEndpointKey(String host, int port) {
        return host + ":" + port;
    }
}