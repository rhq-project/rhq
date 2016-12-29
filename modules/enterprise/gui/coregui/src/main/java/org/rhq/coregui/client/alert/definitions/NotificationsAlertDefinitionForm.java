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

package org.rhq.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author John Mazzitelli
 */
public class NotificationsAlertDefinitionForm extends EnhancedVLayout implements EditAlertDefinitionForm {
    private static final String FIELD_OBJECT = "obj";
    private static final String FIELD_SENDER = "sender";
    private static final String FIELD_CONFIGURATION = "configuration";

    private AlertDefinition alertDefinition;
    private List<AlertNotification> notifications;

    private boolean formBuilt = false;

    private Table table;

    public NotificationsAlertDefinitionForm(AlertDefinition alertDefinition) {
        super();
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

    @Override
    public boolean isResetMatching() {
        return false;
    }

    private void buildForm() {
        if (!formBuilt) {

            table = new NotificationTable();

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

    private class NotificationDataSource extends RPCDataSource<AlertNotification, Criteria> {
        public NotificationDataSource() {
            super();
            List<DataSourceField> fields = addDataSourceFields();
            addFields(fields);
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();

            DataSourceTextField senderField = new DataSourceTextField(FIELD_SENDER,
                MSG.view_alert_definition_notification_editor_field_sender());
            fields.add(senderField);

            DataSourceTextField configField = new DataSourceTextField(FIELD_CONFIGURATION,
                MSG.common_title_configuration());
            fields.add(configField);

            return fields;
        }

        @Override
        public AlertNotification copyValues(Record from) {
            return (AlertNotification) from.getAttributeAsObject(FIELD_OBJECT);
        }

        @Override
        public ListGridRecord copyValues(AlertNotification from) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_OBJECT, from);
            record.setAttribute(FIELD_SENDER, from.getSenderName());
            // our executeFetch will fill in the real value for FIELD_CONFIGURATION
            record.setAttribute(FIELD_CONFIGURATION, "(" + MSG.common_status_unknown() + ")");
            return record;
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            // we don't use criterias for this datasource, just return null
            return null;
        }

        AlertNotification[] prepareNotificationsForPreview(){
            for(AlertNotification n: notifications){
                n.getAlertDefinition().getResource().getAlertDefinitions().clear();
            }
            return notifications.toArray(new AlertNotification[notifications.size()]);
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
            final Record[] records = buildRecords(notifications); // partially builds the records, but we need to do another remote call to get the config preview

            AlertNotification[] notifs = prepareNotificationsForPreview();
            GWTServiceLookup.getAlertDefinitionService().getAlertNotificationConfigurationPreview(notifs,
                new AsyncCallback<String[]>() {
                    @Override
                    public void onSuccess(String[] result) {
                        int i = 0;
                        for (Record record : records) {
                            record.setAttribute(FIELD_CONFIGURATION, result[i++]);
                        }
                        response.setData(records);
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_alert_definition_notification_editor_loadFailed_configPreview(), caught);
                        response.setData(records);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }
    }

    private class NotificationTable extends Table<NotificationDataSource> {
        public NotificationTable() {
            super();
            setShowHeader(false);

            final NotificationDataSource dataSource = new NotificationDataSource();
            setDataSource(dataSource);
        }

        @Override
        protected void configureTable() {
            ListGridField senderField = new ListGridField(FIELD_SENDER,
                MSG.view_alert_definition_notification_editor_field_sender());
            senderField.setWidth("25%");
            ListGridField configField = new ListGridField(FIELD_CONFIGURATION, MSG.common_title_configuration());
            configField.setWidth("75%");
            getListGrid().setFields(senderField, configField);

            setListGridDoubleClickHandler(new DoubleClickHandler() {
                @Override
                public void onDoubleClick(DoubleClickEvent event) {
                    ListGrid listGrid = (ListGrid) event.getSource();
                    ListGridRecord[] selectedRows = listGrid.getSelectedRecords();
                    if (selectedRows != null && selectedRows.length == 1) {
                        AlertNotification notif = (getDataSource()).copyValues(selectedRows[0]);
                        popupNotificationEditor(notif);
                    }
                }
            });

            addTableAction(MSG.common_button_add(), null, ButtonColor.BLUE, new AbstractTableAction() {
                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    popupNotificationEditor(null);
                }
            });

            addTableAction(MSG.common_button_delete(), MSG.view_alert_definition_notification_editor_delete_confirm(),
                ButtonColor.RED, new AbstractTableAction(TableActionEnablement.ANY) {
                    @Override
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        for (ListGridRecord record : selection) {
                            AlertNotification notif = (getDataSource()).copyValues(record);
                            notifications.remove(notif);
                        }
                        table.refresh();
                    }
                });
        }

        private void popupNotificationEditor(AlertNotification notifToEdit) {
            final Window winModal = new Window();
            if (notifToEdit == null) {
                winModal.setTitle(MSG.view_alert_definition_notification_editor_title_add());
            } else {
                winModal.setTitle(MSG.view_alert_definition_notification_editor_title_edit());
            }
            winModal.setOverflow(Overflow.VISIBLE);
            winModal.setShowMinimizeButton(false);
            winModal.setIsModal(true);
            winModal.setShowModalMask(true);
            winModal.setAutoSize(true);
            winModal.setAutoCenter(true);
            //winModal.setShowResizer(true);
            //winModal.setCanDragResize(true);
            winModal.centerInPage();
            winModal.addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    winModal.destroy();
                }
            });

            NewNotificationEditor newEditor = new NewNotificationEditor(alertDefinition, notifications, notifToEdit,
                new Runnable() {
                    @Override
                    public void run() {
                        winModal.destroy();
                        table.refresh();
                    }
                });
            winModal.addItem(newEditor);
            winModal.show();
        }
    }
}
