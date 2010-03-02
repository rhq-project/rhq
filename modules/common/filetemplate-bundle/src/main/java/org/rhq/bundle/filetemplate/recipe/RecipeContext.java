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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * Contains information that is gleened from a recipe after it is parsed.
 * 
 * @author John Mazzitelli
 */
public class RecipeContext {

    private final String recipe;
    private final Map<String, String> deployFiles;
    private final Set<String> replacementVariables;
    private Configuration replacementVariableValues;

    public RecipeContext(String recipe) {
        this.recipe = recipe;
        this.deployFiles = new HashMap<String, String>();
        this.replacementVariables = new HashSet<String>();
    }

    /**
     * The full recipe that this context represents.
     * 
     * @return the actual recipe text
     */
    public String getRecipe() {
        return this.recipe;
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

    /**
     * The names of all replacement variables that are found in the recipe.
     * 
     * @return the replacement variables
     */
    public Set<String> getReplacementVariables() {
        return this.replacementVariables;
    }

    public void addReplacementVariables(Set<String> replacementVariables) {
        this.replacementVariables.addAll(replacementVariables);
    }

    /**
     * If known, the returned value will contain values that are to be used to replace
     * replacement variables found in the recipe.
     * 
     * @return the replacement variable values
     */
    public Configuration getReplacementVariableValues() {
        return this.replacementVariableValues;
    }

    public void setReplacementVariableValues(Configuration configuration) {
        this.replacementVariableValues = configuration;
    }

    /**
     * Adds the given name/value pair to the set of replacement variable values associated
     * with this context.
     * 
     * @param name
     * @param value
     */
    public void addReplacementVariableValue(String name, String value) {
        Configuration values = getReplacementVariableValues();
        if (values == null) {
            values = new Configuration();
            setReplacementVariableValues(values);
        }
        values.put(new PropertySimple(name, value));
    }
}
