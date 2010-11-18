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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedComponent;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.file.FileUtil;
import org.rhq.plugins.jbossas5.connection.ProfileServiceConnection;
import org.rhq.plugins.jbossas5.util.ManagedComponentUtils;

/**
 *
 * @author Lukas Krejci
 */
public class RemoteDownloader implements PackageDownloader {

    private ResourceContext<?> resourceContext;
    private boolean resourceExists;
    private ProfileServiceConnection profileServiceConnection;
    private static final Log LOG = LogFactory.getLog(RemoteDownloader.class);

    /**
     * @param resourceContext the resource context if the resource exists or
     * the parent resource context if the resource doesn't exist yet 
     * @param resourceExists this tells the downloader which API to use. This has to go
     * hand in hand with the resourceContext that is being provided.
     * @param profileServiceConnection
     */
    public RemoteDownloader(ResourceContext<?> resourceContext, boolean resourceExists, ProfileServiceConnection profileServiceConnection) {
        this.resourceContext = resourceContext;        
        this.resourceExists = resourceExists;
        this.profileServiceConnection = profileServiceConnection;
    }

    public File prepareArchive(PackageDetailsKey key, ResourceType resourceType) {
        //we're running in the agent. During the development of this functionality, there was
        //a time when the deployment only worked from within the JBossAS server home.
        //Further investigation never confirmed the problem again but since we have access to
        //server home directory anyway, why not stay on the safe side... ;)
        OutputStream os = null;
        
        try {
            File tempDir = FileUtil.createTempDirectory("jopr-jbossas5-deploy-content", null, getServerTempDirectory(profileServiceConnection));
        
            File archiveFile = new File(key.getName());
        
            //this is to ensure that we only get the filename part no matter whether the key contains
            //full path or not.
            File contentCopy = new File(tempDir, archiveFile.getName());
        
            os = new BufferedOutputStream(new FileOutputStream(contentCopy));
            ContentContext contentContext = resourceContext.getContentContext();
            ContentServices contentServices = contentContext.getContentServices();
            
            if (resourceExists) {
                contentServices.downloadPackageBits(contentContext, key, os, true);
            } else {
                contentServices.downloadPackageBitsForChildResource(contentContext, resourceType.getName(), key, os);
            }
            
            return contentCopy;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to copy the deployed archive to destination.", e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    LOG.warn("Failed to close the stream when copying deployment to destination.");
                }
            }
        }
    }

    public void destroyArchive(File archive) {
        File tempDir = archive.getParentFile();
        archive.delete();
        tempDir.delete();
    }

    private static File getServerTempDirectory(ProfileServiceConnection profileServiceConnection) {
        ManagementView managementView = profileServiceConnection.getManagementView();
        ManagedComponent serverConfigComponent = ManagedComponentUtils.getSingletonManagedComponent(managementView,
            new ComponentType("MCBean", "ServerConfig"));
        String serverTempDir = (String) ManagedComponentUtils.getSimplePropertyValue(serverConfigComponent,
            "serverTempDir");

        return new File(serverTempDir);
    }            
}
