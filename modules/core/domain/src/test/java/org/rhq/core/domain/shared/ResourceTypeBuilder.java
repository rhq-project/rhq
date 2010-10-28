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

package org.rhq.core.domain.shared;

import java.util.HashSet;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * ResourceTypeBuilder is a builder that creates ResourceType objects. The builder ensures that the ResourceType is
 * created in a valid state, specifically fields that are not nullable are required to have non-null values. Using the
 * builder should help make the intent of tests clearer and more self-documenting.
 * <br/><br/>
 * Note that this class currently does not yet provide support for all ResourceType fields/properties.
 *
 * @author John Sanda
 */
public class ResourceTypeBuilder {

    private ResourceType resourceType;

    public ResourceTypeBuilder createResourceType() {
        resourceType = new ResourceType();
        resourceType.setParentResourceTypes(new HashSet<ResourceType>());
        resourceType.setChildResourceTypes(new HashSet<ResourceType>());

        return this;
    }

    public ResourceTypeBuilder createPlatformResourceType() {
        return createResourceType().withCategory(ResourceCategory.PLATFORM);
    }

    public ResourceTypeBuilder createServerResourceType() {
        return createResourceType().withCategory(ResourceCategory.SERVER);
    }

    public ResourceTypeBuilder createServiceResourceType() {
        return createResourceType().withCategory(ResourceCategory.SERVICE);
    }

    public ResourceTypeBuilder withId(int id) {
        resourceType.setId(id);
        return this;
    }

    public ResourceTypeBuilder withName(String name) {
        resourceType.setName(name);
        return this;
    }

    public ResourceTypeBuilder withPlugin(String plugin) {
        resourceType.setPlugin(plugin);
        return this;
    }

    public ResourceTypeBuilder withCategory(ResourceCategory category) {
        resourceType.setCategory(category);
        return this;
    }

    public ResourceTypeBuilder thatIsDeleted() {
        resourceType.setDeleted(true);
        return this;
    }

    public ResourceTypeBuilder withParentResourceType(ResourceType parentResourceType) {
        if (parentResourceType != null) {
            resourceType.addParentResourceType(parentResourceType);
        }
        return this;
    }

    public ResourceTypeBuilder withParentResourceTypes(ResourceType... parentResourceTypes) {
        for (ResourceType parent : parentResourceTypes) {
            resourceType.addParentResourceType(parent);
        }
        return this;
    }

    public ResourceType build() {
        String errors = valdiate();
        if (errors != null) {
            throw new BuilderException(errors);
        }
        
        return resourceType;
    }

    private String valdiate() {
        StringBuilder errors = new StringBuilder();

        if (resourceType.getName() == null) {
           errors.append("name is a required property\n");
        }

        if (resourceType.getCategory() == null) {
            errors.append("category is a required property\n");
        }

        if (resourceType.getCreationDataType() == null) {
            errors.append("creationDate is a required property\n");
        }

        if (resourceType.getCreateDeletePolicy() == null) {
            errors.append("createDeletePolicy is a required property\n");
        }

        if (resourceType.getPlugin() == null) {
            errors.append("plugin is a required property\n");
        }

        if (errors.length() == 0) {
            return null;
        }

        return "Unable to build ResourceType instance due to the following validation errors:\n" + errors;
    }

}
