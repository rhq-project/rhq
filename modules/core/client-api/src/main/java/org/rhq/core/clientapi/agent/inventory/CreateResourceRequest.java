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

/**
 * Transfer object for requesting a new resource be created.
 *
 * @author Jason Dobies
 */
public class CreateResourceRequest implements Serializable {
    // Attributes  --------------------------------------------

    // General ---------

    private int requestId;
    private int parentResourceId;
    private String resourceTypeName;
    private String pluginName;
    private String resourceName;

    // Configuration ----------

    private Configuration resourceConfiguration;
    private Configuration pluginConfiguration;

    // Artifact ----------

    private String packageName;
    private String packageTypeName;
    private Configuration deploymentTimeConfiguration;

    // Constructors  --------------------------------------------

    public CreateResourceRequest() {
    }

    public CreateResourceRequest(int requestId, int parentResourceId, String resourceName, String resourceTypeName,
        String pluginName, Configuration pluginConfiguration, Configuration resourceConfiguration) {
        this.resourceName = resourceName;
        this.requestId = requestId;
        this.parentResourceId = parentResourceId;
        this.resourceTypeName = resourceTypeName;
        this.pluginName = pluginName;
        this.pluginConfiguration = pluginConfiguration;
        this.resourceConfiguration = resourceConfiguration;
    }

    public CreateResourceRequest(int requestId, int parentResourceId, String resourceName, String resourceTypeName,
        String pluginName, Configuration pluginConfiguration, String packageName, String packageTypeName,
        Configuration deploymentTimeConfiguration) {
        this.resourceName = resourceName;
        this.requestId = requestId;
        this.parentResourceId = parentResourceId;
        this.resourceTypeName = resourceTypeName;
        this.pluginName = pluginName;
        this.pluginConfiguration = pluginConfiguration;
        this.packageName = packageName;
        this.packageTypeName = packageTypeName;
        this.deploymentTimeConfiguration = deploymentTimeConfiguration;
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

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageTypeName() {
        return packageTypeName;
    }

    public void setPackageTypeName(String packageTypeName) {
        this.packageTypeName = packageTypeName;
    }

    public Configuration getDeploymentTimeConfiguration() {
        return deploymentTimeConfiguration;
    }

    public void setDeploymentTimeConfiguration(Configuration deploymentTimeConfiguration) {
        this.deploymentTimeConfiguration = deploymentTimeConfiguration;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return "CreateResourceRequest[RequestId=" + requestId + ",ParentResourceId=" + parentResourceId
            + ",ResourceType=" + resourceTypeName + ", PluginName=" + pluginName + "]";
    }
}