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

import java.util.Properties;

import org.testng.annotations.Test;

import org.jboss.remoting.InvokerLocator;

import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryListener;

/**
 * This tests the communications layer in the agent.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-comm")
public class AgentComm4Test extends AgentCommTestBase {
    private static final boolean ENABLE_TESTS = true;

    /**
     * Starts and immediately stops the agents - showing that you can have more than one agent running on the box at any
     * one time.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentStartStop() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        agent1.shutdown();
        agent2.shutdown();

        assert !agent1.isStarted() : "agent1 should have been shutdown";
        assert !agent2.isStarted() : "agent2 should have been shutdown";

        return;
    }

    /**
     * Checks that you can override configuration properties.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAgentConfigurationOverride() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "socket", "127.0.0.1", 22345, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "12345");
        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "socket", "127.0.0.1", 12345, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22345");
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.isStarted() : "agent1 should have been started";
        assert agent2.isStarted() : "agent2 should have been started";

        assert agent1.getServiceContainer().getConfiguration().getConnectorBindPort() == 12345;
        assert agent2.getServiceContainer().getConfiguration().getConnectorBindPort() == 22345;

        agent1.shutdown();
        agent2.shutdown();

        assert !agent1.isStarted() : "agent1 should have been shutdown";
        assert !agent2.isStarted() : "agent2 should have been shutdown";

        return;
    }

    /**
     * Sends a message from one remote server to another.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendMessage() throws Exception {
        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        IdentifyCommand command = new IdentifyCommand();
        IdentifyCommandResponse response;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends messages from one remote server to another and makes sure the metrics are collected properly.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testMetrics() throws Exception {

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        Thread.sleep(2000); // let's wait for the senders to start and send the connectAgent messages (which will fail)

        ServiceContainerMetricsMBean server_metrics1 = m_agent1Test.getServerMetrics();
        ServiceContainerMetricsMBean server_metrics2 = m_agent2Test.getServerMetrics();
        ClientCommandSenderMetrics client_metrics1 = m_agent1Test.getClientMetrics();
        ClientCommandSenderMetrics client_metrics2 = m_agent2Test.getClientMetrics();

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics1.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getAverageExecutionTimeSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 0;
        assert server_metrics1.getNumberTotalCommandsReceived() == 0;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics2.getNumberFailedCommandsReceived() == 0;
        assert server_metrics2.getNumberTotalCommandsReceived() == 0;
        assert server_metrics2.getAverageExecutionTimeReceived() == 0;

        IdentifyCommand command = new IdentifyCommand();
        IdentifyCommandResponse response;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 1;
        assert client_metrics1.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 0;
        assert server_metrics1.getNumberTotalCommandsReceived() == 0;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 1;
        assert server_metrics2.getNumberFailedCommandsReceived() == 0;
        assert server_metrics2.getNumberTotalCommandsReceived() == 1;

        response = (IdentifyCommandResponse) agent1.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent1 to agent2";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 0;
        assert client_metrics2.getNumberFailedCommandsSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 0;
        assert server_metrics1.getNumberFailedCommandsReceived() == 0;
        assert server_metrics1.getNumberTotalCommandsReceived() == 0;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 0;
        assert server_metrics2.getNumberTotalCommandsReceived() == 2;

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 1;
        assert client_metrics2.getNumberFailedCommandsSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 1;
        assert server_metrics1.getNumberFailedCommandsReceived() == 0;
        assert server_metrics1.getNumberTotalCommandsReceived() == 1;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 0;
        assert server_metrics2.getNumberTotalCommandsReceived() == 2;

        response = (IdentifyCommandResponse) agent2.getClientCommandSender().sendSynch(command);
        assert response.isSuccessful() : "Failed to send command from agent2 to agent1";

        assert client_metrics1.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics1.getNumberFailedCommandsSent() == 0;
        assert client_metrics2.getNumberSuccessfulCommandsSent() == 2;
        assert client_metrics2.getNumberFailedCommandsSent() == 0;
        assert server_metrics1.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics1.getNumberFailedCommandsReceived() == 0;
        assert server_metrics1.getNumberTotalCommandsReceived() == 2;
        assert server_metrics2.getNumberSuccessfulCommandsReceived() == 2;
        assert server_metrics2.getNumberFailedCommandsReceived() == 0;
        assert server_metrics2.getNumberTotalCommandsReceived() == 2;

        return;
    }

    /**
     * Tests auto-discovery listeners.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    // WHY DOES THIS ALWAYS FAIL?
    public void testAutoDiscoveryListeners() throws Exception {
        System.out.println("testAutoDiscoveryListeners - WHY DOES THIS PERIODICALLY FAIL?");

        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();
        agent1.getClientCommandSender().getRemoteCommunicator().setInitializeCallback(null);

        final Boolean[] online = new Boolean[] { null };
        agent1.getServiceContainer().addDiscoveryListener(new AutoDiscoveryListener() {
            public void serverOnline(InvokerLocator locator) {
                online[0] = Boolean.TRUE;
            }

            public void serverOffline(InvokerLocator locator) {
                online[0] = Boolean.FALSE;
            }
        });

        AgentMain agent2 = null;
        try {
            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            for (int i = 0; (i < 20) && (online[0] == null); i++) {
                Thread.sleep(1000L);
            }

            assert online[0].booleanValue() : "The server running in agent2 was never auto-discovered by agent1";

            // now shutdown agent2 and see that we auto-discovery it going down
            agent2.shutdown();

            for (int i = 0; (i < 20) && (online[0].booleanValue()); i++) {
                Thread.sleep(1000L);
            }

            assert !online[0].booleanValue() : "The server running in agent2 went down but that was not auto-detected by agent1";
        } finally {
            if (agent1 != null) {
                agent1.shutdown();
            }

            if (agent2 != null) {
                agent2.shutdown();
            }
        }

        return;
    }

    /**
     * Tests auto-discovery.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testAutoDiscovery() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "true");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();
        agent1.getClientCommandSender().getRemoteCommunicator().setInitializeCallback(null);

        AgentMain agent2 = null;
        try {
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode yet";

            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            // our canary-in-the-mine is the sending mode - when auto-discovery works, the sender goes into sending mode
            boolean discovered = false;
            for (int i = 0; (i < 20) && !discovered; i++) {
                Thread.sleep(1000L);
                discovered = agent1.getClientCommandSender().isSending();
            }

            assert discovered : "The server running in agent2 was never auto-discovered by agent1";

            // now shutdown agent2 and see that we auto-discovery it going down
            agent2.shutdown();

            boolean gone_down = false;
            for (int i = 0; (i < 20) && !gone_down; i++) {
                Thread.sleep(1000L);
                gone_down = !agent1.getClientCommandSender().isSending();
            }

            assert gone_down : "The server running in agent2 went down but that was not auto-detected by agent1";
        } finally {
            if (agent1 != null) {
                agent1.shutdown();
            }

            if (agent2 != null) {
                agent2.shutdown();
            }
        }

        return;
    }

    /**
     * Tests server polling (which is like auto-discovery).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testServerPolling() throws Exception {
        Properties props1 = new Properties();
        props1.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "1000");

        Properties props2 = new Properties();
        props2.setProperty(AgentConfigurationConstants.SERVER_AUTO_DETECTION, "false");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SERVER_POLLING_INTERVAL, "-1");

        m_agent1Test.setConfigurationOverrides(props1);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        agent1.start();
        agent1.getClientCommandSender().getRemoteCommunicator().setInitializeCallback(null);

        AgentMain agent2 = null;
        try {
            assert !agent1.getClientCommandSender().isSending() : "Should not be in sending mode yet";

            // now start agent2 which creates the remoting server that agent1 should auto-detect
            agent2 = m_agent2Test.createAgent(true);

            // our canary-in-the-mine is the sending mode - when server polling detects the server, the sender goes into sending mode
            boolean discovered = false;
            for (int i = 0; (i < 5) && !discovered; i++) {
                Thread.sleep(1000L);
                discovered = agent1.getClientCommandSender().isSending();
            }

            assert discovered : "The server running in agent2 was never detected by agent1's server polling";

            // now shutdown agent2 and see that we discover it going down via server polling
            agent2.shutdown();

            boolean gone_down = false;
            for (int i = 0; (i < 60) && !gone_down; i++) {
                Thread.sleep(1000L);
                gone_down = !agent1.getClientCommandSender().isSending();
            }

            assert gone_down : "The server running in agent2 went down but that was not detected by agent1's server polling";
        } finally {
            if (agent1 != null) {
                agent1.shutdown();
            }

            if (agent2 != null) {
                agent2.shutdown();
            }
        }

        return;
    }
}