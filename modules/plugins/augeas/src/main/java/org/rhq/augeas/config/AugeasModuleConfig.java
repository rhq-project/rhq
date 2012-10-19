/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.augeas.config;

import java.util.ArrayList;
import java.util.List;

import org.rhq.augeas.AugeasProxy;

/**
 * Represents the configuration of an Augeas module to be used by the {@link AugeasProxy}.
 * 
 * @author Filip Drabek
 *
 */
public class AugeasModuleConfig {

    private String moduletName;
    private String lensPath;
    private List<String> excludedGlobs;
    private List<String> includedGlobs;
    private List<String> configFiles;

    public AugeasModuleConfig() {
        excludedGlobs = new ArrayList<String>();
        includedGlobs = new ArrayList<String>();
        configFiles = new ArrayList<String>();
    }

    public List<String> getConfigFiles() {
        return configFiles;
    }

    public void setConfigFiles(List<String> configFiles) {
        this.configFiles = configFiles;
    }

    public void addConfigFile(String configFile) {
        this.configFiles.add(configFile);
    }

    public String getModuletName() {
        return moduletName;
    }

    public void setModuletName(String moduletName) {
        this.moduletName = moduletName;
    }

    public String getLensPath() {
        return lensPath;
    }

    public void setLensPath(String lensPath) {
        this.lensPath = lensPath;
    }

    public List<String> getExcludedGlobs() {
        return excludedGlobs;
    }

    public void setExcludedGlobs(List<String> excludedGlobs) {
        this.excludedGlobs = excludedGlobs;
    }

    public List<String> getIncludedGlobs() {
        return includedGlobs;
    }

    public void setIncludedGlobs(List<String> includedGlobs) {
        this.includedGlobs = includedGlobs;
    }

    public void addIncludedGlob(String name) {
        if (!includedGlobs.contains(name))
            this.includedGlobs.add(name);
    }

    public void addExcludedGlob(String name) {
        if (!excludedGlobs.contains(name))
            this.excludedGlobs.add(name);
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        AugeasModuleConfig that = (AugeasModuleConfig) obj;
        if (!this.moduletName.equals(that.getModuletName()))
            return false;
        if (!this.lensPath.equals(that.getLensPath()))
            return false;

        return true;
    }

}