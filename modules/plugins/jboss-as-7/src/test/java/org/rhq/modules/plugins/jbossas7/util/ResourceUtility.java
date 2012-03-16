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
package org.rhq.modules.plugins.jbossas7.util;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * TODO: Move these method to a utils module.
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

    private ResourceUtility() {
    }

}
