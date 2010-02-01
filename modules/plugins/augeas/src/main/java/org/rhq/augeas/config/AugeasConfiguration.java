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

import java.util.List;

import net.augeas.Augeas;

/**
 * Represents the augeas configuration.
 * 
 * @author Filip Drabek
 */
public interface AugeasConfiguration {

    /**
     * @return the list of modules augeas should load
     */
    public List<AugeasModuleConfig> getModules();

    /**
     * @return a module configuration by name
     */
    public AugeasModuleConfig getModuleByName(String name);

    /**
     * @return The filesystem root path for augeas tree.
     */
    public String getRootPath();

    /**
     * @return path to the augeas lenses directory
     */
    public String getLoadPath();

    /**
     * @return augeas loading mode
     * @see {@link Augeas#Augeas(int)}
     */
    public int getMode();
    
    /**
     * Initializes the modules. This can check that all the included files
     * exist or provide some additional runtime-determined configuration
     * to the modules. 
     */
    public void loadFiles();
    
}
