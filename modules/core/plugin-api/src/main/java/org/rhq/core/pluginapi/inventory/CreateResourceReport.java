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
package org.rhq.core.pluginapi.inventory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Value object used between the plugin container and a plugin to carry all data necessary for a create resource
 * operation as well as the resulting status code.
 *
 * @author Jason Dobies 
 */
public class CreateResourceReport {
    // Attributes  --------------------------------------------

    // Data used by the plugin to create the resource ----------

    private String resourceName;
    private ResourceType resourceType;
    private Configuration pluginConfiguration;
    private Configuration resourceConfiguration;
    private ResourcePackageDetails packageDetails;

    // Data set by the plugin after resource creation ----------

    private String resourceKey;
    private CreateResourceStatus status;
    private String errorMessage;
    private Throwable exception;

    // Constructors  --------------------------------------------

    /**
     * TODO
     *
     * @param resourceName
     * @param resourceType
     * @param pluginConfiguration
     * @param resourceConfiguration
     * @param resourcePackageDetails
     */
    public CreateResourceReport(String resourceName, ResourceType resourceType, Configuration pluginConfiguration,
        Configuration resourceConfiguration, ResourcePackageDetails packageDetails) {
        this.resourceName = resourceName;
        this.resourceType = resourceType;
        this.pluginConfiguration = pluginConfiguration;
        this.resourceConfiguration = resourceConfiguration;
        this.packageDetails = packageDetails;
    }

    // Public  --------------------------------------------

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setResourceKey(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setStatus(CreateResourceStatus status) {
        this.status = status;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public Configuration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public ResourcePackageDetails getPackageDetails() {
        return packageDetails;
    }

    public String getResourceKey() {
        return resourceKey;
    }

    public CreateResourceStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Throwable getException() {
        return exception;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "CreateResourceReport: ResourceType=[" + resourceType + "], ResourceKey=[" + resourceKey + "]";
    }
}