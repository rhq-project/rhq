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

import org.rhq.core.domain.bundle.BundleVersion;

/**
 * Information found when a uber bundle file was cracked open and inspected.
 * 
 * @author John Mazzitelli
 */
public class UberBundleFileInfo {
    private final BundleVersion bundleVersion;
    private final RecipeParseResults recipeParseResults;
    private final Map<String, File> bundleFiles;

    public UberBundleFileInfo(BundleVersion bundleVersion, RecipeParseResults recipeParseResults,
        Map<String, File> bundleFiles) {

        this.bundleVersion = bundleVersion;
        this.recipeParseResults = recipeParseResults;
        this.bundleFiles = bundleFiles;
    }

    /**
     * The metadata about the bundle version represented by the uber bundle file.
     * The uber bundle file must have metadata encoded in it that provides details
     * such as the name of the bundle, the version of the bundle, etc. Usually this
     * information is found within the recipe itself but that is not a requirement - the
     * uber bundle file merely has to have the information somewhere that the bundle type
     * server side plugin can find.
     * 
     * @return the bundle version information
     */
    public BundleVersion getBundleVersion() {
        return bundleVersion;
    }

    /**
     * The information gleened from the recipe that was found in the uber bundle file.
     * 
     * @return the recipe information
     */
    public RecipeParseResults getRecipeParseResults() {
        return recipeParseResults;
    }

    /**
     * If the uber bundle file contained within it one or more bundle files that need
     * to be associated with the bundle version, this map will contain those files.
     * The key to the map is the name of the bundle file as referenced to it via
     * the recipe (i.e. the keys will be filenames that are mentioned in the
     * {@link #getRecipeParseResults() parse results}). The values are files referencing
     * the bundle files' content - it is the job of the server side plugin to extract
     * the bundle files from the uber bundle file and store them on the local file
     * system.
     * 
     * @return names and file locations of all bundle files found in the uber bundle file
     */
    public Map<String, File> getBundleFiles() {
        return bundleFiles;
    }

}
