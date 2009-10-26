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
package org.rhq.enterprise.visualizer;

import ch.randelshofer.tree.TreeNode;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Greg Hinkle
*/
public class RandelshoferTreeNodeResource implements ch.randelshofer.tree.TreeNode {

    int id;
    int parentId;
    int descendents;
    String name;
    boolean up;
    long lastAvailStart;
    double upness;
    int size;
    double weight;
    RandelshoferTreeNodeResource parent;
    List<RandelshoferTreeNodeResource> children = new ArrayList<RandelshoferTreeNodeResource>();
    SwingTreeNodeResource resourceTreeNode;


    public RandelshoferTreeNodeResource(int id, String name, boolean up, long lastAvailStart, int parent) {
        this.id = id;
        this.name = name;
        this.up = up;
        this.lastAvailStart = lastAvailStart;
        this.parentId = parent;

        resourceTreeNode = new SwingTreeNodeResource(this);
    }

    public List<TreeNode> children() {
        return new ArrayList<TreeNode>(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public SwingTreeNodeResource getResourceTreeNode() {
        return resourceTreeNode;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isUp() {
        return up;
    }

    public long getLastAvailStart() {
        return lastAvailStart;
    }

    public int getDescendents() {
        return descendents;
    }

    public List<RandelshoferTreeNodeResource> getChildren() {
        return children;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RandelshoferTreeNodeResource that = (RandelshoferTreeNodeResource) o;

        if (id != that.id) return false;

        return true;
    }

    public int hashCode() {
        return id;
    }

    public String toString() {
        return this.name;
    }

}
