package org.rhq.enterprise.server.resource.metadata;

import java.io.File;
import java.util.List;

import javax.ejb.Local;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;

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

    /**
     * @return All plugins that have been marked deleted.
     */
    List<Plugin> findAllDeletedPlugins();


    /**
     * @return All plugins that are scheduled to be purged.
     */
    List<Plugin> findPluginsMarkedForPurge();

    /**
     * Returns a list of plugins with the specified ids. Both installed and deleted plugins will be included in the
     * results.
     *
     * @param pluginIds The ids of the plugins to fetch
     * @return A list of plugins with the specified ids
     */
    List<Plugin> getAllPluginsById(List<Integer> pluginIds);

    List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory);

    List<PluginStats> getPluginStats(List<Integer> pluginIds);

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
     * Schedules a plugin to be purged. Purging a plugin permanently deletes it from the database. Purging is done
     * asynchronously and will not happen until all resource types defined by the plugin have first been purged. Plugins
     * must first be deleted before they can be purged. A plugin is considered a candidate for being purged if its
     * status is set to <code>DELETED</code> and its <code>ctime</code> is set to {@link Plugin#PURGED}. This method
     * does not flip the status of the plugins to <code>DELETED</code> since it assumes that has already been done. It
     * only sets <code>ctime</code> to <code>PURGED</code>.
     *
     * @param subject The user purging the plugin
     * @param pluginIds The ids of the plugins to be purged
     * @throws Exception if an error occurs
     * @see  org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob PurgePluginsJob
     */
    void markPluginsForPurge(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * @param plugin The plugin to check
     * @return true if the plugin can be purged, false otherwise. A plugin can only be purged when all resource types
     * defined by the plugin have already been purged.
     */
    boolean isReadyForPurge(Plugin plugin);

    /**
     * Permanently deletes the plugins from the database. This method assumes that the plugins are already in the
     * deleted state. This method is not intended for general use. It is called from
     * {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob PurgePluginsJob}.
     * @param plugins The plugins to remove from the database.
     */
    void purgePlugins(List<Plugin> plugins);

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
