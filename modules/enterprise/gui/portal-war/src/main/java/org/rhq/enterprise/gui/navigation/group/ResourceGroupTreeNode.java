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
package org.rhq.enterprise.gui.navigation.group;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.resource.flyweight.AutoGroupCompositeFlyweight;
import org.rhq.core.domain.resource.flyweight.ResourceFlyweight;
import org.rhq.core.domain.resource.group.ClusterKey;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.util.sort.HumaneStringComparator;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeNode;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeNode implements Comparable<ResourceGroupTreeNode> {
    private final Log log = LogFactory.getLog(ResourceGroupTreeNode.class);

    private static ResourceGroupTreeNode[] CHILDREN_ABSENT = new ResourceGroupTreeNode[0];

    private ClusterKey clusterKey;

    private TreeSet<ResourceGroupTreeNode> children = new TreeSet<ResourceGroupTreeNode>();

    private List<ResourceTreeNode> members = new ArrayList<ResourceTreeNode>();

    private Object level;

    private String shortPath;

    private ResourceGroupTreeNode parent;

    public ResourceGroupTreeNode(Object level, ResourceGroupTreeNode parent) {
        this.level = level;
        this.parent = parent;
    }

    public ClusterKey getClusterKey() {
        return clusterKey;
    }

    public void setClusterKey(ClusterKey clusterKey) {
        this.clusterKey = clusterKey;
    }

    public void addMembers(Set<ResourceTreeNode> members) {
        this.members.addAll(members);
    }

    public void addMember(ResourceTreeNode member) {
        this.members.add(member);
    }

    public TreeSet<ResourceGroupTreeNode> getChildren() {
        return children;
    }

    public List<ResourceTreeNode> getMembers() {
        return members;
    }

    public synchronized TreeSet<ResourceGroupTreeNode> getNodes() {
        return children;
    }

    public Object getData() {
        return level;
    }

    public ResourceGroupTreeNode getParent() {
        return parent;
    }

    public void setParent(ResourceGroupTreeNode parent) {
        this.parent = parent;
    }

    public String getNodeType() {
        if (level instanceof ResourceGroup) {
            return "ResourceGroup";
        } else if (level instanceof AutoGroupCompositeFlyweight) {
            return "AutoGroupComposite";
        } else if (level instanceof ResourceFlyweight) {
            return "Resource";
        } else {
            return level.getClass().getSimpleName();
        }
    }

    public String toString() {
        if (level instanceof ResourceGroup) {
            ResourceGroup group = (ResourceGroup) level;
            return group.getName();
        } else if (level instanceof AutoGroupCompositeFlyweight) {
            AutoGroupCompositeFlyweight ag = (AutoGroupCompositeFlyweight) level;
            if (ag.getResourceType() != null) {
                return ag.getResourceType().getName();
            } else if (ag.getSubcategory() != null) {
                return ag.getSubcategory();
            }
        }
        return level.toString();
    }

    public int compareTo(ResourceGroupTreeNode that) {
        try {
            return HumaneStringComparator.DEFAULT.compare(toString(), that.toString());
        } catch (Exception e) {
            log.warn("Couldn't compare: " + toString() + " || " + that + " - cause: " + e);
            return toString().compareTo(that.toString());
        }
    }

    public void addChildren(Collection<ResourceGroupTreeNode> resourceGroupTreeNodes) {
        this.children.addAll(resourceGroupTreeNodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ResourceGroupTreeNode that = (ResourceGroupTreeNode) o;

        return level.equals(that.level);
    }

    @Override
    public int hashCode() {
        return level.hashCode();
    }
}
