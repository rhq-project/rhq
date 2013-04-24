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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;

import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.ResourceCreationDataType;

/**
 * A resource type
 * @author Heiko W. Rupp
 */
@ApiClass("A resource type")
@XmlRootElement(name = "resourceType")
public class ResourceTypeRest {

    int id;
    String name;
    String pluginName;
    CreateDeletePolicy createPolicy;
    ResourceCreationDataType dataType;

    List<Link> links = new ArrayList<Link>();

    public ResourceTypeRest() {
    }

    public CreateDeletePolicy getCreatePolicy() {
        return createPolicy;
    }

    public void setCreatePolicy(CreateDeletePolicy createPolicy) {
        this.createPolicy = createPolicy;
    }

    public ResourceCreationDataType getDataType() {
        return dataType;
    }

    public void setDataType(ResourceCreationDataType dataType) {
        this.dataType = dataType;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }
}
