/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.detail.monitoring;

import java.util.Set;
import java.util.TreeSet;

/**
 * This singleton momento holds the state of the open rows in metrics screen.
 *
 * @author  Mike Thompson
 */
public class ExpandedRowsMomento {

    private static ExpandedRowsMomento INSTANCE = new ExpandedRowsMomento();
    private Set<Integer> expandedRows;

    public static ExpandedRowsMomento getInstance() {
        return INSTANCE;
    }

    private ExpandedRowsMomento() {
        expandedRows = new TreeSet<Integer>();
    }

    public Set<Integer> getExpandedRows() {
        return expandedRows;
    }

    public void setExpandedRows(Set<Integer> expandedRows) {
        this.expandedRows = expandedRows;
    }

    public void clear() {
        expandedRows.clear();
    }
}
