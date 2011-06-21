/*
 * RHQ Management Platform
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
package org.rhq.plugins.jbossas5.deploy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.managed.api.DeploymentState;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.NoSuchDeploymentException;
import org.jboss.profileservice.spi.ProfileKey;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ConversionUtils;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * Abstract base class capturing the common deploy functionality for
 * embedded and remote scenarios.
 * 
 * @author Lukas Krejci
 */
public abstract class AbstractDeployer implements Deployer {
    private static final ProfileKey FARM_PROFILE_KEY = new ProfileKey("farm");
    private static final ProfileKey APPLICATIONS_PROFILE_KEY = new ProfileKey("applications");
    public static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";

    private final Log log = LogFactory.getLog(this.getClass());

    private ProfileServiceConnection profileServiceConnection;

    protected AbstractDeployer(ProfileServiceConnection profileService) {
        this.profileServiceConnection = profileService;
    }

    public void deploy(CreateResourceReport createResourceReport, ResourceType resourceType) {
        createResourceReport.setStatus(null);
        File archiveFile = null;

        try {
            ResourcePackageDetails details = createResourceReport.getPackageDetails();
            PackageDetailsKey key = details.getKey();

            archiveFile = prepareArchive(key, resourceType);

            String archiveName = key.getName();

            if (!DeploymentUtils.hasCorrectExtension(archiveName, resourceType)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("Incorrect extension specified on filename [" + archiveName + "]");
                return;
            }

            abortIfApplicationAlreadyDeployed(resourceType, archiveFile);

            Configuration deployTimeConfig = details.getDeploymentTimeConfiguration();
            @SuppressWarnings( { "ConstantConditions" })
            boolean deployExploded = deployTimeConfig.getSimple("deployExploded").getBooleanValue();

            DeploymentManager deploymentManager = this.profileServiceConnection.getDeploymentManager();
            boolean deployFarmed = deployTimeConfig.getSimple("deployFarmed").getBooleanValue();
            if (deployFarmed) {
                Collection<ProfileKey> profileKeys = deploymentManager.getProfiles();
                boolean farmSupported = false;
                for (ProfileKey profileKey : profileKeys) {
                    if (profileKey.getName().equals(FARM_PROFILE_KEY.getName())) {
                        farmSupported = true;
                        break;
                    }
                }
                if (!farmSupported) {
                    throw new IllegalStateException("This application server instance is not a node in a cluster, "
                        + "so it does not support farmed deployments. Supported deployment profiles are " + profileKeys
                        + ".");
                }
                if (deployExploded) {
                    throw new IllegalArgumentException(
                        "Deploying farmed applications in exploded form is not supported by the Profile Service.");
                }
                deploymentManager.loadProfile(FARM_PROFILE_KEY);
            }

            String[] deployedArchives;
            try {
                deployedArchives = DeploymentUtils.deployArchive(deploymentManager, archiveFile, deployExploded);
            } finally {
                // Make sure to switch back to the 'applications' profile if we switched to the 'farm' profile above.
                if (deployFarmed) {
                    deploymentManager.loadProfile(APPLICATIONS_PROFILE_KEY);
                }
            }

            //if deployed exploded, we need to store the sha of source package for correct versioning
            if (deployExploded) {
                String shaString = new MessageDigestGenerator(MessageDigestGenerator.SHA_256)
                    .getDigestString(archiveFile);
                String deploymentName = deployTimeConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
                URI deployePackageURI = URI.create(deploymentName);
                // e.g.: foo.war
                String path = deployePackageURI.getPath();
                File location = new File(path);
                //We've located the deployed
                if ((location != null) && (location.isDirectory())) {
                    File manifestFile = new File(location, "META-INF/MANIFEST.MF");
                    Manifest manifest;
                    if (manifestFile.exists()) {
                        FileInputStream inputStream = new FileInputStream(manifestFile);
                        manifest = new Manifest(inputStream);
                        inputStream.close();
                    } else {
                        manifest = new Manifest();
                    }
                    Attributes attribs = manifest.getMainAttributes();
                    attribs.putValue("RHQ-Sha256", shaString);
                    FileOutputStream outputStream = new FileOutputStream(manifestFile);
                    manifest.write(outputStream);
                    outputStream.close();
                }
            }

            ManagementView managementView = this.profileServiceConnection.getManagementView();
            managementView.load();
            for (String deployedArchive : deployedArchives) {
                ManagedDeployment managedDeployment;
                try {
                    managedDeployment = managementView.getDeployment(deployedArchive);
                } catch (NoSuchDeploymentException e) {
                    log.error("Failed to find managed deployment '" + deployedArchive + "' after deploying '"
                            + archiveName + "'.");
                    continue;
                }
                DeploymentState state = managedDeployment.getDeploymentState();
                if (state != DeploymentState.STARTED) {
                    // The app failed to start - do not consider this a FAILURE, since it was at least deployed
                    // successfully. However, set the status to INVALID_ARTIFACT and set an error message, so
                    // the user is informed of the condition.
                    createResourceReport.setStatus(CreateResourceStatus.INVALID_ARTIFACT);
                    createResourceReport.setErrorMessage("Failed to start application '" + deployedArchive + "' after deploying it.");
                    break;
                }
            }

            createResourceReport.setResourceName(archiveName);
            createResourceReport.setResourceKey(archiveName);
            if (createResourceReport.getStatus() == null) {
                // Deployment was 100% successful, including starting the app.
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            }
        } catch (Throwable t) {
            log.error("Error deploying application for request [" + createResourceReport + "].", t);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
            createResourceReport.setException(t);
        } finally {
            if (archiveFile != null) {
                destroyArchive(archiveFile);
            }
        }
    }

    protected Log getLog() {
        return log;
    }

    protected ProfileServiceConnection getProfileServiceConnection() {
        return profileServiceConnection;
    }

    protected abstract File prepareArchive(PackageDetailsKey key, ResourceType resourceType);

    protected abstract void destroyArchive(File archive);

    private void abortIfApplicationAlreadyDeployed(ResourceType resourceType, File archiveFile) throws Exception {
        String archiveFileName = archiveFile.getName();
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();
        ManagementView managementView = profileServiceConnection.getManagementView();
        managementView.load();
        Set<ManagedDeployment> managedDeployments = managementView.getDeploymentsForType(deploymentTypeString);
        for (ManagedDeployment managedDeployment : managedDeployments) {
            if (managedDeployment.getSimpleName().equals(archiveFileName))
                throw new IllegalArgumentException("An application named '" + archiveFileName
                    + "' is already deployed.");
        }
    }
}
