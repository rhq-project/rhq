/*
 * Jopr Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
import java.net.MalformedURLException;
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
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent;

/**
 * A set of utility methods for deploying applications.
 *
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
     * @throws Exception if the deployment fails for any reason
     */
    public static String[] deployArchive(DeploymentManager deploymentManager, File archiveFile, boolean deployExploded)
        throws Exception {
        String archiveFileName = archiveFile.getName();
        LOG.debug("Deploying '" + archiveFileName + "' (deployExploded=" + deployExploded + ")...");
        URL contentURL;
        try {
            contentURL = archiveFile.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Failed to convert archive file path '" + archiveFile + "' to URL.", e);
        }
        List<DeploymentOption> deploymentOptions = new ArrayList<DeploymentOption>();
        if (deployExploded) {
            deploymentOptions.add(DeploymentOption.Explode);
        }
        DeploymentProgress progress = null;
        DeploymentStatus distributeStatus;
        Exception distributeFailure = null;
        try {
            progress = deploymentManager.distribute(archiveFileName, contentURL,
                deploymentOptions.toArray(new DeploymentOption[deploymentOptions.size()]));
            distributeStatus = run(progress);
            if (distributeStatus.isFailed()) {
                distributeFailure = (distributeStatus.getFailure() != null) ? distributeStatus.getFailure()
                    : new Exception("Distribute failed for unknown reason.");
            }
        } catch (Exception e) {
            distributeFailure = e;
        }
        if (distributeFailure != null) {
            throw new Exception("Failed to distribute '" + contentURL + "' to '" + archiveFileName + "' - cause: "
                + ThrowableUtil.getAllMessages(distributeFailure));
        }

        // Now that we've successfully distributed the deployment, try to start it.
        String[] deploymentNames = progress.getDeploymentID().getRepositoryNames();
        DeploymentStatus startStatus;
        Exception startFailure = null;
        try {
            progress = deploymentManager.start(deploymentNames);
            startStatus = run(progress);
            if (startStatus.isFailed()) {
                startFailure = (startStatus.getFailure() != null) ? startStatus.getFailure() : new Exception(
                    "Start failed for unknown reason.");
            }
        } catch (Exception e) {
            startFailure = e;
        }
        if (startFailure != null) {
            // There are times when the user expects the app to fail to start, so just log a warning.
            LOG.warn("Failed to start deployment " + Arrays.asList(deploymentNames) + " during deployment of '"
                + archiveFileName + "'.", startFailure);
        }
        // If we made it this far, the deployment (distribution+start) was successful.
        return deploymentNames;
    }

    public static DeploymentStatus run(DeploymentProgress progress) {
        progress.run();
        return progress.getDeploymentStatus();
    }

    private DeploymentUtils() {
    }
}
