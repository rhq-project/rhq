 /*
  * Jopr Management Platform
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
 package org.rhq.core.pluginapi.content.version;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/**
 * Object that will be persisted to store the package versions for applications (EARs, WARs) detected by plugins.
 *
 * @author Jason Dobies
 */
public class ApplicationVersionData implements Serializable {

    /**
     * Mapping of package key to package version.
     */
    private Map<String, String> applicationVersions = new HashMap<String, String>();

    /**
     * Returns the package version associated with the package identified by the specified key.
     *
     * @param packageKey identifies the package
     *
     * @return version of the package if it is known; <code>null</code> otherwise
     */
    public String getVersion(String packageKey) {
        return applicationVersions.get(packageKey);
    }

    /**
     * Updates the store with a new version for the package identified by the specified key. If there is an existing
     * version in the store, it will be overwritten.
     *
     * @param packageKey identifies the package
     * @param version    version of the package                           
     */
    public void setVersion(String packageKey, String version) {
        applicationVersions.put(packageKey, version);
    }
}
