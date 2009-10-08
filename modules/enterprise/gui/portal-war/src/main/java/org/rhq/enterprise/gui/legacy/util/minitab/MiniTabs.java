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
package org.rhq.enterprise.gui.legacy.util.minitab;

import java.util.List;

/**
 * This class has one purpose in life: To provide a collection of MiniTab's for the UI to display in such a way that the
 * colspan for the minitab table doesn't break when the number of minitab elements varies
 */
public class MiniTabs {
    private static final int COLSPAN_OFFSET = 2;
    private List minitabs;
    private int offset;
    private boolean offsetIsSet = false;

    public MiniTabs(List minitabs) {
        this.minitabs = minitabs;
    }

    public Integer getColspan() {
        return new Integer(minitabs.size() + COLSPAN_OFFSET);
    }

    /**
     * @return List
     */
    public List getList() {
        return minitabs;
    }

    /**
     * Sets the minitabs.
     *
     * @param minitabs The minitabs to set
     */
    public void setList(List minitabs) {
        this.minitabs = minitabs;
    }

    /**
     * @return int
     */
    public int getOffset() {
        if (!offsetIsSet) {
            return COLSPAN_OFFSET;
        }

        return offset;
    }

    /**
     * Sets the offset.
     *
     * @param offset The offset to set
     */
    public void setOffset(int offset) {
        offsetIsSet = true;
        this.offset = offset;
    }
}