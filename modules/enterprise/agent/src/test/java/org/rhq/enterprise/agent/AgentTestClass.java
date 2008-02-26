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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.BackingStoreException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.ServiceContainerMetricsMBean;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderMetrics;

/**
 * The class that tests can use to configure and create agents. This class also provides the output of the agent as well
 * as allowing input to the agent.
 *
 * @author John Mazzitelli
 */
public class AgentTestClass {
    /**
     * The default configuration file this test class will use if one isn't specified.
     */
    public static final String DEFAULT_CONFIG_FILE = "test-agent-configuration.xml";

    /**
     * The default preference node used by the configuration file - this is used if the node name isn't specified.
     */
    public static final String DEFAULT_CONFIG_PREF_NODE = "test";

    private String m_configFilePath = DEFAULT_CONFIG_FILE;
    private String m_preferenceNodeName = DEFAULT_CONFIG_PREF_NODE;
    private Properties m_configOverrides = null;
    private List m_inputCommands = null;
    private File m_outputFile = null;
    private File m_inputFile = null;
    private boolean m_captureOutput = false;
    private AgentMain m_agent = null;

    /**
     * Constructor for {@link AgentTestClass}.
     */
    public AgentTestClass() {
    }

    /**
     * Sets the location of the configuration file and the name of the preferences node that is in the configuration
     * file. The file path can be a path in the classloader or an absolute file system path.
     *
     * @param file_path      the preferences configuration file
     * @param pref_node_name the preferences node name that corresponds to the node name in the configuration file
     */
    public void setConfigurationFile(String file_path, String pref_node_name) {
        assert file_path != null : "File path must not be null";
        assert pref_node_name != null : "Preference node name must not be null";

        m_configFilePath = file_path;
        m_preferenceNodeName = pref_node_name;
    }

    /**
     * Sets some properties that can be used to override those properties in the
     * {@link #setConfigurationFile(String, String) configuration file}. This is useful when a series of tests want to
     * use mostly the same configuration with one or two config properties that vary.
     *
     * @param props properties that override those found in the configuration file
     */
    public void setConfigurationOverrides(Properties props) {
        m_configOverrides = props;
    }

    /**
     * This sets a list of commands that will be input to the agent - the commands will be stored to a temporary file
     * and passed to the agent via its "-i" argument.
     *
     * @param commands set of commands for the agent to execute
     */
    public void setInputCommands(List commands) {
        m_inputCommands = commands;
    }

    /**
     * Sets the flag to indicate if the agent's output should be captured. If a test wants to be able to get the
     * {@link #getAgentOutput() agent output}, it must set this flag to <code>true</code>.
     *
     * @param flag if <code>true</code>, the agent's output will be redirected to a file that will later be read in via
     *             {@link #getAgentOutput()}.
     */
    public void setCaptureOutput(boolean flag) {
        m_captureOutput = flag;
    }

