/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.plugin.pc;

import java.io.File;

/**
 * A global context containing information about a server-side plugin.
 * 
 * @author John Mazzitelli
 */
public class ServerPluginContext {
    private final ServerPluginEnvironment pluginEnvironment;
    private final File temporaryDirectory;
    private final File dataDirectory;

    /**
     * Creates a new {@link ServerPluginContext} object. The plugin container is responsible for instantiating these
     * objects; plugin writers should never have to actually create context objects.
     *
     * @param env           the environment of the plugin - includes the plugin name and other info
     * @param dataDirectory a directory where plugins can store persisted data that survives server restarts
     * @param tmpDirectory  a temporary directory for plugin use
     */
    public ServerPluginContext(ServerPluginEnvironment env, File dataDirectory, File tmpDirectory) {
        this.pluginEnvironment = env;
        this.dataDirectory = dataDirectory;
        if (tmpDirectory == null) {
            this.temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "SERVERPLUGIN_TMP");
            this.temporaryDirectory.mkdirs();
        } else {
            this.temporaryDirectory = tmpDirectory;
        }
    }

    /**
     * The environment of the plugin, including its name and other information.
     * 
     * @return plugin environment
     */
    public ServerPluginEnvironment getPluginEnvironment() {
        return pluginEnvironment;
    }

    /**
     * A temporary directory for plugin use. Plugins should use this if they need to
     * write temporary files that they do not expect to remain after the server is restarted. This directory is shared
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
}
