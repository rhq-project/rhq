/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.plugins.jbossas5.deploy;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.transfer.ContentResponseResult;
import org.rhq.core.domain.content.transfer.DeployIndividualPackageResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jbossas5.script.ScriptFileFinder;

/**
 * This implementation merely copies the script file into the bin directory of the server.
 * 
 * @author Lukas Krejci
 */
public class ScriptDeployer implements Deployer {

    private static final String PROP_TARGET_FILE_NAME = "targetFileName";

    private PackageDownloader downloader;
    private File binDir;
    private SystemInfo systemInfo;

    public ScriptDeployer(String jbossHomeDir, SystemInfo systemInfo, PackageDownloader downloader) {
        this.downloader = downloader;
        binDir = new File(jbossHomeDir, "bin");
        this.systemInfo = systemInfo;
    }

    public void deploy(CreateResourceReport createResourceReport, ResourceType resourceType) {
        File script = null;
        try {
            ResourcePackageDetails details = createResourceReport.getPackageDetails();
            script = downloader.prepareArchive(details.getKey(), resourceType);

            String targetFileName = script.getName();

            Configuration deploymentConfig = details.getDeploymentTimeConfiguration();
            if (deploymentConfig != null) {
                targetFileName = deploymentConfig.getSimpleValue(PROP_TARGET_FILE_NAME, targetFileName);
            }

            File target = new File(binDir, targetFileName);

            if (!ScriptFileFinder.getScriptFileFilter(systemInfo).accept(target)) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("The supplied filename has a wrong extension. One of: "
                    + ScriptFileFinder.getSupportedFileExtensions(systemInfo) + " was expected.");

                return;
            }

            if (!target.createNewFile()) {
                createResourceReport.setStatus(CreateResourceStatus.FAILURE);
                createResourceReport.setErrorMessage("A file called '" + targetFileName
                    + "' already exists in the server bin directory.");

                return;
            }

            FileUtil.copyFile(script, target);

            createResourceReport.setResourceKey(target.getAbsolutePath());
            createResourceReport.setResourceName(targetFileName);

            createResourceReport.setStatus(CreateResourceStatus.SUCCESS);
        } catch (Exception e) {
            createResourceReport.setException(e);
            createResourceReport.setStatus(CreateResourceStatus.FAILURE);
        } finally {
            if (script != null) {
                downloader.destroyArchive(script);
            }
        }
    }

    public DeployIndividualPackageResponse update(ResourcePackageDetails existingPackage, ResourceType resourceType) {
        File script = null;
        DeployIndividualPackageResponse response = new DeployIndividualPackageResponse(existingPackage.getKey());
        
        try {
            
            ResourcePackageDetails details = existingPackage;
            script = downloader.prepareArchive(details.getKey(), resourceType);

            String targetFileName = script.getName();

            Configuration deploymentConfig = details.getDeploymentTimeConfiguration();
            if (deploymentConfig != null) {
                targetFileName = deploymentConfig.getSimpleValue(PROP_TARGET_FILE_NAME, targetFileName);
            }

            File target = new File(binDir, targetFileName);

            if (!ScriptFileFinder.getScriptFileFilter(systemInfo).accept(target)) {
                response.setResult(ContentResponseResult.FAILURE);
                response.setErrorMessage("The supplied filename has a wrong extension. One of: "
                    + ScriptFileFinder.getSupportedFileExtensions(systemInfo) + " was expected.");

                return response;
            }

            if (!target.exists()) {
                response.setResult(ContentResponseResult.FAILURE);
                response.setErrorMessage("A file called '" + targetFileName
                    + "' already exists in the server bin directory.");

                return response;
            }

            FileUtil.copyFile(script, target);

            response.setResult(ContentResponseResult.SUCCESS);
            
            return response;
        } catch (Exception e) {
            response.setErrorMessage(e.getMessage());
            response.setResult(ContentResponseResult.FAILURE);
            
            return response;
        } finally {
            if (script != null) {
                downloader.destroyArchive(script);
            }
        }
    }
}
