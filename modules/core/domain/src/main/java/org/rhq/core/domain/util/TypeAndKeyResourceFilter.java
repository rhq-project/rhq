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

package org.rhq.core.domain.util;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @since 4.4
 * @author Ian Springer
 */
public class TypeAndKeyResourceFilter implements ResourceFilter {

    private ResourceType resourceType;
    private String resourceKey;

    public TypeAndKeyResourceFilter(ResourceType resourceType, String resourceKey) {
        this.resourceType = resourceType;
        this.resourceKey = resourceKey;
    }

    @Override
    public boolean accept(Resource resource) {
        return (resource.getResourceType().equals(resourceType) && resource.getResourceKey().equals(resourceKey));
    }

}
