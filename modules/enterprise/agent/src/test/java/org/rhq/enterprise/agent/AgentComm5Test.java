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
@Test(groups = "agent.comm")
public class AgentComm5Test extends AgentCommTestBase {
    /**
     * Sends a secure message from one remote server to another where both servers want client authentication and both
     * clients have truststores with the appropriate keys (therefore, the clients should be able to successfully send).
     *
     * @throws Exception
     */
    public void testSendSecureMessageClientAuthWantWithTruststore() throws Exception {
        // each keystore is the other's truststore
        Properties props1 = new Properties();
        setServerLocatorUriProperties(props1, "sslsocket", "127.0.0.1", AGENT2_SERVER_BIND_PORT, null);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, AGENT1_COMM_CONNECTOR_BIND_PORT);
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_TRUSTSTORE_FILE,
            "target/testdata2/keystore.dat");
        props1.setProperty(AgentConfigurationConstants.CLIENT_SENDER_SECURITY_SERVER_AUTH_MODE, "true");
        props1.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_SECURITY_CLIENT_AUTH_MODE,
            SSLSocketBuilder.CLIENT_AUTH_MODE_WANT);

        m_agent1Test.setConfigurationOverrides(props1);

        Properties props2 = new Properties();
        setServerLocatorUriProperties(props2, "sslsocket", "127.0.0.1", AGENT1_SERVER_BIND_PORT, null);
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_TRANSPORT, "sslsocket");
        props2.setProperty(ServiceContainerConfigurationConstants.CONNECTOR_BIND_PORT, AGENT2_COMM_CONNECTOR_BIND_PORT);
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
}