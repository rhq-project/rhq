/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;

/**
 * Transfer object for requesting a new resource be created.
 *
 * @author Jason Dobies
 */
public class CreateResourceRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    private int requestId;
    private int parentResourceId;
    private String resourceTypeName;
    private String pluginName;
    private String resourceName;
    private Integer timeout;

    // Configuration ----------

    private Configuration resourceConfiguration;
    private Configuration pluginConfiguration;

    // Package ----------

    private ResourcePackageDetails packageDetails;

    // Constructors  --------------------------------------------

    public CreateResourceRequest() {
    }

    public CreateResourceRequest(int requestId, int parentResourceId, String resourceName, String resourceTypeName,
        String pluginName, Configuration pluginConfiguration, Configuration resourceConfiguration, Integer timeout) {
        this.resourceName = resourceName;
        this.requestId = requestId;
        this.parentResourceId = parentResourceId;
        this.resourceTypeName = resourceTypeName;
        this.pluginName = pluginName;
        this.pluginConfiguration = pluginConfiguration;
        this.resourceConfiguration = resourceConfiguration;
        this.timeout = timeout;
    }

    public CreateResourceRequest(int requestId, int parentResourceId, String resourceName, String resourceTypeName,
        String pluginName, Configuration pluginConfiguration, ResourcePackageDetails packageDeatils, Integer timeout) {
        this.resourceName = resourceName;
        this.requestId = requestId;
        this.parentResourceId = parentResourceId;
        this.resourceTypeName = resourceTypeName;
        this.pluginName = pluginName;
        this.pluginConfiguration = pluginConfiguration;
        this.packageDetails = packageDeatils;
        this.timeout = timeout;
    }

    // Public  --------------------------------------------

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getParentResourceId() {
        return parentResourceId;
    }

    public void setParentResourceId(int parentResourceId) {
        this.parentResourceId = parentResourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public void setResourceTypeName(String resourceTypeName) {
        this.resourceTypeName = resourceTypeName;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public Configuration getPluginConfiguration() {
        return pluginConfiguration;
    }

    public void setPluginConfiguration(Configuration pluginConfiguration) {
        this.pluginConfiguration = pluginConfiguration;
    }

    public Configuration getResourceConfiguration() {
        return resourceConfiguration;
    }

    public void setResourceConfiguration(Configuration resourceConfiguration) {
        this.resourceConfiguration = resourceConfiguration;
    }

    public ResourcePackageDetails getPackageDetails() {
        return packageDetails;
    }

    public void setPackageDetails(ResourcePackageDetails packageDetails) {
        this.packageDetails = packageDetails;
    }

    // Object Overridden Methods  --------------------------------------------

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "CreateResourceRequest[RequestId=" + requestId + ",ParentResourceId=" + parentResourceId
            + ",ResourceType=" + resourceTypeName + ",PluginName=" + pluginName + ",Timeout=" + timeout + "]";
    }
}
