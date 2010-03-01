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
import java.util.List;
import java.util.Map;

/**
 * Parses the file template recipe.
 * 
 * @author John Mazzitelli
 *
 */
public class RecipeParser {
    private Map<String, RecipeCommand> recipeCommands;

    public RecipeParser() {
        setupRecipeCommands();
    }

    public RecipeContext parseRecipe(String recipe) throws Exception {
        RecipeContext context = new RecipeContext(recipe);

        BufferedReader recipeReader = new BufferedReader(new StringReader(recipe));
        String line = recipeReader.readLine();
        while (line != null) {
            String[] commandLineArray = splitCommandLine(line);
            String commandName = commandLineArray[0];
            String[] arguments = extractArguments(commandLineArray);

            RecipeCommand recipeCommand = recipeCommands.get(commandName);
            if (recipeCommand == null) {
                throw new Exception("Unknown command in recipe: " + commandName);
            }

            recipeCommand.parse(context, arguments);

            line = recipeReader.readLine();
        }

        return context;
    }

    private void setupRecipeCommands() {
        recipeCommands = new HashMap<String, RecipeCommand>();

        RecipeCommand[] knownCommands = new RecipeCommand[] { new ConfigdefRecipeCommand(), //
            new DeployRecipeCommand() //
        };

        for (RecipeCommand recipeCommand : knownCommands) {
            recipeCommands.put(recipeCommand.getName(), recipeCommand);
        }
    }

    private String[] splitCommandLine(String cmdLine) {
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

    private String[] extractArguments(String[] commandLine) {
        // strip the first element (the command name) from the array, leaving only the arguments
        int newLength = commandLine.length - 1;
        String[] argsOnly = new String[newLength];
        System.arraycopy(commandLine, 1, argsOnly, 0, newLength);
        return argsOnly;
    }
}
