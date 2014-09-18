/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.profileservice.spi.ProfileKey;

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
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.FileContentDelegate;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.ContentFileInfo;
import org.rhq.core.util.file.JarContentFileInfo;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * A resource component for managing a standalone/top-level Profile Service managed deployment.
 *  
 * @author Ian Springer
 */
public class StandaloneManagedDeploymentComponent extends AbstractManagedDeploymentComponent implements
    MeasurementFacet, ContentFacet, DeleteResourceFacet {

    private static final Log LOG = LogFactory.getLog(StandaloneManagedDeploymentComponent.class);

    private static final String CUSTOM_PATH_TRAIT = "custom.path";
    private static final String CUSTOM_EXPLODED_TRAIT = "custom.exploded";

    /**
     * @deprecated as of 4.13. No longer used, at least since 4.12.
     */
    @Deprecated
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

    private static final ProfileKey FARM_PROFILE_KEY = new ProfileKey("farm");
    private static final ProfileKey APPLICATIONS_PROFILE_KEY = new ProfileKey("applications");

    // ------------ MeasurementFacet Implementation ------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {
        Set<MeasurementScheduleRequest> remainingRequests = new HashSet<MeasurementScheduleRequest>();
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            if (metricName.equals(CUSTOM_PATH_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(request, deploymentFile.getPath());
                report.addData(trait);
            } else if (metricName.equals(CUSTOM_EXPLODED_TRAIT)) {
                boolean exploded = deploymentFile.isDirectory();
                MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                report.addData(trait);
            } else {
                remainingRequests.add(request);
            }
        }
        super.getValues(report, remainingRequests);
    }

    // ------------ ContentFacet implementation -------------

    @Override
    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            /*
             * TODO: This all seems very broken. We are first setting packageFile 
             * to the key used for packageDetails and not the fileName of packageDetails. 
             * Additionally, PackageDetails.fileName contains the file name without its
             * path making its use invalid here.   
             */
            // If the file isn't real then lets fall-back to this ManagedDeploymentComponent's file name and hope its valid
            if (!packageFile.exists() && deploymentFile != null) {
                packageFile = deploymentFile;
            }
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

    @Override
    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType packageType) {
        if (!deploymentFile.exists())
            throw new IllegalStateException("Deployment file '" + deploymentFile + "' for " + getResourceDescription()
                + " does not exist.");

        String fileName = deploymentFile.getName();
        String sha256 = getSHA256(deploymentFile);
        String version = getVersion(sha256);
        String displayVersion = getDisplayVersion(deploymentFile);

        // Package name is the deployment's file name (e.g. foo.ear).
        PackageDetailsKey key = new PackageDetailsKey(fileName, version, PKG_TYPE_FILE, ARCHITECTURE);
        ResourcePackageDetails packageDetails = new ResourcePackageDetails(key);
        packageDetails.setFileName(fileName);
        packageDetails.setLocation(deploymentFile.getPath());
        if (!deploymentFile.isDirectory())
            packageDetails.setFileSize(deploymentFile.length());
        packageDetails.setFileCreatedDate(null); // TODO: get created date via SIGAR
        packageDetails.setSHA256(sha256);
        packageDetails.setInstallationTimestamp(Long.valueOf(System.currentTimeMillis()));
        packageDetails.setDisplayVersion(displayVersion);

        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();
        packages.add(packageDetails);

        return packages;
    }

    /**
     * Retrieve SHA256 for a deployed app.
     *
     * @param file application file
     * @return SHA256 of the content
     */
    private String getSHA256(File file) {
        String sha256 = null;

        try {
            FileContentDelegate fileContentDelegate = new FileContentDelegate();
            sha256 = fileContentDelegate.retrieveDeploymentSHA(file, getResourceContext().getResourceDataDirectory());
        } catch (Exception iex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Problem calculating digest of package [" + file.getPath() + "]." + iex.getMessage());
            }
        }

        return sha256;
    }

    private String getVersion(String sha256) {
        return "[sha256=" + sha256 + "]";
    }

    /**
     * Retrieve the display version for the component. The display version should be stored
     * in the manifest of the application (implementation and/or specification version).
     * It will attempt to retrieve the version for both archived or exploded deployments.
     *
     * @param file component file
     * @return
     */
    private String getDisplayVersion(File file) {
        //JarContentFileInfo extracts the version from archived and exploded deployments
        ContentFileInfo contentFileInfo = new JarContentFileInfo(file);
        return contentFileInfo.getVersion(null);
    }

    @Override
    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove the package backing an EAR/WAR resource.");
    }

    @Override
    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        // Intentional - there are no steps involved in installing an EAR or WAR.
        return null;
    }

    @Override
    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        String resourceTypeName = getResourceContext().getResourceType().getName();

        // You can only update the one application file referenced by this resource, so punch out if multiple are
        // specified.
        if (packages.size() != 1) {
            LOG.warn("Request to update " + resourceTypeName + " file contained multiple packages: " + packages);
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Only one " + resourceTypeName + " can be updated at a time.");
            return response;
        }

        ResourcePackageDetails packageDetails = packages.iterator().next();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating EAR/WAR file '" + deploymentFile + "' using [" + packageDetails + "]...");
        }
        // Find location of existing application.
        if (!deploymentFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + deploymentFile,
                packageDetails);
        }

        LOG.debug("Writing new EAR/WAR bits to temporary file...");
        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(contentServices, packageDetails);
        } catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e,
                packageDetails);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Wrote new EAR/WAR bits to temporary file '" + tempFile + "'.");
        }

        boolean deployExploded = deploymentFile.isDirectory();

        // Backup the original app file/dir.
        File tempDir = getResourceContext().getTemporaryDirectory();
        File backupDir = new File(tempDir, "deployBackup" + UUID.randomUUID().getLeastSignificantBits());
        File backupOfOriginalFile = new File(backupDir, deploymentFile.getName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("Backing up existing EAR/WAR '" + deploymentFile + "' to '" + backupOfOriginalFile + "'...");
        }
        try {
            if (backupOfOriginalFile.exists()) {
                FileUtils.forceDelete(backupOfOriginalFile);
            }
            if (deploymentFile.isDirectory()) {
                FileUtils.copyDirectory(deploymentFile, backupOfOriginalFile, true);
            } else {
                FileUtils.copyFile(deploymentFile, backupOfOriginalFile, true);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to backup existing " + resourceTypeName + "'" + deploymentFile
                + "' to '" + backupOfOriginalFile + "'.");
        }

        ProfileServiceConnection connection = getConnection();
        if (connection == null) {
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("No profile service connection available");
            return response;
        }

        DeploymentManager deploymentManager = connection.getDeploymentManager();

        // as crazy as it might sound, there is apparently no way for you to ask the profile service
        // if a deployment was deployed to the farm profile. Thus, we must resort to a poor man's solution:
        // if the deployment name has the "farm/" directory in it, assume it needs to be deployed to the farm
        boolean deployFarmed = getDeploymentKey().contains("/farm/");
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
            try {
                deploymentManager.loadProfile(FARM_PROFILE_KEY);
            } catch (Exception e) {
                LOG.info("Failed to switch to farm profile - could not update " + resourceTypeName + " file '"
                    + deploymentFile + "' using [" + packageDetails + "].");
                String errorMessage = ThrowableUtil.getAllMessages(e);
                return failApplicationDeployment(errorMessage, packageDetails);
            }
        }

        String deploymentName = getDeploymentName();
        if (deploymentName == null) {
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("Did not find deployment with key [" + getDeploymentKey() + "]");
            return response;
        }

        // Now stop the original app.
        try {
            DeploymentProgress progress = deploymentManager.stop(deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop deployment [" + deploymentName + "].", e);
        }

        // And then remove it (this will delete the physical file/dir from the deploy dir).
        try {
            DeploymentProgress progress = deploymentManager.remove(deploymentName);
            DeploymentUtils.run(progress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove deployment [" + deploymentName + "].", e);
        }

        // Deploy away!
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Deploying '" + tempFile + "'...");
            }
            DeploymentUtils.deployArchive(deploymentManager, tempFile, deployExploded);
        } catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            LOG.debug("Redeploy failed - rolling back to original archive...", e);
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                // Try to delete the new app file, which failed to deploy, if it still exists.
                if (deploymentFile.exists()) {
                    try {
                        FileUtils.forceDelete(deploymentFile);
                    } catch (IOException e1) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Failed to delete application file '" + deploymentFile
                                + "' that failed to deploy.", e1);
                        }
                    }
                }
                // Now redeploy the original file - this generally should succeed.
                DeploymentUtils.deployArchive(deploymentManager, backupOfOriginalFile, deployExploded);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";

                // If the redeployment of the original backup succeeded then cleanup the backup from disk
                deleteTemporaryFile(backupDir);
                // If the redeployment fails the original backup is preserved on disk until agent restart
            } catch (Exception e1) {
                LOG.debug("Rollback failed!", e1);
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: "
                    + ThrowableUtil.getAllMessages(e1);
            }

            //since the deployment failed remove the temp application downloaded for deployment
            deleteTemporaryFile(tempFile);

            LOG.info("Failed to update " + resourceTypeName + " file '" + deploymentFile + "' using [" + packageDetails
                + "].");
            return failApplicationDeployment(errorMessage, packageDetails);
        } finally {
            // Make sure to switch back to the 'applications' profile if we switched to the 'farm' profile above.
            if (deployFarmed) {
                try {
                    deploymentManager.loadProfile(APPLICATIONS_PROFILE_KEY);
                } catch (Exception e) {
                    LOG.debug("Failed to switch back to applications profile from farm profile", e);
                }
            }
        }

        // Store SHA256 in the agent file if deployment was exploded
        if (deploymentFile.isDirectory()) {
            FileContentDelegate fileContentDelegate = new FileContentDelegate();
            fileContentDelegate.saveDeploymentSHA(tempFile, deploymentFile, getResourceContext()
                .getResourceDataDirectory());
        }

        // Remove temporary files created by this deployment.
        deleteTemporaryFile(backupDir);
        deleteTemporaryFile(tempFile.getParentFile());

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(),
            ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated " + resourceTypeName + " file '" + deploymentFile
                + "' successfully - returning response [" + response + "]...");
        }

        return response;
    }

    // ------------ DeleteResourceFacet implementation -------------

    @Override
    public void deleteResource() throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting " + getResourceDescription() + "...");
        }

        ProfileServiceConnection connection = getConnection();
        if (connection == null) {
            throw new Exception("No profile service connection available");
        }

        DeploymentManager deploymentManager = connection.getDeploymentManager();
        try {
            getManagedDeployment();
        } catch (Exception e) {
            // The deployment no longer exists, so there's nothing for us to do. Someone most likely undeployed it
            // outside of Jopr or EmbJopr, e.g. via the jmx-console or by deleting the app file from the deploy dir.
            LOG.warn("Cannot delete the deployment [" + getDeploymentKey() + "], since it no longer exists");
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Stopping deployment [" + getDeploymentKey() + "]...");
        }

        String deploymentName = getDeploymentName();
        if (deploymentName == null) {
            throw new IllegalStateException("Deployment " + getDeploymentKey() + " has vanished");
        }

        DeploymentProgress progress = deploymentManager.stop(deploymentName);
        DeploymentStatus stopStatus = DeploymentUtils.run(progress);
        if (stopStatus.isFailed()) {
            LOG.error("Failed to stop deployment '" + deploymentName + "'.", stopStatus.getFailure());
            throw new Exception("Failed to stop deployment '" + deploymentName + "' - cause: "
                + stopStatus.getFailure());
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Removing deployment [" + deploymentName + "]...");
        }
        progress = deploymentManager.remove(deploymentName);
        DeploymentStatus removeStatus = DeploymentUtils.run(progress);
        if (removeStatus.isFailed()) {
            LOG.error("Failed to remove deployment '" + deploymentName + "'.", removeStatus.getFailure());
            throw new Exception("Failed to remove deployment '" + deploymentName + "' - cause: "
                + removeStatus.getFailure());
        }
        ManagementView managementView = connection.getManagementView();
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

    private void deleteTemporaryFile(File temporaryFile) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting temporary file '" + temporaryFile + "'...");
        }
        try {
            FileUtils.forceDelete(temporaryFile);
        } catch (Exception e) {
            // not critical.
            LOG.warn("Failed to temporary file: " + temporaryFile);
        }
    }

    private File writeNewAppBitsToTempFile(ContentServices contentServices, ResourcePackageDetails packageDetails)
        throws Exception {
        File tempDir = new File(getResourceContext().getTemporaryDirectory(), "deploy"
            + UUID.randomUUID().getLeastSignificantBits());
        tempDir.mkdirs();

        File tempFile = new File(tempDir, deploymentFile.getName());

        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            long bytesWritten = contentServices.downloadPackageBits(getResourceContext().getContentContext(),
                packageDetails.getKey(), tempOutputStream, true);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Wrote " + bytesWritten + " bytes to '" + tempFile + "'.");
            }
        } catch (IOException e) {
            LOG.error("Error writing updated application bits to temporary location: " + tempFile, e);
            throw e;
        } finally {
            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException e) {
                    LOG.error("Error closing temporary output stream", e);
                }
            }
        }
        if (!tempFile.exists()) {
            LOG.error("Temporary file for application update not written to: " + tempFile);
            throw new Exception();
        }
        return tempFile;
    }
}
