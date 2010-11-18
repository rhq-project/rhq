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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.selection;

import java.util.ArrayList;

import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;

import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;

/**
 * @author Greg Hinkle
 */
public class SelectedResourceDataSource extends ResourceDatasource {

    private ArrayList<Integer> selection;

    ListGrid availableGrid;
    ListGrid assignedGrid;

    public SelectedResourceDataSource(ArrayList<Integer> selection, ListGrid availableGrid, ListGrid assignedGrid) {
        this.selection = selection;
        if (this.selection == null) {
            selection = new ArrayList<Integer>();
        }
        this.availableGrid = availableGrid;
        this.assignedGrid = assignedGrid;

        assignedGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                for (ListGridRecord record : recordDropEvent.getDropRecords()) {

                    record.setCanDrag(false);
                    record.setEnabled(false);

                    SelectedResourceDataSource.this.selection.add(record.getAttributeAsInt("id"));
                }
            }
        });
    }
}
