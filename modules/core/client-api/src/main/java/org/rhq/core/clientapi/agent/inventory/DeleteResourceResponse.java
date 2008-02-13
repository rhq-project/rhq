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
import org.rhq.core.domain.resource.DeleteResourceStatus;

/**
 * @author Jason Dobies
 */
public class DeleteResourceResponse implements Serializable {
    // Attributes  --------------------------------------------

    private int requestId;
    private DeleteResourceStatus status;
    private String errorMessage;

    // Constructors  --------------------------------------------

    public DeleteResourceResponse() {
    }

    public DeleteResourceResponse(int requestId, DeleteResourceStatus status, String errorMessage) {
        this.requestId = requestId;
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