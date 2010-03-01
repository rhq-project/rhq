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

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test
public class RecipeParserTest {
    private StringBuilder recipe;

    @BeforeMethod
    public void cleanRecipeBeforeMethod() {
        cleanRecipe();
    }

    public void testSimpleRecipe() throws Exception {
        addRecipeCommand("deploy -f jboss.tar -d /opt/jboss");
        addRecipeCommand("configdef -f config.xml");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = parser.parseRecipe(getRecipe());
        assert context.getConfigurationDefinitionFilename().equals("config.xml");
        assert context.getDeployFiles().containsKey("jboss.tar");
        assert context.getDeployFiles().get("jboss.tar").equals("/opt/jboss");
    }

    public void testSimpleRecipeError() throws Exception {
        addRecipeCommand("deploy -f jboss.tar");
        RecipeParser parser = new RecipeParser();

        try {
            parser.parseRecipe(getRecipe());
            assert false : "This should have failed - need to provide a -d to the deploy command";
        } catch (Exception ok) {
            // to be expected
        }

        cleanRecipe();
        addRecipeCommand("configdef");

        try {
            parser.parseRecipe(getRecipe());
            assert false : "This should have failed - need to provide a -f to the configdef command";
        } catch (Exception ok) {
            // to be expected
        }
    }

    public void testSimpleRecipeComments() throws Exception {
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("deploy -f jboss.tar -d /opt/jboss");
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("configdef -f config.xml");
        addRecipeCommand("");
        addRecipeCommand("#");
        addRecipeCommand("### comment here");
        addRecipeCommand("");
        RecipeParser parser = new RecipeParser();
        RecipeContext context = parser.parseRecipe(getRecipe());
        assert context.getConfigurationDefinitionFilename().equals("config.xml");
        assert context.getDeployFiles().containsKey("jboss.tar");
        assert context.getDeployFiles().get("jboss.tar").equals("/opt/jboss");
    }

    private void cleanRecipe() {
        this.recipe = new StringBuilder();
    }

    private void addRecipeCommand(String commandLine) {
        this.recipe.append(commandLine).append("\n");
    }

    private String getRecipe() {
        return this.recipe.toString();
    }
}
