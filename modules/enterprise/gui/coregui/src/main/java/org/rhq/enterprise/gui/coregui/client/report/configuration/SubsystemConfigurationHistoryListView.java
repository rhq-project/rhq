package org.rhq.enterprise.gui.coregui.client.report.configuration;

import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;

public class SubsystemConfigurationHistoryListView extends ResourceConfigurationHistoryListView {

    private TableAction exportAction;

    public SubsystemConfigurationHistoryListView(String locatorId, boolean hasWritePerm, TableAction exportAction) {
        super(locatorId, hasWritePerm);
        this.exportAction = exportAction;
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addTableAction("export", "Export", exportAction);
    }

}
