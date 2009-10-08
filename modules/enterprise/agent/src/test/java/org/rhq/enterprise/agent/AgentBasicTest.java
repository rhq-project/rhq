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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.enterprise.communications.ServiceContainerConfigurationConstants;

/**
 * This is mainly to test the test infrastructure than anything else. Tests will do no more than simply start and stop
 * the agent with different configurations.
 *
 * @author John Mazzitelli
 */
@Test
public class AgentBasicTest {
    private AgentTestClass m_agentTest;

    /**
     * Nulls out any previously existing agent test class.
     */
    @BeforeMethod
    public void setUp() {
        m_agentTest = null;
    }

    /**
     * Ensures any agent that was started is shutdown and all configuration is cleared so as not to retain overridden
     * preferences left over by the tests.
     */
    @AfterMethod
    public void tearDown() {
        if (m_agentTest != null) {
            AgentMain agent = m_agentTest.getAgent();
            if (agent != null) {
                agent.shutdown();
                m_agentTest.clearAgentConfiguration();
                m_agentTest.cleanUpFiles();
            }
        }

        return;
    }

    /**
     * Starts and immediately stops the agent.
     *
     * @throws Exception
     */
    public void testAgentStartStop() throws Exception {
        m_agentTest = new AgentTestClass();
        AgentMain agent = m_agentTest.createAgent(true);
        assert agent.isStarted() : "The agent should have been started";
        agent.shutdown();
        assert !agent.isStarted() : "The agent should have been shutdown";

        return;
    }

    /**
     * Checks that the agent configuration can build a correct plugin container configuration.
     *
     * @throws Exception
     */
    public void testPluginContainerConfiguration() throws Exception {
        m_agentTest = new AgentTestClass();

        AgentMain agent = m_agentTest.createAgent(false);
        assert !agent.isStarted() : "The agent should not have been started";
        assert agent.getConfiguration() != null : "Configuration should be available";

        PluginContainerConfiguration plugin_config = agent.getConfiguration().getPluginContainerConfiguration();
        assert plugin_config != null;
        assert plugin_config.getServerDiscoveryPeriod() == 5555;
        assert plugin_config.getServerDiscoveryInitialDelay() == 11;
        assert plugin_config.getServiceDiscoveryPeriod() == 4444;
        assert plugin_config.getServiceDiscoveryInitialDelay() == 12;
        assert plugin_config.getAvailabilityScanPeriod() == 3333;
        assert plugin_config.getAvailabilityScanInitialDelay() == 13;
        assert plugin_config.getMeasurementCollectionInitialDelay() == 14;
        assert plugin_config.getMeasurementCollectionThreadPoolSize() == 3;
        assert plugin_config.getOperationInvokerThreadPoolSize() == 2;

        return;
    }

    /**
     * Checks that you can override configuration properties.
     *
     * @throws Exception
     */
    public void testAgentConfigurationOverride() throws Exception {
        m_agentTest = new AgentTestClass();

        Properties props = new Properties();
        props.setProperty(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE, "12345");
        m_agentTest.setConfigurationOverrides(props);

        AgentMain agent = m_agentTest.createAgent(false);
        assert !agent.isStarted() : "The agent should not have been started";
        assert agent.getConfiguration() != null : "Configuration should be available";
        assert agent.getConfiguration().getClientSenderQueueSize() == 12345;

        return;
    }

    /**
     * Checks that you can override configuration properties with system properties.
     *
     * @throws Exception
     */
    public void testAgentConfigurationOverrideWithSystemProperties() throws Exception {
        AgentMain agent = new AgentMain();
        Preferences default_prefs = null;
        Preferences test_prefs = null;

        try {
            assert !agent.isStarted() : "The agent should not have been started";

            // just confirm that the default configuration does have the values we will override them to
            assert agent.getConfiguration() != null : "Configuration should be available";
            default_prefs = agent.getConfiguration().getPreferences();
            assert !(agent.getConfiguration().getClientSenderQueueSize() == 321);
            assert !(agent.getConfiguration().getClientSenderRetryInterval() == 112233);
            assert !(agent.getConfiguration().getPreferences().getBoolean(
                ServiceContainerConfigurationConstants.DISABLE_COMMUNICATIONS, false));
            assert !(agent.getConfiguration().getPreferences().getLong(
                ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME, 0L) == 12321);

            // set the properties that will override the preferences we will load in
            System.setProperty(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE, "321");
            System.setProperty(AgentConfigurationConstants.CLIENT_SENDER_RETRY_INTERVAL, "112233");
            System.setProperty(ServiceContainerConfigurationConstants.DISABLE_COMMUNICATIONS, "true");
            System.setProperty(ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME, "12321");

            agent.loadConfigurationFile(AgentTestClass.DEFAULT_CONFIG_PREF_NODE, AgentTestClass.DEFAULT_CONFIG_FILE);
            agent.overlaySystemPropertiesOnAgentConfiguration();

            assert agent.getConfiguration() != null : "Configuration should be available";
            test_prefs = agent.getConfiguration().getPreferences();
            assert agent.getConfiguration().getClientSenderQueueSize() == 321;
            assert agent.getConfiguration().getClientSenderRetryInterval() == 112233;
            assert agent.getConfiguration().getPreferences().getBoolean(
                ServiceContainerConfigurationConstants.DISABLE_COMMUNICATIONS, false);
            assert agent.getConfiguration().getPreferences().getLong(
                ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME, 0L) == 12321;
        } finally {
            System.getProperties().remove(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE);
            System.getProperties().remove(AgentConfigurationConstants.CLIENT_SENDER_RETRY_INTERVAL);
            System.getProperties().remove(ServiceContainerConfigurationConstants.DISABLE_COMMUNICATIONS);
            System.getProperties().remove(ServiceContainerConfigurationConstants.REMOTE_STREAM_MAX_IDLE_TIME);

            if (default_prefs != null) {
                default_prefs.clear();
            }

            if (test_prefs != null) {
                test_prefs.clear();
            }

            File dataDir = new File(AgentConfigurationConstants.DEFAULT_DATA_DIRECTORY);

            if (dataDir != null) {
                dataDir.delete();
            }
        }

        return;
    }

    /**
     * Makes sure agent doesn't get created when an invalid argument is passed to it.
     */
    public void testInvalidArgument() {
        try {
            new AgentMain(new String[] { "--wotgorilla" });
            assert false : "Should not have been allowed to create agent - an invalid argument was passed to it";
        } catch (Exception ignore) {
        }

        return;
    }

    /**
     * Tests input/output files to/from agent.
     *
     * @throws Exception
     */
    public void testInputOutput() throws Exception {
        m_agentTest = new AgentTestClass();

        List<String> commands = new ArrayList<String>(2);
        commands.add("help");
        commands.add("version");

        m_agentTest.setInputCommands(commands);
        m_agentTest.setCaptureOutput(true);

        m_agentTest.createAgent(true);
        Thread.sleep(1000L); // give it a second to execute the commands
        String output = m_agentTest.getAgentOutput();
        assert output.indexOf("version:") > 0 : "Does not look like the help command executed";
        assert output.indexOf("Build-Date") > 0 : "Does not look like the version command executed";

        return;
    }
}