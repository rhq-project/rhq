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
}
