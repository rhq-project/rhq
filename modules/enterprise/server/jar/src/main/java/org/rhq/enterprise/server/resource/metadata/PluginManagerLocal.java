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
     * Returns the list of all plugins deployed in the server.
     *
     * @return list of plugins deployed
     */
    List<Plugin> getPlugins();

    List<Plugin> findAllDeletedPlugins();

    List<Plugin> getAllPluginsById(List<Integer> pluginIds);

    List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName, ResourceCategory resourceCategory);

    void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception;

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
