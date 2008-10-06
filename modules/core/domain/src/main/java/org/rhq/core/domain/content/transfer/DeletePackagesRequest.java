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
import java.util.Set;

/**
 * Transfer object used to carry information about a request to delete packages from a resource.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public class DeletePackagesRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int requestId;
    private final int resourceId;
    private final Set<ResourcePackageDetails> packages;

    public DeletePackagesRequest(int requestId, int resourceId, Set<ResourcePackageDetails> packages) {
        if (packages == null)
            throw new IllegalArgumentException("packages cannot be null");

        this.requestId = requestId;
        this.resourceId = resourceId;
        this.packages = packages;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public Set<ResourcePackageDetails> getPackages() {
        return packages;
    }

    @Override
    public String toString() {
        return "DeletePackagesRequest[RequestId=" + requestId + ", ResourceId=" + resourceId + ", NumPackages="
            + packages.size();
    }
}