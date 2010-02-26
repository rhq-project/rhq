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
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
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

        DataSourceField idDataField = new DataSourceIntegerField("id", "Id");
        idDataField.setPrimaryKey(true);
        idDataField.setHidden(true);

        DataSourceField resourceIdDataField = new DataSourceIntegerField("alertDefinition.resource.Id", "Resource Id");
        idDataField.setHidden(true);

        DataSourceTextField nameField = new DataSourceTextField("alertDefinition.name", "Name", 100);

        DataSourceTextField conditionTextField = new DataSourceTextField("conditionText", "Condition Text");
        conditionTextField.setCanSortClientOnly(true);

        DataSourceTextField conditionValueField = new DataSourceTextField("conditionValue", "Condition Value");
        conditionValueField.setCanSortClientOnly(true);

        DataSourceTextField recoveryInfoField = new DataSourceTextField("recoveryInfo", "Recovery Info");
        recoveryInfoField.setCanSortClientOnly(true);

        // TODO: Use DataSourceEnumField here?
        DataSourceTextField priorityField = new DataSourceTextField("alertDefinition.priority", "Priority", 15);

        DataSourceTextField ctimeField = new DataSourceTextField("ctime", "Creation Time");

        setFields(idDataField, nameField, conditionTextField, conditionValueField, recoveryInfoField, priorityField,
                ctimeField);
    }

    void deleteAlerts(final ListGrid listGrid, final AlertsView alertsView) {
        ListGridRecord[] records = listGrid.getSelection();
        final Map<Integer, List<ListGridRecord>> alertIdMap = new HashMap<Integer, List<ListGridRecord>>();
        for (int i = 0, selectionLength = records.length; i < selectionLength; i++) {
            ListGridRecord record = records[i];
            Integer resourceId = record.getAttributeAsInt("alertDefinition.resource.id");
            List<ListGridRecord> recordsForResource;
            if (alertIdMap.containsKey(resourceId)) {
                recordsForResource = alertIdMap.get(resourceId);
            } else {
                recordsForResource = new ArrayList<ListGridRecord>();
                alertIdMap.put(resourceId, recordsForResource);
            }
            recordsForResource.add(record);
        }

        AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();
        final Set<Integer> successfulResourceIds = new HashSet<Integer>();
        final Set<Integer> failedResourceIds = new HashSet<Integer>();
        for (final Integer resourceId : alertIdMap.keySet()) {
            final List<ListGridRecord> recordsForResource = alertIdMap.get(resourceId);
            Integer[] alertIds = new Integer[recordsForResource.size()];
            for (int i = 0; i < recordsForResource.size(); i++) {
                ListGridRecord listGridRecord = recordsForResource.get(i);
                Integer alertId = listGridRecord.getAttributeAsInt("id");
                alertIds[i] = alertId;
            }

            alertService.deleteAlerts(resourceId, alertIds, new AsyncCallback<Void>() {
                public void onSuccess(Void blah) {
                    /*for (ListGridRecord record : recordsForResource) {
                        removeData(record);
                    }*/
                    successfulResourceIds.add(resourceId);
                    if (successfulResourceIds.size() + failedResourceIds.size() == alertIdMap.size()) {
                        alertsView.reportSelectedAlertsDeleted(listGrid);
                    }
                }

                public void onFailure(Throwable caught) {
                    Window.alert("Failed to delete Alerts for Resource with id " + resourceId + " - cause: " + caught);
                    System.err.println("Failed to delete Alerts for Resource with id " + resourceId + " - cause: " + caught);
                    failedResourceIds.add(resourceId);
                    if (successfulResourceIds.size() + failedResourceIds.size() == alertIdMap.size()) {
                        // TODO: Report failure.
                    }
                }
            });
        }
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);
        criteria.fetchConditionLogs(true);
        criteria.fetchRecoveryAlertDefinition(true);

        criteria.setPageControl(getPageControl(request, criteria.getAlias()));

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
        record.setAttribute("alertDefinition.resource.id", alert.getAlertDefinition().getResource().getId());        
        record.setAttribute("alertDefinition.name", alert.getAlertDefinition().getName());
        record.setAttribute("alertDefinition.priority", alert.getAlertDefinition().getPriority().name());
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
            conditionText = AlertDefinitionUtility.formatAlertConditionForDisplay(condition);
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

        String recoveryInfo = AlertDefinitionUtility.getAlertRecoveryInfo(alert);
        record.setAttribute("recoveryInfo", recoveryInfo);
        return record;
    }

    @Override
    protected List<OrderingField> getDefaultOrderingFields(String alias) {
        List<OrderingField> orderingFields = new ArrayList<OrderingField>(2);
        orderingFields.add(new OrderingField(alias + ".alertDefinition.name", PageOrdering.ASC));
        orderingFields.add(new OrderingField(alias + ".ctime", PageOrdering.DESC));
        return orderingFields;
    }
}