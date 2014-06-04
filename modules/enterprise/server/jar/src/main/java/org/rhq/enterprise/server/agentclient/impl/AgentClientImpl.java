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
package org.rhq.enterprise.server.agentclient.impl;

import java.util.Date;

import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.bundle.BundleAgentService;
import org.rhq.core.clientapi.agent.configuration.ConfigurationAgentService;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.clientapi.agent.inventory.ResourceFactoryAgentService;
import org.rhq.core.clientapi.agent.lifecycle.PluginContainerLifecycle;
import org.rhq.core.clientapi.agent.measurement.MeasurementAgentService;
import org.rhq.core.clientapi.agent.operation.OperationAgentService;
import org.rhq.core.clientapi.agent.ping.PingAgentService;
import org.rhq.core.clientapi.agent.support.SupportAgentService;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.server.ExternalizableStrategy;
import org.rhq.enterprise.communications.Ping;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientRemotePojoFactory;
import org.rhq.enterprise.communications.command.client.SendCallback;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;

/**
 * Provides the mechanism by which you obtain remote interface proxies to a particular agent. Using those remote
 * proxies, you can send commands to an agent.
 *
 * @author John Mazzitelli
 */
public class AgentClientImpl implements AgentClient {
    /**
     * This is the name of the command configuration property that will get assigned the security token string.
     * All commands sent to the agent will get this command property set which contains the agent's token.
     */
    private static final String CMDCONFIG_PROP_SECURITY_TOKEN = "rhq.security-token";

    /**
     * The agent that this client communicates with.
     */
    private final Agent agent;

    /**
     * This is the underlying communications object that actually performs the sending of messages to the agent.
     */
    private final ClientCommandSender sender;

    /**
     * This is the object that is created by the sender and is used to obtain remote proxies to the agent.
     */
    private final ClientRemotePojoFactory clientRemotePojoFactory;

    /**
     * Constructor for {@link AgentClientImpl}.
     *
     * @param  agent  the agent that this client will communicate with
     * @param  sender the object that is used to send commands to the given agent
     *
     * @throws IllegalArgumentException if <code>agent</code> or <code>sender</code> is <code>null</code>
     */
    public AgentClientImpl(Agent agent, ClientCommandSender sender) {
        if (agent == null) {
            throw new IllegalArgumentException("agent==null");
        }

        if (sender == null) {
            throw new IllegalArgumentException("sender==null");
        }

        // Note that to decrease the footprint of this object, we create a single remote pojo invoker
        // and use it to build all of our remote pojo proxies.  This means that all settings within
        // that one pojo invoker are set across all remote proxy invocations (unless there are annotations
        // overriding those settings in the remote interfaces).  It is OK if, in the future, we want to
        // have one pojo invoker with one set of settings that is used to create one type of remote proxy,
        // with another pojo invoker with another set of settings that is used to create another type of remote proxy.
        // For now, we assume the remote pojo invoker configuration is OK for all remote proxies.
        this.agent = agent;
        this.sender = sender;
        this.clientRemotePojoFactory = sender.getClientRemotePojoFactory();
        this.sender.setSendCallbacks(new SendCallback[] { new AgentSendCallback(agent) });
        // enforce the restriction (instituted in 1.1 due to multi-server HA concerns)
        // that no server->agent calls use guaranteedDelivery
        this.clientRemotePojoFactory.setDeliveryGuaranteed(ClientRemotePojoFactory.GuaranteedDelivery.DISABLED);
    }

    @Override
    public Agent getAgent() {
        return this.agent;
    }

    @Override
    public String toString() {
        return getAgent().toString();
    }

    @Override
    public void startSending() {
        this.sender.startSending();
    }

    @Override
    public void stopSending() {
        // Passing in false means any current commands that are queue will be aborted;
        // guaranteed commands will be persisted, but any volatile commands will be lost.
        // If we find that this method is called when we can't afford to lose messages
        // currently queued or in-flight, think about passing in true here instead.
        this.sender.stopSending(false);
    }

    @Override
    public void updatePlugins() {
        PluginContainerLifecycle lifecycle = clientRemotePojoFactory.getRemotePojo(PluginContainerLifecycle.class);
        lifecycle.updatePlugins();
    }

