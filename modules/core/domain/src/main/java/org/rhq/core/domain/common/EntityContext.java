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
package org.rhq.core.domain.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
public class EntityContext implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        Resource, // 
        ResourceTemplate, //
        ResourceGroup, // 
        AutoGroup, //
        SubsystemView;
    }

    // can't make these fields final because need public no-arg ctor for GWT-compile
    public Type type;
    public int resourceId;
    public int groupId;
    public int parentResourceId;
    public int resourceTypeId;

    public EntityContext() {
    }

    public Type getType() {
        return type;
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
            type = Type.ResourceGroup;
        } else if (this.resourceTypeId > 0) {
            if (this.parentResourceId > 0) {
                type = Type.AutoGroup;
            } else {
                type = Type.ResourceTemplate;
            }
        } else if (this.resourceId > 0) {
            type = Type.Resource;
        } else {
            type = Type.SubsystemView;
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

    public static EntityContext forTemplate(int resourceTypeId) {
        return new EntityContext(null, null, null, resourceTypeId);
    }

    public static EntityContext forSubsystemView() {
        return new EntityContext(null, null, null, null);
    }

    public static EntityContext fromCriteriaMap(Map<String, Object> criteriaMap) {
        return new EntityContext(//
            Integer.valueOf((String) criteriaMap.get("resourceId")), //
            Integer.valueOf((String) criteriaMap.get("groupId")), //
            Integer.valueOf((String) criteriaMap.get("parentResourceId")), //
            Integer.valueOf((String) criteriaMap.get("resourceTypeId")));
    }

    public Map<String, String> toCriteriaMap() {
        Map<String, String> criteriaMap = new HashMap<String, String>();

        switch (type) {
        case Resource:
            criteriaMap.put("resourceId", String.valueOf(resourceId));
            break;
        case ResourceGroup:
            criteriaMap.put("groupId", String.valueOf(groupId));
            break;
        case AutoGroup:
            criteriaMap.put("parentResourceId", String.valueOf(parentResourceId));
            criteriaMap.put("resourceTypeId", String.valueOf(resourceTypeId));
            break;
        case ResourceTemplate:
            criteriaMap.put("resourceTypeId", String.valueOf(resourceTypeId));
            break;
        }

        return criteriaMap;
    }

    public String getLegacyKey() {
        switch (type) {
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
        return "Unsupported EntityContext '" + this + "'";
    }

    @Override
    public String toString() {
        return "EntityContext[category=" + type + ",resourceId=" + resourceId + "," + "groupId=" + groupId + ","
            + "parent=" + parentResourceId + "," + "type=" + resourceTypeId + "]";
    }

    public String toShortString() {
        switch (type) {
        case Resource:
            return "resource[id=" + resourceId + "]";
        case ResourceGroup:
            return "resourceGroup[groupId=" + resourceId + "]";
        case AutoGroup:
            return "autoGroup[parent=" + parentResourceId + ",type=" + resourceTypeId + "]";
        case ResourceTemplate:
            return "template[type=" + resourceTypeId + "]";
        default:
            return toString();
        }
    }
}
