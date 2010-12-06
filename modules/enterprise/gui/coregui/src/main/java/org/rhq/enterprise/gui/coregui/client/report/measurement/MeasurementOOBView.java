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
package org.rhq.enterprise.gui.coregui.client.report.measurement;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * @author Greg Hinkle
 */
public class MeasurementOOBView extends Table {

    public static final ViewName VIEW_ID = new ViewName("SuspectMetrics", MSG.view_measurementOob_title());

    public MeasurementOOBView(String locatorId) {
        super(locatorId, VIEW_ID.getTitle());

        setDataSource(new MeasurementOOBDataSource());
    }

    @Override
    protected void configureTable() {
        ListGrid grid = getListGrid();
        grid.setAlternateRecordStyles(false);
        grid.getField("resourceName").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Resource/" + listGridRecord.getAttribute("resourceId") + "\">" + o + "</a>";
            }
        });
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.NONE;
    }
}
