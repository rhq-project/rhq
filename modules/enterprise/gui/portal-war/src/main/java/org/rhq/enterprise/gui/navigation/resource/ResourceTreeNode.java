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
package org.rhq.enterprise.gui.navigation.resource;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.util.sort.HumaneStringComparator;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Just a basic node to hold resources, resource auto groups and subcategories
 * in a tree.
 *
 * @author Greg Hinkle
 */
public class ResourceTreeNode implements Comparable<ResourceTreeNode> {

    private static ResourceTreeNode[] CHILDREN_ABSENT = new ResourceTreeNode[0];

    private Set<ResourceTreeNode> children = new TreeSet<ResourceTreeNode>();

    private Object level;

    private String shortPath;


    public ResourceTreeNode(Object level) {
           this.level = level;
       }


    public ResourceTreeNode(Object level, List<Resource> resources) {
        this.level = level;
    }

    public Set<ResourceTreeNode> getChildren() {
        return children;
    }

    public Object getData() {
        return level;
    }

    public String toString() {
        if (level instanceof AutoGroupComposite) {
            AutoGroupComposite composite = ((AutoGroupComposite) level) ;
            ResourceType type = composite.getResourceType();
            if (type != null) {
                return type.getName();
            } else {
                return composite.getSubcategory().getName();
            }
        } else if (level instanceof ResourceWithAvailability) {
            return ((ResourceWithAvailability) level).getResource().getName();
        } else if (level instanceof Resource) {
            return ((Resource) level).getName();
        }
        return level.toString();
    }

    public int compareTo(ResourceTreeNode that) {
        int i = HumaneStringComparator.DEFAULT.compare(toString(), that.toString());
        if (i == 0) {
            i = new Integer(level.hashCode()).compareTo(that.level.hashCode());
        }
        return i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceTreeNode that = (ResourceTreeNode) o;

        if (!level.equals(that.level)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return level.hashCode();
    }

}