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
package org.rhq.core.clientapi.agent.inventory;

import java.io.Serializable;

/**
 * @author Jason Dobies
 */
public class DeleteResourceRequest implements Serializable {
    // Attributes  --------------------------------------------

    private int requestId;
    private int resourceId;

    // Constructors  --------------------------------------------

    public DeleteResourceRequest() {
    }

    public DeleteResourceRequest(int requestId, int resourceId) {
        this.requestId = requestId;
        this.resourceId = resourceId;
    }

    // Public  --------------------------------------------

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    // Object Overridden Methods  --------------------------------------------

    public String toString() {
        return "DeleteResourceRequest[RequestId=" + requestId + ", ResourceId=" + resourceId + "]";
    }
}