    /**
     * This creates an agent with the configuration specified by the
     * {@link #setConfigurationFile(String, String) config file} and
     * {@link #setConfigurationOverrides(Properties) override properties}.
     *
     * @param  start_it if <code>true</code>, the agent will be started before it is returned
     *
     * @return the newly created agent instance
     *
     * @throws Exception if an error occurred while created or starting the agent
     */
    public AgentMain createAgent(boolean start_it) throws Exception {
        // make sure we clean up any previously created agent
        if (m_agent != null) {
            m_agent.shutdown();
            m_agent = null;
        }

        assert m_configFilePath != null : "No configuration file has been specified for the agent";

        ArrayList<String> args = new ArrayList<String>();

        if (m_captureOutput) {
            m_outputFile = createOutputFile();
            args.add("-o");
            args.add(m_outputFile.getAbsolutePath());
        }

        if ((m_inputCommands != null) && (m_inputCommands.size() > 0)) {
            m_inputFile = createInputFile();
            args.add("-i");
            args.add(m_inputFile.getAbsolutePath());
        }

        args.add("-d"); // put it daemon mode, otherwise, the agent main input loop thread never dies in our tests
        args.add("-l"); // clear out any old preferences
        args.add("-c");
        args.add(m_configFilePath);
        args.add("-p");
        args.add(m_preferenceNodeName);

        if ((m_configOverrides != null) && (m_configOverrides.size() > 0)) {
            for (Iterator iter = m_configOverrides.entrySet().iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry) iter.next();
                args.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        m_agent = new AgentMain(args.toArray(new String[0]));

        if (start_it) {
            // start the agent *and* allow it to send messages immediately
            m_agent.start();
            m_agent.getClientCommandSender().startSending();
        }

        return m_agent;
    }

    /**
     * Returns the agent that was created by this class. If this class hasn't created an agent yet, <code>null</code> is
     * returned.
     *
     * @return the created agent or <code>null</code>
     */
    public AgentMain getAgent() {
        return m_agent;
    }

    /**
     * Returns a proxy to the server metrics MBean.
     *
     * @return metrics mbean that contains server-side metrics
     *
     * @throws RuntimeException if can't create the MBean proxy
     */
    public ServiceContainerMetricsMBean getServerMetrics() {
        MBeanServer mbs = getAgent().getServiceContainer().getMBeanServer();
        ServiceContainerMetricsMBean metrics;

        try {
            metrics = (ServiceContainerMetricsMBean) MBeanServerInvocationHandler.newProxyInstance(mbs,
                ServiceContainerMetricsMBean.OBJECTNAME_METRICS, ServiceContainerMetricsMBean.class, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return metrics;
    }

    /**
     * Returns the set of metrics for the agent's client sender.
     *
     * @return metrics
     */
    public ClientCommandSenderMetrics getClientMetrics() {
        return getAgent().getClientCommandSender().getMetrics();
    }

    /**
     * This will clear any and all agent configuration preferences. After this method returns, only default values will
     * remain in effect. An exception is thrown if an agent has not yet been {@link #createAgent(boolean) created}.
     *
     * @throws RuntimeException if the agent hasn't been created yet or if the configuration failed to get cleared
     */
    public void clearAgentConfiguration() {
        if (m_agent == null) {
            throw new RuntimeException("Cannot clear configuration - the agent has not yet been created");
        }

        try {
            m_agent.getConfiguration().getPreferences().clear();
        } catch (BackingStoreException e) {
            throw new RuntimeException("Failed to clear the agent configuration", e);
        }
    }

    /**
     * Call this when you want to delete the temporary input and output files this class created.
     */
    public void cleanUpFiles() {
        if (m_outputFile != null) {
            getAgent().getOut().close();
            m_outputFile.delete();
        }

        if (m_inputFile != null) {
            m_inputFile.delete();
        }

        return;
    }

    /**
     * This returns the agent's previous output.
     *
     * @return agent output as one big string
     *
     * @throws FileNotFoundException if the agent was not told to capture output or the output file cannot be opened for
     *                               some reason
     */
    public String getAgentOutput() throws FileNotFoundException {
        String output = null;

        if (m_outputFile != null) {
            FileInputStream fis = new FileInputStream(m_outputFile);
            output = new String(StreamUtil.slurp(fis));
        } else {
            throw new FileNotFoundException("The agent was not told to capture its output - no output available");
        }

        return output;
    }

    /**
     * Creates the input file with the set of {@link #setInputCommands(List) input commands}. The file will be created
     * in a temporary location and will be deleted on exit.
     *
     * @return the file that was created
     *
     * @throws Exception if failed to create the file
     */
    private File createInputFile() throws Exception {
        File file = File.createTempFile("input-commands", null);
        file.deleteOnExit();

        PrintWriter pw = new PrintWriter(new FileWriter(file));

        for (Iterator iter = m_inputCommands.iterator(); iter.hasNext();) {
            String cmd = (String) iter.next();
            pw.println(cmd);
        }

        pw.close();

        return file;
    }

    /**
     * Creates the output file where the agent will dump its output. The file will be created in a temporary location
     * and will be deleted on exit.
     *
     * @return the file that was created
     *
     * @throws Exception if failed to create the file
     */
    private File createOutputFile() throws Exception {
        File file = File.createTempFile("agent-output", null);
        file.deleteOnExit();

        return file;
    }
}