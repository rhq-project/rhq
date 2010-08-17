/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.common;

/**
 * Category is assigned with the following preference assuming multiple categories can be satisfied with the available context information:
 * <pre>
 * ResourceGroup
 * AutoGroup
 * ResourceTemplate
 * Resource
 * <pre>
 * 
 * @author Joseph Marques
 */
public class EntityContext {

    public enum Category {
        Resource, // 
        ResourceTemplate, //
        ResourceGroup, // 
        AutoGroup;
    }

    public final Category category;
    public final int resourceId;
    public final int groupId;
    public final int parentResourceId;
    public final int resourceTypeId;

    public Category getCategory() {
        return category;
    }

    public int getResourceId() {
        return resourceId;
    }

    public int getGroupId() {
        return groupId;
    }

    public int getParentResourceId() {
        return parentResourceId;
    }

    public int getResourceTypeId() {
        return resourceTypeId;
    }

    public EntityContext(Integer resourceId, Integer groupId, Integer parentResourceId, Integer resourceTypeId) {
        int rId = (resourceId != null && resourceId > 0) ? resourceId : -1;
        int gId = (groupId != null && groupId > 0) ? groupId : -1;
        int prId = (parentResourceId != null && parentResourceId > 0) ? parentResourceId : -1;
        int rtId = (resourceTypeId != null && resourceTypeId > 0) ? resourceTypeId : -1;

        this.resourceId = rId;
        this.groupId = gId;
        this.parentResourceId = prId;
        this.resourceTypeId = rtId;

        if (this.groupId > 0) {
            category = Category.ResourceGroup;
        } else if (this.resourceTypeId > 0) {
            if (this.parentResourceId > 0) {
                category = Category.AutoGroup;
            } else {
                category = Category.ResourceTemplate;
            }
        } else if (this.resourceId > 0) {
            category = Category.Resource;
        } else {
            throw new IllegalArgumentException(getUnknownContextMessage());
        }
    }

    public static EntityContext forResource(int resourceId) {
        return new EntityContext(resourceId, null, null, null);
    }

    public static EntityContext forGroup(int groupId) {
        return new EntityContext(null, groupId, null, null);
    }

    public static EntityContext forAutoGroup(int parentResourceId, int resourceTypeId) {
        return new EntityContext(null, null, parentResourceId, resourceTypeId);
    }

    public String getLegacyKey() {
        switch (category) {
        case Resource:
            return String.valueOf(resourceId);
        case ResourceGroup:
            return "cg=" + String.valueOf(groupId);
        case AutoGroup:
            return "ag=" + String.valueOf(parentResourceId) + ":" + String.valueOf(resourceTypeId);
        default:
            throw new IllegalArgumentException(getUnknownContextMessage());
        }
    }

    public String getUnknownContextMessage() {
        return "Unsupported " + EntityContext.class.getSimpleName() + " '" + this + "'";
    }

    @Override
    public String toString() {
        return EntityContext.class.getSimpleName() + "[category=" + category + ",resourceId=" + resourceId + ","
            + "groupId=" + groupId + "," + "parent=" + parentResourceId + "," + "type=" + resourceTypeId + "]";
    }

    public String toShortString() {
        switch (category) {
        case Resource:
            return "resource[id=" + resourceId + "]";
        case ResourceGroup:
            return "resourceGroup[groupId=" + resourceId + "]";
        case AutoGroup:
            return "autoGroup[parent=" + parentResourceId + ",type=" + resourceTypeId + "]";
        default:
            return toString();
        }
    }
}
