package org.rhq.enterprise.gui.coregui.client.report.alert;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.enterprise.gui.coregui.client.PopupWindow;
import org.rhq.enterprise.gui.coregui.client.alert.SubsystemResourceAlertView;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;

public class SubsystemRecentAlertsView extends SubsystemResourceAlertView {

    private TableAction exportAction;

    public SubsystemRecentAlertsView(String locatorId, boolean hasWriteAccess, TableAction exportAction) {
        super(locatorId, hasWriteAccess);
        this.exportAction = exportAction;
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addTableAction("export", "Export", exportAction);
    }

}
