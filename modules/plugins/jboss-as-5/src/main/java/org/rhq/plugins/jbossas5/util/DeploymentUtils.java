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
package org.rhq.plugins.jbossas5.util;

import java.io.File;

import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.factory.ProfileServiceFactory;

/**
 * @author Ian Springer
 */
public abstract class DeploymentUtils {
    private static final String RESOURCE_TYPE_EAR = "Enterprise Application (EAR)";
    private static final String RESOURCE_TYPE_WAR = "Web Application (WAR)";

    public static boolean isCorrectExtension(ResourceType resourceType, String archiveName)
    {
        String resourceTypeName = resourceType.getName();
        String expectedExtension;
        if (resourceTypeName.equals(RESOURCE_TYPE_EAR)) {
            expectedExtension = "ear";
        } else if (resourceTypeName.equals(RESOURCE_TYPE_WAR)){
            expectedExtension = "war";
        } else {
            expectedExtension = "jar";
        }

        int lastPeriod = archiveName.lastIndexOf(".");
        String extension = archiveName.substring(lastPeriod + 1);
        return (lastPeriod == -1 || !expectedExtension.equals(extension));
    }

    public static DeploymentStatus deployArchive(File tempFile) throws Exception
    {
        DeploymentManager deploymentManager = ProfileServiceFactory.getDeploymentManager();
        String archiveName = tempFile.getName();
        boolean copyContent = true;
        DeploymentProgress progress = deploymentManager.distribute(archiveName, tempFile.toURL(), copyContent);
        //progress.addProgressListener(this);
        progress.run();

        // Get the repository name of the distributed deployment
        String[] repositoryNames = progress.getDeploymentID().getRepositoryNames();
        
        DeploymentStatus status = progress.getDeploymentStatus();
        DeploymentStatus.StateType state = status.getState();
        if (state == DeploymentStatus.StateType.FAILED || state == DeploymentStatus.StateType.CANCELLED)
            return status;

        progress = deploymentManager.start(repositoryNames);
        //progress.addProgressListener(this);
        progress.run();

        status = progress.getDeploymentStatus();
        return status;
    }
}
