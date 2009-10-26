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

import java.util.Enumeration;
import java.util.Vector;

/**
 * @author Greg Hinkle
*/
public class SwingTreeNodeResource implements javax.swing.tree.TreeNode {

    RandelshoferTreeNodeResource simpleResource;

    public SwingTreeNodeResource(RandelshoferTreeNodeResource simpleResource) {
        this.simpleResource = simpleResource;
    }

    public javax.swing.tree.TreeNode getChildAt(int childIndex) {
        return ((RandelshoferTreeNodeResource) this.simpleResource.children().get(childIndex)).getResourceTreeNode();
    }

    public int getChildCount() {
        return this.simpleResource.children().size();
    }

    public javax.swing.tree.TreeNode getParent() {
        return simpleResource.parent != null ? simpleResource.parent.getResourceTreeNode() : null;
    }

    public int getIndex(javax.swing.tree.TreeNode node) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getAllowsChildren() {
        return !simpleResource.isLeaf();
    }

    public boolean isLeaf() {
        return simpleResource.isLeaf();
    }

    public Enumeration children() {
        Vector<SwingTreeNodeResource> children = new Vector<SwingTreeNodeResource>();
        for (RandelshoferTreeNodeResource childResource : simpleResource.children) {
            children.add(childResource.getResourceTreeNode());
        }
        return children.elements();
    }

    public String toString() {
        return simpleResource.name;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SwingTreeNodeResource that = (SwingTreeNodeResource) o;

        if (simpleResource != null ? !simpleResource.equals(that.simpleResource) : that.simpleResource != null)
            return false;

        return true;
    }

    public int hashCode() {
        return (simpleResource != null ? simpleResource.hashCode() : 0);
    }
}
