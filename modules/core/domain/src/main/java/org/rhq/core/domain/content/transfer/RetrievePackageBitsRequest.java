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
package org.rhq.core.domain.content.transfer;

import java.io.Serializable;

/**
 * Transfer object used to carry information about a request to retrieve package contents from a resource.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */

public class RetrievePackageBitsRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int requestId;
    private final int resourceId;
    private final ResourcePackageDetails packageDetails;

    public RetrievePackageBitsRequest(int requestId, int resourceId, ResourcePackageDetails packageDetails) {
        if (packageDetails == null) {
            throw new IllegalArgumentException("packageDetails cannot be null");
        }

        this.requestId = requestId;
        this.resourceId = resourceId;
        this.packageDetails = packageDetails;
    }

    public int getRequestId() {
        return requestId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public ResourcePackageDetails getPackageDetails() {
        return packageDetails;
    }

    @Override
    public String toString() {
        return "RetrievePackageBitsRequest[RequestId=" + requestId + ", ResourceId=" + resourceId + ", Package="
            + packageDetails;
    }
}