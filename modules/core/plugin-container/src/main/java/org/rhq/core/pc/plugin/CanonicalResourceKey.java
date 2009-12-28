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
package org.rhq.core.pc.plugin;

import java.io.Serializable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This object represents a canonical ID that can uniquely identify a resource
 * within a resource hierarchy.
 * 
 * @author John Mazzitelli
 */
public class CanonicalResourceKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String resourceKey;
    private final String resourceTypeName;
    private final String resourcePlugin;
    private final String parentKey;
    private final String parentTypeName;
    private final String parentPlugin;

    public CanonicalResourceKey(Resource resource, Resource parent) throws PluginContainerException {
        if (resource == null) {
            throw new PluginContainerException("resource must not be null");
        }
        if (parent == null) {
            throw new PluginContainerException("parent must not be null");
        }

        ResourceType resourceType = resource.getResourceType();
        ResourceType parentType = parent.getResourceType();

        if (resourceType == null) {
            throw new PluginContainerException("resource type must not be null");
        }
        if (parentType == null) {
            throw new PluginContainerException("parent type must not be null");
        }

        this.resourceKey = resource.getResourceKey();
        this.resourceTypeName = resourceType.getName();
        this.resourcePlugin = resourceType.getPlugin();
        this.parentKey = parent.getResourceKey();
        this.parentTypeName = parentType.getName();
        this.parentPlugin = parentType.getPlugin();

        if (this.resourceKey == null) {
            throw new PluginContainerException("resource key must not be null");
        }
        if (this.resourceTypeName == null) {
            throw new PluginContainerException("resource type name must not be null");
        }
        if (this.resourcePlugin == null) {
            throw new PluginContainerException("resource plugin must not be null");
        }

        if (this.parentKey == null) {
            throw new PluginContainerException("parent key must not be null");
        }
        if (this.parentTypeName == null) {
            throw new PluginContainerException("parent type name must not be null");
        }
        if (this.parentPlugin == null) {
            throw new PluginContainerException("parent plugin must not be null");
        }
    }

    public String getResourceKey() {
        return this.resourceKey;
    }

    public String getResourceTypeName() {
        return this.resourceTypeName;
    }

    public String getResourcePlugin() {
        return this.resourcePlugin;
    }

    public String getParentKey() {
        return this.parentKey;
    }

    public String getParentTypeName() {
        return this.parentTypeName;
    }

    public String getParentPlugin() {
        return this.parentPlugin;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        builder.append("resourceKey=").append(this.resourceKey).append(",");
        builder.append("resourceTypeName=").append(this.resourceTypeName).append(",");
        builder.append("resourcePlugin=").append(this.resourcePlugin).append(",");
        builder.append("parentKey=").append(this.parentKey).append(",");
        builder.append("parentTypeName=").append(this.parentTypeName).append(",");
        builder.append("parentPlugin=").append(this.parentPlugin);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.resourceKey.hashCode();
        result = prime * result + this.resourceTypeName.hashCode();
        result = prime * result + this.resourcePlugin.hashCode();
        result = prime * result + this.parentKey.hashCode();
        result = prime * result + this.parentTypeName.hashCode();
        result = prime * result + this.parentPlugin.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CanonicalResourceKey)) {
            return false;
        }

        CanonicalResourceKey other = (CanonicalResourceKey) obj;

        if (!this.resourceKey.equals(other.resourceKey)) {
            return false;
        }

        if (!this.resourceTypeName.equals(other.resourceTypeName)) {
            return false;
        }

        if (!this.resourcePlugin.equals(other.resourcePlugin)) {
            return false;
        }

        if (!this.parentKey.equals(other.parentKey)) {
            return false;
        }

        if (!this.parentTypeName.equals(other.parentTypeName)) {
            return false;
        }

        if (!this.parentPlugin.equals(other.parentPlugin)) {
            return false;
        }

        return true;
    }
}
