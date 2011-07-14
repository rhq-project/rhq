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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.util.HashSet;
import java.util.Set;

public class ConfigDefRecipeCommand implements RecipeCommand {

    public String getName() {
        return "configdef";
    }

    public void parse(RecipeParser parser, RecipeContext context, String[] args) {
        String sopts = ":n:d:";
        LongOpt[] lopts = { new LongOpt("default", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
            new LongOpt("name", LongOpt.REQUIRED_ARGUMENT, null, 'n') };

        String replacementVariableName = null;
        String defaultValue = null;

        Getopt getopt = new Getopt(getName(), args, sopts, lopts);
        int code;

        while ((code = getopt.getopt()) != -1) {
            switch (code) {
            case ':':
            case '?': {
                throw new IllegalArgumentException("Bad recipe command.");
            }

            case 1: {
                throw new IllegalArgumentException("Bad recipe command!");
            }

            case 'n': {
                replacementVariableName = getopt.getOptarg();
                break;
            }

            case 'd': {
                defaultValue = getopt.getOptarg();
                break;
            }

            default: {
                throw new IllegalArgumentException("Unexpected error in recipe command");
            }

            }
        }

        if (replacementVariableName == null) {
            throw new IllegalArgumentException(
                "Did not specify the name of the configuration definition replacement variable");
        }

        Set<String> replacementVariableNames = new HashSet<String>(1);
        replacementVariableNames.add(replacementVariableName);
        context.addReplacementVariables(replacementVariableNames);
        if (defaultValue != null) {
            context.assignDefaultValueToReplacementVariable(replacementVariableName, defaultValue);
        }

        return;
    }
}
