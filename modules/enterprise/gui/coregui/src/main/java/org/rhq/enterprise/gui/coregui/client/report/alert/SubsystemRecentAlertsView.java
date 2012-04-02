package org.rhq.enterprise.gui.coregui.client.report.alert;

import java.util.Date;

import com.smartgwt.client.util.DateUtil;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.ReportExporter;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

import static com.smartgwt.client.data.RelativeDate.END_OF_TODAY;
import static com.smartgwt.client.data.RelativeDate.START_OF_TODAY;
import static com.smartgwt.client.types.RelativeDateRangePosition.END;
import static com.smartgwt.client.types.RelativeDateRangePosition.START;

public class SubsystemRecentAlertsView extends SubsystemResourceAlertView {

    public SubsystemRecentAlertsView(String locatorId, boolean hasWriteAccess) {
        super(locatorId, hasWriteAccess);
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
                Date fromDate = fromDateFilter.getValueAsDate();
                Date toDate = toDateFilter.getValueAsDate();

                fromDate = DateUtil.getAbsoluteDate(START_OF_TODAY, fromDate, START);
                toDate = DateUtil.getAbsoluteDate(END_OF_TODAY, toDate, END);

                ReportExporter exporter = ReportExporter.createExporterForRecentAlerts("recentAlerts",
                    priorityFilter.getValues(), fromDate, toDate);
                exporter.export();
                refreshTableInfo();
            }
        });
    }

}
