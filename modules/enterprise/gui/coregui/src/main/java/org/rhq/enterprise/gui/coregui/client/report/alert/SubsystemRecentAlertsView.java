package org.rhq.enterprise.gui.coregui.client.report.alert;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.ExportModalWindow;
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

                List<AlertPriority> alertPriorityList = new ArrayList<AlertPriority>(3);
                String[] priorities = priorityFilter.getValues();
                for (String priority : priorities) {
                   if(priority.equals(AlertPriority.HIGH.getDisplayName())){
                        alertPriorityList.add(AlertPriority.HIGH);
                    } else if(priority.equals(AlertPriority.MEDIUM.getDisplayName())){
                        alertPriorityList.add(AlertPriority.MEDIUM);
                    } else if(priority.equals(AlertPriority.LOW.getDisplayName())){
                        alertPriorityList.add(AlertPriority.LOW);
                    }
                }
                ExportModalWindow exportModalWindow = ExportModalWindow.createExportWindowForRecentAlerts(
                    "recentAlerts", alertPriorityList.toArray(new String[alertPriorityList.size()]));
                exportModalWindow.show();
                refreshTableInfo();
            }
        });
    }

}
