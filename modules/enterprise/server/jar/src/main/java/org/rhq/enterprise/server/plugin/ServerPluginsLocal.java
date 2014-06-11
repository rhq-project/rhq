/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.server.plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.xmlschema.ControlDefinition;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Interface to the methods that interact with the serve-side plugin infrastructure.
 * Most of these methods will only return information on {@link PluginStatusType#INSTALLED}
 * plugins; only when explicitly stated will a method return data on
 * {@link PluginStatusType#DELETED} plugins, too.
 *
 * @author John Mazzitelli
 */
@Local
public interface ServerPluginsLocal extends ServerPluginsRemote {

    /**
     * Returns a list of all the installed server plugins in the database
     *
     * @return all installed server plugins found in the DB
     */
    List<ServerPlugin> getServerPlugins();

    /**
     * Returns a list of all the installed and deleted server plugins in the database.
     * When a plugin is "undeployed", it still exists in the database, but is flagged
     * as "deleted". This method returns those deleted plugins in addition to those plugins
     * that are still installed.
     *
     * @return all installed and deleted server plugins found in the DB
     * @deprecated do not use this as the deleted plugins are essentially ephemeral and will be removed from the
     *             database in due time automatically.
     */
    @Deprecated
    List<ServerPlugin> getAllServerPlugins();

    /**
     * Returns the plugins that have been marked as deleted.
     * This is more or less a helper method to {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob}
     * which goes ahead and removes such plugins from database once it's safe to do so.
     */
    List<ServerPlugin> getDeletedPlugins();

    /**
     * Returns a plugin with the given key.
     * @param key identifies the plugin to find
     * @return the named plugin
     * @throws NoResultException when no plugin with that name exists
     */
    ServerPlugin getServerPlugin(PluginKey key);

    /**
     * Methods in this object that return plugins normally do not include
     * the data from relationships with the plugin (for example, the
     * plugin configuration and scheduled jobs related to the plugin).
     *
     * Call this method to fill in that data that wasn't originally loaded.
     *
     * @param plugin
     * @return the same plugin, with the relationship data loaded
     * @throws NoResultException when no plugin with that name exists
     */
    ServerPlugin getServerPluginRelationships(ServerPlugin plugin);

    /**
     * Get a list of plugins from their IDs.
     *
     * @param pluginIds the IDs of the plugins to load.
     * @return plugins matching the given IDs
     */
    List<ServerPlugin> getServerPluginsById(List<Integer> pluginIds);

    /**
     * Get a list of both installed and deleted plugins from their IDs.
     *
     * @param pluginIds the IDs of the plugins to load.
     * @return plugins matching the given IDs
     */
    List<ServerPlugin> getAllServerPluginsById(List<Integer> pluginIds);

    /**
     * Given a plugin ID, this will return a timestamp (in epoch millis)
     * that indicates the last time when the plugin's configuration changed.
     * This looks at both plugin configuration and schedule job configuration.
     *
     * @param pluginId
     * @return time when the plugin's configuration was last updated; will be 0
     *         if the plugin has no configuration to change.
     */
    long getLastConfigurationChangeTimestamp(int pluginId);

    /**
     * Given a plugin key, returns the descriptor for that plugin.
     *
     * @param pluginKey
     * @return descriptor parsed from the file in the plugin jar
     * @throws Exception if the descriptor could not be retrieved or parsed for the given plugin
     */
    ServerPluginDescriptorType getServerPluginDescriptor(PluginKey pluginKey) throws Exception;

    /**
     * Returns a list of plugin keys for only those server plugins whose
     * enabled flag is equal to the given parameter.
     *
     * @param enabled if <code>true</code>, return only the keys of plugins that are enabled;
     *                if <code>false</code>, return only the keys of plugins that are disabled.
     * @return list of plugin keys that match the enabled criteria
     */
    List<PluginKey> getServerPluginKeysByEnabled(boolean enabled);

    /**
     * Turns on or off the enabled flag in the database but does NOT restart the server plugin.
     * This has "requires new" semantics, so the results are committed immediately upon return.
     *
     * @param subject user making the request
     * @param pluginId the plugin to be enabled
     * @param enabled the value of the enabled flag for the plugin
     * @throws if failed to update the plugin
     */
    void setServerPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception;

    /**
     * Sets the status flag in the database but does NOT restart the server plugin container.
     * If the status is {@link PluginStatusType#DELETED}, the enabled flag is also flipped to <code>false</code>.
     *
     * This has "requires new" semantics, so the results are committed immediately upon return.
     *
     * @param subject user making the request
     * @param pluginIds the plugins to be enabled
     * @param enabled the value of the enabled flag for the plugins
     * @throws if failed to update one of the plugins
     */
    void setServerPluginStatus(Subject subject, List<Integer> pluginIds, PluginStatusType status) throws Exception;

    /**
     * Registers the given plugin to the database. This ensures the database is up-to-date with the
     * new plugin details.
     *
     * If the master plugin container is up, it will attempt to restart the plugin so the new
     * changes are picked up.
     *
     * @param subject the user that needs to have permissions to add a plugin to the system
     * @param plugin the plugin definition
     * @param pluginFile the actual plugin file itself
     * @return the plugin after being persisted
     * @throws Exception if failed to fully register the plugin
     */
    ServerPlugin registerServerPlugin(Subject subject, ServerPlugin plugin, File pluginFile) throws Exception;

    /**
     * Given a plugin that already exists, this will update that plugin's data in the database,
     * except for the content, which is left as-is. If the plugin did not yet exist, an exception is thrown.
     * You can use this to update the plugin's scheduled jobs configuration and plugin configuration.
     *
     * @param subject user making the request
     * @param plugin existing plugin with updated data
     * @return the updated plugin
     * @throws Exception if the plugin did not already exist or an error occurred that caused the update to fail
     */
    ServerPlugin updateServerPluginExceptContent(Subject subject, ServerPlugin plugin) throws Exception;

    /**
     * Purges the server plugin from the database. This ensures that, after this method returns,
     * the given plugin will be unknown. The plugin can be installed again later.
     *
     * This has "requires new" semantics, so the results are committed immediately upon return.
     * <p/>
     * Do not invoke this method directly. It is meant as a support for
     * {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob}.
     *
     * @param pluginId the id of the server plugin to delete
     */
    void purgeServerPlugin(int pluginId);

    /**
     * Given the key of a server plugin, this will return the status of that plugin.
     * Use this to determine if a plugin has been deleted or not.
     *
     * @param pluginKey the key of the plugin whose status is to be returned.
     * @return the status of the plugin, to indicate if the plugin has been deleted or is installed.
     *         <code>null</code> indicates an unknown plugin.
     */
    PluginStatusType getServerPluginStatus(PluginKey pluginKey);

    /**
     * This will return a map containing all installed plugins that are both enabled and disabled.
     *
     * @return keys of all enabled and disabled plugins, keyed on their types
     */
    Map<ServerPluginType, List<PluginKey>> getInstalledServerPluginsGroupedByType();

    /**
     * Returns the definition for the given plugin's global plugin configuration.
     *
     * @param pluginKey
     * @return the plugin configuration definition
     * @throws Exception
     */
    ConfigurationDefinition getServerPluginConfigurationDefinition(PluginKey pluginKey) throws Exception;

    /**
     * Returns the definition for the given plugin's scheduled jobs configuration.
     *
     * @param pluginKey
     * @return the scheduled jobs definition
     * @throws Exception
     */
    ConfigurationDefinition getServerPluginScheduledJobsDefinition(PluginKey pluginKey) throws Exception;

    /**
     * Returns the metadata for all control operations for the given plugin.
     * If there are no control operations, an empty list is returned.
     *
     * @param pluginKey
     * @return list of control definitions that are defined for the given plugin
     * @throws Exception if failed to determine a plugin's control definitions
     */
    List<ControlDefinition> getServerPluginControlDefinitions(PluginKey pluginKey) throws Exception;

    /**
     * Return all the server plugins that match a certain type string.
     * This type is defined in the XML schema of the plugin descriptor.
     * Example for alert sender plugins:
     * "org.rhq.enterprise.server.xmlschema.generated.serverplugin.alert.AlertPluginDescriptorType"
     * from rhq-serverplugin-alert.xsd.
     * @param type Name of the type
     * @return List of server plugins matching that type.
     */
    List<ServerPlugin> getEnabledServerPluginsByType(String type);

    /**
     * A helper method for {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob}.
     * Checks whether given server plugin is safe for purging from the database.
     */
    boolean isReadyForPurge(int pluginId);

    /**
     * The provided server acknowledges the deletion of all plugins marked as deleted by calling this method.
     * Once all the servers in the HA cloud acknowledge the deletion of a  plugin and the plugin is made purgable
     * (after its resource types are deleted, etc) it will be automatically purged from the database.
     * <p/>
     * This method is not meant for "public" consumption and is only called from
     * {@link org.rhq.enterprise.server.core.plugin.ServerPluginScanner}
     *
     * @param serverId the id of the server that wants to acknowledge that it has seen the deleted plugins
     */
    void acknowledgeDeletedPluginsBy(int serverId);

}
