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
package org.rhq.enterprise.gui.image.widget;

import org.rhq.enterprise.gui.image.data.IResourceTreeNode;
import org.rhq.enterprise.gui.image.data.ITreeNode;

public class ResourceTreeNode extends TreeNode implements IResourceTreeNode {
    private int m_type;

    //**************** Constructors *************
    public ResourceTreeNode(String name, String desc) {
        this(name, desc, false, NONE);
    }

    public ResourceTreeNode(String name, int type) {
        this(name, null, false, type);
    }

    public ResourceTreeNode(String name, String desc, int type) {
        this(name, desc, false, type);
    }

    public ResourceTreeNode(String name, String desc, boolean selected) {
        this(name, desc, selected, NONE);
    }

    public ResourceTreeNode(String name, String desc, boolean selected, int type) {
        super(name, desc, selected);

        if (this.isValidType(type) == false) {
            throw new IllegalArgumentException("Invalid Type: " + type);
        }

        m_type = type;
    }

    //************ ITreeNode Methods ************
    /**
     * @see org.rhq.enterprise.gui.image.data.ITreeNode#getUpChildren()
     */
    public ITreeNode[] getUpChildren() {
        return (IResourceTreeNode[]) m_upChildren.toArray(new IResourceTreeNode[m_upChildren.size()]);
    }

    /**
     * @see org.rhq.enterprise.gui.image.data.ITreeNode#getDownChildren()
     */
    public ITreeNode[] getDownChildren() {
        return (IResourceTreeNode[]) m_downChildren.toArray(new IResourceTreeNode[m_downChildren.size()]);
    }

    /**
     * @see org.rhq.enterprise.gui.image.data.ITreeNode#getResourceType()
     */
    public int getType() {
        return m_type;
    }

    //******** IResourceTreeNode Methods ********

    //************ Public Methods ***************
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[").append(" name=").append(getName()).append(" desc=").append(getDescription()).append(" type=")
            .append(getType()).append(" up-children: ").append(getUpChildren()).append(" down-chilren: ").append(
                getDownChildren()).append(" ]");
        return buf.toString();
    }

    //************* Private Methods *************
    private boolean isValidType(int type) {
        return ((type >= NONE) && (type <= CLUSTER));
    }
}

// EOF
