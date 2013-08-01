/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.bundle.UnknownRecipeException;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * @author Lukas Krejci
 */
public class TestBundlePluginComponent implements BundleServerPluginFacet {
    public RecipeParseResults parseRecipe_returnValue;
    public BundleDistributionInfo processBundleDistributionFile_returnValue;

    @Override
    public RecipeParseResults parseRecipe(String recipe) throws UnknownRecipeException, Exception {

        if (parseRecipe_returnValue != null) {
            return parseRecipe_returnValue;
        }

        return doParseRecipe(recipe);
    }

    protected RecipeParseResults doParseRecipe(String recipe) throws UnknownRecipeException, Exception {

        ConfigurationDefinition configDef;
        Set<String> bundleFileNames;
        DeploymentProperties metadata;

        metadata = new DeploymentProperties(0, "bundletest", "1.0", "bundle test description");

        configDef = new ConfigurationDefinition("bundletest-configdef", "Test Config Def for testing BundleVersion");
        configDef.put(new PropertyDefinitionSimple("bundletest.property",
            "Test property for BundleVersion Config Def testing", true, PropertySimpleType.STRING));

        bundleFileNames = new HashSet<String>();
        for (int i = 0; i < AbstractEJB3Test.DEFAULT_CRITERIA_PAGE_SIZE + 2; i++) {
            bundleFileNames.add("bundletest-bundlefile-" + i);
        }

        return new RecipeParseResults(metadata, configDef, bundleFileNames);
    }

    @Override
    public BundleDistributionInfo processBundleDistributionFile(File distributionFile)
        throws UnknownRecipeException, Exception {
        if (processBundleDistributionFile_returnValue != null) {
            return processBundleDistributionFile_returnValue;
        }

        return doProcessBundleDistributionFile(distributionFile);
    }

    protected BundleDistributionInfo doProcessBundleDistributionFile(File distributionFile) {
        throw new UnsupportedOperationException("this mock object cannot do this");
    }
}
