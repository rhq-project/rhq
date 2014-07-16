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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;

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

    /**
     * Test importing and exporting the config files from agent
     */
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

    /**
     * Test importing config file from an old agent which hasn't declared DOCTYPE
     */
    @Test
    public void testImportWithMissingDoctype() throws Exception {
        m_agentTest = new AgentTestClass();
        AgentMain agent = m_agentTest.createAgent(true);

        // Store something
        Properties props = new Properties();
        props.setProperty(AgentConfigurationConstants.CLIENT_SENDER_QUEUE_SIZE, "12345");
        m_agentTest.setConfigurationOverrides(props);

        File testFile = File.createTempFile("config-test", ".xml", new File("target/"));
        testFile.deleteOnExit();

        agent.executePromptCommand("config export " + testFile.getAbsolutePath());
        File withoutDoctype = removeDoctype(testFile);
        agent.executePromptCommand("config import " + withoutDoctype.getAbsolutePath());
    }

    /**
     * Remove DOCTYPE declaration from the XML file
     * @param fileToProcess
     * @return
     * @throws Exception
     */
    private File removeDoctype(File fileToProcess) throws Exception {
        XMLEventReader eventReader = null;
        XMLEventWriter eventWriter = null;

        XMLInputFactory inputFactory = XMLInputFactory.newFactory();
        XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();

        FileInputStream fis = new FileInputStream(fileToProcess);
        File output = File.createTempFile("config-test-nodoctype", ".xml", new File("target/"));
        output.deleteOnExit();
        FileOutputStream fos = new FileOutputStream(output);

        eventReader = inputFactory.createXMLEventReader(fis);
        eventWriter = outputFactory.createXMLEventWriter(fos);

        while(eventReader.hasNext()) {
            XMLEvent xmlEvent = eventReader.nextEvent();
            switch(xmlEvent.getEventType()) {
                case XMLEvent.DTD:
                    // We have DTD declaration, remove it by skipping it
                    break;
                default:
                    eventWriter.add(xmlEvent);
                    break;
            }
        }
        eventReader.close();
        eventWriter.close();

        return output;
    }
}
