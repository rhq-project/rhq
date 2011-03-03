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
package org.rhq.enterprise.server.plugin.pc.content;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The key to a content source package details that a {@link ContentProvider} will use when referring to package
 * versions it finds in the remote repository. It is the same as {@link PackageDetailsKey} with the addition of a
 * resource type natural key (which is name and agent plugin name), since that is needed to make package types unique
 * (along with the package type name itself, which is specified in the {@link PackageDetailsKey} superclass).
 */
public class ContentProviderPackageDetailsKey extends PackageDetailsKey {
    private static final long serialVersionUID = 1L;

    private final String resourceTypeName;
    private final String resourceTypePluginName;

    public ContentProviderPackageDetailsKey(String name, String version, String packageTypeName,
        String architectureName, String resourceTypeName, String resourceTypePluginName) {
        super(name, version, packageTypeName, architectureName);

        if (resourceTypeName == null) {
            throw new IllegalArgumentException("resourceTypeName cannot be null");
        }

        if (resourceTypePluginName == null) {
            throw new IllegalArgumentException("resourceTypePluginName cannot be null");
        }

        this.resourceTypeName = resourceTypeName;
        this.resourceTypePluginName = resourceTypePluginName;
    }

    /**
     * The name of the {@link ResourceType} that this package's type belongs to. Package types are usually defined and
     * supported by a particular {@link ResourceType}, this name is part of the natural key of a resource type. See
     * {@link #getResourceTypePluginName() plugin name} for the other part.
     *
     * @return resource type name or null if the package type isn't tied to a resource type
     */
    public String getResourceTypeName() {
        return resourceTypeName;
    }

    /**
     * The name of the plugin that defined the {@link ResourceType} that this package's type belongs to. Package
     * types are usually defined and supported by a particular {@link ResourceType}, this plugin name is part of the natural key
     * of a resource type. See {@link #getResourceTypeName() resource type name} for the other part.
     *
     * @return the name of the plugin that defines the resource type that defined the package type or null if the package
     * type isn't tied to a resource type
     */
    public String getResourceTypePluginName() {
        return resourceTypePluginName;
    }

    @Override
    public String toString() {
        return "ContentProviderPackageDetailsKey[" + super.toString() + ", ResourceTypeName=" + resourceTypeName
            + ", PluginName=" + resourceTypePluginName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + resourceTypeName.hashCode();
        result = (prime * result) + resourceTypePluginName.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof ContentProviderPackageDetailsKey)) {
            return false;
        }

        final ContentProviderPackageDetailsKey other = (ContentProviderPackageDetailsKey) obj;

        if (!resourceTypeName.equals(other.resourceTypeName)) {
            return false;
        }

        if (!resourceTypePluginName.equals(other.resourceTypePluginName)) {
            return false;
        }

        return true;
    }
}