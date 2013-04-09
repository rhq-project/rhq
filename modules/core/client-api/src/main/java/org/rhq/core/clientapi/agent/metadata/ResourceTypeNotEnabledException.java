/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.clientapi.agent.metadata;

import org.rhq.core.domain.resource.ResourceType;

/**
 * Indicates a specific resource type is not enabled and no resources should be instantiated
 * that are of this type.
 *
 * @author John Mazzitelli
 */
public class ResourceTypeNotEnabledException extends Exception {
    private static final long serialVersionUID = 1L;

    private final ResourceType resourceType;

    public ResourceTypeNotEnabledException() {
        this((ResourceType) null);
    }

    public ResourceTypeNotEnabledException(ResourceType rt) {
        super();
        this.resourceType = rt;
    }

    public ResourceTypeNotEnabledException(String message) {
        this(message, null);
    }

    public ResourceTypeNotEnabledException(String message, ResourceType rt) {
        super(message);
        this.resourceType = rt;
    }

    /**
     * @return the resource type that was not enabled.
     */
    public ResourceType getResourceType() {
        return this.resourceType;
    }

    @Override
    public String toString() {
        return super.toString() + " [" + this.resourceType + "]";
    }
}
