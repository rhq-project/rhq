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
    private String resourceName;
    private String resourceKey;
    private CreateResourceStatus status;
    private String errorMessage;
    private Configuration resourceConfiguration;

    // Constructors  --------------------------------------------

    public CreateResourceResponse() {
    }

    public CreateResourceResponse(int requestId, String resourceName, String resourceKey,
                                  CreateResourceStatus status, String errorMessage,
                                  Configuration resourceConfiguration) {
        this.requestId = requestId;
        this.resourceName = resourceName;
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

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
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