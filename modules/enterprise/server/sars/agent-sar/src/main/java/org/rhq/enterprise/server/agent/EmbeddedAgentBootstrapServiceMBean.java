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
package org.rhq.enterprise.server.agent;

import java.io.File;
import java.util.Properties;
import javax.management.ObjectName;
import org.jboss.mx.util.ObjectNameFactory;

/**
 * The interface to the MBean that embeds a JON Agent in the JON Server.
 *
 * @author John Mazzitelli
 */
public interface EmbeddedAgentBootstrapServiceMBean {
    /**
     * The object name that the MBean service will be registered under.
     */
    ObjectName OBJECT_NAME = ObjectNameFactory.create("rhq:service=EmbeddedAgentBootstrap");

    /**
     * If <code>true</code>, this indicates that the agent should be started when the JON Server is started. This
     * service is will not start the agent at its own startup, regardless of the value of this attribute. Instead, this
     * attribute is examined by the JON Server itself - if the JON Server sees that the embedded agent should be started
     * at startup, the JON Server will call {@link #startAgent()}.
     *
     * @return enabled flag, boolean string
     */
    String getAgentEnabled();

    /**
     * If <code>true</code>, this indicates that the agent should be started when the JON Server is started. This
     * service is will not start the agent at its own startup, regardless of the value of this attribute. Instead, this
     * attribute is examined by the JON Server itself - if the JON Server sees that the embedded agent should be started
     * at startup, the JON Server will call {@link #startAgent()}.
     *
     * @param enabled (a boolean string)
     */
    void setAgentEnabled(String enabled);

    /**
     * Returns <code>true</code> if the embedded agent should reset its configuration at startup. Resetting the
     * configuration means to clear out any configuration settings currently persisted in the preferences store and
     * reload the configuration from its configuration file. If <code>false</code>, the embedded agent will retain the
     * configuration it had when it last was running - which may be different from its original configuration if a user
     * changed its configuration settings from the UI.
     *
     * @return reset configuration flag, as a String
     */
    String getResetConfiguration();

    /**
     * Sets the flag to determine if the agent should reset its configuration to its original configuration as defined
     * in the configuration file, or if it should retain its configuration from when it last ran. See
     * {@link #getResetConfiguration()} for more information.
     *
     * @param reset
     */
    void setResetConfiguration(String reset);

    /**
     * Indicates the location where the embedded agent and all its resources will be found. There must be a <code>
     * lib</code> subdirectory under this directory which will include all jar files that are to be included in the
     * embedded agent's classloader.
     *
     * @return location of the embedded agent and its resources
     */
    File getEmbeddedAgentDirectory();

    /**
     * Indicates the location where the embedded agent and all its resources will be found. There must be a <code>
     * lib</code> subdirectory under this directory which will include all jar files that are to be included in the
     * embedded agent's classloader.
     *
     * @param directory location of the embedded agent and its resources
     */
    void setEmbeddedAgentDirectory(File directory);

    /**
     * Returns the location of the configuration file where all agent preferences are defined. The file location can be
     * either a URL, a local file system path or a path within this service's classloader.
     *
     * @return configuration file location
     */
    String getConfigurationFile();

    /**
     * Defines the location of the configuration file where all agent preferences are defined.
     *
     * @param location
     */
    void setConfigurationFile(String location);

    /**
     * Returns the preferences node name used to identify the configuration set to use. See the Java Preferences API for
     * the definition of a preference node name.
     *
     * @return the name of the Java Preferences node where the agent's configuration lives
     */
    String getPreferencesNodeName();

    /**
     * Defines the preferences node name used to identify the configuration set to use. See the Java Preferences API for
     * the definition of a preference node name.
     *
     * <p>If this isn't specified, a suitable default will be used.</p>
     *
     * @param node the name of the Java Preferences node where the agent's configuration will or already lives
     */
    void setPreferencesNodeName(String node);

    /**
     * Returns a set of properties that will override the configuration preferences. If this returns <code>null</code>,
     * then the configuration preferences takes effect as-is.
     *
     * @return configuration setting overrides (may be <code>null</code>)
     */
    Properties getConfigurationOverrides();

    /**
     * This allows you to explicitly override configuration preferences found in the configuration file. If this isn't
     * set, then the settings specified by the configuration preferences file take effect as-is. These overrides can be
     * set in the bootstrap deployment file (and thus allow you to be able to use any app-server specific settings, like
     * <code>${jboss.server.data.dir}</code>, in the configuration preference values).
     *
     * @param overrides configuration settings that override the configuration preferences (may be<code>null</code>)
     */
    void setConfigurationOverrides(Properties overrides);

    /**
     * Returns the arguments that are passed to the agent's main startup method.
     *
     * @return agent arguments
     */
    String[] getAgentArguments();

    /**
     * Sets the arguments that will be passed to the agent's main startup method.
     *
     * @param args
     */
    void setAgentArguments(String[] args);

    /**
     * This will clear any and all current configuration preferences and then reload the
     * {@link #getConfigurationFile() configuration file}. The agent will need to be restarted for the configuration to
     * take effect.
     *
     * @throws Exception if failed to clear and reload the configuration
     */
    void reloadAgentConfiguration() throws Exception;

    /**
     * This will clean out the embedded agent's data directory. You usually invoke if you want to start the agent in a
     * clean state (i.e. use this in conjunction with {@link #reloadAgentConfiguration()}).
     */
    void cleanDataDirectory();

    /**
     * Returns the configuration preferencese the agent is or will be using.
     *
     * @return the server configuration, as a set of properties
     */
    Properties getAgentConfiguration();

    /**
     * Passes the given prompt command to the agent so it can be executed. Note that if the command requires additional
     * input, the embedded agent must have a valid input stream (System.in) or the command will fail. This means the JON
     * Server must be running in a console that can have keyboard input in order to execute those prompt commands that
     * ask for additional input from the user.
     *
     * @param command the agent prompt command to execute
     */
    void executeAgentPromptCommand(String command);

    /**
     * <code>true</code> indicates that the agent has been {@link #startAgent() started}. <code>false</code> means the
     * agent has not been started yet, or it was started but has since been {@link #stopAgent() stopped}.
     *
     * @return state of the agent
     */
    boolean isAgentStarted();

    /**
     * Starts the agent. Note that this will not start the agent if {@link #getAgentEnabled() the agent is not enabled}.
     *
     * @throws Exception if failed to start the agent successfully
     */
    void startAgent() throws Exception;

    /**
     * Stops the agent. Note that this will do nothing if {@link #getAgentEnabled() the agent is not enabled} or was
     * never {@link #startAgent() started}.
     *
     * @throws Exception if failed to stop the agent successfully
     */
    void stopAgent() throws Exception;

    /**
     * Stops this service - if the agent has been {@link #startAgent() started}, it will also be
     * {@link #stopAgent() stopped}.
     *
     * @throws Exception if failed to stop the agent
     */
    void stop() throws Exception;
}