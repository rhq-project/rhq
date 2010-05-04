/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.bundle;

import java.io.File;
import java.util.Map;

/**
 * Information found when a Bundle Distribution file is cracked open and inspected.
 * 
 * @author John Mazzitelli
 */
public class BundleDistributionInfo {
    private final String recipe;
    private final RecipeParseResults recipeParseResults;
    private final Map<String, File> bundleFiles;
    private String bundleTypeName;

    public BundleDistributionInfo(String recipe, RecipeParseResults recipeParseResults, Map<String, File> bundleFiles) {

        this.recipe = recipe;
        this.recipeParseResults = recipeParseResults;
        this.bundleFiles = bundleFiles;
        this.bundleTypeName = null; // typically set later
    }

    /**
     * The information gleened from the recipe that was found in the bundle distribution file.
     * 
     * @return the recipe information
     */
    public RecipeParseResults getRecipeParseResults() {
        return recipeParseResults;
    }

    /**
     * If the bundle distribution file contained within it one or more bundle files that need
     * to be associated with the bundle version, this map will contain those files.
     * The key to the map is the name of the bundle file as referenced to it via
     * the recipe (i.e. the keys will be filenames that are mentioned in the
     * {@link #getRecipeParseResults() parse results}). The values are files referencing
     * the bundle files' content - it is the job of the server side plugin to extract
     * the bundle files from the bundle distribution file and store them on the local file
     * system.
     * 
     * @return names and file locations of all bundle files found in the bundle distribution file
     */
    public Map<String, File> getBundleFiles() {
        return bundleFiles;
    }

    public String getBundleTypeName() {
        return bundleTypeName;
    }

    public String getRecipe() {
        return recipe;
    }

    public void setBundleTypeName(String bundleTypeName) {
        this.bundleTypeName = bundleTypeName;
    }
}
