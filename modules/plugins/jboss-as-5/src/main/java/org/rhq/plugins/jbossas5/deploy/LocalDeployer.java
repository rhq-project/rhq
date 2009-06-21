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

import java.io.File;

import org.jboss.profileservice.spi.ProfileService;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A deployer used when running inside the JBoss AS 5 process.
 * 
 * @author Lukas Krejci
 */
public class LocalDeployer extends AbstractDeployer {

    /**
     * @param profileService
     */
    public LocalDeployer(ProfileService profileService) {
        super(profileService);
    }

    @Override
    protected void destroyArchive(File archive) {
        archive.delete();
    }

    @Override
    protected File prepareArchive(PackageDetailsKey key, ResourceType resourceType) {
        //the name of the package is set to the full path to the deployed file
        return new File(key.getName());
    }

}
