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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final Map<String, String> files;
    private final Set<String> replacementVariables;
    private Configuration replacementVariableValues;
    private final List<Script> scripts;

    public RecipeContext(String recipe) {
        this.recipe = recipe;
        this.deployFiles = new HashMap<String, String>();
        this.files = new HashMap<String, String>();
        this.replacementVariables = new HashSet<String>();
        this.scripts = new ArrayList<Script>();
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
     * Returns all the files that are strictly to be copied to some location.
     * The key is the bundle file's name, the value is the full path and filename where
     * the file should be copied to. No processing of these files will occur other than
     * they are copied.
     * 
     * @return map of all files to be deployed and the location where they are to be deployed
     */
    public Map<String, String> getFiles() {
        return this.files;
    }

    public void addFile(String source, String destination) {
        this.files.put(source, destination);
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

    /**
     * Returns a set of all the names of scripts found in the recipe.
     * 
     * @return script names
     */
    public Set<String> getScriptFiles() {
        Set<String> scriptFiles = new HashSet<String>();
        for (Script script : this.scripts) {
            scriptFiles.add(script.getExecutable());
        }
        return scriptFiles;
    }

    /**
     * Adds a script that the recipe wants invoked.
     * 
     * @param exe the script executable
     * @param exeArgs the arguments to pass to the script
     */
    public void addScript(String exe, List<String> exeArgs) {
        this.scripts.add(new Script(exe, exeArgs));
    }

    class Script {
        private final List<String> args;
        private final String exe;

        public Script(String exe, List<String> args) {
            this.exe = exe;
            this.args = args;
        }

        public String getExecutable() {
            return exe;
        }

        public List<String> getArguments() {
            return args;
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RecipeContext:\n");
        str.append("Recipe Text:\n").append(getRecipe()).append("\n");
        str.append("Deploy Files: ").append(getDeployFiles()).append("\n");
        str.append("Replacement Vars: ").append(getReplacementVariables()).append("\n");

        str.append("Replacement Values: ");
        Configuration values = getReplacementVariableValues();
        if (values == null) {
            str.append("<none>");
        } else {
            str.append(values.getProperties());
        }
        str.append("\n");

        str.append("Script Files: ").append(getScriptFiles()).append("\n");
        for (Script script : this.scripts) {
            str.append("* ").append(script.getExecutable()).append(" ").append(script.getArguments()).append("\n");
        }
        return str.toString();
    }
}
