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

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.rhq.bundle.ant.AntLauncher;
import org.rhq.bundle.ant.BundleAntProject;
import org.rhq.bundle.ant.DeployPropertyNames;
import org.rhq.bundle.ant.LoggerAntBuildListener;
import org.rhq.core.domain.bundle.*;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.bundle.BundleDeployRequest;
import org.rhq.core.pluginapi.bundle.BundleDeployResult;
import org.rhq.core.pluginapi.bundle.BundleFacet;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.core.util.updater.DeployDifferences;

/**
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class AntBundlePluginComponent implements ResourceComponent, BundleFacet {

    private final Log log = LogFactory.getLog(AntBundlePluginComponent.class);

    private ResourceContext resourceContext;

    private File tmpDirectory;

    public void start(ResourceContext context) throws Exception {
        this.resourceContext = context;
        this.tmpDirectory = new File(context.getTemporaryDirectory(), "ant-bundle-plugin");
        this.tmpDirectory.mkdirs();
        if (!this.tmpDirectory.exists() || !this.tmpDirectory.isDirectory()) {
            throw new Exception("Failed to create tmp dir [" + this.tmpDirectory + "] - cannot process Ant bundles.");
        }
        return;
    }

    public void stop() {
        return;
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
            PrintWriter logFileOutput = null;
            try {
                // Open the log file for writing.
                logFileOutput = new PrintWriter(new FileOutputStream(logFile, true));

                // Store the recipe in the tmp recipe file.
                ByteArrayInputStream in = new ByteArrayInputStream(recipe.getBytes());
                FileOutputStream out = new FileOutputStream(recipeFile);
                StreamUtil.copy(in, out);

                // Get the bundle's configuration values and the global system facts and
                // add them as Ant properties so the ant script can get their values.
                Properties antProps = createAntProperties(bundleDeployment);

                List<BuildListener> buildListeners = new ArrayList();
                LoggerAntBuildListener logger = new LoggerAntBuildListener(null, logFileOutput, Project.MSG_DEBUG);
                buildListeners.add(logger);
                DeploymentAuditorBuildListener auditor = new DeploymentAuditorBuildListener(
                        request.getBundleManagerProvider(), resourceDeployment);
                buildListeners.add(auditor);

                // Parse & execute the Ant script.
                AntLauncher antLauncher = new AntLauncher();
                BundleAntProject project = antLauncher.executeBundleDeployFile(recipeFile, null, antProps,
                        buildListeners);

                // Send the diffs to the Server so it can store them as an entry in the deployment history.
                BundleManagerProvider bundleManagerProvider = request.getBundleManagerProvider();
                DeployDifferences diffs = project.getDeployDifferences();
                bundleManagerProvider.auditDeployment(resourceDeployment, BundleDeploymentAction.DEPLOYMENT_STEP,
                        BundleDeploymentStatus.SUCCESS, diffs.toString());
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    try {
                        log.debug(new String(StreamUtil.slurp(new FileInputStream(logFile))));
                    } catch (Exception e) {
                    }
                }
                throw new Exception("Failed to execute the bundle Ant script", t);
            } finally {
                if (logFileOutput != null) {
                    logFileOutput.close();
                }
                recipeFile.delete();
                logFile.delete();
            }

        } catch (Throwable t) {
            log.error("Failed to deploy bundle [" + request + "]", t);
            result.setErrorMessage(t);
        }
        return result;
    }

    private Properties createAntProperties(BundleDeployment bundleDeployment) {
        Properties antProps = new Properties();

        String installDir = bundleDeployment.getInstallDir();
        if (installDir == null) {
            throw new IllegalStateException("Bundle deployment does not specify install dir: "
                + bundleDeployment);
        }
        antProps.setProperty(DeployPropertyNames.DEPLOY_DIR, installDir);

        int deploymentId = bundleDeployment.getId();
        antProps.setProperty(DeployPropertyNames.DEPLOY_ID, Integer.toString(deploymentId));

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
        return antProps;
    }
}
