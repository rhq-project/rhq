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

import org.rhq.core.util.updater.DeploymentProperties;

public class BundleRecipeCommand implements RecipeCommand {

    public String getName() {
        return "bundle";
    }

    public void parse(RecipeParser parser, RecipeContext context, String[] args) {
        String sopts = ":n:v:d:";
        LongOpt[] lopts = { new LongOpt("name", LongOpt.REQUIRED_ARGUMENT, null, 'n'),
            new LongOpt("version", LongOpt.REQUIRED_ARGUMENT, null, 'v'),
            new LongOpt("description", LongOpt.REQUIRED_ARGUMENT, null, 'd') };

        String name = null;
        String version = null;
        String description = null;

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
                name = getopt.getOptarg();
                break;
            }

            case 'v': {
                version = getopt.getOptarg();
                break;
            }

            case 'd': {
                description = getopt.getOptarg();
                break;
            }

            default: {
                throw new IllegalArgumentException("Unexpected error in recipe command");
            }

            }
        }

        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("Did not specify the name of the bundle");
        }

        if (version == null || version.trim().length() == 0) {
            throw new IllegalArgumentException("Did not specify the description of the bundle");
        }

        DeploymentProperties props = context.getDeploymentProperties();
        if (props.isValid()) {
            throw new IllegalArgumentException("Cannot specify multiple bundle commands in the same recipe");
        }

        props.setBundleName(name.trim());
        props.setBundleVersion(version.trim());
        if (description != null) {
            props.setDescription(description);
        }
        props.setDeploymentId(0);

        return;
    }
}
