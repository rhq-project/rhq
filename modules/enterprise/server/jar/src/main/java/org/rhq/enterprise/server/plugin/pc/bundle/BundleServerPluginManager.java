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

import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.enterprise.server.bundle.BundleDistributionInfo;
import org.rhq.enterprise.server.bundle.RecipeParseResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle.BundlePluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle.BundleType;

/**
 * This loads in all bundle server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author John Mazzitelli
 */
public class BundleServerPluginManager extends ServerPluginManager {
    public BundleServerPluginManager(BundleServerPluginContainer pc) {
        super(pc);
    }

    // TODO override methods like initialize, shutdown, loadPlugin, etc. for custom bundle functionality

    @Override
    public void initialize() throws Exception {
        super.initialize();
    }

    @Override
    protected void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        if (enabled) {
            // validate some things about this plugin that are specific for bundle functionality

            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();
            BundleType bt = descriptor.getBundle();
            if (bt == null || bt.getType() == null || bt.getType().length() == 0) {
                // if the xml parser did its job, this will probably never happen, but just in case, make sure there is
                // a non-null, valid bundle type name - we have other code that expects this to be true
                throw new Exception("The bundle plugin [" + env.getPluginKey().getPluginName()
                    + "] did not specify a valid bundle type in its descriptor");
            }

            ServerPluginComponent component = createServerPluginComponent(env);
            if (!(component instanceof BundleServerPluginFacet)) {
                throw new Exception("The bundle plugin [" + env.getPluginKey().getPluginName()
                    + "] has an invalid component [" + component + "]. It does not implement ["
                    + BundleServerPluginFacet.class + "]");
            }
        }

        super.loadPlugin(env, enabled);
    }

    /**
     * Given the {@link BundleType#getName() name of a bundle type}, this will parse the given recipe by asking the
     * bundle plugin that can parse a recipe of that bundle type.
     * 
     * @param bundleTypeName essentially identifies the kind of recipe that is to be parsed
     * @param recipe the recipe to parse
     *
     * @return the results of the parse
     * 
     * @throws Exception if the recipe could not be parsed successfully
     */
    public RecipeParseResults parseRecipe(String bundleTypeName, String recipe) throws Exception {

        if (bundleTypeName == null) {
            throw new IllegalArgumentException("bundleTypeName == null");
        }
        if (recipe == null) {
            throw new IllegalArgumentException("recipe == null");
        }

        // find the plugin environment for the bundle plugin of the given type
        ServerPluginEnvironment pluginEnv = null;
        for (ServerPluginEnvironment env : getPluginEnvironments()) {
            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();
            if (bundleTypeName.equals(descriptor.getBundle().getType())) {
                pluginEnv = env;
                break;
            }
        }

        if (pluginEnv == null) {
            throw new IllegalArgumentException("Bundle type [" + bundleTypeName + "] is not known to the system");
        }

        // get the facet and call the parse method in the appropriate classloader
        String pluginName = pluginEnv.getPluginKey().getPluginName();
        ServerPluginComponent component = getServerPluginComponent(pluginName);
        BundleServerPluginFacet facet = (BundleServerPluginFacet) component; // we know this cast will work because our loadPlugin ensured so
        getLog().debug("Bundle server plugin [" + pluginName + "] is parsing a bundle recipe");
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginEnv.getPluginClassLoader());
            RecipeParseResults results = facet.parseRecipe(recipe);
            ensureDisplayNameIsSet(results);
            return results;
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
        }
    }

    /**
     * Given just a recipe, this will attempt to parse the given recipe by asking all the
     * bundle plugins to see if any can parse it successfully. If the recipe cannot be
     * parsed by any plugin, an exception is thrown, otherwise, results are returned.
     * 
     * @param recipe the recipe to parse
     *
     * @return the results of the parse, which also includes the bundle type
     * 
     * @throws Exception if the recipe could not be parsed successfully
     */
    public BundleDistributionInfo parseRecipe(String recipe) throws Exception {

        if (recipe == null) {
            throw new IllegalArgumentException("recipe == null");
        }

        BundleDistributionInfo info = null;

        for (ServerPluginEnvironment env : getPluginEnvironments()) {
            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();

            // get the facet and see if this plugin can deal with the recipe
            String pluginName = env.getPluginKey().getPluginName();
            ServerPluginComponent component = getServerPluginComponent(pluginName);
            BundleServerPluginFacet facet = (BundleServerPluginFacet) component; // we know this cast will work because our loadPlugin ensured so
            getLog().debug("Bundle server plugin [" + pluginName + "] is parsing a recipe");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                try {
                    RecipeParseResults results = facet.parseRecipe(recipe);
                    info = new BundleDistributionInfo(recipe, results, null);
                    info.setBundleTypeName(descriptor.getBundle().getType());
                    break;
                } catch (UnknownRecipeException ure) {
                    // the recipe is not a type that the plugin can handle, go on to the next
                    info = null;
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }

        if (null == info) {
            throw new IllegalArgumentException("Invalid recipe not recognized by any deployed server bundle plugin.");
        }

        ensureDisplayNameIsSet(info.getRecipeParseResults());

        return info;
    }

    /**
     * Given an bundle distribution file, this will find the appropriate server side plugin that can process it
     * and will ask that plugin to crack open the bundle distribution file and return information about it.
     * 
     * An bundle distribution file is a zip file that contains a recipe and 0, 1 or more bundle files.
     * 
     * @param distributionFile
     * @return the information gleened by cracking open the bundle distribution file and examining its contents
     * @throws Exception if the bundle distribution file could not be processed successfully
     */
    public BundleDistributionInfo processBundleDistributionFile(File distributionFile) throws Exception {

        if (null == distributionFile) {
            throw new IllegalArgumentException("bundleDistributionFile == null");
        }

        // find the bundle plugin that can handle this distribution and get the distro info
        BundleDistributionInfo info = null;

        for (ServerPluginEnvironment env : getPluginEnvironments()) {
            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();

            // get the facet and see if this plugin can deal with the distro
            String pluginName = env.getPluginKey().getPluginName();
            ServerPluginComponent component = getServerPluginComponent(pluginName);
            BundleServerPluginFacet facet = (BundleServerPluginFacet) component; // we know this cast will work because our loadPlugin ensured so
            getLog().debug(
                "Bundle server plugin [" + pluginName + "] is parsing a distribution file [" + distributionFile + "]");
            ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(env.getPluginClassLoader());
                try {
                    info = facet.processBundleDistributionFile(distributionFile);
                    info.setBundleTypeName(descriptor.getBundle().getType());
                    break;
                } catch (UnknownRecipeException ure) {
                    // the recipe is not a type that the plugin can handle, go on to the next
                    info = null;
                }
            } finally {
                Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            }
        }
        if (null == info) {
            throw new IllegalArgumentException(
                "Invalid bundle distribution file. BundleType/Recipe not recognized by any deployed server bundle plugin.");
        }

        ensureDisplayNameIsSet(info.getRecipeParseResults());

        return info;
    }

    private void ensureDisplayNameIsSet(RecipeParseResults recipeParseResults) {
        if (recipeParseResults != null && recipeParseResults.getConfigurationDefinition() != null) {
            ConfigurationDefinition configDef = recipeParseResults.getConfigurationDefinition();
            for (PropertyDefinition propDef : configDef.getPropertyDefinitions().values()) {
                if (propDef instanceof PropertyDefinitionSimple) {
                    if (propDef.getDisplayName() == null) {
                        propDef.setDisplayName(propDef.getName());
                    }
                }
            }
        }
        return;
    }
}