/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
 * All rights reserved.
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

import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;

/**
 * A view that displays a paginated table of fired {@link org.rhq.core.domain.alert.Alert alert}s, along with the
 * ability to filter or sort those alerts, click on an alert to view details about that alert's definition, or delete
 * selected alerts.
 *
 * @author Ian Springer
 * @author Heiko W. Rupp
 */
public class AlertsView extends Table {
    private static final String TITLE = "Alerts";

    private static final SortSpecifier[] SORT_SPECIFIERS = new SortSpecifier[] {
        new SortSpecifier(AlertCriteria.SORT_FIELD_CTIME, SortDirection.DESCENDING),
        new SortSpecifier(AlertCriteria.SORT_FIELD_NAME, SortDirection.ASCENDING) };

    private static final String DELETE_CONFIRM_MESSAGE = "Are you sure you want to delete the selected alert(s)?";

    private AlertDataSource dataSource;
    private static final String SENDER = "sender";

    Criteria criteria;
    String[] excludedFieldNames;
    boolean showDetails;

    public AlertsView(String locatorId) {
        this(locatorId, null, null);
        showDetails = false;

    }

    public AlertsView(String locatorId, Criteria criteria, String[] excludedFieldNames) {
        super(locatorId, TITLE, criteria, SORT_SPECIFIERS, excludedFieldNames);

        this.dataSource = new AlertDataSource();

        setDataSource(this.dataSource);
        this.criteria = criteria;
        this.excludedFieldNames = excludedFieldNames;
        showDetails = false;
    }

