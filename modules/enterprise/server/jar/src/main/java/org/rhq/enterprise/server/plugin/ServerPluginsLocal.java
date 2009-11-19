package org.rhq.enterprise.server.plugin;

import java.util.List;

import javax.ejb.Local;
import javax.persistence.NoResultException;

import org.rhq.core.domain.plugin.Plugin;

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

    List<Plugin> getServerPluginsById(List<Integer> pluginIds);

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
     */
    void disableServerPlugins(List<Integer> pluginIds);

    /**
     * Removes the plugin from the system and restarts the server plugin container.
     *
     * @param pluginIds
     */
    void undeployServerPlugins(List<Integer> pluginIds);

    /**
     * Turns on or off the enabled flag in the database but does NOT restart the server plugin container.
     *
     * @param pluginIds the plugins to be enabled
     * @param enabled the value of the enabled flag for the plugins
     */
    void setPluginEnabledFlag(List<Integer> pluginIds, boolean enabled);
}
