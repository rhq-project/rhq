 /*
  * RHQ Management Platform
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
package org.rhq.core.domain.content.transfer;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.PackageDetails;
import org.rhq.core.domain.content.PackageDetailsKey;

/**
 * Contains data to describe a package that has been (or is in the process of being) installed on a resource. This
 * couples the package information with the deployment information, such as the deployment time configuration values.
 * This is heavily used in both the server <-> agent communications as well as the agent <-> plugin APIs.
 *
 * @author Jason Dobies
 */
public class ResourcePackageDetails extends PackageDetails {
    // Attributes  --------------------------------------------

    /**
     * Values the plugin should use when installing this package.
     */
    private Configuration deploymentTimeConfiguration;

    // Constructors  --------------------------------------------

    public ResourcePackageDetails(PackageDetailsKey key) {
        super(key);
    }

    // Public  --------------------------------------------

    public Configuration getDeploymentTimeConfiguration() {
        return deploymentTimeConfiguration;
    }

    public void setDeploymentTimeConfiguration(Configuration deploymentTimeConfiguration) {
        this.deploymentTimeConfiguration = deploymentTimeConfiguration;
    }
}