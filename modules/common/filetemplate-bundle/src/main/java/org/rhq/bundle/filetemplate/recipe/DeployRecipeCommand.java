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

public class DeployRecipeCommand implements RecipeCommand {

    public String getName() {
        return "deploy";
    }

    public void parse(RecipeContext context, String[] args) {
        String sopts = ":f:d:";
        LongOpt[] lopts = { new LongOpt("file", LongOpt.REQUIRED_ARGUMENT, null, 'f'),
            new LongOpt("directory", LongOpt.REQUIRED_ARGUMENT, null, 'd') };

        String filename = null;
        String directory = null;

        Getopt getopt = new Getopt(context.toString(), args, sopts, lopts);
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

            case 'f': {
                filename = getopt.getOptarg();
                break;
            }

            case 'd': {
                directory = getopt.getOptarg();
                break;
            }

            default: {
                throw new IllegalArgumentException("Unexpected error in recipe command");
            }

            }
        }

        if (filename == null) {
            throw new IllegalArgumentException("Did not specify the name of the file to deploy");
        }

        if (directory == null) {
            throw new IllegalArgumentException("Did not specify the directory where the file should be deployed");
        }

        context.addDeployFile(filename, directory);
        return;
    }
}