    @Override
    protected void onInit() {
        super.onInit();

        // Add the list table as the top half of the view.
        //Criteria criteria = new Criteria(AlertCriteria.);
        ListGrid listGrid = getListGrid();
        listGrid.getField("name").setWidth("15%");
        listGrid.getField("conditionText").setWidth("30%");
        listGrid.getField("conditionValue").setWidth("10%");
        listGrid.getField("resourceName").setWidth("20%");
        //            listGrid.getField("recoveryInfo").setWidth("20%");
        listGrid.getField("priority").setWidth("7%");
        ListGridField ctimeField = listGrid.getField("ctime");
        ctimeField.setWidth("13%");
        ctimeField.setCellFormatter(new TimestampCellFormatter());
        listGrid.getField("ack").setWidth("5%");

        listGrid.getField("resourceName").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("resourceId"))
                    + "\">" + o + "</a>";
            }
        });

        addTableAction("DeleteAlert", "Delete", Table.SelectionEnablement.ANY, DELETE_CONFIRM_MESSAGE,
            new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    AlertsView.this.dataSource.deleteAlerts(AlertsView.this);
                }
            });
        addTableAction("AcknowledgeAlert", "Acknowledge", Table.SelectionEnablement.ANY, null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                AlertsView.this.dataSource.acknowledgeAlerts(AlertsView.this);
            }
        });

        if (showDetails) {
            listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                public void onSelectionChanged(SelectionEvent event) {
                    ListGridRecord[] selectedRecords = AlertsView.this.getListGrid().getSelection();
                    if (selectedRecords.length == 1) {
                        ListGridRecord record = selectedRecords[0];

                        // Clean out existing details and provide new ones
                        for (int i = 1; i <= getChildren().length; i++)
                            getChildren()[1].destroy();

                        addMember(getDetailsTabSet(record));

                    } else {
                        // Clean out existing details and show the "nothing selected message"
                        for (int i = 1; i <= getChildren().length; i++)
                            getChildren()[1].destroy();
                        addMember(getNoAlertSelectedMessage());
                    }
                }
            });
        }

        //        // Add the details panel as the bottom half of the view.
        //        // Default is the "nothing selected" message
        //        addMember(getNoAlertSelectedMessage());

    }

    private TabSet getDetailsTabSet(Record record) {
        TabSet tabset = new TabSet();
        Tab generalTab = new Tab("General");
        generalTab.setPane(getDetailsTableForAlert(record));
        Tab conditionsTab = new Tab("Conditions");
        conditionsTab.setPane(getConditionsForAlert(record));
        Tab notificationsTab = new Tab("Notifications");
        notificationsTab.setPane(getNotificationsForAlert(record));

        tabset.addTab(generalTab);
        tabset.addTab(conditionsTab);
        tabset.addTab(notificationsTab);

        return tabset;
    }

    private HTMLFlow getNoAlertSelectedMessage() {
        HTMLFlow f = new HTMLFlow("<h3>Select a single alert above to display its details here.</h3>");
        f.setHeight("50%");
        return f;
    }

    private DynamicForm getDetailsTableForAlert(Record record) {

        DynamicForm form = new DynamicForm();
        form.setNumCols(4);
        form.setHeight("15%");
        form.setWrapItemTitles(false);
        form.setAlign(Alignment.LEFT);

        List<FormItem> items = new ArrayList<FormItem>();

        StaticTextItem nameTextItem = new StaticTextItem("name", "Name");
        nameTextItem.setValue(record.getAttribute("name"));
        nameTextItem.setTooltip("Id = " + record.getAttribute("id"));
        items.add(nameTextItem);

        StaticTextItem descriptionTextItem = new StaticTextItem("description", "Description");
        descriptionTextItem.setValue(record.getAttribute("description"));
        items.add(descriptionTextItem);

        StaticTextItem prioTextItem = new StaticTextItem("priority", "Priority");
        prioTextItem.setValue(record.getAttribute("priority"));
        items.add(prioTextItem);

        StaticTextItem createdTextItem = new StaticTextItem("ctime", "Created at");
        createdTextItem.setValue(record.getAttribute("ctime"));
        items.add(createdTextItem);

        StaticTextItem ackByItem = new StaticTextItem("ack_by", "Acknowledged by");
        if (record.getAttribute("ack_by") != null) {
            ackByItem.setValue(record.getAttribute("ack_by"));
        }
        items.add(ackByItem);

        StaticTextItem ackTimeItem = new StaticTextItem("ack_time", "Acknowledged at");
        if (record.getAttribute("ack_time") != null) {
            ackTimeItem.setValue(record.getAttribute("ack_time"));
        }
        items.add(ackTimeItem);

        StaticTextItem recoveryItem = new StaticTextItem("recovery", "Recovery Info");
        recoveryItem.setValue(record.getAttribute("recoveryInfo"));
        items.add(recoveryItem);

        form.setItems(items.toArray(new FormItem[items.size()]));

        return form;
    }

    private Table getNotificationsForAlert(Record record) {

        DataClass[] input = record.getAttributeAsRecordArray("notificationLogs");

        Table notifTable = new Table(extendLocatorId("Notifications"), "Notifications", false);
        notifTable.setHeight("35%");
        notifTable.setWidth100();
        ListGrid grid = notifTable.getListGrid();
        grid.setData((Record[]) input);

        ListGridField sender = new ListGridField(SENDER, "Sender");
        sender.setWidth("10%");
        ListGridField status = new ListGridField("status", "Result");
        status.setWidth("8%");
        ListGridField message = new ListGridField("message", "Message");
        message.setWidth("32%");
        ListGridField allEmails = new ListGridField("allEmails", "All Emails");
        allEmails.setWidth("25%");
        ListGridField badEmails = new ListGridField("badEmails", "Bad Emails");
        badEmails.setWidth("25%");

        grid.setFields(sender, status, message, allEmails, badEmails);

        return notifTable;
    }

    private Table getConditionsForAlert(Record record) {

        DataClass[] input = record.getAttributeAsRecordArray("conditionLogs");
        String mode = record.getAttribute("conditionExpression");

        Table table = new Table(extendLocatorId("ConditionLog"), "Conditions: match = " + mode, false);
        table.setHeight("35%");
        table.setWidth100();
        ListGrid grid = table.getListGrid();
        grid.setData((Record[]) input);

        ListGridField condition = new ListGridField("text", "Matching condition");
        condition.setWidth("60%");
        ListGridField value = new ListGridField("value", "Value");

        grid.setFields(condition, value);

        return table;

    }

}
