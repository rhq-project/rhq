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
package org.rhq.plugins.filetemplate;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.util.FileUtils;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class FileTemplateBundlePluginServerComponent implements ResourceComponent, BundleFacet {

    /** property that should always be available to scripts - it's the location where the deployment should be installed */
    private static final String DEPLOY_DIR = "rhq.deploy.dir";

    /** property that should always be available to scripts - it's the ID of the bundle deployment */
    private static final String DEPLOY_ID = "rhq.deploy.id";

    /** property that should always be available to scripts - it's the name of the bundle deployment */
    public static final String DEPLOY_NAME = "rhq.deploy.name";

    private final Log log = LogFactory.getLog(FileTemplateBundlePluginServerComponent.class);

    private ResourceContext resourceContext;

    public void start(ResourceContext context) {
        resourceContext = context;
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public BundleDeployResult deployBundle(BundleDeployRequest request) {
        BundleDeployResult result = new BundleDeployResult();

        // the file template recipe processor does not support reverting deployments
        // thus we need to fail-fast if a revert deployment is being requested
        if (request.isRevert()) {
            result.setErrorMessage("File template bundles cannot be reverted");
            return result;
        }

        try {
            BundleResourceDeployment resourceDeployment = request.getResourceDeployment();
            BundleDeployment bundleDeployment = resourceDeployment.getBundleDeployment();
            BundleVersion bundleVersion = bundleDeployment.getBundleVersion();
            BundleManagerProvider bundleManagerProvider = request.getBundleManagerProvider();

            // before processing the recipe, wipe the dest dir if we need to perform a clean deployment
            if (request.isCleanDeployment()) {
                File deployDir = new File(bundleDeployment.getDestination().getDeployDir());
                if (deployDir.exists()) {
                    bundleManagerProvider.auditDeployment(resourceDeployment, "Cleaning Deployment", deployDir
                        .getAbsolutePath(), null, null, "The existing deployment found at ["
                        + deployDir.getAbsolutePath() + "] will be removed.", null);
                    FileUtils.purge(deployDir, true);
                }
            }

            // process the recipe
            String recipe = bundleVersion.getRecipe();
            RecipeParser parser = new RecipeParser();
            ProcessingRecipeContext recipeContext = new ProcessingRecipeContext(recipe, request
                .getPackageVersionFiles(), this.resourceContext.getSystemInformation(), request
                .getBundleFilesLocation().getAbsolutePath(), resourceDeployment, bundleManagerProvider);

            bundleManagerProvider.auditDeployment(resourceDeployment, "Configurtion Variable Replacement",
                bundleDeployment.getName(), null, null, "setting replacement variable values using ["
                    + bundleDeployment.getConfiguration().toString(true) + "]", null);
            recipeContext.setReplacementVariableValues(bundleDeployment.getConfiguration());
            recipeContext.addReplacementVariableValue(DEPLOY_DIR, bundleDeployment.getDestination().getDeployDir());
            recipeContext.addReplacementVariableValue(DEPLOY_ID, Integer.toString(bundleDeployment.getId()));
            recipeContext.addReplacementVariableValue(DEPLOY_NAME, bundleDeployment.getName());

            parser.setReplaceReplacementVariables(true);

            bundleManagerProvider.auditDeployment(resourceDeployment, "Parse Recipe", bundleDeployment.getName(), null,
                null, "Parsing Recipe using context [" + recipeContext + "]", null);
            parser.parseRecipe(recipeContext);
        } catch (Throwable t) {
            log.error("Failed to deploy bundle [" + request + "]", t);
            result.setErrorMessage(t);
        }

        return result;
    }
}
