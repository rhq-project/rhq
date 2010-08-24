/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common.event;

import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;

/**
 * @author Joseph Marques
 */
public class EventCompositeHistoryTable extends Table {

    public EventCompositeHistoryTable(String tableTitle, EntityContext context) {
        super(tableTitle);

        setDataSource(new EventCompositeDatasource(context));

        ListGrid grid = getListGrid();

        // getListGrid().getField("id").setWidth(60);

        grid.getField("timestamp").setWidth(125);
        grid.getField("timestamp").setSortDirection(SortDirection.DESCENDING);

        grid.getField("severity").setWidth(75);
        grid.getField("severity").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return Canvas.imgHTML("subsystems/event/" + o + "_16.png", 16, 16) + o;
            }
        });

        grid.getField("source").setWidth(275);
        grid.getField("source").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String sourceLocation = (String) o;
                int length = sourceLocation.length();
                if (length > 40) {
                    return "..." + sourceLocation.substring(length - 40); // the last 40 chars
                }
                return sourceLocation;
            }
        });

        setupTableInteractions();

        grid.sort(); // will sort by timestamp, whose default ordering is descending
    }

    private void setupTableInteractions() {
        getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
            public void onCellDoubleClick(CellDoubleClickEvent cellDoubleClickEvent) {
                ListGridRecord record = cellDoubleClickEvent.getRecord();
                EventCompositeDetailsView detailsView = new EventCompositeDetailsView(record);
                detailsView.displayInDialog();
            }
        });
    }
}
