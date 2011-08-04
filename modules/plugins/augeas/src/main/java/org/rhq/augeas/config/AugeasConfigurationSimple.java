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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.augeas.util.Glob;

/**
 * Basic implementation of the {@link AugeasConfiguration}.
 *  
 * @author Filip Drabek
 *
 */
public class AugeasConfigurationSimple implements AugeasConfiguration {

    private String loadPath;
    private int mode;
    private String rootPath;
    private List<AugeasModuleConfig> modules;

    /**
     * Sets the path to the Augeas lenses directory.
     * 
     * @param loadPath
     */
    public void setLoadPath(String loadPath) {
        this.loadPath = loadPath;
    }

    /**
     * Sets the Augeas load mode.
     * 
     * @see {@link Augeas#Augeas(int)}
     * 
     * @param mode
     */
    public void setMode(int mode) {
        this.mode = mode;
    }

    /**
     * Sets the path to the file system root used by Augeas.
     * 
     * @param rootPath
     */
    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    /**
     * Sets the modules to use.
     * 
     * @param modules
     */
    public void setModules(List<AugeasModuleConfig> modules) {
        this.modules = modules;
    }

    public AugeasConfigurationSimple() {
        modules = new ArrayList<AugeasModuleConfig>();
    }

    public String getLoadPath() {
        return loadPath;
    }

    public int getMode() {
        return mode;
    }

    public List<AugeasModuleConfig> getModules() {
        return modules;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void addModuleConfig(AugeasModuleConfig config) {
        if (modules.contains(config))
            return;
        modules.add(config);
    }

    public AugeasModuleConfig getModuleByName(String name) {
        for (AugeasModuleConfig module : modules) {
            if (module.getModuletName().equals(name))
                return module;
        }
        return null;
    }

    /**
     * Checks that all the files to be loaded, specified by the modules,
     * exist.
     * 
     * @see AugeasConfiguration#loadFiles()
     */
    public void loadFiles() {
        File root = new File(getRootPath());

        for (AugeasModuleConfig module : modules) {
            List<String> includeGlobs = module.getIncludedGlobs();

            if (includeGlobs.size() <= 0) {
                throw new IllegalStateException("Expecting at least once inclusion pattern for configuration files.");
            }

            List<File> files = Glob.matchAll(root, includeGlobs, Glob.ALPHABETICAL_COMPARATOR);

            if (module.getExcludedGlobs() != null) {
                List<String> excludeGlobs = module.getExcludedGlobs();
                Glob.excludeAll(files, excludeGlobs);
            }

            for (File configFile : files) {
                if (!configFile.isAbsolute()) {
                    throw new IllegalStateException(
                        "Configuration files inclusion patterns contain a non-absolute file.");
                }
                if (!configFile.exists()) {
                    throw new IllegalStateException(
                        "Configuration files inclusion patterns refer to a non-existent file.");
                }
                if (configFile.isDirectory()) {
                    throw new IllegalStateException("Configuration files inclusion patterns refer to a directory.");
                }
                if (!module.getConfigFiles().contains(configFile.getAbsolutePath()))
                    module.addConfigFile(configFile.getAbsolutePath());
            }
        }
    }
}
