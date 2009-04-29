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
package org.rhq.plugins.jbossas5.connection;

import org.jboss.profileservice.spi.ProfileService;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;

/**
 * @author Ian Springer
 */
public class ProfileServiceConnectionImpl implements ProfileServiceConnection
{
    private AbstractProfileServiceConnectionProvider connectionProvider;
    private ProfileService profileService;
    private ManagementView managementView;
    private DeploymentManager deploymentManager;

    protected ProfileServiceConnectionImpl(AbstractProfileServiceConnectionProvider connectionProvider,
                                           ProfileService profileService, ManagementView managementView, DeploymentManager deploymentManager)
    {
        this.connectionProvider = connectionProvider;
        this.profileService = profileService;
        this.managementView = managementView;
        this.deploymentManager = deploymentManager;
    }

    public ProfileServiceConnectionProvider getConnectionProvider()
    {
        return this.connectionProvider;
    }

    public ProfileService getProfileService()
    {
        return this.profileService;
    }

    public ManagementView getManagementView()
    {
        return this.managementView;
    }

    public DeploymentManager getDeploymentManager()
    {
        return this.deploymentManager;
    }
}
