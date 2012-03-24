package org.rhq.enterprise.gui.coregui.client.report.configuration;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.enterprise.gui.coregui.client.components.ExportModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;

public class SubsystemConfigurationHistoryListView extends ResourceConfigurationHistoryListView {


    public SubsystemConfigurationHistoryListView(String locatorId, boolean hasWritePerm ) {
        super(locatorId, hasWritePerm);
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
                ExportModalWindow exportModalWindow = new ExportModalWindow("configurationHistory");
                exportModalWindow.show();
            }

        });
    }

}
