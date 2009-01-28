/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.jboss.on.plugins.tomcat;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.on.plugins.tomcat.helper.MainDeployer;
import org.rhq.core.domain.configuration.Configuration;
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
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * A resource component for managing an application (e.g. EAR or WAR) deployed to a JBossAS server.
 *
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class TomcatApplicationComponent extends MBeanResourceComponent<TomcatServerComponent> implements ContentFacet, DeleteResourceFacet {
    private static final String BACKUP_FILE_EXTENSION = ".rej";

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

    protected static final String PROPERTY_CONTEXT_ROOT = "contextRoot";
    protected static final String PROPERTY_FILENAME = "filename";
    protected static final String PROPERTY_VHOST = "vHost";

    private static final String APPLICATION_PATH_TRAIT = "Application.path";
    private static final String APPLICATION_EXPLODED_TRAIT = "Application.exploded";

    private final Log log = LogFactory.getLog(this.getClass());

    /**
     * Entry point to the persisted store of EAR/WAR package versions.
     */
    private PackageVersions versions;

    // ContentFacet Implementation  --------------------------------------------

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

    public Set<ResourcePackageDetails> discoverDeployedPackages(PackageType type) {

        Set<ResourcePackageDetails> packages = new HashSet<ResourcePackageDetails>();

        Configuration pluginConfiguration = super.resourceContext.getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimpleValue(PROPERTY_FILENAME, null);

        if (fullFileName == null) {
            throw new IllegalStateException("Plugin configuration does not contain the full file name of the EAR/WAR file.");
        }

        // If the parent EAR/WAR resource was found, this file should exist
        File file = new File(fullFileName);
        if (file.exists()) {
            // Package name and file name of the application are the same
            String fileName = new File(fullFileName).getName();

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
            details.setLocation(file.getPath());
            if (!file.isDirectory())
                details.setFileSize(file.length());
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
            log.warn("Request to update an EAR/WAR file contained multiple packages.");
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("When deploying an EAR/WAR, only one EAR/WAR can be updated at a time.");
            return response;
        }

        ResourcePackageDetails packageDetails = packages.iterator().next();

        // Find location of existing application
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        File appFile = new File(pluginConfig.getSimple(PROPERTY_FILENAME).getStringValue());
        if (!appFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + appFile, packageDetails);
        }

        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(appFile, contentServices, packageDetails);
        } catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e, packageDetails);
        }

        // Backup the existing app file/dir to <filename>.rej.
        File backupOfOriginalFile = new File(appFile.getPath() + BACKUP_FILE_EXTENSION);
        appFile.renameTo(backupOfOriginalFile);

        // Write the new bits for the application
        moveTempFileToDeployLocation(tempFile, appFile);

        // The file has been written successfully to the deploy dir. Now try to actually deploy it.
        MainDeployer mainDeployer = getParentResourceComponent().getMainDeployer();
        try {
            mainDeployer.redeploy(appFile);
        } catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                FileUtils.purge(appFile, true);
                backupOfOriginalFile.renameTo(appFile);
                // Need to redeploy the original file - this generally should succeed.
                mainDeployer.redeploy(appFile);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            } catch (Exception e1) {
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: " + ThrowableUtil.getAllMessages(e1);
            }
            return failApplicationDeployment(errorMessage, packageDetails);
        }

        // Deploy was successful!

        deleteBackupOfOriginalFile(backupOfOriginalFile);
        persistApplicationVersion(packageDetails, appFile);

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        return response;
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception {
        Configuration pluginConfiguration = super.resourceContext.getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimple(PROPERTY_FILENAME).getStringValue();

        File file = new File(fullFileName);

        if (!file.exists()) {
            throw new Exception("Cannot find application file to delete: " + fullFileName);
        }

        try {
            getParentResourceComponent().undeployFile(file);
        } catch (Exception e) {
            log.error("Failed to undeploy file [" + file + "].", e);
            throw e;
        } finally {
            try {
                FileUtils.purge(file, true);
            } catch (IOException e) {
                log.error("Failed to delete file [" + file + "].", e);
                // if the undeploy also failed that exception will be lost
                // and this one will be seen by the caller instead.
                // arguably both these conditions indicate failure, since
                // not being able to delete the file will mean that it will
                // likely get picked up again by the deployment scanner
                throw e;
            }
        }

    }

    // MeasurementFacet Implementation  --------------------------------------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        if (!requests.isEmpty()) {
            Configuration pluginConfig = super.resourceContext.getPluginConfiguration();
            String path = pluginConfig.getSimpleValue(PROPERTY_FILENAME, null);
            for (MeasurementScheduleRequest request : requests) {
                String metricName = request.getName();
                if (metricName.equals(APPLICATION_PATH_TRAIT)) {
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, path);
                    report.addData(trait);
                } else if (metricName.equals(APPLICATION_EXPLODED_TRAIT)) {
                    boolean exploded = new File(path).isDirectory();
                    MeasurementDataTrait trait = new MeasurementDataTrait(request, (exploded) ? "yes" : "no");
                    report.addData(trait);
                }
            }
        }
    }

    // Public  --------------------------------------------

    /**
     * Returns the name of the application.
     *
     * @return application name
     */
    public String getApplicationName() {
        String resourceKey = resourceContext.getResourceKey();
        return resourceKey.substring(resourceKey.lastIndexOf('=') + 1);
    }

    /**
     * Returns the file name of this application.
     *
     * @return full directory and file name of the application
     */
    public String getFileName() {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();
        return pluginConfiguration.getSimple(PROPERTY_FILENAME).getStringValue();
    }

    public TomcatServerComponent getParentResourceComponent() {
        return this.resourceContext.getParentResourceComponent();
    }

    // Private  --------------------------------------------

    /**
     * Returns an instantiated and loaded versions store access point.
     *
     * @return will not be <code>null</code>
     */
    private PackageVersions loadApplicationVersions() {
        if (versions == null) {
            ResourceType resourceType = super.resourceContext.getResourceType();
            String pluginName = resourceType.getPlugin();

            File dataDirectoryFile = super.resourceContext.getDataDirectory();

            if (!dataDirectoryFile.exists()) {
                dataDirectoryFile.mkdir();
            }

            String dataDirectory = dataDirectoryFile.getAbsolutePath();

            log.debug("Creating application versions store with plugin name [" + pluginName + "] and data directory [" + dataDirectory + "]");

            versions = new PackageVersions(pluginName, dataDirectory);
            versions.loadFromDisk();
        }

        return versions;
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

        DeployIndividualPackageResponse packageResponse = new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);

        response.addPackageResponse(packageResponse);

        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {
        String packageName = appFile.getName();
        PackageVersions versions = loadApplicationVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        try {
            FileUtils.purge(backupOfOriginalFile, true);
        } catch (Exception e) {
            // not critical.
            log.warn("Failed to delete backup of original file: " + backupOfOriginalFile);
        }
    }

    private void moveTempFileToDeployLocation(File tempFile, File appFile) {
        InputStream tempIs = null;
        try {
            if (appFile.isDirectory()) {
                tempIs = new BufferedInputStream(new FileInputStream(tempFile));
                appFile.mkdir();
                ZipUtil.unzipFile(tempIs, appFile);
            } else {
                tempFile.renameTo(appFile);
            }
        } catch (IOException e) {
            log.error("Error writing updated package bits to the existing application location: " + appFile, e);
            //return failApplicationDeployment("Error writing updated package bits to the existing application location: " +
            //    appFile, packageDetails);
        } finally {
            if (tempIs != null) {
                try {
                    tempIs.close();
                } catch (IOException e) {
                    log.error("Error closing temporary input stream", e);
                }
            }
        }
    }

    private File writeNewAppBitsToTempFile(File file, ContentServices contentServices, ResourcePackageDetails packageDetails) throws Exception {
        File tempDir = getResourceContext().getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), file.getName() + System.currentTimeMillis());

        // The temp file shouldn't be there, but check and delete it if it is
        if (tempFile.exists()) {
            log.warn("Existing temporary file found and will be deleted at: " + tempFile);
            tempFile.delete();
        }
        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            contentServices.downloadPackageBits(resourceContext.getContentContext(), packageDetails.getKey(), tempOutputStream, true);
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
