/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

import java.util.Comparator;

import com.smartgwt.client.widgets.tree.TreeNode;

/**
 * Compares two tree nodes - first compares the title but if that is null, compares the name.
 * 
 * @author John Mazzitelli
 */
public class TreeNodeComparator implements Comparator<TreeNode> {

    @Override
    public int compare(TreeNode o1, TreeNode o2) {
        String s1 = getStringToCompare(o1);
        String s2 = getStringToCompare(o2);
        return s1.compareTo(s2);
    }

    private String getStringToCompare(TreeNode node) {
        // this method will never return null
        if (node.getTitle() != null) {
            return node.getTitle();
        } else if (node.getName() != null) {
            return node.getName();
        } else {
            // last ditch effort; if this node has an ID attribute, use it to compare
            Object id = node.getAttributeAsObject("id");
            if (id != null) {
                return id.toString();
            } else {
                return "";
            }
        }
    }
}
