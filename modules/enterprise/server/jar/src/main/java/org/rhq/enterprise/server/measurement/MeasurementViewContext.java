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
package org.rhq.enterprise.server.measurement;


/**
 * @author Joseph Marques
 */
public class MeasurementViewContext {

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

    public MeasurementViewContext(Integer resourceId, Integer groupId, Integer parentResourceId, Integer resourceTypeId) {
        int rId = (resourceId != null && resourceId > 0) ? resourceId : -1;
        int gId = (groupId != null && groupId > 0) ? groupId : -1;
        int prId = (parentResourceId != null && parentResourceId > 0) ? parentResourceId : -1;
        int rtId = (resourceTypeId != null && resourceTypeId > 0) ? resourceTypeId : -1;

        this.resourceId = rId;
        this.groupId = gId;
        this.parentResourceId = prId;
        this.resourceTypeId = rtId;

        if (this.resourceId > 0) {
            category = Category.Resource;
        } else if (this.groupId > 0) {
            category = Category.ResourceGroup;
        } else if (this.resourceTypeId > 0) {
            if (this.parentResourceId > 0) {
                category = Category.ResourceTemplate;
            } else {
                category = Category.AutoGroup;
            }
        } else {
            throw new IllegalArgumentException("Unknown or unsupported " + MeasurementViewContext.class.getSimpleName()
                + " '" + this + "'");
        }
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
            throw new IllegalArgumentException("Unknown or unsupported MeasurementViewContext '" + category + "'");
        }
    }

    public String toString() {
        return MeasurementViewContext.class.getSimpleName() + "[category=" + category + ",resourceId =" + resourceId
            + "," + "groupId =" + groupId + "," + "parentResourceId =" + parentResourceId + "," + "resourceTypeId ="
            + resourceTypeId + "]";
    }
}
