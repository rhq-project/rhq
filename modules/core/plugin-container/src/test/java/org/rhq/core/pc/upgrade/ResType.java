/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.core.pc.upgrade;

/**
 * A helper class representing a resource type in a simplistic manner.
 * 
 *
 * @author Lukas Krejci
 */
class ResType {
    private String resourceTypeName;
    private String resourceTypePluginName;

    public ResType(String resourceTypeName, String resourceTypePluginName) {
        super();
        this.resourceTypeName = resourceTypeName;
        this.resourceTypePluginName = resourceTypePluginName;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public String getResourceTypePluginName() {
        return resourceTypePluginName;
    }

    @Override
    public int hashCode() {
        return resourceTypeName.hashCode() * resourceTypePluginName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof ResType)) {
            return false;
        }

        ResType o = (ResType)other;

        return resourceTypeName.equals(o.getResourceTypeName()) && resourceTypePluginName.equals(o.getResourceTypePluginName());
    }

    @Override
    public String toString() {
        return "ResType[name='" + resourceTypeName + "', plugin='" + resourceTypePluginName + "']";
    }
}