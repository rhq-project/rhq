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
package org.rhq.enterprise.gui.coregui.client.report.operation;

import com.smartgwt.client.util.DateUtil;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.components.ReportExporter;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.operation.OperationHistoryView;

import java.util.Date;

import static com.smartgwt.client.data.RelativeDate.END_OF_TODAY;
import static com.smartgwt.client.types.RelativeDateRangePosition.END;

/**
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
public class SubsystemOperationHistoryListView extends OperationHistoryView {


    public SubsystemOperationHistoryListView(String locatorId, boolean hasControlPermission ) {
        super(locatorId, OperationHistoryView.SUBSYSTEM_VIEW_ID.getTitle(), EntityContext.forSubsystemView(),
            hasControlPermission);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExportAction();
    }

    private void addExportAction() {
        addTableAction("Export", "Export", new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return true;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                Date fromDate = startDateFilter.getValueAsDate();
                Date toDate = endDateFilter.getValueAsDate();

                if (fromDate.equals(toDate)) {
                    toDate = DateUtil.getAbsoluteDate(END_OF_TODAY, fromDate, END);
                }

                ReportExporter exporter = ReportExporter.createExporterForRecentOperations("recentOperations",
                    statusFilter.getValues(), fromDate, toDate);
                exporter.export();
                refreshTableInfo();
            }

        });
    }


}
