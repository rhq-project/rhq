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
package org.rhq.enterprise.server.plugin.content;

import java.io.File;

/**
 * A very simple object used to contain content source server plugin container configuration.
 *
 * @author John Mazzitelli
 */
public class ContentSourcePluginContainerConfiguration {
    private File pluginDirectory;
    private File tmpDirectory;

    public File getPluginDirectory() {
        return pluginDirectory;
    }

    public void setPluginDirectory(File pluginDirectory) {
        if (pluginDirectory == null) {
            throw new IllegalArgumentException("pluginDirectory == null");
        }

        this.pluginDirectory = pluginDirectory;
    }

    public File getTemporaryDirectory() {
        if (tmpDirectory != null) {
            return tmpDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir", "."));
    }

    public void setTemporaryDirectory(File tmpDir) {
        this.tmpDirectory = tmpDir;
    }

    @Override
    public String toString() {
        File pdir = getPluginDirectory();
        File tdir = getTemporaryDirectory();

        StringBuilder str = new StringBuilder("ContentSourcePluginContainerConfiguration: ");
        str.append("plugin-dir=[" + ((pdir != null) ? pdir.getAbsolutePath() : "<null>"));
        str.append("], tmp-dir=[" + ((tdir != null) ? tdir.getAbsolutePath() : "<null>"));
        str.append("]");

        return str.toString();
    }
}