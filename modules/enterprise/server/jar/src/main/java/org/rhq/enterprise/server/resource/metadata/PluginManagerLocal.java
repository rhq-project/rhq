package org.rhq.enterprise.server.resource.metadata;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;

import javax.ejb.Local;
import java.io.File;
import java.util.List;

@Local
public interface PluginManagerLocal {

    /**
     * Given the plugin name, will return that plugin.  The name is defined in the plugin descriptor.
     *
     * @param  name name of plugin as defined in plugin descriptor.
     *
     * @return the plugin
     *
     * @throws javax.persistence.NoResultException when no plugin with that name exists
     */
    Plugin getPlugin(String name);

    /**
     * @return A list of all plugins deployed in the server, including deleted plugins
     */
    List<Plugin> getPlugins();

    /**
     * Returns the list of all plugins deployed in the server.
     *
     * @return list of plugins deployed
     */
    List<Plugin> getInstalledPlugins();

    List<Plugin> findAllDeletedPlugins();

    /**
     * Returns a list of plugins with the specified ids. Both installed and deleted plugins will be included in the
     * results.
     *
     * @param pluginIds The ids of the plugins to fetch
     * @return A list of plugins with the specified ids
     */
    List<Plugin> getAllPluginsById(List<Integer> pluginIds);

    List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * This method puts the plugin into a <i>deleted</i> state and removes the plugin JAR file from the file system. It
     * does not remove the plugin from the database. This method does not purge the plugin from the database in order
     * to support HA deployments. In a HA deployment, if server A handles the request to delete the plugin and if it
     * purges the plugin from the database, server B might see the plugin on the file system and not in the database.
     * Server B would then proceed to try and re-install the plugin, not knowing it was deleted.
     *
     * @param subject The user performing the deletion
     * @param pluginIds The ids of the plugins to be deleted
     * @throws Exception if an error occurs
     */
    void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Permanently removes the plugins with the specified ids from the database. This operation first calls
     * {@link #deletePlugins(Subject, List)} as a safeguard to ensure that the plugins are first deleted. In a HA
     * deployment however, you should wait at least 5 minutes after deleting a plugin before purging it. Five minutes
     * is the default interval of the agent plugin scanner. This should be sufficient time for servers in the cluster
     * to delete the plugin from the file system.
     *
     * @param subject The user purging the plugin
     * @param pluginIds The ids of the plugins to be purged
     * @throws Exception if an error occurs
     */
    void purgePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception;

    /**
     * For server-side registration of plugin archives. At server startup or as new plugins are runtime deployed the jar
     * will have its descriptor read and parsed and the metadata for the plugin will be updated in the db.
     * If you provide a non-null <code>pluginFile</code>, and the plugin is deemed to be new or updated, the content
     * of the file will be streamed to the database. Note that if you provide a non-null file, you must ensure
     * its MD5 matches that of the file (i.e. this method will not attempt to recompute the file's MD5, it will assume
     * the caller has already done that and provided the proper MD5 in <code>plugin</code>).
     * <br/><br/>
     * NOTE ** This call will register the plugin in a new transaction.
     *
     * @param plugin   The plugin object being deployed
     * @param metadata The plugin descriptor file
     * @param pluginFile the actual plugin file whose content will be stored in the database (will be ignored if null)
     * @param forceUpdate if <code>true</code>, the plugin's types will be updated, even if the plugin hasn't changed since
     *                    the last time it was registered
     */
    void registerPlugin(Subject subject, Plugin plugin, PluginDescriptor metadata, File pluginFile, boolean forceUpdate)
        throws Exception;

    /** Exists only to for transactional boundary reasons. Not for general consumption. */
    boolean registerPluginTypes(Subject subject, Plugin plugin, PluginDescriptor pluginDescriptor, File pluginFile,
        boolean forceUpdate) throws Exception;
}
