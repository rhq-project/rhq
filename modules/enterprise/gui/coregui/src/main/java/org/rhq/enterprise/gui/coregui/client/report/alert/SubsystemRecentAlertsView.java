package org.rhq.enterprise.gui.coregui.client.report.alert;

import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.ExportModalWindow;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

import java.util.ArrayList;
import java.util.List;

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

                List<AlertPriority> alertPriorityList = new ArrayList<AlertPriority>(3);
                alertPriorityList.add(AlertPriority.HIGH);
                alertPriorityList.add(AlertPriority.MEDIUM);
                alertPriorityList.add(AlertPriority.LOW);
                ExportModalWindow exportModalWindow = ExportModalWindow.createExportWindowForRecentAlerts("recentAlerts",alertPriorityList);
                exportModalWindow.show();
                refreshTableInfo();
            }
        });
    }

}
