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
import java.io.PrintWriter;
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
import org.rhq.bundle.ant.DeploymentPhase;
import org.rhq.bundle.ant.InvalidBuildFileException;
import org.rhq.bundle.ant.LoggerAntBuildListener;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleVersion;
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
import org.rhq.core.util.updater.Deployer;
import org.rhq.core.util.updater.DeploymentsMetadata;

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
                // add them as Ant properties so the Ant script can get their values.
                Properties antProps = createAntProperties(request);
                // TODO: Eventually the phase to be executed should be passed in by the PC when it calls us.
                // TODO: Invoke STOP phase.
                antProps.setProperty(DeployPropertyNames.DEPLOY_PHASE, "INSTALL");
                // TODO: Invoke START phase.

                List<BuildListener> buildListeners = new ArrayList();
                LoggerAntBuildListener logger = new LoggerAntBuildListener(null, logFileOutput, Project.MSG_DEBUG);
                buildListeners.add(logger);
                DeploymentAuditorBuildListener auditor = new DeploymentAuditorBuildListener(request
                    .getBundleManagerProvider(), resourceDeployment);
                buildListeners.add(auditor);

                // Parse and execute the Ant script.
                executeDeploymentPhase(recipeFile, antProps, buildListeners,
                        DeploymentPhase.STOP);
                String deployDirString = bundleDeployment.getDestination().getDeployDir();
                File deployDir = new File(deployDirString);
                DeploymentsMetadata deployMetadata = new DeploymentsMetadata(deployDir);
                DeploymentPhase installPhase = (deployMetadata.isManaged()) ? DeploymentPhase.UPGRADE :
                        DeploymentPhase.INSTALL;
                BundleAntProject project = executeDeploymentPhase(recipeFile, antProps, buildListeners,
                        installPhase);
                executeDeploymentPhase(recipeFile, antProps, buildListeners,
                        DeploymentPhase.START);

                // Send the diffs to the Server so it can store them as an entry in the deployment history.
                BundleManagerProvider bundleManagerProvider = request.getBundleManagerProvider();
                DeployDifferences diffs = project.getDeployDifferences();
                bundleManagerProvider.auditDeployment(resourceDeployment, "Deployment Differences", project.getName(),
                    BundleResourceDeploymentHistory.Category.DEPLOY_STEP, null, diffs.toString(), null);
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

    private BundleAntProject executeDeploymentPhase(File recipeFile, Properties antProps, List<BuildListener> buildListeners, DeploymentPhase stop) throws InvalidBuildFileException {
        AntLauncher antLauncher = new AntLauncher();
        BundleAntProject project = antLauncher.executeBundleDeployFile(recipeFile, antProps, buildListeners);
        return project;
    }

    private Properties createAntProperties(BundleDeployRequest request) {
        Properties antProps = new Properties();

        BundleResourceDeployment resourceDeployment = request.getResourceDeployment();
        BundleDeployment bundleDeployment = resourceDeployment.getBundleDeployment();
        String deployDir = bundleDeployment.getDestination().getDeployDir();
        if (deployDir == null) {
            throw new IllegalStateException("Bundle deployment does not specify install dir: " + bundleDeployment);
        }
        antProps.setProperty(DeployPropertyNames.DEPLOY_DIR, deployDir);

        int deploymentId = bundleDeployment.getId();
        antProps.setProperty(DeployPropertyNames.DEPLOY_ID, Integer.toString(deploymentId));
        antProps.setProperty(DeployPropertyNames.DEPLOY_NAME, bundleDeployment.getName());
        antProps.setProperty(DeployPropertyNames.DEPLOY_REVERT, String.valueOf(request.isRevert()));
        antProps.setProperty(DeployPropertyNames.DEPLOY_CLEAN, String.valueOf(request.isCleanDeployment()));

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
