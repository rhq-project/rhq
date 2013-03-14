package org.rhq.enterprise.gui.coregui.client.report.configuration;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.ReportExporter;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;

public class SubsystemConfigurationHistoryListView extends ResourceConfigurationHistoryListView {


    public SubsystemConfigurationHistoryListView(boolean hasWritePerm) {
        super(hasWritePerm);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExportAction();
    }

    private void addExportAction() {
        addTableAction("Export",  MSG.common_button_reports_export(), new TableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return true;
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                ReportExporter exporter = ReportExporter.createStandardExporter("configurationHistory");
                exporter.export();
                refreshTableInfo();
            }
        });
    }

}
