package org.rhq.core.pluginapi.plugin;

import java.io.File;

import org.rhq.core.system.SystemInfo;

/**
 * A global context containing information about a plugin.
 * 
 * @author John Mazzitelli
 */
public class PluginContext {
    private final String pluginName;
    private final SystemInfo systemInformation;
    private final File temporaryDirectory;
    private final File dataDirectory;
    private final String pluginContainerName;

    /**
     * Creates a new {@link PluginContext} object. The plugin container is responsible for instantiating these
     * objects; plugin writers should never have to actually create context objects.
     *
     * @param pluginName                 the name of the plugin that corresponds to this context
     * @param systemInfo                 information about the system on which the plugin and its plugin container are
     *                                   running
     * @param temporaryDirectory         a temporary directory for plugin use that is destroyed at agent shutdown
     * @param dataDirectory              a directory where plugins can store persisted data that survives agent restarts
     * @param pluginContainerName        the name of the plugin container in which the plugin is running.
     *                                   You can be assured this name is unique across <b>all</b> plugin
     *                                   containers/agents running in the RHQ environment.
     */
    public PluginContext(String pluginName, SystemInfo systemInfo, File temporaryDirectory, File dataDirectory,
        String pluginContainerName) {
        this.pluginName = pluginName;
        this.systemInformation = systemInfo;
        this.dataDirectory = dataDirectory;
        this.pluginContainerName = pluginContainerName;
        if (temporaryDirectory == null) {
            this.temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "AGENT_TMP");
        } else {
            this.temporaryDirectory = temporaryDirectory;
        }
    }

    /**
     * The name of the plugin which corresponds to this context object.
     * 
     * @return plugin name as it is specified in the plugin descriptor
     */
    public String getPluginName() {
        return pluginName;
    }

    /**
     * Returns a {@link SystemInfo} object that contains information about the platform/operating system that the
     * plugin is running on. With this object, you can natively obtain things such as the operating system name, its
     * hostname,and other things. Please refer to the javadoc on {@link SystemInfo} for more details on the types of
     * information you can access.
     *
     * @return system information object
     */
    public SystemInfo getSystemInformation() {
        return this.systemInformation;
    }

    /**
     * A temporary directory for plugin use that is destroyed at agent shutdown. Plugins should use this if they need to
     * write temporary files that they do not expect to remain after the agent is restarted. This directory is shared
     * among all plugins - plugins must ensure they write unique files here, as other plugins may be using this same
     * directory. Typically, plugins will use the {@link File#createTempFile(String, String, File)} API when writing to
     * this directory.
     *
     * @return location for plugin temporary files
     */
    public File getTemporaryDirectory() {
        return temporaryDirectory;
    }

    /**
     * Directory where plugins can store persisted data that survives agent restarts. Each plugin will have their own
     * data directory. The returned directory may not yet exist - it is up to each individual plugin to manage this
     * directory as they see fit (this includes performing the initial creation when the directory is first needed).
     *
     * @return location for plugins to store persisted data
     */
    public File getDataDirectory() {
        return this.dataDirectory;
    }

    /**
     * The name of the plugin container in which the plugin is running. You
     * can be assured this name is unique across <b>all</b> plugin containers/agents running
     * in the RHQ environment.
     * 
     * @return the name of the plugin container
     */
    public String getPluginContainerName() {
        return this.pluginContainerName;
    }
}
