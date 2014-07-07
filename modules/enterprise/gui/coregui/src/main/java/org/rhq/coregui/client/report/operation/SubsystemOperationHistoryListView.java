/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.report.operation;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.operation.OperationHistoryView;

/**
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public class SubsystemOperationHistoryListView extends OperationHistoryView {

    public SubsystemOperationHistoryListView(boolean hasControlPermission) {
        super(OperationHistoryView.SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(), hasControlPermission);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExportAction();
    }

    private void addExportAction() {
        addTableAction("Export", MSG.common_button_reports_export(), ButtonColor.BLUE, new AbstractTableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return enableIfRecordsExist(getListGrid());
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {

                ReportExporter exporter = ReportExporter.createExporterForRecentOperations("recentOperations",
                    statusFilter.getValues(), startDateFilter.getValueAsDays(), endDateFilter.getValueAsDays());
                exporter.export();
                refreshTableInfo();
            }

        });
    }

}
