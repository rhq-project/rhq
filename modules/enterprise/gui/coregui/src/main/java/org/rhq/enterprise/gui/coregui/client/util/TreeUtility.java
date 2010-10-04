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
package org.rhq.enterprise.gui.coregui.client.util;

import com.smartgwt.client.widgets.tree.Tree;
import com.smartgwt.client.widgets.tree.TreeNode;
import org.rhq.enterprise.gui.coregui.client.components.tree.EnhancedTreeNode;

/**
 * A collection of utility methods for working with SmartGWT {@link Tree}s.
 *
 * @author Ian Springer
 */
public class TreeUtility {
    public static void printTree(Tree tree) {
        TreeNode rootNode = tree.getRoot();
        printTreeNode(tree, rootNode);
    }

    private static void printTreeNode(Tree tree, TreeNode node) {
        int level = tree.getLevel(node);
        for (int i = 0; i < level; i++) {
            System.out.print("    ");
        }
        com.allen_sauer.gwt.log.client.Log.info("* " + toString(node));
        TreeNode[] childNodes = tree.getChildren(node);
        for (TreeNode childNode : childNodes) {
            printTreeNode(tree, childNode);
        }
    }

    private static String toString(TreeNode node) {
        if (node instanceof EnhancedTreeNode) {
            return node.toString();
        } else {
            StringBuilder buffer = new StringBuilder();
            String className = node.getClass().getName();
            String simpleClassName = className.substring(className.lastIndexOf(".") + 1);
            buffer.append(simpleClassName).append("[");
            String id = node.getAttribute("id");
            buffer.append("id=").append(id);
            String parentId = node.getAttribute("parentId");
            buffer.append(", parentId=").append(parentId);
            String name = node.getName();
            buffer.append(", name=").append(name);
            buffer.append("]");
            return buffer.toString();
        }
    }

    private TreeUtility() {
    }
}
