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
package org.rhq.coregui.client.report.measurement;

import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.components.view.HasViewName;
import org.rhq.coregui.client.components.view.ViewName;

/**
 * @author Greg Hinkle
 */
public class MeasurementOOBView extends Table<MeasurementOOBDataSource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("SuspectMetrics", MSG.view_measurementOob_title(), IconEnum.SUSPECT_METRICS);

    public MeasurementOOBView() {
        super();
        setDataSource(new MeasurementOOBDataSource());
    }

    @Override
    protected void configureTable() {
        setListGridFields(getDataSource().getListGridFields().toArray(new ListGridField[0]));
        addExportAction();
    }

    @Override
    protected SelectionStyle getDefaultSelectionStyle() {
        return SelectionStyle.NONE;
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }


    private void addExportAction() {
        addTableAction("Export",  MSG.common_button_reports_export(), ButtonColor.BLUE, new AbstractTableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return enableIfRecordsExist(getListGrid());
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                ReportExporter exporter = ReportExporter.createStandardExporter("suspectMetrics");
                exporter.export();
                refreshTableInfo();
            }
        });
    }

}
