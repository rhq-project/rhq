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

package org.rhq.enterprise.server.plugins.filetemplate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.filetemplate.recipe.RecipeContext;
import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeploymentProperties;
import org.rhq.enterprise.server.bundle.BundleDistributionInfo;
import org.rhq.enterprise.server.bundle.RecipeParseResults;
import org.rhq.enterprise.server.plugin.pc.ControlFacet;
import org.rhq.enterprise.server.plugin.pc.ControlResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;
import org.rhq.enterprise.server.plugin.pc.bundle.UnknownRecipeException;

/**
 * A bundle server-side plugin component that the server uses to process file template bundles.
 * 
 * @author John Mazzitelli
 */
public class BundleServerPluginComponent implements ServerPluginComponent, BundleServerPluginFacet, ControlFacet {

    private final Log log = LogFactory.getLog(BundleServerPluginComponent.class);

    private ServerPluginContext context;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        log.debug("The filetemplate bundle plugin has been initialized!!! : " + this);
    }

    public void start() {
        log.debug("The filetemplate bundle plugin has started!!! : " + this);
    }

    public void stop() {
        log.debug("The filetemplate bundle plugin has stopped!!! : " + this);
    }

    public void shutdown() {
        log.debug("The filetemplate bundle plugin has been shut down!!! : " + this);
    }

    public RecipeParseResults parseRecipe(String recipe) throws UnknownRecipeException, Exception {
        RecipeParser parser = new RecipeParser();
        RecipeContext recipeContext = new RecipeContext(recipe);
        try {
            parser.parseRecipe(recipeContext);
        } catch (Exception e) {
            if (recipeContext.isUnknownRecipe()) {
                throw new UnknownRecipeException("Not a valid file template recipe");
            }
            throw e;
        }

        DeploymentProperties bundleMetadata = recipeContext.getDeploymentProperties();

        Set<String> bundleFileNames = new HashSet<String>();
        Map<String, String> deployFiles = recipeContext.getDeployFiles();
        bundleFileNames.addAll(deployFiles.keySet());
        Set<String> scriptFiles = recipeContext.getScriptFiles();
        bundleFileNames.addAll(scriptFiles);
        Set<String> files = recipeContext.getFiles().keySet();
        bundleFileNames.addAll(files);

        ConfigurationDefinition configDef = null;
        if (recipeContext.getReplacementVariables() != null) {
            configDef = new ConfigurationDefinition("replacementVariables", null);
            for (String replacementVar : recipeContext.getReplacementVariables()) {
                PropertyDefinitionSimple prop = new PropertyDefinitionSimple(replacementVar,
                    "Needed by bundle recipe.", false, PropertySimpleType.STRING);
                prop.setDisplayName(replacementVar);
                configDef.put(prop);
            }
        }

        RecipeParseResults results = new RecipeParseResults(bundleMetadata, configDef, bundleFileNames);
        return results;

    }

    public BundleDistributionInfo processBundleDistributionFile(File distributionFile) throws UnknownRecipeException,
        Exception {
        if (null == distributionFile) {
            throw new IllegalArgumentException("distributionFile == null");
        }

        BundleDistributionInfo info = null;
        String recipe = null;
        RecipeParseResults recipeParseResults = null;
        Map<String, File> bundleFiles = null;

        // try and parse the recipe, if successful then process the distributionFile completely 
        RecipeVisitor recipeVisitor = new RecipeVisitor(this, "deploy.txt");
        ZipUtil.walkZipFile(distributionFile, recipeVisitor);
        recipe = recipeVisitor.getRecipe();
        recipeParseResults = recipeVisitor.getResults();

        if (null == recipeParseResults) {
            // we also want to support the ability to provide just a file template recipe as the distro file
            // so see if we can parse it, but note that we don't even bother if its a really big file since
            // that's probably not a recipe file and we don't want to risk loading in a huge file in memory
            if (distributionFile.length() < 50000L) {
                byte[] content = StreamUtil.slurp(new FileInputStream(distributionFile));
                recipe = new String(content);
                content = null;
                recipeParseResults = parseRecipe(recipe); // if it isn't a recipe either, this will throw UnknownRecipeException
            } else {
                throw new UnknownRecipeException("Not a File Template Bundle");
            }
        } else {
            // if we parsed the recipe, then this is a distribution zip we can deal with, get the bundle file Map                 
            BundleFileVisitor bundleFileVisitor = new BundleFileVisitor(recipeParseResults.getBundleMetadata()
                .getBundleName(), recipeParseResults.getBundleFileNames());
            ZipUtil.walkZipFile(distributionFile, bundleFileVisitor);
            bundleFiles = bundleFileVisitor.getBundleFiles();
        }

        info = new BundleDistributionInfo(recipe, recipeParseResults, bundleFiles);

        return info;
    }

    public ControlResults invoke(String name, Configuration parameters) {
        ControlResults controlResults = new ControlResults();
        if (name.equals("testControl")) {
            System.out.println("Invoked 'testControl': " + this);
        } else {
            controlResults.setError("Unknown operation name: " + name);
        }
        return controlResults;
    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl()).append(",");
        str.append("plugin-config=[").append(getPluginConfigurationString()).append(']'); // do not append ,
        return str.toString();
    }

    private String getPluginConfigurationString() {
        String results = "";
        Configuration config = this.context.getPluginConfiguration();
        for (PropertySimple prop : config.getSimpleProperties().values()) {
            if (results.length() > 0) {
                results += ", ";
            }
            results = results + prop.getName() + "=" + prop.getStringValue();
        }
        return results;
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

        public boolean visit(ZipEntry entry, ZipInputStream stream) throws Exception {
            if (this.recipeName.equalsIgnoreCase(entry.getName())) {
                // this should be safe downcast, recipes are not that big
                ByteArrayOutputStream out = new ByteArrayOutputStream((int) entry.getSize());
                StreamUtil.copy(stream, out, false);
                this.recipe = new String(out.toByteArray());
                out = null; // no need for this anymore, help out GC
                this.results = this.facet.parseRecipe(this.recipe);
                return false; // whether we parsed it or not, we found the file we are looking for so stop walking
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

        public BundleFileVisitor(String bundleName, Set<String> bundleFileNames) throws IOException {
            this.bundleFileNames = bundleFileNames;
            this.bundleFiles = new HashMap<String, File>(bundleFileNames.size());
            this.tmpDir = FileUtil.createTempDirectory("file-template-bundle", ".dir", null);
        }

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
}
