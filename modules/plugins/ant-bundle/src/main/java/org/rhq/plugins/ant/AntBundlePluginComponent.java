/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.plugins.ant;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.bundle.ant.BundleAntProject;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class AntBundlePluginComponent implements ResourceComponent, BundleFacet {

    private final Log log = LogFactory.getLog(AntBundlePluginComponent.class);

    private ResourceContext resourceContext;

    private File tmpDirectory;

    public void start(ResourceContext context) throws Exception {
        resourceContext = context;
        this.tmpDirectory = new File(context.getTemporaryDirectory(), "ant-bundle-plugin");
        this.tmpDirectory.mkdirs();
        if (!this.tmpDirectory.exists() || !this.tmpDirectory.isDirectory()) {
            throw new Exception("Failed to create tmp dir [" + this.tmpDirectory + "] - cannot process Ant bundles.");
        }
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public BundleDeployResult deployBundle(BundleDeployRequest request) {
        BundleDeployResult result = new BundleDeployResult();
        try {
            BundleResourceDeployment resourceDeployment = request.getResourceDeployment();
            BundleDeployment bundleDeployment = resourceDeployment.getBundleDeployment();
            BundleVersion bundleVersion = bundleDeployment.getBundleVersion();

            String recipe = bundleVersion.getRecipe();
            File recipeFile = File.createTempFile("ant-bundle-recipe", ".xml", request.getBundleFilesLocation());
            File logFile = File.createTempFile("ant-bundle-recipe", ".log", this.tmpDirectory);
            try {
                // store the recipe in the tmp recipe file
                ByteArrayInputStream in = new ByteArrayInputStream(recipe.getBytes());
                FileOutputStream out = new FileOutputStream(recipeFile);
                StreamUtil.copy(in, out);

                // get the bundle's configuration values and the global system facts and
                // add them as Ant properties so the ant script can get their values
                Properties antProps = new Properties();

                String installDir = bundleDeployment.getInstallDir();
                if (installDir == null) {
                    throw new IllegalStateException("Bundle deployment does not specify install dir: " + bundleDeployment);
                }
                antProps.setProperty(AntLauncher.DEPLOY_DIR_PROP, installDir);

                Map<String, String> sysFacts = SystemInfoFactory.fetchTemplateEngine().getTokens();
                for (Map.Entry<String, String> fact : sysFacts.entrySet()) {
                    antProps.setProperty(fact.getKey(), fact.getValue());
                }

                Configuration config = bundleDeployment.getConfiguration();
                if (config != null) {
                    Map<String, Property> allProperties = config.getAllProperties();
                    for (Map.Entry<String, Property> entry : allProperties.entrySet()) {
                        String name = entry.getKey();
                        Property prop = entry.getValue();
                        String value;
                        if (prop instanceof PropertySimple) {
                            value = ((PropertySimple) prop).getStringValue();
                        } else {
                            // for now, just skip all property lists and maps, just assume we are getting simples
                            continue;
                        }
                        antProps.setProperty(name, value);
                    }
                }

                // parse & execute, the ant script
                AntLauncher antLauncher = new AntLauncher();
                BundleAntProject project = antLauncher.startAnt(recipeFile, null, null, antProps, logFile, true, true);
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug(new String(StreamUtil.slurp(new FileInputStream(logFile))));
                    } catch (Exception e) {
                    }
                }
                throw new Exception("Failed to parse the bundle Ant script", t);
            } finally {
                recipeFile.delete();
                logFile.delete();
            }

        } catch (Throwable t) {
            log.error("Failed to deploy bundle [" + request + "]", t);
            result.setErrorMessage(t);
        }
        return result;
    }
}
