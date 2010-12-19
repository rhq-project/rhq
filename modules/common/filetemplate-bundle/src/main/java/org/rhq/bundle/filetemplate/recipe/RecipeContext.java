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
import org.rhq.core.util.updater.DeploymentProperties;

/**
 * Contains information that is gleened from a recipe during and after it is parsed.
 * 
 * @author John Mazzitelli
 */
public class RecipeContext {

    private RecipeParser parser;
    private final String recipe;
    private boolean unknownRecipe;
    private final DeploymentProperties deploymentProperties;
    private final Map<String, String> deployFiles;
    private final Map<String, String> files;
    private final Set<String> realizedFiles;
    private final Set<String> replacementVariables;
    private final Map<String, String> replacementVariableDefaultValues;
    private Configuration replacementVariableValues;
    private final List<Script> scripts;
    private final List<Command> commands;

    public RecipeContext(String recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("recipe == null");
        }
        this.recipe = recipe;
        this.deployFiles = new HashMap<String, String>();
        this.files = new HashMap<String, String>();
        this.realizedFiles = new HashSet<String>();
        this.replacementVariables = new HashSet<String>();
        this.replacementVariableDefaultValues = new HashMap<String, String>();
        this.scripts = new ArrayList<Script>();
        this.commands = new ArrayList<Command>();
        this.deploymentProperties = new DeploymentProperties();
        this.unknownRecipe = true; // will be false if the recipe at least looks like one we understand
    }

    /**
     * If this context is currently in use by a parser, this will be the reference to that parser.
     * @return parser currently using this context
     */
    public RecipeParser getParser() {
        return parser;
    }

    public void setParser(RecipeParser parser) {
        this.parser = parser;
    }

    /**
     * Returns true if the recipe content does not look like a valid type that can be at least attempted
     * to be parsed. In other words, this returns true if the recipe does not look like a file template
     * recipe. This returns false if it looks like its file template recipe (even though it may have
     * syntax errors in it).
     * 
     * @return flag to indicate if the recipe looks like it might be a file template recipe
     */
    public boolean isUnknownRecipe() {
        return unknownRecipe;
    }

    public void setUnknownRecipe(boolean unknownRecipe) {
        this.unknownRecipe = unknownRecipe;
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
     * Returns the deployment properties as defined in the recipe.
     * 
     * @return the deployment properties
     */
    public DeploymentProperties getDeploymentProperties() {
        return deploymentProperties;
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
     * Returns the pathnames of all files that are to be realized (meaning, those that should
     * have replacement variables replaced).
     * 
     * @return set of all realized files
     */
    public Set<String> getRealizedFiles() {
        return this.realizedFiles;
    }

    public void addRealizedFile(String file) {
        this.realizedFiles.add(file);
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
     * If a replacement varaible is known to have a default value, call this
     * to set that default value.
     * 
     * @param replacementVariable the variable whose default is being set
     * @param defaultValue the value of the default for the given replacement variable
     */
    public void assignDefaultValueToReplacementVariable(String replacementVariable, String defaultValue) {
        this.replacementVariableDefaultValues.put(replacementVariable, defaultValue);
    }

    /**
     * Returns a map keyed on replacement variables whose values are the default
     * values for the replacement variables. Note that not all replacement variables
     * found in {@link #getReplacementVariables()} will have an associated entry
     * in the returned map (that is, not all variables have a default value).
     * 
     * @return the map of all known default values for replacement variables
     */
    public Map<String, String> getReplacementVariableDefaultValues() {
        return this.replacementVariableDefaultValues;
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
        private final String exe;
        private final List<String> args;

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

    /**
     * Adds a command that the recipe wants invoked. This command refers to an existing
     * executable or shell command.
     * 
     * @param cmd the main executable that this command wants to execute
     * @param exeArgs the arguments to pass to the command executable
     */
    public void addCommand(String exe, List<String> exeArgs) {
        this.commands.add(new Command(exe, exeArgs));
    }

    class Command {
        private final String exe;
        private final List<String> args;

        public Command(String exe, List<String> args) {
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
        str.append("Deployment Properties: ").append(getDeploymentProperties()).append("\n");
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

        str.append("Replacement Default Values: ");
        Map<String, String> defaults = getReplacementVariableDefaultValues();
        if (defaults == null) {
            str.append("<none>");
        } else {
            str.append(defaults);
        }
        str.append("\n");

        str.append("Script Files: ").append(getScriptFiles()).append("\n");
        for (Script script : this.scripts) {
            str.append("* ").append(script.getExecutable()).append(" ").append(script.getArguments()).append("\n");
        }

        str.append("Commands: ").append("\n");
        for (Command cmd : this.commands) {
            str.append("* ").append(cmd.getExecutable()).append(" ").append(cmd.getArguments()).append("\n");
        }

        str.append("Recipe Text:\n-----------\n").append(getRecipe()).append("\n");

        return str.toString();
    }
}
