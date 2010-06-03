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
package org.rhq.plugins.jbossas5;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.version.PackageVersions;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.JarContentFileInfo;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * A resource component for managing a standalone/top-level Profile Service managed deployment.
 *  
 * @author Ian Springer
 */
public class StandaloneManagedDeploymentComponent extends AbstractManagedDeploymentComponent implements
    MeasurementFacet, ContentFacet, DeleteResourceFacet {
    private static final String CUSTOM_PATH_TRAIT = "custom.path";
    private static final String CUSTOM_EXPLODED_TRAIT = "custom.exploded";
    public static final String RHQ_SHA256 = "RHQ-Sha256";

    /**
     * Name of the backing package type that will be used when discovering packages. This corresponds to the name of the
     * package type defined in the plugin descriptor. For simplicity, the package type for both EARs and WARs is simply
     * called "file". This is still unique within the context of the parent resource type and lets this class use the
     * same package type name in both cases.
     */
    private static final String PKG_TYPE_FILE = "file";

    /**
     * Architecture string used in describing discovered packages.
     */
    private static final String ARCHITECTURE = "noarch";

    private final Log log = LogFactory.getLog(this.getClass());

    private PackageVersions versions;

    // ------------ MeasurementFacet Implementation ------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> remainingRequests = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            if (metricName.equals(CUSTOM_PATH_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(request, this.deploymentFile.getPath());
                report.addData(trait);
            } else if (metricName.equals(CUSTOM_EXPLODED_TRAIT)) {
                boolean exploded = this.deploymentFile.isDirectory();
                MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                report.addData(trait);
            } else {
                remainingRequests.add(request);
            }
        }
        super.getValues(report, remainingRequests);
    }

    // ------------ ContentFacet implementation -------------

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            if (packageFile.isDirectory()) {
                fileToSend = File.createTempFile("rhq", ".zip");
                ZipUtil.zipFileOrDirectory(packageFile, fileToSend);
            } else
                fileToSend = packageFile;
            return new BufferedInputStream(new FileInputStream(fileToSend));
        } catch (IOException e) {
            throw new RuntimeException("Failed to retrieve package bits for " + packageDetails, e);
        }
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType packageType) {
        if (!this.deploymentFile.exists())
            throw new IllegalStateException("Deployment file '" + this.deploymentFile + "' for "
                + getResourceDescription() + " does not exist.");

        String fileName = this.deploymentFile.getName();
        PackageVersions packageVersions = loadPackageVersions();
        String version = packageVersions.getVersion(fileName);

        JarContentFileInfo fileInfo = new JarContentFileInfo(this.deploymentFile);
        String sha256 = getSHA256(fileInfo);

        // First discovery of this EAR/WAR
        if (version == null) {
            // try to use version from manifest. if not there use the sha256. if that fails default to "0".
            version = fileInfo.getVersion((null == sha256) ? "0" : sha256);
            versions.putVersion(fileName, version);
            versions.saveToDisk();
        }

        // Package name is the deployment's file name (e.g. foo.ear).
        PackageDetailsKey key = new PackageDetailsKey(fileName, version, PKG_TYPE_FILE, ARCHITECTURE);
        ResourcePackageDetails packageDetails = new ResourcePackageDetails(key);
        packageDetails.setFileName(fileName);
        packageDetails.setLocation(this.deploymentFile.getPath());
        if (!this.deploymentFile.isDirectory())
            packageDetails.setFileSize(this.deploymentFile.length());
        packageDetails.setFileCreatedDate(null); // TODO: get created date via SIGAR
        packageDetails.setSHA256(sha256);
        packageDetails.setInstallationTimestamp(Long.valueOf(System.currentTimeMillis()));

        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();
        packages.add(packageDetails);

        return packages;
    }

    // TODO: if needed we can speed this up by looking in the ResourceContainer's installedPackage
    // list for previously discovered packages. If there use the sha256 from that record. We'd have to
    // get access to that info by adding access in org.rhq.core.pluginapi.content.ContentServices
    private String getSHA256(JarContentFileInfo fileInfo) {

        String sha256 = null;

        try {
            sha256 = fileInfo.getAttributeValue(RHQ_SHA256, null);
            if (null == sha256) {
                sha256 = new MessageDigestGenerator(MessageDigestGenerator.SHA_256).calcDigestString(fileInfo
                    .getContentFile());
            }
        } catch (IOException iex) {
            //log exception but move on, discovery happens often. No reason to hold up anything.
            if (log.isDebugEnabled()) {
                log.debug("Problem calculating digest of package [" + fileInfo.getContentFile().getPath() + "]."
                    + iex.getMessage());
            }
        }

        return sha256;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove the package backing an EAR/WAR resource.");
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        // Intentional - there are no steps involved in installing an EAR or WAR.
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        String resourceTypeName = getResourceContext().getResourceType().getName();

        // You can only update the one application file referenced by this resource, so punch out if multiple are
        // specified.
        if (packages.size() != 1) {
            log.warn("Request to update " + resourceTypeName + " file contained multiple packages: " + packages);
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Only one " + resourceTypeName + " can be updated at a time.");
            return response;
        }

        ResourcePackageDetails packageDetails = packages.iterator().next();

        log.debug("Updating EAR/WAR file '" + this.deploymentFile + "' using [" + packageDetails + "]...");
        // Find location of existing application.
        if (!this.deploymentFile.exists()) {
            return failApplicationDeployment(
                "Could not find application to update at location: " + this.deploymentFile, packageDetails);
        }

        log.debug("Writing new EAR/WAR bits to temporary file...");
        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(contentServices, packageDetails);
        } catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e,
                packageDetails);
        }
        log.debug("Wrote new EAR/WAR bits to temporary file '" + tempFile + "'.");

        boolean deployExploded = this.deploymentFile.isDirectory();

        // Backup the original app file/dir.
        File tempDir = getResourceContext().getTemporaryDirectory();
        File backupDir = new File(tempDir, "deployBackup");
        File backupOfOriginalFile = new File(backupDir, this.deploymentFile.getName());
        log.debug("Backing up existing EAR/WAR '" + this.deploymentFile + "' to '" + backupOfOriginalFile + "'...");
        try {
            if (backupOfOriginalFile.exists()) {
                FileUtils.forceDelete(backupOfOriginalFile);
            }
            if (this.deploymentFile.isDirectory()) {
                FileUtils.copyDirectory(this.deploymentFile, backupOfOriginalFile, true);
            } else {
                FileUtils.copyFile(this.deploymentFile, backupOfOriginalFile, true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to backup existing " + resourceTypeName + "'" + this.deploymentFile
                + "' to '" + backupOfOriginalFile + "'.");
        }

        // Now stop the original app.
        try {
            DeploymentManager deploymentManager = getConnection().getDeploymentManager();
            DeploymentProgress progress = deploymentManager.stop(this.deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop deployment [" + this.deploymentName + "].", e);
        }

        // And then remove it (this will delete the physical file/dir from the deploy dir).
        try {
            DeploymentManager deploymentManager = getConnection().getDeploymentManager();
            DeploymentProgress progress = deploymentManager.remove(this.deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove deployment [" + this.deploymentName + "].", e);
        }

        // Deploy away!
        log.debug("Deploying '" + tempFile + "'...");
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        try {
            DeploymentUtils.deployArchive(deploymentManager, tempFile, deployExploded);
        } catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            log.debug("Redeploy failed - rolling back to original archive...", e);
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                // Try to delete the new app file, which failed to deploy, if it still exists.
                if (this.deploymentFile.exists()) {
                    try {
                        FileUtils.forceDelete(this.deploymentFile);
                    } catch (IOException e1) {
                        log.debug("Failed to delete application file '" + this.deploymentFile
                            + "' that failed to deploy.", e1);
                    }
                }
                // Now redeploy the original file - this generally should succeed.
                DeploymentUtils.deployArchive(deploymentManager, backupOfOriginalFile, deployExploded);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            } catch (Exception e1) {
                log.debug("Rollback failed!", e1);
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: "
                    + ThrowableUtil.getAllMessages(e1);
            }
            log.info("Failed to update " + resourceTypeName + " file '" + this.deploymentFile + "' using ["
                + packageDetails + "].");
            return failApplicationDeployment(errorMessage, packageDetails);
        }

        // Deploy was successful!
        deleteBackupOfOriginalFile(backupOfOriginalFile);
        persistApplicationVersion(packageDetails, this.deploymentFile);

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(),
            ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        log.debug("Updated " + resourceTypeName + " file '" + this.deploymentFile
            + "' successfully - returning response [" + response + "]...");

        return response;
    }

    // ------------ DeleteResourceFacet implementation -------------

    public void deleteResource() throws Exception {
        log.debug("Deleting " + getResourceDescription() + "...");
        DeploymentManager deploymentManager = getConnection().getDeploymentManager();
        try {
            getManagedDeployment();
        } catch (Exception e) {
            // The deployment no longer exists, so there's nothing for us to do. Someone most likely undeployed it
            // outside of Jopr or EmbJopr, e.g. via the jmx-console or by deleting the app file from the deploy dir.
            log.warn("Cannot delete the deployment [" + this.deploymentName + "], since it no longer exists");
            return;
        }

        log.debug("Stopping deployment [" + this.deploymentName + "]...");
        DeploymentProgress progress = deploymentManager.stop(this.deploymentName);
        DeploymentStatus stopStatus = DeploymentUtils.run(progress);
        if (stopStatus.isFailed()) {
            log.error("Failed to stop deployment '" + this.deploymentName + "'.", stopStatus.getFailure());
            throw new Exception("Failed to stop deployment '" + this.deploymentName + "' - cause: "
                + stopStatus.getFailure());
        }
        log.debug("Removing deployment [" + this.deploymentName + "]...");
        progress = deploymentManager.remove(this.deploymentName);
        DeploymentStatus removeStatus = DeploymentUtils.run(progress);
        if (removeStatus.isFailed()) {
            log.error("Failed to remove deployment '" + this.deploymentName + "'.", removeStatus.getFailure());
            throw new Exception("Failed to remove deployment '" + this.deploymentName + "' - cause: "
                + removeStatus.getFailure());
        }
        ManagementView managementView = getConnection().getManagementView();
        managementView.load();
    }

    /**
     * Creates the necessary transfer objects to report a failed application deployment (update).
     *
     * @param errorMessage   reason the deploy failed
     * @param packageDetails describes the update being made
     * @return response populated to reflect a failure
     */
    private DeployPackagesResponse failApplicationDeployment(String errorMessage, ResourcePackageDetails packageDetails) {
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);

        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(),
            ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);

        response.addPackageResponse(packageResponse);

        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {
        String packageName = appFile.getName();
        log.debug("Persisting application version '" + packageDetails.getVersion() + "' for package '" + packageName
            + "'");
        PackageVersions versions = loadPackageVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        log.debug("Deleting backup of original file '" + backupOfOriginalFile + "'...");
        try {
            FileUtils.forceDelete(backupOfOriginalFile);
        } catch (Exception e) {
            // not critical.
            log.warn("Failed to delete backup of original file: " + backupOfOriginalFile);
        }
    }

    private File writeNewAppBitsToTempFile(ContentServices contentServices, ResourcePackageDetails packageDetails)
        throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir, this.deploymentFile.getName());

        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            long bytesWritten = contentServices.downloadPackageBits(getResourceContext().getContentContext(),
                packageDetails.getKey(), tempOutputStream, true);
            log.debug("Wrote " + bytesWritten + " bytes to '" + tempFile + "'.");
        } catch (IOException e) {
            log.error("Error writing updated application bits to temporary location: " + tempFile, e);
            throw e;
        } finally {
            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException e) {
                    log.error("Error closing temporary output stream", e);
                }
            }
        }
        if (!tempFile.exists()) {
            log.error("Temporary file for application update not written to: " + tempFile);
            throw new Exception();
        }
        return tempFile;
    }

    /**
     * Returns an instantiated and loaded versions store access point.
     *
     * @return will not be <code>null</code>
     */
    private PackageVersions loadPackageVersions() {
        if (this.versions == null) {
            ResourceType resourceType = getResourceContext().getResourceType();
            String pluginName = resourceType.getPlugin();
            File dataDirectoryFile = getResourceContext().getDataDirectory();
            dataDirectoryFile.mkdirs();
            String dataDirectory = dataDirectoryFile.getAbsolutePath();
            log.trace("Creating application versions store with plugin name [" + pluginName + "] and data directory ["
                + dataDirectory + "]");
            this.versions = new PackageVersions(pluginName, dataDirectory);
            this.versions.loadFromDisk();
        }

        return this.versions;
    }
}
