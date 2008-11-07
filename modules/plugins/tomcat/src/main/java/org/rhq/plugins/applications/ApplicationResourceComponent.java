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
package org.rhq.plugins.applications;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.plugins.jmx.JMXComponent;
import org.rhq.plugins.jmx.MBeanResourceComponent;
import org.rhq.plugins.utils.TomcatFileUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 /**
 * RHQ resource component for handling EARs and WARs. Most of the functionality is handled by the JMX plugin's super
 * class. This implementation adds content support for discovery of the EAR/WAR files.
 *
 * @author Jason Dobies
 */
public class ApplicationResourceComponent<T extends JMXComponent>
    extends MBeanResourceComponent<T> implements ContentFacet, DeleteResourceFacet {
    private static final Log LOG = LogFactory.getLog(ApplicationResourceComponent.class);

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

    /**
     * Entry point to the persisted store of EAR/WAR package versions.
     */
    private ApplicationVersions versions;

    protected static final String FILENAME_PLUGIN_CONFIG_PROP = "filename";

    private static final String APPLICATION_PATH_TRAIT = "Application.path";
    private static final String APPLICATION_EXPLODED_TRAIT = "Application.exploded";

    // ContentFacet Implementation  --------------------------------------------

    public InputStream retrievePackageBits(ResourcePackageDetails packageDetails) {
        File packageFile = new File(packageDetails.getName());
        File fileToSend;
        try {
            if (packageFile.isDirectory()) {
                fileToSend = File.createTempFile("rhq", ".zip");
                TomcatFileUtils.zipFileOrDirectory(packageFile, fileToSend);
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

        Configuration pluginConfiguration = super.resourceContext.getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimpleValue(FILENAME_PLUGIN_CONFIG_PROP, null);

        if (fullFileName == null) {
            throw new IllegalStateException("Plugin configuration does not contain the full file name of the EAR/WAR file.");
        }

        // If the parent EAR/WAR resource was found, this file should exist
        File file = new File(fullFileName);
        if (file.exists())
        {
            // Package name and file name of the application are the same
            String fileName = new File(fullFileName).getName();

            ApplicationVersions versions = loadApplicationVersions();
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

    public DeployPackagesResponse deployPackages(Set<ResourcePackageDetails> packages, ContentServices contentServices) {

        // You can only update the one application file referenced by this resource, so punch out if multiple are
        // specified
        if (packages.size() != 1) {
            LOG.warn("Request to update an EAR/WAR file contained multiple packages.");
            DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);
            response.setOverallRequestErrorMessage("When deploying an EAR/WAR, only one EAR/WAR can be updated at a time.");
            return response;
        }

        ResourcePackageDetails packageDetails = packages.iterator().next();

        // Find location of existing application
        String fullFileName = resourceContext.getPluginConfiguration().getSimple(FILENAME_PLUGIN_CONFIG_PROP).getStringValue();
        String filename = new File(fullFileName).getName();

        // Write the new updated application to a temp file
        File tempDir = resourceContext.getTemporaryDirectory();
        File tempFile = new File(tempDir.getAbsolutePath(), filename + System.currentTimeMillis());

        // The temp file shouldn't be there, but check and delete it if it is
        if (tempFile.exists()) {
            LOG.warn("Existing temporary file found and will be deleted at: " + tempFile);
            tempFile.delete();
        }

        // Determine if existing application is exploded or not
        File existingFile = new File(fullFileName);

        if (!existingFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + fullFileName,
                packageDetails);
        }

        boolean unzip = existingFile.isDirectory();

        // Write the updated application bits to a temporary location
        OutputStream tempOutputStream = null;
        try {
            tempOutputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            contentServices.downloadPackageBits(resourceContext.getContentContext(), packageDetails.getKey(), tempOutputStream, true);
            tempOutputStream.close();
        } catch (IOException e) {
            LOG.error("Error writing updated application bits to temporary location: " + tempFile, e);
            return failApplicationDeployment("Error writing updated application bits to temporary location: " +
                tempFile, packageDetails);
        } finally {
            if (tempOutputStream != null) {
                try {
                    tempOutputStream.close();
                } catch (IOException e) {
                    LOG.error("Error closing temporary output stream", e);
                }
            }
        }

        // Make sure the file was downloaded
        if (!tempFile.exists()) {
            LOG.error("Temporary file for application update not written to: " + tempFile);
            return failApplicationDeployment("Temporary file for application update not written to: " + tempFile,
                packageDetails);
        }

        // Delete the existing application
        if (unzip) {
            TomcatFileUtils.deleteDirectoryContents(existingFile.listFiles());
        }
        boolean deleteResult = existingFile.delete();

        if (!deleteResult) {
            return failApplicationDeployment("Could not delete existing application that is being updated at: " +
                existingFile, packageDetails);
        }

        // Write the new bits for the application
        InputStream tempIs = null;
        try {
            tempIs = new BufferedInputStream(new FileInputStream(tempFile));
            
            if (unzip) {
                existingFile.mkdir();
                TomcatFileUtils.unzipFile(tempIs, existingFile);
            } else {
                TomcatFileUtils.writeFile(tempIs, existingFile);
            }

        } catch (IOException e) {
            LOG.error("Error writing updated package bits to the existing application location: " + existingFile, e);
            return failApplicationDeployment("Error writing updated package bits to the existing application location: " +
                existingFile, packageDetails);
        } finally {
            if (tempIs != null) {
                try {
                    tempIs.close();
                } catch (IOException e) {
                    LOG.error("Error closing temporary input stream", e);
                }
            }
        }

        // Quick verification for zipped applications
        if (!existingFile.exists()) {
            LOG.error("Updated application file not found at existing application file location: " + existingFile);
            return failApplicationDeployment("Updated application file not found at existing application file location: " +
                existingFile, packageDetails);
        }

        // If we got this far, it was successful

        // Update the persistent store for this new version
        String packageName = new File(fullFileName).getName();
        ApplicationVersions versions = loadApplicationVersions();
        versions.putVersion(packageName, packageDetails.getVersion());

        // Create and return response
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);

        DeployIndividualPackageResponse packageResponse =
            new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        
        response.addPackageResponse(packageResponse);

        return response;
    }

    public List<DeployPackageStep> generateInstallationSteps(ResourcePackageDetails packageDetails) {
        // Intentional, there are no steps involved in installing an EAR
        return null;
    }

    // DeleteResourceFacet Implementation  --------------------------------------------

    public void deleteResource() throws Exception {
        Configuration pluginConfiguration = super.resourceContext.getPluginConfiguration();
        String fullFileName = pluginConfiguration.getSimple(FILENAME_PLUGIN_CONFIG_PROP).getStringValue();

        File file = new File(fullFileName);

        if (!file.exists()) {
            throw new Exception("Cannot find application file to delete: " + fullFileName);
        }

        if (file.isDirectory()) {
            TomcatFileUtils.deleteDirectoryContents(file.listFiles());
        }

        boolean result = file.delete();

        if (!result) {
            throw new Exception("File delete call returned unsuccessful with no further detail");
        }
    }

    // MeasurementFacet Implementation  --------------------------------------------

    @Override
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) {
        if (!requests.isEmpty()) {
            Configuration pluginConfig = super.resourceContext.getPluginConfiguration();
            String path = pluginConfig.getSimpleValue(FILENAME_PLUGIN_CONFIG_PROP, null);
            for (MeasurementScheduleRequest request : requests) {
                 String metricName = request.getName();
                 if (metricName.equals(APPLICATION_PATH_TRAIT)) {
                     MeasurementDataTrait trait = new MeasurementDataTrait(request, path);
                     report.addData(trait);
                 }
                 else if (metricName.equals(APPLICATION_EXPLODED_TRAIT)) {
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
        String applicationName = resourceKey.substring(resourceKey.lastIndexOf('=') + 1);
        return applicationName;
    }

    /**
     * Returns the file name of this application.
     *
     * @return full directory and file name of the application
     */
    public String getFileName() {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();
        return pluginConfiguration.getSimple(FILENAME_PLUGIN_CONFIG_PROP).getStringValue();
    }

    public T getParentResourceComponent() {
        return this.resourceContext.getParentResourceComponent();
    }


    // Private  --------------------------------------------

    /**
     * Returns an instantiated and loaded versions store access point.
     *
     * @return will not be <code>null</code> 
     */
    protected ApplicationVersions loadApplicationVersions() {
        if (versions == null) {
            ResourceType resourceType = super.resourceContext.getResourceType();
            String pluginName = resourceType.getPlugin();

            File dataDirectoryFile = super.resourceContext.getDataDirectory();

            if (!dataDirectoryFile.exists()) {
                dataDirectoryFile.mkdir();
            }

            String dataDirectory = dataDirectoryFile.getAbsolutePath();

            LOG.debug("Creating application versions store with plugin name [" + pluginName +
                "] and data directory [" + dataDirectory + "]");

            versions = new ApplicationVersions(pluginName, dataDirectory);
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
    protected DeployPackagesResponse failApplicationDeployment(String errorMessage, ResourcePackageDetails packageDetails) {
        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.FAILURE);

        DeployIndividualPackageResponse packageResponse =
            new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.FAILURE);
        packageResponse.setErrorMessage(errorMessage);

        response.addPackageResponse(packageResponse);

        return response;
    }
}