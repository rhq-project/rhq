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

    private String userSpecifiedResourceName;
    private ResourceType resourceType;
    private Configuration pluginConfiguration;
    private Configuration resourceConfiguration;
    private ResourcePackageDetails packageDetails;

    // Data set by the plugin after resource creation ----------

    private String resourceName;
    private String resourceKey;
    private CreateResourceStatus status;
    private String errorMessage;
    private Throwable exception;

    // Constructors  --------------------------------------------

    /**
     * Creates a report with immutable information the plugin should not be able to change. The plugin will add the
     * necessary details to this report to indicate the result of the creation.
     *
     * @param userSpecifiedResourceName resource name the user gave to the resource at creation time;
     *                                  may be <code>null</code>
     * @param resourceType              type of resource being created
     * @param pluginConfiguration       user specified plugin configuration to connect to the resource;
     *                                  may be <code>null</code>
     * @param resourceConfiguration     any configuration values necessary for the resource; may be <code>null</code>
     * @param packageDetails            information regarding the package to be deployed when creating the new
     *                                  resource; may be <code>null</code> 
     */
    public CreateResourceReport(String userSpecifiedResourceName, ResourceType resourceType, Configuration pluginConfiguration,
        Configuration resourceConfiguration, ResourcePackageDetails packageDetails) {
        this.userSpecifiedResourceName = userSpecifiedResourceName;
        this.resourceType = resourceType;
        this.pluginConfiguration = pluginConfiguration;
        this.resourceConfiguration = resourceConfiguration;
        this.packageDetails = packageDetails;
    }

    // Public  --------------------------------------------

    public String getUserSpecifiedResourceName() {
        return userSpecifiedResourceName;
    }

    public void setUserSpecifiedResourceName(String userSpecifiedResourceName) {
        this.userSpecifiedResourceName = userSpecifiedResourceName;
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

    public String getResourceName() {
        return resourceName;
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