/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.gwt;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.plugin.ServerPluginControlDefinition;
import org.rhq.core.domain.plugin.ServerPluginControlResults;

/**
 * Functionality to manage both agent and server plugins.
 *
 * @author John Mazzitelli
 */
public interface PluginGWTService extends RemoteService {
    /**
     * Recycles the master plugin container, essentially shutting down all server plugins
     * and then restarting them.
     */
    void restartMasterPluginContainer() throws RuntimeException;

    /**
     * Given an agent plugin ID, this will return that plugin.
     *
     * @param pluginId identifies a known agent plugin
     * @return the agent plugin or null if the given ID does not identify an agent plugin
     */
    Plugin getAgentPlugin(int pluginId) throws RuntimeException;

    /**
     * Given a server plugin ID, this will return that plugin.
     * Server plugins have additional data that you can optionally request. This related data
     * includes things such as scheduled jobs and plugin configuration. If <code>includeRelationships</code>
     * is <code>true</code>, that additional data will be loaded in the returned plugin object; otherwise
     * that data will be <code>null</code> in the returned object.
     *
     * @param pluginId identifies a known server plugin
     * @param includeRelationships include additional data (such as scheduled jobs and plugin config)
     * @return the server plugin or null if the given ID does not identify a server plugin
     */
    ServerPlugin getServerPlugin(int pluginId, boolean includeRelationships) throws RuntimeException;

    /**
     * Returns the list of all <em>agent</em> plugins.
     *
     * @param includeDeletedPlugins if true, even those plugins that have been deleted (but not purged)
     * @return list of all agent plugins
     */
    ArrayList<Plugin> getAgentPlugins(boolean includeDeletedPlugins) throws RuntimeException;

    /**
     * Returns the list of all <em>server</em> plugins.
     *
     * @param includeDeletedPlugins When a plugin is "undeployed", it still exists in the database, but is flagged as "deleted".
     *                              If this is true, this method returns those deleted plugins in addition to those plugins
     *                              that are still installed.
     * @return list of all server plugins
     */
    ArrayList<ServerPlugin> getServerPlugins(boolean includeDeletedPlugins) throws RuntimeException;

    /**
     * This will ask the server to scan for updates to its plugins and register the changes it finds.
     */
    void scanAndRegister() throws RuntimeException;

    /**
     * Enables the agent plugins with the given IDs.
     *
     * @param selectedPluginIds the IDs of the plugins that are to be enabled
     * @return list of names of those plugins that were enabled
     */
    ArrayList<String> enableAgentPlugins(int[] selectedPluginIds) throws RuntimeException;

    /**
     * Disables the agent plugins with the given IDs.
     *
     * @param selectedPluginIds the IDs of the plugins that are to be disabled
     * @return list of names of those plugins that were disabled
     */
    ArrayList<String> disableAgentPlugins(int[] selectedPluginIds) throws RuntimeException;

    /**
     * Deletes the agent plugins with the given IDs.
     * This method puts the plugins with the given IDs into a <i>deleted</i> state and removes the plugin JAR file
     * from the file system but the plugin is not removed from the database (this is to support HA deployments).
     *
     * @param selectedPluginIds the IDs of the plugins that are to be deleted
     * @return list of names of those plugins that were deleted
     */
    ArrayList<String> deleteAgentPlugins(int[] selectedPluginIds) throws RuntimeException;

    /**
     * Enables the server plugins with the given IDs.
     *
     * @param selectedPluginIds the IDs of the plugins that are to be enabled
     * @return list of names of those plugins that were enabled
     */
    ArrayList<String> enableServerPlugins(int[] selectedPluginIds) throws RuntimeException;

    /**
     * Disables the server plugins with the given IDs.
     *
     * @param selectedPluginIds the IDs of the plugins that are to be disabled
     * @return list of names of those plugins that were disabled
     */
    ArrayList<String> disableServerPlugins(int[] selectedPluginIds) throws RuntimeException;

    /**
     * Removes the server plugins from the system and unschedules their jobs.
     *
     * @param selectedPluginIds the IDs of the server plugins that are to be undeployed
     * @return list of names of those server plugins that were undeployed
     */
    ArrayList<String> deleteServerPlugins(int[] selectedPluginIds) throws RuntimeException;


    /**
     * Returns the definition for the given plugin's global plugin configuration.
     *
     * @param pluginKey
     * @return the plugin configuration definition
     */
    ConfigurationDefinition getServerPluginConfigurationDefinition(PluginKey pluginKey) throws RuntimeException;

    /**
     * Returns the definition for the given plugin's scheduled jobs configuration.
     *
     * @param pluginKey
     * @return the scheduled jobs definition
     */
    ConfigurationDefinition getServerPluginScheduledJobsDefinition(PluginKey pluginKey) throws RuntimeException;

    /**
     * Obtains the available controls for a given server plugin. If a server plugin does not have any
     * controls, an empty list will be returned.
     *
     * @param serverPluginKey identifies the server plugin
     * @return information on all available controls for the given server plugin
     * @throws RuntimeException
     */
    ArrayList<ServerPluginControlDefinition> getServerPluginControlDefinitions(PluginKey serverPluginKey)
        throws RuntimeException;

    /**
     * Invokes a control on a server plugin. A control is simply a runtime operation performed
     * by a server plugin.
     *
     * @param serverPluginKey identifies the server plugin that is to execute the control.
     * @param controlName the name of the control to execute
     * @param params the parameters to pass to the control, if a control accepts parameters.
     * @return the results of the operation; whether success or failure, this will be non-null.
     * @throws RuntimeException if an abnormal failure occurred, such as the plugin does not support the named control
     *                          of the plugin key does not identify a server plugin. If the control executed but
     *                          it failed, no exception will be thrown and the results of the failed execution
     *                          will be returned to the caller
     */
    ServerPluginControlResults invokeServerPluginControl(PluginKey serverPluginKey, String controlName,
        Configuration params) throws RuntimeException;

    /**
     * Updates the server plugin configuration with the given config.
     *
     * @param serverPluginKey identifies the plugin to update
     * @param config the new config
     */
    void updateServerPluginConfiguration(PluginKey serverPluginKey, Configuration config) throws RuntimeException;

    /**
     * Updates the server plugin scheduled jobs configuration with the given config.
     *
     * @param serverPluginKey identifies the plugin to update
     * @param jobsConfig the new config containing the scheduled jobs configuration
     */
    void updateServerPluginScheduledJobs(PluginKey serverPluginKey, Configuration jobsConfig) throws RuntimeException;

    /**
     * Updates the plugins on all agents after given delay
     * @param delayInMilliseconds the delay in milliseconds.
     *
     * @since 4.11
     */
    void updatePluginsOnAgents(long delayInMilliseconds);

    /**
     * returns canned group expressions
     * @return
     */
    ArrayList<CannedGroupExpression> getCannedGroupExpressions();
}
