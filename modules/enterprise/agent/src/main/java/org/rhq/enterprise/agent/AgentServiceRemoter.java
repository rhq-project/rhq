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

import java.io.InputStream;
import java.io.OutputStream;
import mazz.i18n.Logger;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceLifecycleListener;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;
import org.rhq.enterprise.agent.i18n.AgentI18NFactory;
import org.rhq.enterprise.agent.i18n.AgentI18NResourceKeys;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;

/**
 * This {@link AgentServiceLifecycleListener} will enable each agent service that is started for remote access. As agent
 * services go down, this listener ensures that they are no longer remotely accessible.
 *
 * @author John Mazzitelli
 */
public class AgentServiceRemoter implements AgentServiceLifecycleListener, AgentServiceStreamRemoter {
    private static final Logger LOG = AgentI18NFactory.getLogger(AgentServiceRemoter.class);

    /**
     * The agent that provides the remoting infrastructure for the agent services.
     */
    private final AgentMain m_agent;

    /**
     * Creates a new {@link AgentServiceRemoter} object.
     *
     * @param agent the agent that provides the remoting infrastructure
     */
    public AgentServiceRemoter(AgentMain agent) {
        m_agent = agent;
    }

    /**
     * This method will remote the given agent service so external clients can communicate with it.
     *
     * @see AgentServiceLifecycleListener#started(AgentService)
     */
    public void started(AgentService agent_service) {
        LOG.debug(AgentI18NResourceKeys.REMOTING_NEW_AGENT_SERVICE, agent_service.getClass().getName(), agent_service
            .getClientInterface().getName());

        try {
            // make sure our agent is up and running; if it is shutdown, comm_services will be null
            ServiceContainer comm_services = m_agent.getServiceContainer();

            if (comm_services != null) {
                comm_services.addRemotePojo(agent_service, agent_service.getClientInterface());
            } else {
                throw new IllegalStateException(); // the agent is shutdown; we should never have been called
            }
        } catch (Exception e) {
            LOG.error(e, AgentI18NResourceKeys.ERROR_REMOTING_NEW_AGENT_SERVICE, agent_service.getClass().getName(),
                agent_service.getClientInterface().getName());
        }

        return;
    }

    /**
     * This method will ensure that the stopped agent service will no longer be accessible to external clients.
     *
     * @see AgentServiceLifecycleListener#stopped(AgentService)
     */
    public void stopped(AgentService agent_service) {
        LOG.debug(AgentI18NResourceKeys.UNREMOTING_AGENT_SERVICE, agent_service.getClass().getName(), agent_service
            .getClientInterface().getName());

        // comm_services will be null if the agent has already shutdown the comm infrastructure;
        // if null, then all services that were remoted are already unremoted and we don't have to do anything
        ServiceContainer comm_services = m_agent.getServiceContainer();

        if (comm_services != null) {
            comm_services.removeRemotePojo(agent_service.getClientInterface());
        }

        return;
    }

    /**
     * This will remote the input stream using the agent's communications services.
     *
     * @see AgentServiceStreamRemoter#prepareInputStream(java.io.InputStream)
     */
    public InputStream prepareInputStream(InputStream stream) {
        if (stream == null) {
            return null;
        }

        try {
            ServiceContainer sc = m_agent.getServiceContainer();

            if (sc == null) {
                return stream;
            }

            return new RemoteInputStream(stream, sc);
        } catch (Exception e) {
            throw new RuntimeException(m_agent.getI18NMsg().getMsg(AgentI18NResourceKeys.FAILED_TO_REMOTE_STREAM), e);
        }
    }

    /**
     * This will remote the output stream using the agent's communications services.
     *
     * @see AgentServiceStreamRemoter#prepareOutputStream(OutputStream)
     */
    public OutputStream prepareOutputStream(OutputStream stream) {
        if (stream == null) {
            return null;
        }

        try {
            ServiceContainer sc = m_agent.getServiceContainer();

            if (sc == null) {
                return stream;
            }

            return new RemoteOutputStream(stream, sc);
        } catch (Exception e) {
            throw new RuntimeException(m_agent.getI18NMsg().getMsg(AgentI18NResourceKeys.FAILED_TO_REMOTE_OUTSTREAM), e);
        }
    }
}