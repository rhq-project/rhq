/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A set of utility methods for working with {@link Resource}s.
 *
 * @since 4.4
 * @author Ian Springer
 */
public abstract class ResourceUtility {

    public static Resource getChildResource(Resource parent, ResourceType type, String key) {
        for (Resource resource : parent.getChildResources()) {
            if (resource.getResourceType().equals(type) && resource.getResourceKey().equals(key)) {
                return resource;
            }
        }
        return null;
    }

    /**
     * Returns the set of child Resources defined by the given parent Resource, which are accepted by the specified
     * filter. If the filter is null, all of the parent Resource's children will be returned.
     *
     * @param parentResource the parent Resource
     * @param filter the filter; may be null
     *
     * @return the set of MeasurementDefinition defined by the given Resource type, which are accepted by the specified
     *         filter, or, if the filter is null, all of the type's MeasurementDefinitions
     */
    @NotNull
    public static Set<Resource> getChildResources(Resource parentResource,
                                                  ResourceFilter filter) {
        if (filter == null) {
            return parentResource.getChildResources();
        }
        Set<Resource> acceptedChildResources = new LinkedHashSet<Resource>();
        for (Resource childResource : parentResource.getChildResources()) {
            if (filter.accept(childResource)) {
                acceptedChildResources.add(childResource);
            }
        }
        return acceptedChildResources;
    }

    private ResourceUtility() {
    }

}
