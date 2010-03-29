/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.bundle.ant;

import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.Project;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;

public class BundleAntProject extends Project {
    private final Map<String, String> bundleFiles = new HashMap<String, String>();
    private ConfigurationDefinition configDef;

    public Map<String, String> getBundleFiles() {
        return bundleFiles;
    }

    public void addBundleFile(String name, String filename) {
        bundleFiles.put(name, filename);
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        if (configDef == null) {
            configDef = new ConfigurationDefinition("antbundle", null);
        }
        return configDef;
    }
}