    @Override
    public boolean ping(long timeoutMillis) {
        try {
            // create our own factory so we can customize the timeout
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            factory.setTimeout(timeoutMillis);
            Ping pinger = factory.getRemotePojo(Ping.class);
            pinger.ping("", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean pingService(long timeoutMillis) {
        try {
            // create our own factory so we can customize the timeout
            ClientRemotePojoFactory factory = sender.getClientRemotePojoFactory();
            factory.setTimeout(timeoutMillis);
            PingAgentService pinger = factory.getRemotePojo(PingAgentService.class);
            long agentTime = pinger.ping();
            long skew = Math.abs(System.currentTimeMillis() - agentTime);
            if (skew > 5 * 60 * 1000L) {
                LogFactory.getLog(this.getClass()).debug(
                    "Agent [" + agent.getName() + "] either took a long time to process a ping (" + skew
                        + "ms) or its clock is not synced: " + new Date(agentTime));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public BundleAgentService getBundleAgentService() {
        return clientRemotePojoFactory.getRemotePojo(BundleAgentService.class);
    }

    @Override
    public BundleAgentService getBundleAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(BundleAgentService.class, timeout);
    }

    @Override
    public ContentAgentService getContentAgentService() {
        return clientRemotePojoFactory.getRemotePojo(ContentAgentService.class);
    }

    @Override
    public ContentAgentService getContentAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(ContentAgentService.class, timeout);
    }

    @Override
    public ResourceFactoryAgentService getResourceFactoryAgentService() {
        return clientRemotePojoFactory.getRemotePojo(ResourceFactoryAgentService.class);
    }

    @Override
    public ResourceFactoryAgentService getResourceFactoryAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(ResourceFactoryAgentService.class, timeout);
    }

    @Override
    public DiscoveryAgentService getDiscoveryAgentService() {
        return clientRemotePojoFactory.getRemotePojo(DiscoveryAgentService.class);
    }

    @Override
    public DiscoveryAgentService getDiscoveryAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(DiscoveryAgentService.class, timeout);
    }

    @Override
    public MeasurementAgentService getMeasurementAgentService() {
        return clientRemotePojoFactory.getRemotePojo(MeasurementAgentService.class);
    }

    @Override
    public MeasurementAgentService getMeasurementAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(MeasurementAgentService.class, timeout);
    }

    @Override
    public OperationAgentService getOperationAgentService() {
        return clientRemotePojoFactory.getRemotePojo(OperationAgentService.class);
    }

    @Override
    public OperationAgentService getOperationAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(OperationAgentService.class, timeout);
    }

    @Override
    public ConfigurationAgentService getConfigurationAgentService() {
        return clientRemotePojoFactory.getRemotePojo(ConfigurationAgentService.class);
    }

    @Override
    public ConfigurationAgentService getConfigurationAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(ConfigurationAgentService.class, timeout);
    }

    @Override
    public SupportAgentService getSupportAgentService() {
        return clientRemotePojoFactory.getRemotePojo(SupportAgentService.class);
    }

    @Override
    public SupportAgentService getSupportAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(SupportAgentService.class, timeout);
    }

    @Override
    public DriftAgentService getDriftAgentService() {
        return clientRemotePojoFactory.getRemotePojo(DriftAgentService.class);
    }

    @Override
    public DriftAgentService getDriftAgentService(Long timeout) {
        return clientRemotePojoFactory.getRemotePojo(DriftAgentService.class, timeout);
    }

    /**
     * This class is used to ensure that when sending commands from Server to Agent we correctly
     * set the ExternalizableStrategy to AGENT.  Since the server may share threads with RemoteAPI
     * processing it's possible that the thread may have a different strategy set.  We serialize
     * differently for the different strategies, for Agent communication we use much more lightweight
     * serialization (for performance reasons).
     *
     * @author jshaughnessy
     */
    private static class AgentSendCallback implements SendCallback {

        private final String token;

        public AgentSendCallback(Agent targetAgent) {
            token = targetAgent.getAgentToken();
        }

        public void sending(Command command) {
            try {
                HibernateDetachUtility.nullOutUninitializedFields(command,
                    HibernateDetachUtility.SerializationType.SERIALIZATION);
            } catch (Exception e) {
                e.printStackTrace();
            }

            ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.AGENT);

            if (token != null) {
                command.getConfiguration().setProperty(CMDCONFIG_PROP_SECURITY_TOKEN, token);
            }

            return;
        }

        public CommandResponse sent(Command command, CommandResponse response) {
            return response;
        }
    }
}
