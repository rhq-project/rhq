package org.rhq.enterprise.server.resource.metadata;

import java.io.File;
import java.util.List;

import javax.ejb.Local;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.plugin.CannedGroupExpression;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;

@Local
public interface PluginManagerLocal extends PluginManagerRemote {

    /**
     * Given the plugin name, will return that plugin.  The name is defined in the plugin descriptor.
     *
     * @param  name of plugin as defined in plugin descriptor.
     *
     * @return the plugin, or null if the plugin does not exist.
     */
    Plugin getPlugin(String name);

    /**
     * @return A list of all plugins deployed in the server, including deleted plugins
     * @deprecated the deleted plugins will disappear from the database on their own accord when it's safe to do so.
     * @see #getInstalledPlugins() use <code>getInstalledPlugins</code> method instead
     */
    @Deprecated
    List<Plugin> getPlugins();

    /**
     * Returns the list of all plugins deployed in the server.
     *
     * @return list of plugins deployed
     */
    List<Plugin> getInstalledPlugins();

    /**
     * Do not use this method directly. It is a support method for
     * {@link org.rhq.enterprise.server.core.plugin.AgentPluginScanner} and
     * {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob}.
     *
     * @return All plugins that have been marked deleted.
     */
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

    List<PluginStats> getPluginStats(List<Integer> pluginIds);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Not to be called outside of the PluginManagerBean implementation. Used for transaction demarcation.
     */
    void markPluginsDeleted(Subject subject, List<Plugin> plugins) throws Exception;

    /**
     * Not to be used outside of {@link org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob}. You can just use
     * the {@link #deletePlugins(org.rhq.core.domain.auth.Subject, java.util.List)}  method and it will take care of
     * the rest.
     *
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
     */
    void registerPlugin(Plugin plugin, PluginDescriptor metadata, File pluginFile, boolean forceUpdate)
        throws Exception;

    /** Exists only to for transactional boundary reasons. Not for general consumption. */
    boolean registerPluginTypes(String newPluginName, PluginDescriptor pluginDescriptor, boolean newOrUpdated,
        boolean forceUpdate) throws Exception;

    /** Exists only for transactional boundary reasons. Not for general consumption. */
    boolean installPluginJar(Plugin newPlugin, PluginDescriptor pluginDescriptor, File pluginFile) throws Exception;

    /**
     * Returns the directory where plugins can be dropped for inclusion into the system.
     * 
     * @return directory where the plugin dropbox is located
     */
    File getPluginDropboxDirectory();

    /**
     * The provided server acknowledges the deletion of all plugins marked as deleted by calling this method.
     * Once all the servers in the HA cloud acknowledge the deletion of a  plugin and the plugin is made purgable
     * (after its resource types are deleted, etc) it will be automatically purged from the database.
     * <p/>
     * This method is not meant for "public" consumption and is only called from
     * {@link org.rhq.enterprise.server.core.plugin.AgentPluginScanner}.
     *
     * @param serverId the id of the server that wants to acknowledge that it has seen the deleted plugins
     */
    void acknowledgeDeletedPluginsBy(int serverId);

    List<CannedGroupExpression> getCannedGroupExpressions();
}
