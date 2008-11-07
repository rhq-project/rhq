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
package org.rhq.plugins.jbossas;

import java.util.Set;
import java.io.File;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.plugins.applications.ApplicationResourceComponent;
import org.rhq.plugins.applications.ApplicationVersions;
import org.rhq.plugins.jbossas.helper.MainDeployer;
import org.rhq.plugins.utils.TomcatFileUtils;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.util.exception.ThrowableUtil;

/**
 * A ResourceComponent for the EAR and WAR Resource types.
 *
 * @author Ian Springer
 */
public class JBossApplicationComponent extends ApplicationResourceComponent<JBossASServerComponent> {
    private static final String BACKUP_FILE_EXTENSION = ".rej";

    private final Log log = LogFactory.getLog(this.getClass());

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
        File appFile = new File(pluginConfig.getSimple(FILENAME_PLUGIN_CONFIG_PROP).getStringValue());
        if (!appFile.exists()) {
            return failApplicationDeployment("Could not find application to update at location: " + appFile,
                packageDetails);
        }

        File tempFile;
        try {
            tempFile = writeNewAppBitsToTempFile(appFile, contentServices, packageDetails);
        }
        catch (Exception e) {
            return failApplicationDeployment("Error writing new application bits to temporary file - cause: " + e,
                    packageDetails);
        }

        // Backup the existing app file/dir to <filename>.rej.
        File backupOfOriginalFile = new File(appFile.getPath() + BACKUP_FILE_EXTENSION);
        appFile.renameTo(backupOfOriginalFile);

        // Write the new bits for the application
        moveTempFileToDeployLocation(tempFile, appFile);

        // The file has been written successfully to the deploy dir. Now try to actually deploy it.
        MainDeployer mainDeployer = getParentResourceComponent().getMainDeployer();
        try {
            mainDeployer.deploy(appFile);
        }
        catch (Exception e) {
            // Deploy failed - rollback to the original app file...
            String errorMessage = ThrowableUtil.getAllMessages(e);
            try {
                FileUtils.purge(appFile, true);
                backupOfOriginalFile.renameTo(appFile);
                // Need to redeploy the original file - this generally should succeed.
                mainDeployer.deploy(appFile);
                errorMessage += " ***** ROLLED BACK TO ORIGINAL APPLICATION FILE. *****";
            }
            catch (Exception e1) {
                errorMessage += " ***** FAILED TO ROLLBACK TO ORIGINAL APPLICATION FILE. *****: "
                        + ThrowableUtil.getAllMessages(e1);
            }
            return failApplicationDeployment(errorMessage, packageDetails);
        }

        // Deploy was successful!

        deleteBackupOfOriginalFile(backupOfOriginalFile);
        persistApplicationVersion(packageDetails, appFile);

        DeployPackagesResponse response = new DeployPackagesResponse(ContentResponseResult.SUCCESS);
        DeployIndividualPackageResponse packageResponse =
            new DeployIndividualPackageResponse(packageDetails.getKey(), ContentResponseResult.SUCCESS);
        response.addPackageResponse(packageResponse);

        return response;
    }

    private void persistApplicationVersion(ResourcePackageDetails packageDetails, File appFile) {        
        String packageName = appFile.getName();
        ApplicationVersions versions = loadApplicationVersions();
        versions.putVersion(packageName, packageDetails.getVersion());
    }

    private void deleteBackupOfOriginalFile(File backupOfOriginalFile) {
        try {
            FileUtils.purge(backupOfOriginalFile, true);
        }
        catch (Exception e) {
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
                TomcatFileUtils.unzipFile(tempIs, appFile);
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

    private File writeNewAppBitsToTempFile(File file, ContentServices contentServices, ResourcePackageDetails packageDetails
    ) throws Exception {
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
