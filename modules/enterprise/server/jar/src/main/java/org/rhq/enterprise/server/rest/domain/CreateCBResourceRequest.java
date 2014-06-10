/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.rest.domain;

import java.util.HashMap;
import java.util.Map;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * Request to create a content based resource
 * @author Heiko W. Rupp
 */
@ApiClass("A request to create a content-based resource. For this to work, it is required that the content to be deployed is already uploaded to the server.")
public class CreateCBResourceRequest extends ResourceWithType {

    Map<String,Object> pluginConfig = new HashMap<String, Object>();
    Map<String,Object> resourceConfig = new HashMap<String, Object>();

    @ApiProperty("The configuration of the connection properties")
    public Map<String, Object> getPluginConfig() {
        return pluginConfig;
    }

    public void setPluginConfig(Map<String, Object> pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    @ApiProperty("The configuration of the resource to be created")
    public Map<String, Object> getResourceConfig() {
        return resourceConfig;
    }

    public void setResourceConfig(Map<String, Object> resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CreateCBResourceRequest[");
        sb.append("resourceName='").append(resourceName).append('\'');
        sb.append(", resourceId=").append(resourceId);
        sb.append(", typeName='").append(typeName).append('\'');
        sb.append(", typeId=").append(typeId);
        sb.append(", pluginName='").append(pluginName).append('\'');
        sb.append(", parentId=").append(parentId);
        sb.append(", ancestry='").append(getAncestry()).append('\'');
        sb.append(", pluginConfig=").append(pluginConfig);
        sb.append(", resourceConfig=").append(resourceConfig);
        sb.append(']');
        return sb.toString();
    }
}
