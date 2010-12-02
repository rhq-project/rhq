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
package org.rhq.enterprise.gui.coregui.client.alert;

import java.util.ArrayList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
public class AlertDetailsView extends LocatableVLayout implements BookmarkableView {

    private int alertId;
    private ViewId viewId;

    private static AlertDetailsView INSTANCE = new AlertDetailsView("alertDetailsView");

    public static AlertDetailsView getInstance() {
        return INSTANCE;
    }

    private AlertDetailsView(String locatorId) {
        // access through the static singleton only
        super(locatorId);
    }

    private void show(int alertId) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.addFilterId(alertId);
        GWTServiceLookup.getAlertService().findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            @Override
            public void onSuccess(PageList<Alert> result) {
                Alert alert = result.get(0);
                show(alert);
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alert_details_loadFailed(), caught);
            }
        });
    }

    private void show(Alert alert) {
        for (Canvas child : getMembers()) {
            removeChild(child);
        }

        if (this.viewId != null) {
            viewId.getBreadcrumbs().get(0).setDisplayName(MSG.view_alert_details_breadcrumb());
            CoreGUI.refreshBreadCrumbTrail();
        }

        Log.info("AlertDetailsView-Alert: " + alert);
        ListGridRecord record = AlertDataSource.convert(alert);
        Log.info("AlertDetailsView-ListGridRecord: " + record);
        addMember(getDetailsTabSet(record));
    }

    private TabSet getDetailsTabSet(Record record) {
        TabSet tabset = new TabSet();

        Tab generalTab = new Tab(MSG.view_alert_common_tab_general());
        generalTab.setPane(getDetailsTableForAlert(record));

        Tab conditionsTab = new Tab(MSG.view_alert_common_tab_conditions());
        conditionsTab.setPane(getConditionsForAlert(record));

        Tab notificationsTab = new Tab(MSG.view_alert_common_tab_notifications());
        notificationsTab.setPane(getNotificationsForAlert(record));

        tabset.addTab(generalTab);
        tabset.addTab(conditionsTab);
        tabset.addTab(notificationsTab);

        return tabset;
    }

    private DynamicForm getDetailsTableForAlert(Record record) {
        DynamicForm form = new DynamicForm();
        form.setNumCols(4);
        form.setHeight("15%");
        form.setWrapItemTitles(false);
        form.setAlign(Alignment.LEFT);

        List<FormItem> items = new ArrayList<FormItem>();

        StaticTextItem nameTextItem = new StaticTextItem("name", MSG.common_title_name());
        nameTextItem.setValue(record.getAttribute("name"));
        nameTextItem.setTooltip("Id = " + record.getAttribute("id"));
        items.add(nameTextItem);

        StaticTextItem descriptionTextItem = new StaticTextItem("description", MSG.common_title_description());
        descriptionTextItem.setValue(record.getAttribute("description"));
        items.add(descriptionTextItem);

        StaticTextItem prioTextItem = new StaticTextItem("priority", MSG.view_alerts_field_priority());
        prioTextItem.setValue(record.getAttribute("priority"));
        items.add(prioTextItem);

        StaticTextItem createdTextItem = new StaticTextItem("ctime", MSG.view_alerts_field_created_time());
        createdTextItem.setValue(record.getAttribute("ctime"));
        items.add(createdTextItem);

        StaticTextItem ackByItem = new StaticTextItem("ack_by", MSG.view_alert_details_field_ack_by());
        if (record.getAttribute("ack_by") != null) {
            ackByItem.setValue(record.getAttribute("ack_by"));
        }
        items.add(ackByItem);

        StaticTextItem ackTimeItem = new StaticTextItem("ack_time", MSG.view_alert_details_field_ack_at());
        if (record.getAttribute("ack_time") != null) {
            ackTimeItem.setValue(record.getAttribute("ack_time"));
        }
        items.add(ackTimeItem);

        StaticTextItem recoveryItem = new StaticTextItem("recovery", MSG.view_alert_details_field_recovery_info());
        recoveryItem.setValue(record.getAttribute("recoveryInfo"));
        items.add(recoveryItem);

        form.setItems(items.toArray(new FormItem[items.size()]));

        return form;
    }

    private Table getNotificationsForAlert(Record record) {
        DataClass[] input = record.getAttributeAsRecordArray("notificationLogs");

        //Table notifTable = new Table(extendLocatorId("Notifications"), "Notifications", false);
        Table notifTable = new Table("AlertDetailsNotifications", MSG.view_alert_common_tab_notifications(), false);
        notifTable.setHeight("35%");
        notifTable.setWidth100();
        ListGrid grid = notifTable.getListGrid();
        grid.setData((Record[]) input);

        ListGridField sender = new ListGridField("sender", MSG.view_alert_common_tab_notifications_sender());
        sender.setWidth("10%");
        ListGridField status = new ListGridField("status", MSG.view_alert_common_tab_notifications_status());
        status.setWidth("8%");
        ListGridField message = new ListGridField("message", MSG.view_alert_common_tab_notifications_message());
        message.setWidth("32%");
        ListGridField allEmails = new ListGridField("allEmails", MSG.view_alert_common_tab_notifications_all_emails());
        allEmails.setWidth("25%");
        ListGridField badEmails = new ListGridField("badEmails", MSG.view_alert_common_tab_notifications_all_emails());
        badEmails.setWidth("25%");

        grid.setFields(sender, status, message, allEmails, badEmails);

        return notifTable;
    }

    private Table getConditionsForAlert(Record record) {
        DataClass[] input = record.getAttributeAsRecordArray("conditionLogs");
        String mode = record.getAttribute("conditionExpression");

        //Table table = new Table(extendLocatorId("ConditionLog"), "Conditions: match = " + mode, false);
        Table table = new Table("AlertDetailsConditionLog", MSG.view_alert_common_tab_conditions() + ": match = "
            + mode, false);
        table.setHeight("35%");
        table.setWidth100();
        ListGrid grid = table.getListGrid();
        grid.setData((Record[]) input);

        ListGridField condition = new ListGridField("text", MSG.view_alert_common_tab_conditions_text());
        condition.setWidth("60%");
        ListGridField value = new ListGridField("value", MSG.view_alert_common_tab_conditions_value());

        grid.setFields(condition, value);

        return table;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        alertId = viewPath.getCurrentAsInt();
        viewId = viewPath.getCurrent();

        show(alertId);
    }
}
