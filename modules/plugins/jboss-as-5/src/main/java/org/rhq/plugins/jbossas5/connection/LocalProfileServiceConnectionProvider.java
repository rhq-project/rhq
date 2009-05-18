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

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.profileservice.spi.ProfileService;

/**
 * @author Ian Springer
 */
public class LocalProfileServiceConnectionProvider extends AbstractProfileServiceConnectionProvider {
    private static final String PROFILE_SERVICE_JNDI_NAME = "ProfileService";

    private final Log log = LogFactory.getLog(this.getClass());

    protected ProfileServiceConnectionImpl doConnect() {
        log.debug("Connecting to Profile Service via local JNDI...");
        InitialContext initialContext = createInitialContext(null);
        ProfileService profileService = (ProfileService) lookup(initialContext, PROFILE_SERVICE_JNDI_NAME);
        ManagementView managementView = profileService.getViewManager();
        DeploymentManager deploymentManager = profileService.getDeploymentManager();
        return new ProfileServiceConnectionImpl(this, profileService, managementView, deploymentManager);
    }

    protected void doDisconnect() {
        return;
    }
}
