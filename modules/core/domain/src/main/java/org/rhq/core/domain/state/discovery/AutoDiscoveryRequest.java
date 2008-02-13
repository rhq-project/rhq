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
package org.rhq.core.domain.state.discovery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class AutoDiscoveryRequest implements Serializable {
    private Properties properties;

    private List<AutoDiscoveryScanType> scanTypes;

    private List<String> serverPlugins;

    private List<String> paths;

    private List<String> excludes;

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public synchronized List<AutoDiscoveryScanType> getScanTypes() {
        if (scanTypes == null) {
            scanTypes = new ArrayList<AutoDiscoveryScanType>();
        }

        return scanTypes;
    }

    public void setScanTypes(List<AutoDiscoveryScanType> scanTypes) {
        this.scanTypes = scanTypes;
    }

    public List<String> getServerPlugins() {
        return serverPlugins;
    }

    public void setServerPlugins(List<String> serverPlugins) {
        this.serverPlugins = serverPlugins;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public List<String> getExcludes() {
        return excludes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }
}