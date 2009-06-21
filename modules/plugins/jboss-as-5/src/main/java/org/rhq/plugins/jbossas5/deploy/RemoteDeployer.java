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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.profileservice.spi.ProfileService;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

/**
 * A deployer used when running outside the JBoss AS 5 process, i.e. in the agent.
 * 
 * @author Lukas Krejci
 */
public class RemoteDeployer extends AbstractDeployer {

    private ResourceContext<?> resourceContext;

    /**
     * @param profileService
     */
    public RemoteDeployer(ProfileService profileService, ResourceContext<?> resourceContext) {
        super(profileService);
        this.resourceContext = resourceContext;
    }

    @Override
    protected void destroyArchive(File archive) {
        File tempDir = archive.getParentFile();
        archive.delete();
        tempDir.delete();
    }

    @Override
    protected File prepareArchive(PackageDetailsKey key, ResourceType resourceType) {
        //we're running in the agent. For JBoss to be able to deploy a file, the file has to be located
        //somewhere "inside" the running jboss server. So we create a temporary directory in the server's
        //temporary directory and put the the deployed file the created temp dir.
        OutputStream os = null;

        try {
            File tempDir = createTempDirectory("jopr-jbossas5-deploy-content", null, getServerTempDirectory());

            File archiveFile = new File(key.getName());

            //this is to ensure that we only get the filename part no matter whether the key contains
            //full path or not.
            File contentCopy = new File(tempDir, archiveFile.getName());

            os = new BufferedOutputStream(new FileOutputStream(contentCopy));
            ContentContext contentContext = resourceContext.getContentContext();
            ContentServices contentServices = contentContext.getContentServices();
            contentServices.downloadPackageBitsForChildResource(contentContext, resourceType.getName(), key, os);

            return contentCopy;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy the deployed archive to destination.", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    getLog().warn("Failed to close the stream when copying deployment to destination.");
                }
            }
        }
    }

    private File getServerTempDirectory() {
        ManagementView managementView = getProfileService().getViewManager();
        ManagedComponent serverConfigComponent = ManagedComponentUtils.getSingletonManagedComponent(managementView,
            new ComponentType("MCBean", "ServerConfig"));
        String serverTempDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "serverTempDir");

        return new File(serverTempDir);
    }

    /**
     * TODO this should go somewhere nice...
     * 
     * Creates a temporary directory in a given directory.
     * Reuses the logic of {@link File#createTempFile(String, String, File)} but the returned
     * file is a directory instead of a regular file.
     * 
     * @param prefix the unique name prefix
     * @param suffix the unique name suffix
     * @param parentDirectory the parent directory to create the temp dir in
     * 
     * @return the temporary directory
     * 
     * @throws IOException on error
     */
    private static File createTempDirectory(String prefix, String suffix, File parentDirectory) throws IOException {
        // Let's reuse the algorithm the JDK uses to determine a unique name:
        // 1) create a temp file to get a unique name using JDK createTempFile
        // 2) then quickly delete the file and...
        // 3) convert it to a directory

        File tmpDir = File.createTempFile(prefix, suffix, parentDirectory); // create file with unique name
        boolean deleteOk = tmpDir.delete(); // delete the tmp file and...
        boolean mkdirsOk = tmpDir.mkdirs(); // ...convert it to a directory

        if (!deleteOk || !mkdirsOk) {
            throw new IOException("Failed to create temp directory named [" + tmpDir + "]");
        }

        return tmpDir;
    }
}
