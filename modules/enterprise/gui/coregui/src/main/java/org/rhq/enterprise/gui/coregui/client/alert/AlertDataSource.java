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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * A server-side SmartGWT DataSource for CRUD of {@link Alert}s.
 *
 * @author Ian Springer
 */
public class AlertDataSource extends RPCDataSource {
    private static final String NAME = "Alert";
    private static final DateTimeFormat DATE_TIME_FORMAT = DateTimeFormat.getMediumDateTimeFormat();

    private static AlertDataSource INSTANCE;

    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    public static AlertDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AlertDataSource();            
        }
        return INSTANCE;
    }

    protected AlertDataSource() {
        super(NAME);

        setCanMultiSort(true);

        DataSourceField idField = new DataSourceIntegerField("id", "Id");
        idField.setPrimaryKey(true);
        idField.setHidden(true);

        // TODO: Replace 'Resource Id' column with 'Resource Name' and 'Resource Lineage' columns.
        DataSourceField resourceIdField = new DataSourceIntegerField(AlertCriteria.SORT_FIELD_RESOURCE_ID, "Resource Id");

        DataSourceTextField nameField = new DataSourceTextField(AlertCriteria.SORT_FIELD_NAME, "Name", 100);

        DataSourceTextField conditionTextField = new DataSourceTextField("conditionText", "Condition Text");
        conditionTextField.setCanSortClientOnly(true);

        DataSourceTextField conditionValueField = new DataSourceTextField("conditionValue", "Condition Value");
        conditionValueField.setCanSortClientOnly(true);

        DataSourceTextField recoveryInfoField = new DataSourceTextField("recoveryInfo", "Recovery Info");
        recoveryInfoField.setCanSortClientOnly(true);

        // TODO: Will using DataSourceEnumField here allow us to do
        //       record.setAttribute("priority", alert.getAlertDefinition().getPriority()), rather than
        //       record.setAttribute("priority", alert.getAlertDefinition().getPriority().name()) in
        //       createRecord() below?
        DataSourceTextField priorityField = new DataSourceTextField(AlertCriteria.SORT_FIELD_PRIORITY, "Priority", 15);

        DataSourceTextField ctimeField = new DataSourceTextField(AlertCriteria.SORT_FIELD_CTIME, "Creation Time");

        setFields(idField, resourceIdField, nameField, conditionTextField, conditionValueField, recoveryInfoField,
                priorityField, ctimeField);
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
                System.out.println("Deleted Alerts with id's: " + Arrays.toString(alertIds) + ".");
                alertsView.reloadData();
            }

            public void onFailure(Throwable caught) {
                Window.alert("Failed to delete Alerts with id's: " + Arrays.toString(alertIds) + " - cause: " + caught);
                System.err.println("Failed to delete Alerts with id's " + Arrays.toString(alertIds) + " - cause: " + caught);
            }
        });
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);
        criteria.fetchRecoveryAlertDefinition(true);
        // TODO: Uncomment the below once the bad performance of it has been fixed.
        //criteria.fetchConditionLogs(true);

        criteria.setPageControl(getPageControl(request));

        this.alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to fetch Alerts - cause: " + caught);
                System.err.println("Failed to fetch Alerts - cause: " + caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                System.out.println(result.size() + " Alerts fetched in: " + fetchTime + "ms");

                ListGridRecord[] records = new ListGridRecord[result.size()];
                for (int i = 0; i < result.size(); i++) {
                    Alert alert = result.get(i);
                    ListGridRecord record = createRecord(alert);
                    records[i] = record;
                }

                response.setData(records);
                // For paging to work, we have to specify size of full result set.
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected void executeRemove(final DSRequest request, final DSResponse response) {
        JavaScriptObject data = request.getData ();
        ListGridRecord record = new ListGridRecord(data);
        Window.alert(String.valueOf(record.getAttributeAsInt("id")));
    }

    private ListGridRecord createRecord(Alert alert) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", alert.getId());
        record.setAttribute("resourceId", alert.getAlertDefinition().getResource().getId());        
        record.setAttribute("name", alert.getAlertDefinition().getName());
        record.setAttribute("priority", alert.getAlertDefinition().getPriority().name());
        record.setAttribute("ctime", DATE_TIME_FORMAT.format(new Date(alert.getCtime())));

        Set<AlertConditionLog> conditionLogs = alert.getConditionLogs();
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

        String recoveryInfo = AlertFormatUtility.getAlertRecoveryInfo(alert);
        record.setAttribute("recoveryInfo", recoveryInfo);
        return record;
    }
}