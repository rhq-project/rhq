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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceBooleanField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * A server-side SmartGWT DataSource for CRUD of {@link Alert}s.
 *
 * @author Ian Springer
 */
public class AlertDataSource extends RPCDataSource<Alert> {
    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    public AlertDataSource() {
        super();

        setCanMultiSort(true);

        List<DataSourceField> fields = createFields();
        addFields(fields);
    }

    protected List<DataSourceField> createFields() {
        List<DataSourceField> fields = new ArrayList<DataSourceField>();

        DataSourceField idField = new DataSourceIntegerField("id", "Id");
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);

        DataSourceTextField nameField = new DataSourceTextField(AlertCriteria.SORT_FIELD_NAME, "Name");
        fields.add(nameField);

        DataSourceTextField conditionTextField = new DataSourceTextField("conditionText", "Condition Text");
        conditionTextField.setCanSortClientOnly(true);
        fields.add(conditionTextField);

        DataSourceTextField conditionValueField = new DataSourceTextField("conditionValue", "Condition Value");
        conditionValueField.setCanSortClientOnly(true);
        fields.add(conditionValueField);

        DataSourceTextField resourceName = new DataSourceTextField("resourceName", "Resource");
        resourceName.setCanSortClientOnly(true);
        fields.add(resourceName);

        //        DataSourceTextField recoveryInfoField = new DataSourceTextField("recoveryInfo", "Recovery Info");
        //        recoveryInfoField.setCanSortClientOnly(true);
        //        fields.add(recoveryInfoField);

        // TODO: Will using DataSourceEnumField here allow us to do
        //       record.setAttribute("priority", alert.getAlertDefinition().getPriority()), rather than
        //       record.setAttribute("priority", alert.getAlertDefinition().getPriority().name()) in
        //       createRecord() below?
        DataSourceTextField priorityField = new DataSourceTextField(AlertCriteria.SORT_FIELD_PRIORITY, "Priority", 15);
        fields.add(priorityField);

        DataSourceIntegerField ctimeField = new DataSourceIntegerField(AlertCriteria.SORT_FIELD_CTIME, "Creation Time");
        fields.add(ctimeField);

        DataSourceBooleanField boolField = new DataSourceBooleanField("ack", "Ack'd");
        boolField.setCanSortClientOnly(true);
        fields.add(boolField);

