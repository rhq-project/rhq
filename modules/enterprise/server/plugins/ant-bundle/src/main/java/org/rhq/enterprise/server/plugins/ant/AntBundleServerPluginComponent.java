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

package org.rhq.enterprise.server.plugins.ant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.bundle.ant.BundleAntProject;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.bundle.RecipeParseResults;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginFacet;

/**
 * A bundle server-side plugin component that the server uses to process ant-based bundles.
 * 
 * @author John Mazzitelli
 */
public class AntBundleServerPluginComponent implements ServerPluginComponent, BundleServerPluginFacet {

    private final Log log = LogFactory.getLog(AntBundleServerPluginComponent.class);

    private ServerPluginContext context;
    private File tmpDirectory;

    public void initialize(ServerPluginContext context) throws Exception {
        this.context = context;
        this.tmpDirectory = new File(this.context.getTemporaryDirectory(), "ant-bundle-server-plugin");
        this.tmpDirectory.mkdirs();
        if (!this.tmpDirectory.exists() || !this.tmpDirectory.isDirectory()) {
            throw new Exception("Failed to create tmp dir [" + this.tmpDirectory + "] - cannot process ant bundles");
        }
        log.debug("The ant bundle plugin has been initialized: " + this);
    }

    public void start() {
        log.debug("The ant bundle plugin has started: " + this);
    }

    public void stop() {
        log.debug("The ant bundle plugin has stopped: " + this);
    }

    public void shutdown() {
        log.debug("The ant bundle plugin has been shut down: " + this);
    }

    public RecipeParseResults parseRecipe(String recipe) throws Exception {

        Map<String, String> bundleFiles = null;
        ConfigurationDefinition configDef = null;
        RecipeParseResults results;

        File recipeFile = File.createTempFile("ant-bundle-recipe", ".xml", this.tmpDirectory);
        File logFile = File.createTempFile("ant-bundle-recipe", ".log", this.tmpDirectory);
        try {
            // store the recipe in the tmp recipe file
            ByteArrayInputStream in = new ByteArrayInputStream(recipe.getBytes());
            FileOutputStream out = new FileOutputStream(recipeFile);
            StreamUtil.copy(in, out);

            // parse, but do not execute, the ant script
            AntLauncher antLauncher = new AntLauncher();
            BundleAntProject project = antLauncher.startAnt(recipeFile, null, null, null, logFile, false, false);

            // obtain the parse results
            bundleFiles = project.getBundleFiles();
            configDef = project.getConfigurationDefinition();
        } catch (Throwable t) {
            if (log.isDebugEnabled()) {
                try {
                    log.debug(new String(StreamUtil.slurp(new FileInputStream(logFile))));
                } catch (Exception e) {
                }
            }
            throw new Exception("Failed to parse the bundle ANT script", t);
        } finally {
            recipeFile.delete();
            logFile.delete();
        }

        // TODO: FOR IPS!
        results = new RecipeParseResults(null, configDef, new HashSet<String>(bundleFiles.values()));
        return results;

    }

    @Override
    public String toString() {
        if (this.context == null) {
            return "<no context>";
        }

        StringBuilder str = new StringBuilder();
        str.append("plugin-key=").append(this.context.getPluginEnvironment().getPluginKey()).append(",");
        str.append("plugin-url=").append(this.context.getPluginEnvironment().getPluginUrl());
        return str.toString();
    }
}
