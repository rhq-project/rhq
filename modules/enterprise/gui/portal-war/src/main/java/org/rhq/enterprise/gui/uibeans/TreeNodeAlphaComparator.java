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
package org.rhq.enterprise.gui.uibeans;

import java.util.Comparator;

public class TreeNodeAlphaComparator implements Comparator {
    private boolean reverse;

    public TreeNodeAlphaComparator() {
        reverse = false;
    }

    public TreeNodeAlphaComparator(boolean reverse) {
        this.reverse = reverse;
    }

    public int compare(Object o1, Object o2) {
        if (!(o1 instanceof TreeNode) || !(o2 instanceof TreeNode)) {
            throw new IllegalArgumentException("Comparator for TreeNodes only");
        }

        String s1 = ((TreeNode) o1).getName();
        String s2 = ((TreeNode) o2).getName();
        return (reverse) ? s2.toUpperCase().compareTo(s1.toUpperCase()) : s1.toUpperCase().compareTo(s2.toUpperCase());
    }
}