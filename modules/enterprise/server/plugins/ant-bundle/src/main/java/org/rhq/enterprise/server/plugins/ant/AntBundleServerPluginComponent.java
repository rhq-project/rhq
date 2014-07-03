/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.plugins.ant;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.bundle.ant.BundleAntProject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.enterprise.server.bundle.BundleDistributionInfo;
import org.rhq.enterprise.server.bundle.RecipeParseResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.bundle.UnknownRecipeException;

/**
 * A bundle server-side plugin component that the server uses to process Ant-based bundles.
 * 
 * @author John Mazzitelli
 */
public class AntBundleServerPluginComponent implements ServerPluginComponent, BundleServerPluginFacet {
    private static final Log LOG = LogFactory.getLog(AntBundleServerPluginComponent.class);

    private ServerPluginContext context;
    private File tmpDirectory;

    @Override
    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        this.tmpDirectory = new File(this.context.getTemporaryDirectory(), "ant-bundle-server-plugin");
        //noinspection ResultOfMethodCallIgnored
        this.tmpDirectory.mkdirs();
        if (!this.tmpDirectory.exists() || !this.tmpDirectory.isDirectory()) {
            throw new Exception("Failed to create tmp dir [" + this.tmpDirectory + "] - cannot process Ant bundles");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("The Ant bundle plugin has been initialized: " + this);
        }
    }

    @Override
    public void start() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The Ant bundle plugin has started: " + this);
        }
    }

    @Override
    public void stop() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The Ant bundle plugin has stopped: " + this);
        }
    }

    @Override
    public void shutdown() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The Ant bundle plugin has been shut down: " + this);
        }
    }

    @Override
    public RecipeParseResults parseRecipe(String recipe) throws Exception {

        // all Ant recipes must use the RHQ custom ant library; if the recipe doesn't have the
        // string of that antlib URI, that means this probably isn't an Ant recipe in the first place
        if (!recipe.contains("antlib:org.rhq.bundle")) {
            throw new UnknownRecipeException("Not a valid Ant recipe");
        }

        DeploymentProperties deploymentProps;
        Set<String> bundleFiles;
        ConfigurationDefinition configDef;

        RecipeParseResults results;

        File recipeFile = File.createTempFile("ant-bundle-recipe", ".xml", this.tmpDirectory);
        File logFile = File.createTempFile("ant-bundle-recipe", ".log", this.tmpDirectory);
        try {
            // store the recipe in the tmp recipe file
            ByteArrayInputStream in = new ByteArrayInputStream(recipe.getBytes());
            FileOutputStream out = new FileOutputStream(recipeFile);
            StreamUtil.copy(in, out);

            // parse, but do not execute, the Ant script
            AntLauncher antLauncher = new AntLauncher(true);
            BundleAntProject project = antLauncher.parseBundleDeployFile(recipeFile, null);

            // obtain the parse results
            deploymentProps = new DeploymentProperties(0, project.getBundleName(), project.getBundleVersion(),
                project.getBundleDescription(), project.getDestinationCompliance());

            bundleFiles = project.getBundleFileNames();
            configDef = project.getConfigurationDefinition();
        } catch (Throwable t) {
            if (LOG.isDebugEnabled()) {
                try {
                    LOG.debug(new String(StreamUtil.slurp(new FileInputStream(logFile))));
                } catch (Exception ignore) {
                }
            }
            throw new Exception("Failed to parse the bundle Ant script.", t);
        } finally {
            recipeFile.delete();
            logFile.delete();
        }

        results = new RecipeParseResults(deploymentProps, configDef, bundleFiles);
        return results;
    }

    @Override
    public BundleDistributionInfo processBundleDistributionFile(File distributionFile) throws Exception {
        if (null == distributionFile) {
            throw new IllegalArgumentException("distributionFile == null");
        }

        // try and parse the recipe, if successful then process the distributionFile completely 
        RecipeVisitor recipeVisitor = new RecipeVisitor(this, "deploy.xml");
        ZipUtil.walkZipFile(distributionFile, recipeVisitor);
        String recipe = recipeVisitor.getRecipe();
        RecipeParseResults recipeParseResults = recipeVisitor.getResults();

        if (null == recipeParseResults) {
            throw new UnknownRecipeException("Not an Ant Bundle");
        }

        // if we parsed the recipe then this is a distribution we can deal with, get the bundle file Map                 
        BundleFileVisitor bundleFileVisitor = new BundleFileVisitor(recipeParseResults.getBundleFileNames());
        ZipUtil.walkZipFile(distributionFile, bundleFileVisitor);

        return new BundleDistributionInfo(recipe, recipeParseResults, bundleFileVisitor.getBundleFiles());
    }

    private static class RecipeVisitor implements ZipUtil.ZipEntryVisitor {
        private RecipeParseResults results = null;
        private String recipeName = null;
        private BundleServerPluginFacet facet = null;
        private String recipe = null;

        public RecipeVisitor(BundleServerPluginFacet facet, String recipeName) {
            this.facet = facet;
            this.recipeName = recipeName;
        }

        @Override
        public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
            if (this.recipeName.equalsIgnoreCase(entry.getName())) {
                // this should be safe downcast, recipes are not that big
                int contentSize = (int) entry.getSize();
                ByteArrayOutputStream out = new ByteArrayOutputStream((contentSize > 0) ? contentSize : 32768);
                StreamUtil.copy(stream, out, false);
                this.recipe = new String(out.toByteArray());
                //noinspection UnusedAssignment
                out = null; // no need for this anymore, help out GC
                this.results = this.facet.parseRecipe(this.recipe);
                return false; // we found the file we are looking for so stop walking
            }
            return true;
        }

        public RecipeParseResults getResults() {
            return results;
        }

        public String getRecipe() {
            return recipe;
        }
    }

    private static class BundleFileVisitor implements ZipUtil.ZipEntryVisitor {
        private Set<String> bundleFileNames;
        private Map<String, File> bundleFiles;
        private File tmpDir;

        BundleFileVisitor(Set<String> bundleFileNames) throws IOException {
            this.bundleFileNames = bundleFileNames;
            this.bundleFiles = new HashMap<String, File>(bundleFileNames.size());
            this.tmpDir = FileUtil.createTempDirectory("ant-bundle", ".dir", null);
        }

        @Override
        public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
            if (bundleFileNames.contains(entry.getName())) {

                File bundleFile = new File(tmpDir, entry.getName());
                bundleFile.getParentFile().mkdirs();

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(bundleFile);
                    StreamUtil.copy(stream, fos, false);
                } finally {
                    if (null != fos) {
                        try {
                            fos.close();
                        } catch (Exception e) {
                            //
                        }
                    }
                }
                this.bundleFiles.put(entry.getName(), bundleFile);
            }

            return true;
        }

        public Map<String, File> getBundleFiles() {
            return bundleFiles;
        }
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }
        return "plugin-key=" + this.context.getPluginEnvironment().getPluginKey() + "," + "plugin-url="
            + this.context.getPluginEnvironment().getPluginUrl();
    }
}
