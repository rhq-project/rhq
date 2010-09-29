/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.tree;

import com.smartgwt.client.widgets.tree.TreeNode;

import org.rhq.core.domain.util.StringUtils;

/**
 * @author Ian Springer
 */
public class EnhancedTreeNode extends TreeNode {
    public EnhancedTreeNode() {
        this(null);
    }

    public EnhancedTreeNode(String name) {
        this(name, new TreeNode[0]);
    }

    public EnhancedTreeNode(String name, TreeNode... children) {
        super(name, children);
        if (name != null) {
            setTitle(StringUtils.deCamelCase(name));
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        if (name != null && getTitle() == null) {
            setTitle(StringUtils.deCamelCase(name));
        }
    }

    public String getID() {
        return getAttribute(Attributes.ID);
    }

    public String getParentID() {
        return getAttribute(Attributes.PARENT_ID);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        String innerClassName;
        try {
            String className = this.getClass().getName();
            String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
            innerClassName = simpleClassName.substring(simpleClassName.lastIndexOf("$") + 1);
        }
        catch (RuntimeException e) {
            innerClassName = "EnhancedTreeNode";
        }

        buffer.append(innerClassName).append("[");
        String id = getID();
        buffer.append("id=").append(id);
        String parentId = getParentID();
        buffer.append(", parentId=").append(parentId);
        String name = getName();
        buffer.append(", name=").append(name);
        buffer.append("]");
        return buffer.toString();
    }

    public class Attributes {
        public static final String ID = "id";
        public static final String PARENT_ID = "parentId";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";

        private Attributes() {
        }
    }
}
