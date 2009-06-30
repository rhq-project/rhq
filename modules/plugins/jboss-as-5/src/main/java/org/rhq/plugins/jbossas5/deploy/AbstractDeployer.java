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
package org.rhq.plugins.jbossas5.deploy;

import java.io.File;
import java.util.Set;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.profileservice.spi.ProfileService;
import org.jboss.profileservice.spi.ProfileKey;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
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

    private final Log log = LogFactory.getLog(this.getClass());

    private ProfileService profileService;

    protected AbstractDeployer(ProfileService profileService) {
        this.profileService = profileService;
    }

    public void deploy(CreateResourceReport createResourceReport, ResourceType resourceType) {
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

            DeploymentManager deploymentManager = this.profileService.getDeploymentManager();
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
                            + "so it does not support farmed deployments. Supported deployment profiles are "
                            + profileKeys + ".");
                }
                if (deployExploded) {
                    throw new IllegalArgumentException("Deploying farmed applications in exploded form is not supported by the Profile Service.");
                }
                deploymentManager.loadProfile(FARM_PROFILE_KEY);
            }

            DeploymentStatus status;
            try {
                status = DeploymentUtils.deployArchive(deploymentManager, archiveFile, deployExploded);
            }
            finally {
                // Make sure to switch back to the 'applications' profile if we switched to the 'farm' profile above.
                if (deployFarmed) {
                    deploymentManager.loadProfile(APPLICATIONS_PROFILE_KEY);
                }
            }

            if (status.getState() == DeploymentStatus.StateType.COMPLETED) {
                createResourceReport.setResourceName(archiveName);
                createResourceReport.setResourceKey(archiveName);
                createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
            } else {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage(status.getMessage());
                // noinspection ThrowableResultOfMethodCallIgnored
                createResourceReport.setException(status.getFailure());
            }
        } catch (Throwable t) {
            log.error("Error deploying application for report: " + createResourceReport, t);
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

    protected ProfileService getProfileService() {
        return profileService;
    }

    protected abstract File prepareArchive(PackageDetailsKey key, ResourceType resourceType);

    protected abstract void destroyArchive(File archive);

    private void abortIfApplicationAlreadyDeployed(ResourceType resourceType, File archiveFile) throws Exception {
        String archiveFileName = archiveFile.getName();
        KnownDeploymentTypes deploymentType = ConversionUtils.getDeploymentType(resourceType);
        String deploymentTypeString = deploymentType.getType();
        ManagementView managementView = profileService.getViewManager();
        managementView.load();
        Set<ManagedDeployment> managedDeployments = managementView.getDeploymentsForType(deploymentTypeString);
        for (ManagedDeployment managedDeployment : managedDeployments) {
            if (managedDeployment.getSimpleName().equals(archiveFileName))
                throw new IllegalArgumentException("An application named '" + archiveFileName
                    + "' is already deployed.");
        }
    }

}
