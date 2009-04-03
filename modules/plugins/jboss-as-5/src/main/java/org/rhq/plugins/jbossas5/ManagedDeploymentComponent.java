/*
* Jopr Management Platform
* Copyright (C) 2005-2008 Red Hat, Inc.
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.io.FileUtils;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.ProgressEvent;
import org.jboss.deployers.spi.management.deploy.ProgressListener;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedProperty;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.version.PackageVersions;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;

/**
 * ResourceComponent for managing ManagedDeployments (EARs, WARs, SARs, etc.).
 *
 * @author Mark Spritzler
 * @author Ian Springer
 */
public class ManagedDeploymentComponent
        extends AbstractManagedComponent
        implements ResourceComponent, MeasurementFacet, OperationFacet, ContentFacet, DeleteResourceFacet,
        ProgressListener {
    public static final String DEPLOYMENT_NAME_PROPERTY = "deploymentName";
    public static final String DEPLOYMENT_TYPE_NAME_PROPERTY = "deploymentTypeName";

    private static final String CUSTOM_PATH_TRAIT = "custom.path";
    private static final String CUSTOM_EXPLODED_TRAIT = "custom.exploded";

    /**
     * Name of the backing package type that will be used when discovering packages. This corresponds to the name
     * of the package type defined in the plugin descriptor. For simplicity, the package type for both EARs and
     * WARs is simply called "file". This is still unique within the context of the parent resource type and lets
     * this class use the same package type name in both cases.
     */
    private static final String PKG_TYPE_FILE = "file";

    /**
     * Architecture string used in describing discovered packages.
     */
    private static final String ARCHITECTURE = "noarch";

    private static final String BACKUP_FILE_EXTENSION = ".rej";

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * The name of the ManagedDeloyment (e.g.: vfszip:/C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    private String deploymentName;

    /**
     * The type of the ManagedDeloyment.
     */
    private KnownDeploymentTypes deploymentType;

    /**
     * The absolute path of the deployment file (e.g.: C:/opt/jboss-5.0.0.GA/server/default/deploy/foo.war).
     */
    private File deploymentFile;

    private PackageVersions versions;



    // ----------- ResourceComponent Implementation ------------

    public void start(ResourceContext resourceContext) throws Exception {
        super.start(resourceContext);
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        this.deploymentName = pluginConfig.getSimple(DEPLOYMENT_NAME_PROPERTY).getStringValue();
        this.deploymentFile = getDeploymentFile();
        String deploymentTypeName = pluginConfig.getSimple(DEPLOYMENT_TYPE_NAME_PROPERTY).getStringValue();
        this.deploymentType = KnownDeploymentTypes.valueOf(deploymentTypeName);
    }

    public AvailabilityType getAvailability() {
        // TODO (ips, 11/10/08): Verify this is the correct way to check availablity.
        try {
            return (getManagedDeployment() != null) ? AvailabilityType.UP : AvailabilityType.DOWN;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------ MeasurementFacet Implementation ------------

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();
            if (metricName.equals(CUSTOM_PATH_TRAIT)) {
                MeasurementDataTrait trait = new MeasurementDataTrait(request, this.deploymentFile.getPath());
                report.addData(trait);
            }
            else if (metricName.equals(CUSTOM_EXPLODED_TRAIT)) {
                boolean exploded = this.deploymentFile.isDirectory();
                MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                report.addData(trait);
            }
        }
    }

    // ------------ OperationFacet Implementation ------------

    public OperationResult invokeOperation(String name, Configuration parameters) throws InterruptedException, Exception
    {
        DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
        DeploymentProgress progress;
        if (name.equals("start")) {
            progress = deploymentManager.start(this.deploymentName);
        } else if (name.equals("stop")) {
            progress = deploymentManager.stop(this.deploymentName);
        } else if (name.equals("restart")) {
            progress = deploymentManager.stop(this.deploymentName);
            DeploymentUtils.run(progress);
            progress = deploymentManager.start(this.deploymentName);
        } else {
            throw new UnsupportedOperationException(name);
        }
        DeploymentStatus status = DeploymentUtils.run(progress);
        log.debug("Operation '" + name + "' on " + getResourceDescription() + " completed with status [" + status
                + "].");
        return new OperationResult();
    }

    // ------------ DeleteResourceFacet implementation -------------

    public void deleteResource() throws Exception {
        DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
        log.debug("Stopping deployment [" + this.deploymentName + "]...");
        DeploymentProgress progress = deploymentManager.stop(deploymentName);
        DeploymentUtils.run(progress);
        log.debug("Removing deployment [" + this.deploymentName + "]...");
        progress = deploymentManager.remove(deploymentName);
        DeploymentUtils.run(progress);
    }

    private File getDeploymentFile() throws MalformedURLException {
        URL vfsURL = new URL(this.deploymentName);
        String path = vfsURL.getPath();
        while (path.charAt(0) == '/')
            path = path.substring(1);
        return new File(path);
    }

    // ------------ ContentFacet implementation -------------

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            if (packageFile.isDirectory()) {
                fileToSend = File.createTempFile("rhq", ".zip");
                ZipUtil.zipFileOrDirectory(packageFile, fileToSend);
            }
            else
                fileToSend = packageFile;
            return new BufferedInputStream(new FileInputStream(fileToSend));
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to retrieve package bits for " + packageDetails, e);
        }
    }

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {
        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        // If the parent EAR/WAR resource was found, this file should exist
        if (this.deploymentFile.exists()) {
            // Package name and file name of the application are the same
            String fileName = this.deploymentFile.getName();

            PackageVersions versions = loadApplicationVersions();
            String version = versions.getVersion(fileName);

            // First discovery of this EAR/WAR
            if (version == null) {
                version = "1.0";
                versions.putVersion(fileName, version);
                versions.saveToDisk();
            }

            PackageDetailsKey key = new PackageDetailsKey(fileName, version, PKG_TYPE_FILE, ARCHITECTURE);
            ResourcePackageDetails details = new ResourcePackageDetails(key);
            details.setFileName(fileName);
            details.setLocation(this.deploymentFile.getPath());
            if (!this.deploymentFile.isDirectory())
                details.setFileSize(this.deploymentFile.length());
            details.setFileCreatedDate(null); // TODO: get created date via SIGAR

            packages.add(details);
        }

        return packages;
    }

    public RemovePackagesResponse removePackages(Set<ResourcePackageDetails> packages) {
        throw new UnsupportedOperationException("Cannot remove the package backing an EAR/WAR resource.");
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        // Intentional - there are no steps involved in installing an EAR or WAR.
        return null;
    }

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {
        // You can only update the one application file referenced by this resource, so punch out if multiple are
        // specified.
        if (packages.size() != 1) {
            log.warn("Request to update an EAR/WAR file contained multiple packages: " + packages);
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("When updating an EAR/WAR, only one EAR/WAR can be updated at a time.");
            return response;
        }

        ResourcePackageDetails packageDetails = packages.iterator().next();

        log.debug("Updating EAR/WAR file '" + this.deploymentFile + "' using [" + packageDetails + "]...");
        // Find location of existing application.
        if (!this.deploymentFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + this.deploymentFile,
                    packageDetails);
        }

        log.debug("Writing new EAR/WAR bits to temporary file...");
        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(contentServices, packageDetails);
        }
        catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e,
                    packageDetails);
        }
        log.debug("Wrote new EAR/WAR bits to temporary file '" + tempFile + "'.");

        boolean deployExploded = this.deploymentFile.isDirectory();

        // Backup the original app file/dir to <filename>.rej.
        File backupOfOriginalFile = new File(this.deploymentFile.getPath() + BACKUP_FILE_EXTENSION);
        log.debug("Backing up existing EAR/WAR '" + this.deploymentFile + "' to '" + backupOfOriginalFile + "'...");
        try
        {
            if (backupOfOriginalFile.exists())
               FileUtils.forceDelete(backupOfOriginalFile);
            if (this.deploymentFile.isDirectory())
               FileUtils.copyDirectory(this.deploymentFile, backupOfOriginalFile, true);
            else
               FileUtils.copyFile(this.deploymentFile, backupOfOriginalFile, true);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to backup existing EAR/WAR '" + this.deploymentFile + "' to '"
                    + backupOfOriginalFile + "'.");
        }

        // Now remove/undeploy the original app.
        DeploymentProgress progress;
        try
        {
            DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
            progress = deploymentManager.remove(this.deploymentName);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to remove deployment [" + this.deploymentName + "].", e);
        }
        progress.run();
        DeploymentStatus status = progress.getDeploymentStatus();
        if (status.isFailed())
            throw new RuntimeException(status.getMessage(), status.getFailure());

        // Deploy away!
        log.debug("Deploying '" + tempFile + "'...");
        File deployDir = this.deploymentFile.getParentFile();
        try {
            DeploymentUtils.deployArchive(tempFile, deployDir, deployExploded);
        }
        catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            log.debug("Redeploy failed - rolling back to original archive...", e);
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                // Delete the new app, which failed to deploy.
                FileUtils.forceDelete(this.deploymentFile);
                // Need to redeploy the original file - this generally should succeed.
                DeploymentUtils.deployArchive(backupOfOriginalFile, deployDir, deployExploded);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            }
            catch (Exception e1) {
                log.debug("Rollback failed!", e1);
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: "
                        + ThrowableUtil.getAllMessages(e1);
            }
            log.info("Failed to update EAR/WAR file '" + this.deploymentFile + "' using [" + packageDetails + "]." );
            return failApplicationDeployment(errorMessage, packageDetails);
        }

        // Deploy was successful!

        deleteBackupOfOriginalFile(backupOfOriginalFile);
        persistApplicationVersion(packageDetails, this.deploymentFile);

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse =
                new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        log.debug("Updated EAR/WAR file '" + this.deploymentFile + "' successfully - returning response [" + response
                + "]...");

        return response;
    }

    // ------------ ProgressListener implementation -------------

    public void progressEvent(ProgressEvent event) {
        log.debug(event);
    }

    // ------------ AbstractManagedComponent implementation -------------     

    protected Map<String, ManagedProperty> getManagedProperties() throws Exception {
        return getManagedDeployment().getProperties();
    }

    protected Log getLog() {
        return this.log;
    }

    protected void updateComponent() throws Exception {
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        managementView.process();
    }

    // -------------------------------------------------------------

    private ManagedDeployment getManagedDeployment() throws Exception {
        //ProfileServiceFactory.refreshCurrentProfileView();
        ManagementView managementView = ProfileServiceFactory.getCurrentProfileView();
        String resourceKey = getResourceContext().getResourceKey();
        return managementView.getDeployment(resourceKey);
    }

    /**
     * Returns an instantiated and loaded versions store access point.
     *
     * @return will not be <code>null</code>
     */
    private PackageVersions loadApplicationVersions() {
        if (this.versions == null) {
            ResourceType resourceType = getResourceContext().getResourceType();
            String pluginName = resourceType.getPlugin();
            File dataDirectoryFile = getResourceContext().getDataDirectory();
            dataDirectoryFile.mkdirs();
            String dataDirectory = dataDirectoryFile.getAbsolutePath();
            log.trace("Creating application versions store with plugin name [" + pluginName +
                "] and data directory [" + dataDirectory + "]");
            this.versions = new PackageVersions(pluginName, dataDirectory);
            this.versions.loadFromDisk();
        }

        return this.versions;
    }

    /**
     * Creates the necessary transfer objects to report a failed application deployment (update).
     *
     * @param errorMessage   reason the deploy failed
     * @param packageDetails describes the update being made
     *
     * @return response populated to reflect a failure
     */
    private DeployPackagesResponse failApplicationDeployment(String errorMessage, ResourcePackageDetails packageDetails) {
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);

        DeployIndividualPackageResponse packageResponse =
            new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);

        response.addPackageResponse(packageResponse);

        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {
        String packageName = appFile.getName();
        log.debug("Persisting application version '" + packageDetails.getVersion() + "' for package '" + packageName
                + "'");
        PackageVersions versions = loadApplicationVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        log.debug("Deleting backup of original file '" + backupOfOriginalFile + "'...");
        try {
            FileUtils.forceDelete(backupOfOriginalFile);
        }
        catch (Exception e) {
            // not critical.
            log.warn("Failed to delete backup of original file: " + backupOfOriginalFile);
        }
    }

    private File writeNewAppBitsToTempFile(ContentServices contentServices, ResourcePackageDetails packageDetails
    ) throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir, this.deploymentFile.getName());

        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            long bytesWritten = contentServices.downloadPackageBits(getResourceContext().getContentContext(), packageDetails.getKey(),
                    tempOutputStream, true);
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
}
