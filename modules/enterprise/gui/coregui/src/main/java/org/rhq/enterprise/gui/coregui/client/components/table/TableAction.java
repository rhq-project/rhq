/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.table;

import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Greg Hinkle
 */
public interface TableAction {

    /**
     * Returns true if the action should be enabled based on the currently selected record(s).
     *
     * @param selection the currently selected record(s)
     *
     * @return true if the action should be enabled based on the currently selected record(s)
     */
    boolean isEnabled(ListGridRecord[] selection);

    /**
     * Execute the action with the currently selected record(s) as the target(s).
     *
     * @param selection the currently selected record(s)
     */
    void executeAction(ListGridRecord[] selection);

}
