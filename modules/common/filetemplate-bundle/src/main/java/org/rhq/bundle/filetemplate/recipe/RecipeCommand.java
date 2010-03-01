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

public interface RecipeCommand {

    /**
     * The name of the recipe command. This must be the first word in the recipe line in order for this command to be triggered.
     * 
     * @return name of the recipe command
     */
    String getName();

    /**
     * Parses this recipe command and optionally puts information in the context.
     * 
     * @param context information where the parser can store information regarding the command that was processed
     * @param args arguments passed to the command.
     */
    void parse(RecipeContext context, String[] args);
}
