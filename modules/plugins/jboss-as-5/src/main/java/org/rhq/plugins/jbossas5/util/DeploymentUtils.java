/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5.util;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.profileservice.spi.DeploymentOption;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent;

/**
 * @author Ian Springer
 */
public class DeploymentUtils {
    private static final Log LOG = LogFactory.getLog(DeploymentUtils.class);

    public static boolean hasCorrectExtension(String archiveFileName, ResourceType resourceType) {
        Configuration defaultPluginConfig = ResourceTypeUtils.getDefaultPluginConfiguration(resourceType);
        String expectedExtension = defaultPluginConfig.getSimple(AbstractManagedDeploymentComponent.EXTENSION_PROPERTY)
            .getStringValue();
        if (expectedExtension == null)
            throw new IllegalStateException("No value was defined for the required '"
                + AbstractManagedDeploymentComponent.EXTENSION_PROPERTY + "' plugin config prop for " + resourceType);
        int lastPeriod = archiveFileName.lastIndexOf(".");
        String extension = (lastPeriod != -1) ? archiveFileName.substring(lastPeriod + 1) : null;
        // Use File.equals() to compare the extensions so case-sensitivity is correct for this platform.
        return (extension != null && new File(extension).equals(new File(expectedExtension)));
    }

    /**
     * Deploys (i.e. distributes then starts) the specified archive file.
     *
     * @param deploymentManager
     * @param archiveFile
     * @param deployExploded
     * 
     * @return
     *
     * @throws Exception if an unrecoverable error occurred during distribution or starting
     */
    public static DeploymentStatus deployArchive(DeploymentManager deploymentManager, File archiveFile,
        boolean deployExploded) throws Exception {
        //if (deployDirectory == null)
        //    throw new IllegalArgumentException("Deploy directory is null.");
        String archiveFileName = archiveFile.getName();
        LOG.debug("Deploying '" + archiveFileName + "' (deployExploded=" + deployExploded + ")...");
        URL contentURL = archiveFile.toURI().toURL();
        //File deployLocation = new File(deployDirectory, archiveFileName);
        //boolean copyContent = !deployLocation.equals(archiveFile);
        List<DeploymentOption> deploymentOptions = new ArrayList<DeploymentOption>();
        if (deployExploded) {
            deploymentOptions.add(DeploymentOption.Explode);
        }
        DeploymentProgress progress = deploymentManager.distribute(archiveFileName, contentURL, deploymentOptions
            .toArray(new DeploymentOption[deploymentOptions.size()]));
        DeploymentStatus distributeStatus = run(progress);
        if (distributeStatus.isFailed()) {
            return distributeStatus;
        }

        // Now that we've successfully distributed the deployment, we need to start it.
        String[] deploymentNames = progress.getDeploymentID().getRepositoryNames();
        progress = deploymentManager.start(deploymentNames);
        DeploymentStatus startStatus = run(progress);
        if (startStatus.isFailed()) {
            LOG.error("Failed to start deployment " + Arrays.asList(deploymentNames)
                + " during initial deployment of '" + archiveFileName + "'. Backing out the deployment...", startStatus
                .getFailure());
            // If start failed, the app is invalid, so back out the deployment.
            progress = deploymentManager.remove(deploymentNames);
            DeploymentStatus removeStatus = run(progress);
            if (removeStatus.isFailed()) {
                throw new Exception("Failed to remove deployment " + Arrays.asList(deploymentNames)
                    + " after start failure.", removeStatus.getFailure());
            }
        }
        return startStatus;
    }

    public static DeploymentStatus run(DeploymentProgress progress) throws Exception {
        progress.run();
        return progress.getDeploymentStatus();
    }

    private DeploymentUtils() {
    }
}
