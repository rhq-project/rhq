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
import java.util.List;

public class ScriptRecipeCommand implements RecipeCommand {

    public String getName() {
        return "script";
    }

    public void parse(RecipeParser parser, RecipeContext context, String[] args) {

        if (args == null || args.length == 0) {
            throw new IllegalArgumentException("Missing the script command to execute");
        }

        String exe = args[0];
        List<String> exeArgs = new ArrayList<String>(args.length - 1);
        for (int i = 1; i < args.length; i++) {
            exeArgs.add(args[i]);
        }

        context.addScript(exe, exeArgs);

        return;
    }
}
