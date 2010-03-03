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

public class FileRecipeCommand implements RecipeCommand {

    public String getName() {
        return "file";
    }

    public void parse(RecipeContext context, String[] args) {
        String sopts = ":s:d:";
        LongOpt[] lopts = { new LongOpt("source", LongOpt.REQUIRED_ARGUMENT, null, 's'),
            new LongOpt("destination", LongOpt.REQUIRED_ARGUMENT, null, 'd') };

        String source = null;
        String destination = null;

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

            case 's': {
                source = getopt.getOptarg();
                break;
            }

            case 'd': {
                destination = getopt.getOptarg();
                break;
            }

            default: {
                throw new IllegalArgumentException("Unexpected error in recipe command");
            }

            }
        }

        if (source == null) {
            throw new IllegalArgumentException("Did not specify the source file to copy");
        }

        if (destination == null) {
            throw new IllegalArgumentException("Did not specify the destination where the file should be copied");
        }

        context.addFile(source, destination);
        return;
    }
}
