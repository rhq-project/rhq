package org.rhq.enterprise.gui.coregui.client.report.alert;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.ReportExporter;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

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

                ReportExporter exporter = ReportExporter.createExporterForRecentAlerts(
                        "recentAlerts", priorityFilter.getValues(), fromDateFilter.getValueAsDate(), toDateFilter.getValueAsDate());
                exporter.export();
                refreshTableInfo();
            }
        });
    }

}
