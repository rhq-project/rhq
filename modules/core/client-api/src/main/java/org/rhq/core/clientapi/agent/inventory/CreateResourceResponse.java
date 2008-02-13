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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.CreateResourceStatus;

/**
 * Transfer object for indicating the result of a previously submitted request to create a resource.
 *
 * @author Jason Dobies
 */
public class CreateResourceResponse implements Serializable {
    // Attributes  --------------------------------------------

    private int requestId;
    private String resourceKey;
    private CreateResourceStatus status;
    private String errorMessage;
    private Configuration resourceConfiguration;

    // Constructors  --------------------------------------------

    public CreateResourceResponse() {
    }

    public CreateResourceResponse(int requestId, String resourceKey, CreateResourceStatus status, String errorMessage,
        Configuration resourceConfiguration) {
        this.requestId = requestId;
        this.resourceKey = resourceKey;
        this.status = status;
        this.errorMessage = errorMessage;
        this.resourceConfiguration = resourceConfiguration;
    }

    // Public  --------------------------------------------

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public CreateResourceStatus getStatus() {
        return status;
    }

    public void setStatus(CreateResourceStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public void setResourceConfiguration(Configuration resourceConfiguration) {
        this.resourceConfiguration = resourceConfiguration;
    }

    // Object Overridden Methods  --------------------------------------------

    public String toString() {
        return "CreateResourceResponse[RequestId=" + requestId + ", Status=" + status + "]";
    }
}