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
import java.util.Date;
import java.util.LinkedHashMap;
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
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Joseph Marques
 * @author Ian Springer
 */
public class AlertDetailsView extends LocatableVLayout implements BookmarkableView {

    private int alertId;

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
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertDataSource.PRIORITY_ICON_HIGH, AlertDataSource.PRIORITY_ICON_HIGH);
        priorityIcons.put(AlertDataSource.PRIORITY_ICON_MEDIUM, AlertDataSource.PRIORITY_ICON_MEDIUM);
        priorityIcons.put(AlertDataSource.PRIORITY_ICON_LOW, AlertDataSource.PRIORITY_ICON_LOW);
        prioTextItem.setValueIcons(priorityIcons);
        // shouldn't have to do this, but the data source stores the actual URL of the icon in it, we need to map back to value
        LinkedHashMap<String, String> priorityMap = new LinkedHashMap<String, String>(3);
        priorityMap.put(AlertDataSource.PRIORITY_ICON_HIGH, MSG.common_alert_high());
        priorityMap.put(AlertDataSource.PRIORITY_ICON_MEDIUM, MSG.common_alert_medium());
        priorityMap.put(AlertDataSource.PRIORITY_ICON_LOW, MSG.common_alert_low());
        prioTextItem.setValueMap(priorityMap);
        items.add(prioTextItem);

        StaticTextItem createdTextItem = new StaticTextItem("ctime", MSG.view_alerts_field_created_time());
        createdTextItem.setValue(TimestampCellFormatter.format(record.getAttributeAsDate("ctime")));
        items.add(createdTextItem);

        StaticTextItem ackByItem = new StaticTextItem("acknowledgingSubject", MSG.view_alert_details_field_ack_by());
        if (record.getAttribute("acknowledgingSubject") != null) {
            ackByItem.setValue(record.getAttribute("acknowledgingSubject"));
        } else {
            ackByItem.setValue(MSG.view_alerts_field_ack_status_empty());
        }
        items.add(ackByItem);

        StaticTextItem ackTimeItem = new StaticTextItem("acknowledgeTime", MSG.view_alert_details_field_ack_at());
        Date ack_time = record.getAttributeAsDate("acknowledgeTime");
        if (ack_time != null) {
            ackTimeItem.setValue(TimestampCellFormatter.format(ack_time));
        } else {
            ackTimeItem.setValue(MSG.view_alerts_field_ack_status_empty());
        }
        items.add(ackTimeItem);

        StaticTextItem recoveryItem = new StaticTextItem("recovery", MSG.view_alert_details_field_recovery_info());
        recoveryItem.setValue(record.getAttribute("recoveryInfo"));
        items.add(recoveryItem);

        form.setItems(items.toArray(new FormItem[items.size()]));

        return form;
    }

    private class NotificationLogsTable extends Table {
        private final Record record;

        public NotificationLogsTable(String locatorId, String tableTitle, Record record) {
            super(locatorId, tableTitle, false);
            this.record = record;
            setHeight("35%");
            setWidth100();
        }

        @Override
        protected void configureTable() {
            DataClass[] input = record.getAttributeAsRecordArray("notificationLogs");
            ListGrid grid = getListGrid();
            grid.setData((Record[]) input);
            ListGridField sender = new ListGridField("sender", MSG.view_alert_common_tab_notifications_sender());
            sender.setWidth("33%");
            ListGridField status = new ListGridField("status", MSG.view_alert_common_tab_notifications_status());
            status.setWidth("33%");
            ListGridField message = new ListGridField("message", MSG.view_alert_common_tab_notifications_message());
            message.setWidth("34%");

            grid.setFields(sender, status, message);
        }
    }

    private Table getNotificationsForAlert(Record record) {
        Table notifTable = new NotificationLogsTable("AlertDetailsNotifications", MSG
            .view_alert_common_tab_notifications(), record);
        return notifTable;
    }

    private class ConditionLogsTable extends Table {
        private final Record record;

        public ConditionLogsTable(String locatorId, String tableTitle, Record record) {
            super(locatorId, tableTitle, false);
            this.record = record;
            setHeight("35%");
            setWidth100();
        }

        @Override
        protected void configureTable() {
            DataClass[] input = record.getAttributeAsRecordArray("conditionLogs");
            ListGrid grid = getListGrid();
            grid.setData((Record[]) input);
            ListGridField condition = new ListGridField("text", MSG.view_alert_common_tab_conditions_text());
            condition.setWidth("60%");
            ListGridField value = new ListGridField("value", MSG.view_alert_common_tab_conditions_value());

            grid.setFields(condition, value);
        }
    }

    private Table getConditionsForAlert(Record record) {
        String mode = record.getAttribute("conditionExpression");
        Table table = new ConditionLogsTable("AlertDetailsConditionLog", MSG.view_alert_common_tab_conditions()
            + ": match = " + mode, record);
        return table;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        alertId = viewPath.getCurrentAsInt();

        show(alertId);
    }
}
