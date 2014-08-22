package org.rhq.coregui.client.report.alert;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

public class SubsystemRecentAlertsView extends SubsystemResourceAlertView {

    public SubsystemRecentAlertsView(boolean hasWriteAccess) {
        super(hasWriteAccess);
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

                ReportExporter exporter = ReportExporter.createExporterForRecentAlerts("recentAlerts",
                    priorityFilter.getValues(), startDateFilter.getValueAsDays(), endDateFilter.getValueAsDays());
                exporter.export();
                refreshTableInfo();
            }
        });
    }

}
