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


import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceWithAvailability;
import org.rhq.core.domain.resource.group.composite.AutoGroupComposite;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.cluster.ClusterKey;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.navigation.resource.ResourceTreeNode;

import java.util.Set;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupTreeNode implements Comparable {

    private static ResourceGroupTreeNode[] CHILDREN_ABSENT = new ResourceGroupTreeNode[0];

    private ClusterKey clusterKey;

    private List<ResourceGroupTreeNode> children = new ArrayList<ResourceGroupTreeNode>();

    private List<ResourceTreeNode> members = new ArrayList<ResourceTreeNode>();

    private Object level;

    private String shortPath;

    public ResourceGroupTreeNode(Object level) {
        this.level = level;
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

    public List<ResourceGroupTreeNode> getChildren() {
        return children;
    }

    public List<ResourceTreeNode> getMembers() {
        return members;
    }

    public synchronized List<ResourceGroupTreeNode> getNodes() {
        return children;
    }

    public Object getData() {
        return level;
    }

    public String toString() {
        if (level instanceof ResourceGroup) {
            ResourceGroup group = (ResourceGroup) level;
            return group.getName();
        }
        return level.toString();
    }

    public int compareTo(Object o) {
        return toString().compareTo(((ResourceGroupTreeNode)o).toString());
    }

    public void addChildren(Collection<ResourceGroupTreeNode> resourceGroupTreeNodes) {
        this.children.addAll(resourceGroupTreeNodes);
    }
}