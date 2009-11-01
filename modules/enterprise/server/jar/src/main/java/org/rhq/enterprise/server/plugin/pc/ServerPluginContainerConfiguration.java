/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * A very simple object used to contain server plugin container configuration.
 *
 * @author John Mazzitelli
 */
public class ServerPluginContainerConfiguration {
    private File pluginDirectory;
    private File tmpDirectory;
    private String rootClassLoaderRegex;

    public File getPluginDirectory() {
        return this.pluginDirectory;
    }

    public void setPluginDirectory(File pluginDirectory) {
        if (pluginDirectory == null) {
            throw new IllegalArgumentException("pluginDirectory == null");
        }

        this.pluginDirectory = pluginDirectory;
    }

    public File getTemporaryDirectory() {
        if (this.tmpDirectory != null) {
            return this.tmpDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir", "."));
    }

    public void setTemporaryDirectory(File tmpDir) {
        this.tmpDirectory = tmpDir;
    }

    /**
     * Returns the regex that defines what classes the plugin container can provide to its
     * plugins from its own classloader and its parents. If not <code>null</code>, any classes
     * found in the plugin container's classloader (and its parent classloaders) that do
     * NOT match this regex will be hidden from the plugins. If <code>null</code>, there
     * are no hidden classes and any class the plugin container's classloader has is visible
     * to all plugins.
     *
     * @return regular expression (may be <code>null</code>)
     * 
     * @see RootServerPluginClassLoader
     */
    public String getRootServerPluginClassLoaderRegex() {
        return this.rootClassLoaderRegex;
    }

    /**
     * Sets the regex that defines what classes the plugin container should hide from its plugins.
     * 
     * @param regex regular expression
     *
     * @see RootServerPluginClassLoader
     */
    public void setRootPluginClassLoaderRegex(String regex) {
        this.rootClassLoaderRegex = regex;
    }

    @Override
    public String toString() {
        File pdir = getPluginDirectory();
        File tdir = getTemporaryDirectory();
        String regex = getRootServerPluginClassLoaderRegex();

        StringBuilder str = new StringBuilder("ServerPluginContainerConfiguration: ");
        str.append("plugin-dir=[" + ((pdir != null) ? pdir.getAbsolutePath() : "<null>"));
        str.append("], tmp-dir=[" + ((tdir != null) ? tdir.getAbsolutePath() : "<null>"));
        str.append("], root-cl-regex=[" + ((regex != null) ? regex : "<null>"));
        str.append("]");

        return str.toString();
    }
}