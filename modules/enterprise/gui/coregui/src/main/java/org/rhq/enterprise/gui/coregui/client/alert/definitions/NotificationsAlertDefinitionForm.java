/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author John Mazzitelli
 */
public class NotificationsAlertDefinitionForm extends LocatableVLayout implements EditAlertDefinitionForm {
    private static final String FIELD_OBJECT = "obj";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_CONFIGURATION = "configuration";

    private AlertDefinition alertDefinition;
    private ArrayList<AlertNotification> notifications;

    private boolean formBuilt = false;

    private Table table;

    public NotificationsAlertDefinitionForm(String locatorId) {
        this(locatorId, null);
    }

    public NotificationsAlertDefinitionForm(String locatorId, AlertDefinition alertDefinition) {
        super(locatorId);
        this.alertDefinition = alertDefinition;
        extractShallowCopyOfNotifications(this.alertDefinition);
    }

    @Override
    protected void onInit() {
        super.onInit();

        if (!formBuilt) {
            buildForm();
            setAlertDefinition(alertDefinition);
            makeViewOnly();
        }
    }

    @Override
    public AlertDefinition getAlertDefinition() {
        return alertDefinition;
    }

    @Override
    public void setAlertDefinition(AlertDefinition alertDef) {
        alertDefinition = alertDef;

        buildForm();

        if (alertDef == null) {
            clearFormValues();
        } else {
            extractShallowCopyOfNotifications(alertDefinition);
            if (table != null) {
                table.refresh();
            }
        }

        markForRedraw();
    }

    @Override
    public void makeEditable() {
        table.setTableActionDisableOverride(false);
        markForRedraw();
    }

    @Override
    public void makeViewOnly() {
        table.setTableActionDisableOverride(true);
        markForRedraw();
    }

    @Override
    public void saveAlertDefinition() {
        if (notifications != null && notifications.size() > 0) {
            for (AlertNotification notif : notifications) {
                notif.setAlertDefinition(alertDefinition);
            }
        }
        alertDefinition.setAlertNotifications(notifications);

        // make our own new internal copy since we gave ours to the definition object
        extractShallowCopyOfNotifications(alertDefinition);
    }

    @Override
    public void clearFormValues() {
        notifications.clear();
        if (table != null) {
            table.refresh();
        }
        markForRedraw();
    }

    private void buildForm() {
        if (!formBuilt) {

            table = new NotificationTable(extendLocatorId("notificationsTable"));

            addMember(table);

            formBuilt = true;
        }
    }

    private void extractShallowCopyOfNotifications(AlertDefinition alertDefinition) {
        List<AlertNotification> notifs = null;
        if (alertDefinition != null) {
            notifs = alertDefinition.getAlertNotifications();
        }

        // make our own shallow copy of the collection
        if (notifs != null) {
            this.notifications = new ArrayList<AlertNotification>(notifs);
        } else {
            this.notifications = new ArrayList<AlertNotification>();
        }
    }

    private class NotificationDataSource extends RPCDataSource<AlertNotification> {
        public NotificationDataSource() {
            DataSourceTextField senderField = new DataSourceTextField(FIELD_SENDER, "Sender");
            addField(senderField);

            DataSourceTextField configField = new DataSourceTextField(FIELD_CONFIGURATION, "Configuration");
            addField(configField);
        }

        @Override
        public AlertNotification copyValues(ListGridRecord from) {
            return (AlertNotification) from.getAttributeAsObject(FIELD_OBJECT);
        }

        @Override
        public ListGridRecord copyValues(AlertNotification from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_OBJECT, from);
            record.setAttribute(FIELD_SENDER, from.getSenderName());
            // our executeFetch will fill in the real value for FIELD_CONFIGURATION
            record.setAttribute(FIELD_CONFIGURATION, "(unknown)");
            return record;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response) {
            final ListGridRecord[] records = buildRecords(notifications); // partially builds the records, but we need to do another remote call to get the config preview

            AlertNotification[] notifs = notifications.toArray(new AlertNotification[notifications.size()]);
            GWTServiceLookup.getAlertDefinitionService().getAlertNotificationConfigurationPreview(notifs,
                new AsyncCallback<String[]>() {
                    @Override
                    public void onSuccess(String[] result) {
                        int i = 0;
                        for (ListGridRecord record : records) {
                            record.setAttribute(FIELD_CONFIGURATION, result[i++]);
                        }
                        response.setData(records);
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to get notification configuration preview",
                            caught);
                        response.setData(records);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }
    }

    private class NotificationTable extends Table {
        public NotificationTable(String locatorId) {
            super(locatorId);
            setShowHeader(false);

            final NotificationDataSource dataSource = new NotificationDataSource();
            setDataSource(dataSource);
        }

        @Override
        protected void configureTable() {
            ListGridField senderField = new ListGridField(FIELD_SENDER, "Sender");
            senderField.setWidth("25%");
            ListGridField configField = new ListGridField(FIELD_CONFIGURATION, "Configuration");
            configField.setWidth("75%");
            getListGrid().setFields(senderField, configField);

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelection();
                    if (selectedRows != null && selectedRows.length == 1) {
                        AlertNotification notif = ((NotificationDataSource) getDataSource())
                            .copyValues(selectedRows[0]);
                        popupNotificationEditor(notif);
                    }
                }
            });

            addTableAction(this.extendLocatorId("add"), "Add", SelectionEnablement.ALWAYS, null, new TableAction() {
                @Override
                public void executeAction(ListGridRecord[] selection) {
                    popupNotificationEditor(null);
                }
            });

            addTableAction(this.extendLocatorId("delete"), "Delete", SelectionEnablement.ANY, "Are you sure?",
                new TableAction() {
                    @Override
                    public void executeAction(ListGridRecord[] selection) {
                        for (ListGridRecord record : selection) {
                            AlertNotification notif = ((NotificationDataSource) getDataSource()).copyValues(record);
                            notifications.remove(notif);
                        }
                        table.refresh();
                    }
                });
        }

        private void popupNotificationEditor(AlertNotification notifToEdit) {
            final Window winModal = new LocatableWindow(NotificationsAlertDefinitionForm.this
                .extendLocatorId("notificationEditorWindow"));
            if (notifToEdit == null) {
                winModal.setTitle("Add Notification");
            } else {
                winModal.setTitle("Edit Notification");
            }
            winModal.setOverflow(Overflow.VISIBLE);
            winModal.setShowMinimizeButton(false);
            winModal.setIsModal(true);
            winModal.setShowModalMask(true);
            winModal.setAutoSize(true);
            winModal.setAutoCenter(true);
            winModal.setShowResizer(true);
            winModal.setCanDragResize(true);
            winModal.centerInPage();
            winModal.addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClientEvent event) {
                    winModal.markForDestroy();
                }
            });

            NewNotificationEditor newEditor = new NewNotificationEditor(extendLocatorId("newNotificationEditor"),
                alertDefinition, notifications, notifToEdit, new Runnable() {
                    @Override
                    public void run() {
                        winModal.markForDestroy();
                        table.refresh();
                    }
                });
            winModal.addItem(newEditor);
            winModal.show();
        }
    }
}
