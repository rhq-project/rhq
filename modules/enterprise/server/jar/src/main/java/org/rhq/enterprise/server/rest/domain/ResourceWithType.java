/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.rest.domain;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.wordnik.swagger.annotations.ApiClass;
import com.wordnik.swagger.annotations.ApiProperty;

/**
 * A (partial) resource with some type information
 * @author Heiko W. Rupp
 */
@ApiClass("One resource")
@XmlRootElement(name="resource")
public class ResourceWithType {

    String resourceName;
    int resourceId;
    String typeName;
    Integer typeId;
    String pluginName;
    Integer parentId;
    String status;
    String availability;
    List<Link> links = new ArrayList<Link>();
    private String ancestry;
    private String location;
    private String description;

    public ResourceWithType() {
    }

    public ResourceWithType(String resourceName, int resourceId) {
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceWithType(int id) {
        this.resourceId = id;
    }

    @ApiProperty("Name of the resource")
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    @ApiProperty("ID of the resource")
    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    @ApiProperty("Name of the resource type of the resource")
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @ApiProperty("Id of the resource type of the resource")
    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    @ApiProperty("Name of the plugin defining the resource type")
    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    @ApiProperty("Id of the parent resource. Can be null if there is no parent (i.e. the category is platform")
    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    @ApiProperty(value = "Inventory status of the resource.", allowableValues = "NEW, IGNORED, COMMITTED, UNINVENTORIED")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public void addLink(Link link) {
        links.add(link);
    }

    public void setAncestry(String ancestry) {
        this.ancestry = ancestry;
    }

    @ApiProperty("The ancestry gives the path to the root resource")
    public String getAncestry() {
        return ancestry;
    }

    @ApiProperty("The availability of the resource at the time of retrieval. " +
        "Please note that when caching the resource, this value may become outdated.")
    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    @Override
    public String toString() {
        return "ResourceWithType{" +
                "resourceName='" + resourceName + '\'' +
                ", resourceId=" + resourceId +
                ", typeName='" + typeName + '\'' +
                ", typeId=" + typeId +
                ", pluginName='" + pluginName + '\'' +
                ", parentId=" + parentId +
                ", ancestry='" + ancestry + '\'' +
                '}';
    }

    @ApiProperty("The location of the resource (e.g. data center / rack")
    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @ApiProperty("A description of the resource")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
