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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.template.TemplateEngine;

/**
 * Parses the file template recipe.
 * 
 * @author John Mazzitelli
 *
 */
public class RecipeParser {

    private final static String PROPERTY_DEPLOY_DIR = "rhq.deploy.dir";
    /** properties that will be set by the deployment automatically and do not need user definition */
    private final static Set<String> IGNORE_PROPERTIES;

    private Map<String, RecipeCommand> recipeCommands;

    private Pattern replacementVariableDeclarationPattern;
    private Pattern replacementVariableNamePattern;
    private String systemReplacementVariablePrefix;
    private boolean replaceVariables = false;

    static {
        IGNORE_PROPERTIES = new HashSet<String>();
        IGNORE_PROPERTIES.add(PROPERTY_DEPLOY_DIR);
    }

    public RecipeParser() {
        this.recipeCommands = createRecipeCommands();
        setupReplacementPatterns();
    }

    /**
     * If the parser should replace replacement variables with their corresponding values (as found in
     * the parser context), <code>true</code> is returned. If <code>false</code> then the replacement
     * variables remain as is when they are passed to their command processor objects.
     * 
     * @return flag
     */
    public boolean isReplaceReplacementVariables() {
        return this.replaceVariables;
    }

    public void setReplaceReplacementVariables(boolean flag) {
        this.replaceVariables = flag;
    }

    public void parseRecipe(RecipeContext context) throws Exception {
        context.setParser(this);
        try {
            BufferedReader recipeReader = new BufferedReader(new StringReader(context.getRecipe()));
            String line = recipeReader.readLine();
            while (line != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    // only process lines that aren't blank or aren't comment lines that start with #
                    parseRecipeCommandLine(context, line);
                    context.setUnknownRecipe(false); // we've successfully processed at least one line, this must be a file template recipe
                }
                line = recipeReader.readLine();
            }
        } finally {
            context.setParser(null);
        }

        return;
    }

    protected void parseRecipeCommandLine(RecipeContext context, String line) throws Exception {

        if (isReplaceReplacementVariables()) {
            line = replaceReplacementVariables(context, line);
        }

        String[] commandLineArray = splitCommandLine(line);
        String commandName = commandLineArray[0];
        String[] arguments = extractArguments(commandLineArray);

        RecipeCommand recipeCommand = this.recipeCommands.get(commandName);
        if (recipeCommand == null) {
            throw new Exception("Unknown command in recipe [" + commandName + "]");
        }

        Set<String> replacementVars = getReplacementVariables(line);
        if (replacementVars != null) {
            context.addReplacementVariables(replacementVars);
        }

        try {
            recipeCommand.parse(this, context, arguments);
        } catch (Exception e) {
            throw new Exception("Error in recipe, line [" + line + "]", e);
        }

        return;
    }

    protected HashMap<String, RecipeCommand> createRecipeCommands() {
        HashMap<String, RecipeCommand> commands = new HashMap<String, RecipeCommand>();

        RecipeCommand[] knownCommands = new RecipeCommand[] { new ScriptRecipeCommand(), //
            new BundleRecipeCommand(), //
            new ConfigDefRecipeCommand(), //
            new CommandRecipeCommand(), //
            new FileRecipeCommand(), //
            new RealizeRecipeCommand(), //
            new DeployRecipeCommand() //
        };

        for (RecipeCommand recipeCommand : knownCommands) {
            commands.put(recipeCommand.getName(), recipeCommand);
        }

        return commands;
    }

    protected Set<String> getReplacementVariables(String cmdLine) {
        Set<String> replacementVariables = null;
        Matcher matcher = this.replacementVariableDeclarationPattern.matcher(cmdLine);
        while (matcher.find()) {
            String replacementDeclaration = matcher.group();
            Matcher nameMatcher = this.replacementVariableNamePattern.matcher(replacementDeclaration);
            if (!nameMatcher.find()) {
                throw new IllegalArgumentException("Bad replacement declaration [" + replacementDeclaration + "]");
            }
            String replacementVariable = nameMatcher.group();
            if (!replacementVariable.startsWith(this.systemReplacementVariablePrefix)) {
                if (replacementVariables == null) {
                    replacementVariables = new HashSet<String>(1);
                }
                replacementVariables.add(replacementVariable);
            }
        }

        if (null != replacementVariables) {
            replacementVariables.removeAll(IGNORE_PROPERTIES);
        }
        return replacementVariables;
    }

    public String replaceReplacementVariables(RecipeContext context, String input) {

        // since our replacement strings may include \ and $ characters avoid the use of
        // matcher.appendReplacement and like methods when doing this work.

        String result = input;
        Configuration replacementValues = context.getReplacementVariableValues();
        TemplateEngine templateEngine = null;

        Matcher matcher = this.replacementVariableDeclarationPattern.matcher(input);
        while (matcher.find()) {
            String next = matcher.group();
            Matcher nameMatcher = this.replacementVariableNamePattern.matcher(next);
            if (nameMatcher.find()) {
                String key = nameMatcher.group();
                String value = (replacementValues != null) ? replacementValues.getSimpleValue(key, null) : null;
                if (value == null) {
                    // our replacement values don't know how to replace the key, see if our system info template engine can
                    if (templateEngine == null) {
                        templateEngine = SystemInfoFactory.fetchTemplateEngine();
                    }
                    value = templateEngine.replaceTokens(next);
                }
                if (value != null) {
                    result = result.replace(next, value);
                }
            }
        }

        return result;
    }

    protected String[] splitCommandLine(String cmdLine) {
        cmdLine = cmdLine.replace('\\', '/');

        ByteArrayInputStream in = new ByteArrayInputStream(cmdLine.getBytes());
        StreamTokenizer strtok = new StreamTokenizer(new InputStreamReader(in));
        List<String> args = new ArrayList<String>();
        boolean keep_going = true;

        // we don't want to parse numbers and we want ' to be a normal word character
        strtok.ordinaryChars('0', '9');
        strtok.ordinaryChar('.');
        strtok.ordinaryChar('-');
        strtok.ordinaryChar('\'');
        strtok.wordChars(33, 127);
        strtok.quoteChar('\"');

        // parse the command line
        while (keep_going) {
            int nextToken;

            try {
                nextToken = strtok.nextToken();
            } catch (IOException e) {
                nextToken = StreamTokenizer.TT_EOF;
            }

            if (nextToken == java.io.StreamTokenizer.TT_WORD) {
                args.add(strtok.sval);
            } else if (nextToken == '\"') {
                args.add(strtok.sval);
            } else if ((nextToken == java.io.StreamTokenizer.TT_EOF) || (nextToken == java.io.StreamTokenizer.TT_EOL)) {
                keep_going = false;
            }
        }

        return args.toArray(new String[args.size()]);
    }

    protected String[] extractArguments(String[] commandLine) {
        // strip the first element (the command name) from the array, leaving only the arguments
        int newLength = commandLine.length - 1;
        String[] argsOnly = new String[newLength];
        System.arraycopy(commandLine, 1, argsOnly, 0, newLength);
        return argsOnly;
    }

    private void setupReplacementPatterns() {
        // note that we use the same as the core util's template engine
        // the native system replacement variable prefix is used by the agent-side fact variable names
        this.replacementVariableDeclarationPattern = Pattern.compile("@@\\s*(\\w+\\.?)+\\s*@@");
        this.replacementVariableNamePattern = Pattern.compile("(\\w+\\.?)+");
        this.systemReplacementVariablePrefix = SystemInfoFactory.TOKEN_PREFIX;
    }

}
