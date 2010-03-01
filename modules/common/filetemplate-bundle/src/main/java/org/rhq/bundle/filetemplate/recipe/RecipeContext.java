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

package org.rhq.bundle.filetemplate.recipe;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains information that is gleened from a recipe after it is parsed.
 * 
 * @author John Mazzitelli
 */
public class RecipeContext {

    private final String recipe;
    private String configurationDefinitionFilename;
    private Map<String, String> deployFiles;

    public RecipeContext(String recipe) {
        this.recipe = recipe;
        this.configurationDefinitionFilename = null;
        this.deployFiles = new HashMap<String, String>();
    }

    /**
     * The full recipe that this context represents.
     * 
     * @return the actual recipe text
     */
    public String getRecipe() {
        return recipe;
    }

    /**
     * A recipe can have an optional configuration definition file associated with it.
     * If this returns non-<code>null</code>, it refers to this configuration definition file.
     * 
     * @return config def filename
     */
    public String getConfigurationDefinitionFilename() {
        return configurationDefinitionFilename;
    }

    public void setConfigurationDefinitionFilename(String filename) {
        this.configurationDefinitionFilename = filename;
    }

    /**
     * Returns all the files that are to be deployed. The key is the bundle file's name, the
     * value is the directory where the bundle is to be deployed to.
     * 
     * @return map of all files to be deployed and the location where they are to be deployed
     */
    public Map<String, String> getDeployFiles() {
        return this.deployFiles;
    }

    public void addDeployFile(String filename, String directory) {
        this.deployFiles.put(filename, directory);
    }
}
