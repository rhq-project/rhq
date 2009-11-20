package org.rhq.enterprise.server.plugin;

import java.io.File;
import java.util.List;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Interface to the methods that interact with the serve-side plugin infrastructure.
 * 
 * @author John Mazzitelli
 */
@Local
public interface ServerPluginsLocal {
    /**
     * Returns a list of all the server plugins in the database.
     * 
     * @return all plugins found in the DB
     */
    List<Plugin> getServerPlugins();

    /**
     * Returns a plugin with the given name.
     * @param name name of plugin to find
     * @return the named plugin
     * @throws NoResultException when no plugin with that name exists
     */
    Plugin getServerPlugin(String name);

    /**
     * Methods in this object that return plugins normally do not include
     * the data from relationships with the plugin (for example, the
     * plugin configuration and scheduled jobs related to the plugin).
     * 
     * Call this method to fill in that data that wasn't originally loaded.
     * 
     * @param plugin
     *
     * @return the same plugin, with the relationship data loaded
     */
    Plugin getServerPluginRelationships(Plugin plugin);

    /**
     * Get a list of plugins from their IDs.
     * 
     * @param pluginIds the IDs of the plugins to load.
     * 
     * @return plugins matching the given IDs
     */
    List<Plugin> getServerPluginsById(List<Integer> pluginIds);

    /**
     * Given a plugin name, returns the descriptor for that plugin.
     * 
     * @param pluginName
     * @return descriptor parsed from the file in the plugin jar
     * @throws Exception if the descriptor could not be retrieved or parsed for the given plugin
     */
    ServerPluginDescriptorType getServerPluginDescriptor(String pluginName) throws Exception;

    /**
     * Returns a list of plugin names for only those server plugins whose
     * enabled flag is equal to the given parameter.
     * 
     * @param enabled if <code>true</code>, return only the names of plugins that are enabled;
     *                if <code>false</code>, return only the names of plugins that are disabled.
     * @return list of plugin names that match the enabled criteria
     */
    List<String> getPluginNamesByEnabled(boolean enabled);

    /**
     * Enables the plugins and restarts the server plugin container.
     *
     * @param pluginIds the plugins to be enabled
     */
    void enableServerPlugins(List<Integer> pluginIds);

    /**
     * Disables the plugins and restarts the server plugin container.
     *
     * @param pluginIds the plugins to be disabled
     * 
     * @return the list of plugins that were disabled
     */
    List<Plugin> disableServerPlugins(List<Integer> pluginIds);

    /**
     * Removes the plugin from the system and restarts the server plugin container.
     *
     * @param pluginIds
     * 
     * @return the list of plugins that were undeployed
     */
    List<Plugin> undeployServerPlugins(List<Integer> pluginIds);

    /**
     * Turns on or off the enabled flag in the database but does NOT restart the server plugin container.
     *
     * @param pluginIds the plugins to be enabled
     * @param enabled the value of the enabled flag for the plugins
     */
    void setPluginEnabledFlag(List<Integer> pluginIds, boolean enabled);

    /**
     * Registers the given plugin to the database.
     * 
     * @param subject the user that needs to have permissions to add a plugin to the system
     * @param plugin the plugin definition
     * @param descriptor the plugin descriptor that was found in the plugin file
     * @param pluginFile the actual plugin file itself
     * @throws Exception if failed to fully register the plugin 
     */
    void registerPlugin(Subject subject, Plugin plugin, ServerPluginDescriptorType descriptor, File pluginFile)
        throws Exception;

    /**
     * Given a plugin that already exists, this will update that plugin's data in the database,
     * except for the content, which is left as-is. If the plugin did not yet exist, an exception is thrown.
     * You can use this to update the plugin's scheduled jobs configuration and plugin configuration.
     *
     * @param plugin existing plugin with updated data
     * @return the updated plugin
     * @throws Exception if the plugin did not already exist or an error occurred that caused the update to fail
     */
    public Plugin updatePluginExceptContent(Plugin plugin) throws Exception;
}
