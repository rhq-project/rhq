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

import java.util.Set;
import java.util.TreeSet;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.flyweight.AutoGroupCompositeFlyweight;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.util.sort.HumaneStringComparator;

/**
 * Just a basic node to hold resources, resource auto groups and subcategories
 * in a tree.
 *
 * @author Greg Hinkle
 */
public class ResourceTreeNode implements Comparable<ResourceTreeNode> {

    private Set<ResourceTreeNode> children = new TreeSet<ResourceTreeNode>();

    private Object level;
    private ResourceTreeNode parent;
    
    public ResourceTreeNode(Object level) {
        this.level = level;
    }

    public ResourceTreeNode(Object level, ResourceTreeNode parent) {
        this.level = level;
        this.parent = parent;
    }

    public Set<ResourceTreeNode> getChildren() {
        return children;
    }

    public Object getData() {
        return level;
    }

    public ResourceTreeNode getParent() {
        return parent;
    }

    public void setParent(ResourceTreeNode parent) {
        this.parent = parent;
    }

    public String getNodeType() {
        if (level instanceof AutoGroupCompositeFlyweight) {
            return "AutoGroupComposite";
        } else if (level instanceof ResourceFlyweight) {
            return ((ResourceFlyweight)level).isLocked() ? "LockedResource" : "Resource";
        } else {
            return level.getClass().getSimpleName();
        }
    }
    
    public String toString() {
        if (level == null) {
            return "";
        }
        if (level instanceof AutoGroupCompositeFlyweight) {
            AutoGroupCompositeFlyweight composite = ((AutoGroupCompositeFlyweight) level);
            return composite.getName();
        } else if (level instanceof ResourceWithAvailability) {
            return ((ResourceWithAvailability) level).getResource().getName();
        } else if (level instanceof ResourceFlyweight) {
            ResourceFlyweight fly = (ResourceFlyweight) level;
            String name = fly.getName();
            if (fly.isLocked()) {
                name += " (locked)";
            }
            return name;
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ResourceTreeNode that = (ResourceTreeNode) o;

        return level.equals(that.level);
    }

    @Override
    public int hashCode() {
        return level.hashCode();
    }

}
