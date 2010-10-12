/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;

import org.rhq.core.domain.resource.Resource;

/**
 * @author jay shaughnessy
 */
public class ResourceLineageComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private Resource resource;
    private boolean isLocked;

    // for gwt serialization
    @SuppressWarnings("unused")
    private ResourceLineageComposite() {
    }

    public ResourceLineageComposite(Resource resource, boolean isLocked) {
        this.resource = resource;
        this.isLocked = isLocked;
    }

    public Resource getResource() {
        return resource;
    }

    public boolean isLocked() {
        return isLocked;
    }

    /* 
     * Uses Resource.equals() so composites for the same Resource will be equal.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ResourceLineageComposite)) {
            return false;
        }

        ResourceLineageComposite other = (ResourceLineageComposite) obj;
        return this.resource.equals(other.getResource());
    }

    /*
     * Uses Resource.hascode()
     */
    @Override
    public int hashCode() {
        return this.resource.hashCode();
    }

}