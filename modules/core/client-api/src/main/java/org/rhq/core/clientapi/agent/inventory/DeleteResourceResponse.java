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
package org.rhq.core.clientapi.agent.inventory;

import java.io.Serializable;

import org.rhq.core.domain.resource.DeleteResourceStatus;

/**
 * @author Jason Dobies
 */
public class DeleteResourceResponse implements Serializable {
    // Attributes  --------------------------------------------

    private int requestId;
    private int resourceId;
    private DeleteResourceStatus status;
    private String errorMessage;

    // Constructors  --------------------------------------------

    public DeleteResourceResponse() {
    }

    public DeleteResourceResponse(int requestId, int resourceId, DeleteResourceStatus status, String errorMessage) {
        this.requestId = requestId;
        this.resourceId = resourceId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Attributes  --------------------------------------------

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

    public DeleteResourceStatus getStatus() {
        return status;
    }

    public void setStatus(DeleteResourceStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Object Overridden Methods  --------------------------------------------

    public String toString() {
        return "DeleteResourceResponse[RequestId=" + requestId + ", Status=" + status + "]";
    }
}