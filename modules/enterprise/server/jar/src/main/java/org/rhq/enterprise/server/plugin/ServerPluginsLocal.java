package org.rhq.enterprise.server.plugin;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
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
public interface ServerPluginsLocal {
    /**
     * Recycles the master plugin container, essentially shutting down all server plugins
     * and then restarting them.
     * 
     * @param subject the user asking to restart the master plugin container 
     */
    void restartMasterPluginContainer(Subject subject);

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
     */
    List<ServerPlugin> getAllServerPlugins();

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
     * Enables the plugins and restarts them.
     *
     * @param subject user making the request
     * @param pluginIds the plugins to be enabled
     * @return the list of keys of the plugins that were enabled
     * @throws Exception if failed to disable a plugin
     */
    List<PluginKey> enableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Disables the plugins and unschedules their jobs.
     *
     * @param subject user making the request
     * @param pluginIds the plugins to be disabled
     * @return the list of keys of the plugins that were disabled
     * @throws Exception if failed to disable a plugin
     */
    List<PluginKey> disableServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Removes the plugins from the system and unschedules their jobs.
     *
     * @param subject user making the request
     * @param pluginIds the plugins to be undeployed
     * @return the list of keys of plugins that were undeployed
     * @throws Exception if failed to undeploy a plugin
     */
    List<PluginKey> undeployServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Purges the undeployed plugins from the system so there is no record of them to have
     * ever existed. This deletes all remnants of the plugin from the database.
     *
     * @param subject user making the request
     * @param pluginIds the plugins to be purged
     * @return the list of keys of plugins that were purged
     * @throws Exception if failed to purge a plugin
     */
    List<PluginKey> purgeServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Turns on or off the enabled flag in the database but does NOT restart the server plugin container.
     * This has "requires new" semantics, so the results are committed immediately upon return.
     * 
     * @param subject user making the request
     * @param pluginIds the plugins to be enabled
     * @param enabled the value of the enabled flag for the plugins
     * @throws if failed to update a plugin
     */
    void setServerPluginEnabledFlag(Subject subject, List<Integer> pluginIds, boolean enabled) throws Exception;

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
     * Registers the given plugin to the database. This does nothing with the master plugin container,
     * all it does is ensure the database is up-to-date with this new plugin.
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
     * This is really a supporting method for {@link #reRegisterServerPlugin(Subject, ServerPlugin, File)} - you'll
     * probably want to use that instead. Do not blindly purge server plugins using this method unless you
     * know what you are doing.
     * 
     * @param subject user making the request
     * @param pluginKey the key of the server plugin to delete
     */
    void purgeServerPlugin(Subject subject, PluginKey pluginKey);

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
}
