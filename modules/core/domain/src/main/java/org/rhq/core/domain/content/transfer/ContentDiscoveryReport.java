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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.rhq.core.domain.resource.Resource;

/**
 * Transfer object used to send {@link org.rhq.core.domain.content.InstalledPackage}s from the {@link Resource} to the
 * server.
 *
 * @author Jason Dobies
 */
public class ContentDiscoveryReport implements Serializable {
    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    /**
     * Resource against which the installed package set applies.
     */
    private int resourceId;

    /**
     * Full set of installed packages on the resource. Packages that were previously returned in a similar fashion but
     * not in this set will be marked as deleted.
     */
    private Set<ResourcePackageDetails> deployedPackages = new HashSet<ResourcePackageDetails>();

    // Public  --------------------------------------------

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public Set<ResourcePackageDetails> getDeployedPackages() {
        return deployedPackages;
    }

    public void addDeployedPackage(ResourcePackageDetails deployedPackage) {
        this.deployedPackages.add(deployedPackage);
    }

    public void addAllDeployedPackages(Collection<ResourcePackageDetails> deployedPackages) {
        this.deployedPackages.addAll(deployedPackages);
    }

    public String toString() {
        return "ContentDiscoveryReport[ResourceId=" + resourceId + ", Package Count=" + deployedPackages.size() + "]";
    }
}