        return fields;
    }

    void deleteAlerts(final AlertsView alertsView) {
        ListGrid listGrid = alertsView.getListGrid();
        ListGridRecord[] records = listGrid.getSelection();

        final Integer[] alertIds = new Integer[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer alertId = record.getAttributeAsInt("id");
            alertIds[i] = alertId;
        }

        this.alertService.deleteResourceAlerts(alertIds, new AsyncCallback<Void>() {
            public void onSuccess(Void blah) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Deleted [" + alertIds.length + "] alerts", Message.Severity.Info));
                alertsView.refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to delete alerts with id's: " + Arrays.toString(alertIds), caught);
            }
        });
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = getCriteria(request);

        this.alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch alerts data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                com.allen_sauer.gwt.log.client.Log.info(result.size() + " alerts fetched in: " + fetchTime + "ms");
                response.setData(buildRecords(result));
                // For paging to work, we have to specify size of full result set.
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected AlertCriteria getCriteria(DSRequest request) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);
        criteria.fetchRecoveryAlertDefinition(true);
        // TODO: Uncomment the below once the bad performance of it has been fixed.
        //criteria.fetchConditionLogs(true);

        Criteria requestCriteria = request.getCriteria();
        if (requestCriteria != null) {
            Map values = requestCriteria.getValues();
            for (Object key : values.keySet()) {
                String fieldName = (String) key;
                if (fieldName.equals(AlertCriteria.SORT_FIELD_RESOURCE_ID)) {
                    Integer resourceId = (Integer) values.get(fieldName);
                    criteria.addFilterResourceIds(resourceId);
                }
                // TODO: Add support for other fields we need to filter by (e.g. resourceGroupId).
            }
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    public Alert copyValues(ListGridRecord from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(Alert from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("resourceId", from.getAlertDefinition().getResource().getId());
        record.setAttribute("resourceName", from.getAlertDefinition().getResource().getName());
        record.setAttribute("name", from.getAlertDefinition().getName());
        record.setAttribute("priority", from.getAlertDefinition().getPriority().name());
        record.setAttribute("ctime", from.getCtime());
        if (from.getAcknowledgeTime() > 0) {
            record.setAttribute("ack", "true");
        }

        Set<AlertConditionLog> conditionLogs = from.getConditionLogs();
        String conditionText;
        String conditionValue;
        if (conditionLogs.size() > 1) {
            conditionText = "Multiple Conditions";
            conditionValue = "--";
        } else if (conditionLogs.size() == 1) {
            AlertConditionLog conditionLog = conditionLogs.iterator().next();
            AlertCondition condition = conditionLog.getCondition();
            conditionText = AlertFormatUtility.formatAlertConditionForDisplay(condition);
            conditionValue = conditionLog.getValue();
            if (condition.getMeasurementDefinition() != null) {
                conditionValue = MeasurementConverterClient.format(Double.valueOf(conditionLog.getValue()), condition
                    .getMeasurementDefinition().getUnits(), true);
            }
        } else {
            conditionText = "No Conditions";
            conditionValue = "--";
        }
        record.setAttribute("conditionText", conditionText);
        record.setAttribute("conditionValue", conditionValue);

        // We also need the'raw' notification data to show in details
        DataClass[] conditions = new DataClass[from.getConditionLogs().size()];
        int i = 0;
        for (AlertConditionLog log : from.getConditionLogs()) {
            AlertCondition condition = log.getCondition();
            DataClass dc = new DataClass();
            dc.setAttribute("text", AlertFormatUtility.formatAlertConditionForDisplay(condition));
            String value = log.getValue();
            if (condition.getMeasurementDefinition() != null) {
                value = MeasurementConverterClient.format(Double.valueOf(log.getValue()), condition
                    .getMeasurementDefinition().getUnits(), true);
            }
            dc.setAttribute("value", value);
            conditions[i++] = dc;
        }
        record.setAttribute("conditionLogs", conditions);
        record.setAttribute("conditionExpression", from.getAlertDefinition().getConditionExpression());

        String recoveryInfo = AlertFormatUtility.getAlertRecoveryInfo(from);
        record.setAttribute("recoveryInfo", recoveryInfo);

        // Alert notification logs
        DataClass[] notifications = new DataClass[from.getAlertNotificationLogs().size()];
        i = 0;
        for (AlertNotificationLog log : from.getAlertNotificationLogs()) {
            DataClass dc = new DataClass();
            dc.setAttribute("sender", log.getSender());
            dc.setAttribute("status", log.getResultState());
            dc.setAttribute("message", log.getMessage());

            notifications[i++] = dc;
        }
        record.setAttribute("notificationLogs", notifications);
        return record;
    }

    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData();
        ListGridRecord record = new ListGridRecord(data);
        Window.alert(String.valueOf(record.getAttributeAsInt("id")));
    }

    public void acknowledgeAlerts(final AlertsView alertsView) {
        ListGrid listGrid = alertsView.getListGrid();
        ListGridRecord[] records = listGrid.getSelection();

        final Integer[] alertIds = new Integer[records.length];
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer alertId = record.getAttributeAsInt("id");
            alertIds[i] = alertId;
        }

        this.alertService.acknowledgeResourceAlerts(alertIds, new AsyncCallback<Void>() {
            public void onSuccess(Void blah) {
                CoreGUI.getMessageCenter().notify(
                    new Message("Acknowledged [" + alertIds.length + "] alerts", Message.Severity.Info));

                com.allen_sauer.gwt.log.client.Log.info("Acknowledged Alerts with id's: " + Arrays.toString(alertIds) + ".");
                alertsView.refresh();
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    "Failed to acknowledge Alerts with id's: " + Arrays.toString(alertIds), caught);
                System.err.println("Failed to acknowledge Alerts with id's " + Arrays.toString(alertIds) + " - cause: "
                    + caught);
            }
        });
    }

    public AlertGWTServiceAsync getAlertService() {
        return alertService;
    }
}