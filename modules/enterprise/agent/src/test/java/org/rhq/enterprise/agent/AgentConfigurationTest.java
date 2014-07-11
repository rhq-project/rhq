/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test the configuration importing and exporting functionality.
 *
 * @author Michael Burman
 */
public class AgentConfigurationTest {

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

    @Test
    public void testExportAndImport() throws Exception {
        m_agentTest = new AgentTestClass();
        AgentMain agent = m_agentTest.createAgent(true);

        // Store something
        Properties props = new Properties();
        props.setProperty(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE, "12345");
        m_agentTest.setConfigurationOverrides(props);

        File testFile = File.createTempFile("config-test", ".xml", new File("target/"));
        agent.executePromptCommand("config export " + testFile.getAbsolutePath());
        agent.executePromptCommand("config import " + testFile.getAbsolutePath());
        testFile.deleteOnExit();
    }
}
