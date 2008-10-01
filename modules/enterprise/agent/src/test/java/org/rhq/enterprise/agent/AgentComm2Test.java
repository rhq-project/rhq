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

import java.io.File;
import java.util.Properties;

import org.testng.annotations.Test;

import org.jboss.remoting.InvokerLocator;
import org.jboss.remoting.security.SSLSocketBuilder;

import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommand;
import org.rhq.enterprise.communications.command.impl.identify.IdentifyCommandResponse;

/**
 * This tests the communications layer in the agent.
 *
 * @author John Mazzitelli
 */
@Test(groups = "agent-comm")
public class AgentComm2Test extends AgentCommTestBase {
    private static final boolean ENABLE_TESTS = true;

    /**
     * Show that the agent will create the keystore when just the agent is secured (but the server is not).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCreateKeystoreSecureAgent() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "socket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");

        m_agent1Test.setConfigurationOverrides(props1);

        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        m_agent1Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";
    }

    /**
     * Show that the agent will create the keystore when just the server is secured (but the agent is not).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testCreateKeystoreSecureServer() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "socket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");

        m_agent1Test.setConfigurationOverrides(props1);

        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        m_agent1Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";
    }

    /**
     * Sends a secure message from one remote server to another with server authentication on without a truststore to
     * see the failure actually occurs.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageServerAuthFailure() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        assert !agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Should have been able to send - agent1 does not need to auth the server: "
            + cmdresponse;

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert !cmdresponse.isSuccessful() : "Should not have been able to send - agent2 don't have a truststore to authenticate the server";

        return;
    }

    /**
     * Sends a secure message from one remote server to another with client authentication on without a keystore to see
     * the failure actually occurs.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageClientAuthFailure() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_KEYSTORE_FILE, "");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Should have been able to send - agent2 only 'wants' to auth clients, but is not required to: "
            + cmdresponse;

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert !cmdresponse.isSuccessful() : "Should not have been able to send - agent1 'needs' client auth but it doesn't have a truststore to authenticate clients";

        return;
    }

    /**
     * Sends a secure message from one remote server to another - there will be no authentication, just encryption.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageNoAuth() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        // side test - see the agent create the keystore when it starts
        File keystore1 = new File("target/testdata/keystore.dat");
        keystore1.delete();
        assert !keystore1.exists() : "Strange - we deleted the keystore but it still exists";

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert keystore1.exists() : "The agent failed to create its keystore upon startup";

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends a secure message from one remote server to another - there will be full authentication including server and
     * client authentication.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageFullAuth() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        // JBoss Remoting 2 doesn't seem to like me doing bi-directional messaging within the same VM
        //      cmdresponse = agent2.getClientCommandSender().sendSynch( command );
        //      assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        //      response = (IdentifyCommandResponse) cmdresponse;
        //      assert new InvokerLocator( response.getIdentification().getInvokerLocator() ).getPort()
        //             == agent1.getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Prepares to send a secure message from one remote server to another - there will be full authentication including
     * server and client authentication but the keystore passwords will be invalid thus causing errors.
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageFullAuthWrongKeystorePassword() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD,
            "invalidpassword");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_KEYSTORE_PASSWORD,
            "invalidpassword2");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NEED);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(false);
        AgentMain agent2 = m_agent2Test.createAgent(false);

        try {
            agent1.start();
            assert false : "Should not have been able to start agent1 - the wrong keystore password should cause problems starting";
        } catch (Exception e) {
        }

        try {
            agent2.start();
            assert false : "Should not have been able to start agent2 - the wrong keystore password should cause problems starting";
        } catch (Exception e) {
        }

        return;
    }

    /**
     * Sends a secure message from one remote server to another where both servers want client authentication and both
     * clients have truststores with the appropriate keys (therefore, the clients should be able to successfully send).
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageClientAuthWantWithTruststore() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata/keystore.dat");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getConfiguration().isClientSenderSecurityServerAuthMode();
        assert agent2.getConfiguration().isClientSenderSecurityServerAuthMode();

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecurityClientAuthMode().equals(
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        // JBoss Remoting 2.2.SP4 doesn't seem to like me doing bi-directional messaging within the same VM
        //      cmdresponse = agent2.getClientCommandSender().sendSynch( command );
        //      assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        //      response = (IdentifyCommandResponse) cmdresponse;
        //      assert new InvokerLocator( response.getIdentification().getInvokerLocator() ).getPort()
        //             == agent1.getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }

    /**
     * Sends a secure message from one remote server to another with the secure protocol being different for each remote
     * server (cool, huh?) - there will be no authentication, just encryption.
     *
     * <p>What this tests:<br>
     * agent1 ---- TLS ---> agent2<br>
     * agent1 <--- SSL ---- agent2<br>
     * </p>
     *
     * @throws Exception
     */
    @Test(enabled = ENABLE_TESTS)
    public void testSendSecureMessageNoAuthDifferentProtocol() throws Exception {
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", 22222, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "11111");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "TLS");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "SSL");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", 11111, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, "22222");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SOCKET_PROTOCOL, "SSL");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_SOCKET_PROTOCOL, "TLS");
        props2.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "false");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_NONE);
        m_agent2Test.setConfigurationOverrides(props2);

        AgentMain agent1 = m_agent1Test.createAgent(true);
        AgentMain agent2 = m_agent2Test.createAgent(true);

        assert agent1.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");
        assert agent2.getConfiguration().getServerLocatorUri().startsWith("sslsocket:");

        assert agent1.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");
        assert agent2.getServiceContainer().getConfiguration().getConnectorTransport().equals("sslsocket");

        assert agent1.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("SSL");
        assert agent2.getServiceContainer().getConfiguration().getConnectorSecuritySocketProtocol().equals("TLS");

        assert agent1.getConfiguration().getClientSenderSecuritySocketProtocol().equals("TLS");
        assert agent2.getConfiguration().getClientSenderSecuritySocketProtocol().equals("SSL");

        IdentifyCommand command = new IdentifyCommand();
        CommandResponse cmdresponse;
        IdentifyCommandResponse response;

        cmdresponse = agent1.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent1 to agent2: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent2
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identify of agent2 - what remoting server did we just communicate with??";

        cmdresponse = agent2.getClientCommandSender().sendSynch(command);
        assert cmdresponse.isSuccessful() : "Failed to send command from agent2 to agent1: " + cmdresponse;
        response = (IdentifyCommandResponse) cmdresponse;
        assert new InvokerLocator(response.getIdentification().getInvokerLocator()).getPort() == agent1
            .getServiceContainer().getConfiguration().getConnectorBindPort() : "Didn't get the identity of agent1 - what remoting server did we just communicate with??";

        return;
    }
}