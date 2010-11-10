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

import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceDateField;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementConverterClient;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Ian Springer
 * @author Joseph Marques
 */
public class AlertDataSource extends RPCDataSource<Alert> {
    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    private EntityContext entityContext;

    public AlertDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public AlertDataSource(EntityContext context) {
        super();
        this.entityContext = context;

        // TODO: when these fields are added, AlertHistoryView breaks -- why?

        //List<DataSourceField> fields = addDataSourceFields();
        //addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceDateField ctimeField = new DataSourceDateField(AlertCriteria.SORT_FIELD_CTIME, "Creation Time");
        addField(ctimeField);

        DataSourceIntegerField ackTimeField = new DataSourceIntegerField("acknowledgeTime", "Ack Time");
        addField(ackTimeField);

        DataSourceTextField ackSubjectField = new DataSourceTextField("acknowledgingSubject", "Ack Subject");
        addField(ackSubjectField);

        DataSourceTextField nameField = new DataSourceTextField(AlertCriteria.SORT_FIELD_NAME, "Name");
        addField(nameField);

        DataSourceTextField conditionTextField = new DataSourceTextField("conditionText", "Condition Text");
        addField(conditionTextField);

        DataSourceTextField conditionValueField = new DataSourceTextField("conditionValue", "Condition Value");
        addField(conditionValueField);

        DataSourceTextField resourceNameField = new DataSourceTextField("resourceName", "Resource");
        addField(resourceNameField);

        DataSourceTextField priorityField = new DataSourceTextField(AlertCriteria.SORT_FIELD_PRIORITY, "Priority", 15);
        addField(priorityField);

        return fields;
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = getCriteria(request);

        //check for still logged in before submitting server side request
        if (userStillLoggedIn()) {
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
        } else {//dump request
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            Log.debug("user not logged in. Not fetching any alerts now.");
        }

    }

    protected AlertCriteria getCriteria(DSRequest request) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.setPageControl(getPageControl(request));

        criteria.addFilterPriorities(getArrayFilter(request, "severities", AlertPriority.class));
        criteria.addFilterEntityContext(entityContext);

        return criteria;
    }

    @Override
    public Alert copyValues(Record from) {
        return null; // TODO: Implement this method.
    }

    @Override
    public ListGridRecord copyValues(Alert from) {
        return convert(from);
    }

    public static ListGridRecord convert(Alert from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getId());
        record.setAttribute("ctime", from.getCtime());
        record.setAttribute("acknowledgeTime", from.getAcknowledgeTime());
        record.setAttribute("acknowledgingSubject", from.getAcknowledgingSubject());

        record.setAttribute("resourceId", from.getAlertDefinition().getResource().getId());
        record.setAttribute("resourceName", from.getAlertDefinition().getResource().getName());
        record.setAttribute("name", from.getAlertDefinition().getName());
        record.setAttribute("priority", from.getAlertDefinition().getPriority().name());

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

    public AlertGWTServiceAsync getAlertService() {
        return alertService;
    }
}