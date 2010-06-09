/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc.bundle;

import java.io.File;

import org.rhq.enterprise.server.bundle.BundleDistributionInfo;
import org.rhq.enterprise.server.bundle.RecipeParseResults;

/**
 * All bundle server plugins must implement this facet.
 * 
 * @author John Mazzitelli
 */
public interface BundleServerPluginFacet {
    /**
     * The server side plugin is being given a recipe which must be parsed. The results
     * of the parse are to be returned.
     * 
     * @param recipe the content of the recipe to be parsed
     * @return the information gleened from the recipe after being parsed
     * @throws UnknownRecipeException if the recipe is not understood by the plugin (i.e.
     *                                it is a recipe that some other bundle plugin probably
     *                                can process)
     * @throws Exception if the recipe could not be successfully parsed - this exception
     *                   usually means the recipe type is known to the plugin but it just
     *                   had some error that caused it to fail to parse successfully
     */
    RecipeParseResults parseRecipe(String recipe) throws UnknownRecipeException, Exception;

    /**
     * The server side plugin is being given an bundle distribution file that must be procssed.
     * The results of the processing are to be returned.
     * 
     * An bundle distribution file is a zip file that contains a recipe and 0, 1 or more bundle files.
     * 
     * @param distributionFile
     * @return the information gleened by cracking open the bundle distribution file and examining its contents
     * @throws UnknownRecipeException if the recipe in the distribution file is not understood by the
     *                                plugin (i.e. the distribution file can probably be processed by some other
     *                                bundle plugin).
     * @throws Exception if the bundle distribution file could not be successfully processed - this exception
     *                   usually means the distribution file is of a known type to the plugin but it just
     *                   had some error that caused it to fail to be processed successfully.
     */
    BundleDistributionInfo processBundleDistributionFile(File distributionFile) throws UnknownRecipeException,
        Exception;
}
