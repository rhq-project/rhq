 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.pc.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Finds all JAR files located in a set of plugin directories. This does not load in any plugin jars nor does it parse
 * any plugin descriptors. This merely looks for jar files in its plugin directories and reports where it finds them.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public class FileSystemPluginFinder implements PluginFinder {
    private final Log log = LogFactory.getLog(FileSystemPluginFinder.class);

    private static final FilenameFilter JAR_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
        }
    };

    /**
     * Set of directories that will be searched when looking for plugins
     */
    private List<File> deploymentDirectories = new ArrayList<File>();

    /**
     * Cache of all plugins already loaded. Value is the last modified time.
     */
    private Map<File, Long> pluginModifiedTimes = new HashMap<File, Long>();

    /**
     * Creates a new {@link FileSystemPluginFinder} object.
     *
     * @param pluginDir an initial directory where this object will look for plugins
     */
    public FileSystemPluginFinder(File pluginDir) {
        deploymentDirectories.add(pluginDir);
    }

    /**
     * Creates a new {@link FileSystemPluginFinder} object.
     *
     * @param pluginDirs an initial set of directories where this object will look for plugins
     */
    public FileSystemPluginFinder(Collection<File> pluginDirs) {
        deploymentDirectories.addAll(pluginDirs);
    }

    /**
     * Searches all the plugin directories (as defined by the argument passed to this object's constructor) and will
     * load in all plugins it finds.
     *
     * @return a set of URLs that point to all the plugins that were loaded.
     */
    public Collection<URL> findPlugins() {
        Collection<URL> allPluginUrls = new HashSet<URL>();

        for (File deploymentDirectory : deploymentDirectories) {
            Collection<URL> dirPluginUrls = findPluginsInDirectory(deploymentDirectory);
            allPluginUrls.addAll(dirPluginUrls);
        }

        return allPluginUrls;
    }

    /**
     * Loads all plugins found in the given directory.
     *
     * @param  pluginDir
     *
     * @return URLs that point to all plugins found in the given directory
     */
    private Collection<URL> findPluginsInDirectory(File pluginDir) {
        Collection<URL> pluginUrls = new HashSet<URL>();

        File[] jars = pluginDir.listFiles(JAR_FILTER);

        if (jars == null) {
            return pluginUrls;
        }

        for (File jar : jars) {
            if (pluginModifiedTimes.containsKey(jar) && (jar.lastModified() == pluginModifiedTimes.get(jar))) {
                // Already loaded and has not been modified
                continue;
            }

            pluginModifiedTimes.put(jar, jar.lastModified());

            try {
                URL jarUrl = jar.toURL();
                pluginUrls.add(jarUrl);
            } catch (MalformedURLException e) {
                log.error("Could not get URL for plugin jar: " + jar);
            }
        }

        return pluginUrls;
    }
}