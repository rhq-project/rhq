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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.event;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

/**
 * @author Greg Hinkle
 */
public class EventHistoryView extends Table {


    public EventHistoryView(Criteria criteria) {
        super("Event History", criteria);

        setDataSource(new EventDatasource());

//         getListGrid().getField("id").setWidth(60);
        getListGrid().getField("severity").setWidth(120);
        getListGrid().getField("severity").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return Canvas.imgHTML("subsystems/event/" + o + "_16.png", 16, 16) + o;
            }
        });

        getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
            public void onCellDoubleClick(CellDoubleClickEvent cellDoubleClickEvent) {
                // TODO: Implement this method.
                showEventDetails(cellDoubleClickEvent.getRecord());
            }
        });

        addTableAction("Details", SelectionEnablement.SINGLE, null,
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        showEventDetails(selection[0]);
                    }
                });

        getListGrid().getField("sourceLocation").setWidth(200);
        getListGrid().getField("timestamp").setWidth(160);
    }

    public static void showEventDetails(ListGridRecord record) {
        new EventDetailsView(record).displayInDialog();
    }

    public static EventHistoryView createResourceHistoryView(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria("resourceId",resourceId);
        return new EventHistoryView(criteria);
    }
}
