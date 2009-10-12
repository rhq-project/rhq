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
package org.rhq.plugins.jbossas5.connection;

import java.lang.reflect.Proxy;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.profileservice.spi.ProfileService;

/**
 * @author Ian Springer
 */
public class JaasAuthenticationProxyProfileServiceConnection extends AbstractProfileServiceConnection {    
    private ProfileService profileService;
    private ManagementView managementView;
    private DeploymentManager deploymentManager;

    protected JaasAuthenticationProxyProfileServiceConnection(RemoteProfileServiceConnectionProvider connectionProvider,
        ProfileService profileService, ManagementView managementView, DeploymentManager deploymentManager) {
        super(connectionProvider);

        JaasAuthenticationInvocationHandler profileServiceInvocationHandler =
                new JaasAuthenticationInvocationHandler(profileService,
                    connectionProvider.getPrincipal(), connectionProvider.getCredentials());
        this.profileService = (ProfileService)Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[] {ProfileService.class}, profileServiceInvocationHandler);

        JaasAuthenticationInvocationHandler managementViewInvocationHandler =
                new JaasAuthenticationInvocationHandler(managementView,
                    connectionProvider.getPrincipal(), connectionProvider.getCredentials());
        this.managementView = (ManagementView)Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[] {ManagementView.class}, managementViewInvocationHandler);

        JaasAuthenticationInvocationHandler deploymentManagerInvocationHandler =
                new JaasAuthenticationInvocationHandler(deploymentManager,
                    connectionProvider.getPrincipal(), connectionProvider.getCredentials());
        this.deploymentManager = (DeploymentManager)Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[] {DeploymentManager.class}, deploymentManagerInvocationHandler);
    }

    public ProfileService getProfileService() {
        return this.profileService;
    }

    public ManagementView getManagementView() {
        return this.managementView;
    }

    public DeploymentManager getDeploymentManager() {
        return this.deploymentManager;
    }
}