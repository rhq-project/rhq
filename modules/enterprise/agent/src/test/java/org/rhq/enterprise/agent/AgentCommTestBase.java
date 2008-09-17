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
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.rhq.enterprise.communications.ServiceContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;
import org.rhq.enterprise.communications.util.SecurityUtil;

/**
 * Base class used by the tests of communications layer in the agent.
 *
 * @author John Mazzitelli
 */
public class AgentCommTestBase {
    protected static final int LARGE_STRING_SIZE = 100000;
    protected static final String LARGE_STRING;
    protected static final byte[] LARGE_STRING_BYTES;

    static {
        StringBuffer stream_data_buf = new StringBuffer(LARGE_STRING_SIZE);

        for (int i = 0; i < LARGE_STRING_SIZE; i += 10) {
            stream_data_buf.append(".123456789");
        }

        LARGE_STRING = stream_data_buf.toString();
        LARGE_STRING_BYTES = LARGE_STRING.getBytes();
    }

    protected File m_veryLargeFile = null;

    protected AgentTestClass m_agent1Test;
    protected AgentTestClass m_agent2Test;

    /**
     * Creates new agent test classes.
     *
     * @throws Exception
     */
    @BeforeMethod
    public void setUp() throws Exception {
        m_agent1Test = new AgentTestClass();
        m_agent1Test.setConfigurationFile("test-agent-configuration.xml", "test");

        m_agent2Test = new AgentTestClass();
        m_agent2Test.setConfigurationFile("test-agent2-configuration.xml", "test2");

        // make sure we create keystore files - some tests needs both existing at the same time as soon as they begin
        File keystore_file1 = new File("target/testdata/keystore.dat");

        if (!keystore_file1.exists()) {
            keystore_file1.getParentFile().mkdirs();
            m_agent1Test.createAgent(false);
            AgentConfiguration agent_config1 = m_agent1Test.getAgent().getConfiguration();
            ServiceContainerConfiguration server_config1 = agent_config1.getServiceContainerPreferences();

            SecurityUtil.createKeyStore(server_config1.getConnectorSecurityKeystoreFile(), server_config1
                .getConnectorSecurityKeystoreAlias(), "CN=JBoss ON Communications Server, OU=JBoss, O=jboss.com, C=US",
                server_config1.getConnectorSecurityKeystorePassword(), server_config1
                    .getConnectorSecurityKeystoreKeyPassword(), "DSA", 36500);
        }

        File keystore_file2 = new File("target/testdata2/keystore.dat");
        if (!keystore_file2.exists()) {
            keystore_file2.getParentFile().mkdirs();
            m_agent2Test.createAgent(false);
            AgentConfiguration agent_config2 = m_agent2Test.getAgent().getConfiguration();
            ServiceContainerConfiguration server_config2 = agent_config2.getServiceContainerPreferences();

            SecurityUtil.createKeyStore(server_config2.getConnectorSecurityKeystoreFile(), server_config2
                .getConnectorSecurityKeystoreAlias(), "CN=JBoss ON Communications Server, OU=JBoss, O=jboss.com, C=US",
                server_config2.getConnectorSecurityKeystorePassword(), server_config2
                    .getConnectorSecurityKeystoreKeyPassword(), "DSA", 36500);
        }
    }

    /**
     * Ensures any agent that was started is shutdown and all configuration is cleared so as not to retain overridden
     * preferences left over by the tests.
     */
    @AfterMethod
    public void tearDown() {
        if (m_agent1Test != null) {
            AgentMain agent = m_agent1Test.getAgent();
            if (agent != null) {
                agent.shutdown();
                m_agent1Test.clearAgentConfiguration();
                m_agent1Test.cleanUpFiles();
            }
        }

        if (m_agent2Test != null) {
            AgentMain agent = m_agent2Test.getAgent();
            if (agent != null) {
                agent.shutdown();
                m_agent2Test.clearAgentConfiguration();
                m_agent2Test.cleanUpFiles();
            }
        }

        // Because overrides (-D arguments to AgentMain) also set System properties, you can't run multiple agents in a single VM
        // when you override arguments that are NOT to be the same on all agents in the VM (like bind port).  You can override
        // things like queue size and other params that can be the same across all agents in the VM.
        // Here, I just clear all system properties that any test might have set so we don't run into this problem.
        Properties sysprops = System.getProperties();
        Properties to_remove = new Properties();
        for (Iterator iter = sysprops.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            if (((String) entry.getKey()).startsWith(ServiceContainerConfigurationConstants.PROPERTY_NAME_PREFIX)
                || ((String) entry.getKey()).startsWith(AgentConfigurationConstants.PROPERTY_NAME_PREFIX)) {
                to_remove.put(entry.getKey(), entry.getValue());
            }
        }

        for (Iterator iter = to_remove.keySet().iterator(); iter.hasNext();) {
            System.getProperties().remove(iter.next());
        }

        return;
    }

    /**
     * Cleans up after all the tests in this class have executed.
     */
    @AfterClass
    public void cleanUp() {
        if (m_veryLargeFile != null) {
            m_veryLargeFile.delete();
        }
    }

    /**
     * This will set the appropriate properties in <code>props</code> to define the server locator URI.
     *
     * @param props
     * @param transport
     * @param addr
     * @param port
     * @param transport_params
     */
    protected void setServerLocatorUriProperties(Properties props, String transport, String addr, int port,
        String transport_params) {
        props.setProperty(AgentConfigurationConstants.SERVER_TRANSPORT, transport);
        props.setProperty(AgentConfigurationConstants.SERVER_BIND_ADDRESS, addr);
        props.setProperty(AgentConfigurationConstants.SERVER_BIND_PORT, Integer.toString(port));
        if (transport_params != null) {
            props.setProperty(AgentConfigurationConstants.SERVER_TRANSPORT_PARAMS, transport_params);
        }
    }

    /**
     * Creates a very large file that we can use for testing streaming of large data.
     *
     * @return the file
     *
     * @throws Exception
     */
    protected File createVeryLargeFile() throws Exception {
        if (m_veryLargeFile == null) {
            File file = File.createTempFile("agent-comm-test-large-file", null);
            FileOutputStream o = new FileOutputStream(file);

            // creates a 1MB file
            for (int i = 0; i < 1000000; i++) {
                o.write("123456789\n".getBytes());
            }

            o.close();

            m_veryLargeFile = file;
        }

        return m_veryLargeFile;
    }
}