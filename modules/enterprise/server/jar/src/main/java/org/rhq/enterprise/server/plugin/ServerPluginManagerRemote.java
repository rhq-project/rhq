package org.rhq.enterprise.server.plugin;

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.enterprise.server.plugin.pc.ControlResults;

/**
 * Remote management interface for server plugins.
 */
@Remote
public interface ServerPluginManagerRemote {

    /**
     * Recycles the master plugin container, essentially shutting down all server plugins
     * and then restarting them.
     *
     * @param subject the user asking to restart the master plugin container
     */
    void restartMasterPluginContainer(Subject subject);

    /**
     * Returns a list of all the installed server plugins in the database, minus
     * the content object.
     *
     * @return all installed server plugins found in the DB
     */
    List<ServerPlugin> getServerPlugins(Subject subject);

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
    List<PluginKey> deleteServerPlugins(Subject subject, List<Integer> pluginIds) throws Exception;

    /**
     * Invokes a control operation on a given plugin and returns the results. This method blocks until
     * the plugin component completes the invocation.
     *
     * @param subject user making the request, must have the proper permissions
     * @param pluginKey identifies the plugin whose control operation is to be invoked
     * @param controlName identifies the name of the control operation to invoke
     * @param params parameters to pass to the control operation; may be <code>null</code>
     * @return the results of the invocation
     *
     * @throws if failed to obtain the plugin component and invoke the control. This usually means an
     *         abnormal error occurred - if the control operation merely failed to do what it needed to do,
     *         the error will be reported in the returned results, not as a thrown exception.
     */
    ControlResults invokeServerPluginControl(Subject subject, PluginKey pluginKey, String controlName,
        Configuration params) throws Exception;

}
