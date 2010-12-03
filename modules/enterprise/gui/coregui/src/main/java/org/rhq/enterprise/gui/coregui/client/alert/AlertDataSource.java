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
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Ian Springer
 * @author Joseph Marques
 * @author John Mazzitelli
 */
public class AlertDataSource extends RPCDataSource<Alert> {
    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    private EntityContext entityContext;

    public static final String PRIORITY_ICON_HIGH = ImageManager.getAlertIcon(AlertPriority.HIGH);
    public static final String PRIORITY_ICON_MEDIUM = ImageManager.getAlertIcon(AlertPriority.MEDIUM);
    public static final String PRIORITY_ICON_LOW = ImageManager.getAlertIcon(AlertPriority.LOW);

    public AlertDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public AlertDataSource(EntityContext context) {
        super();
        this.entityContext = context;

        addDataSourceFields();
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        // for some reason, the client seems to crash if you don't specify any data source fields
        // even though we know we defined override ListGridFields for all columns.
        List<DataSourceField> fields = super.addDataSourceFields();
        fields.add(new DataSourceTextField("name"));
        return fields;
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     * 
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

        ListGridField ctimeField = new ListGridField(AlertCriteria.SORT_FIELD_CTIME, MSG
            .view_alerts_field_created_time());
        ctimeField.setWidth("15%");
        ctimeField.setAutoFitWidth(true);
        ctimeField.setAutoFitWidthApproach(AutoFitWidthApproach.TITLE);
        ctimeField.setCellFormatter(new TimestampCellFormatter());
        fields.add(ctimeField);

        ListGridField nameField = new ListGridField("name", MSG.view_alerts_field_name());
        nameField.setWidth("25%");
        nameField.setAutoFitWidth(true);
        nameField.setAutoFitWidthApproach(AutoFitWidthApproach.TITLE);
        fields.add(nameField);

        ListGridField conditionField = new ListGridField("conditionText", MSG.view_alerts_field_condition_text());
        conditionField.setWidth("30%");
        conditionField.setAutoFitWidth(true);
        conditionField.setAutoFitWidthApproach(AutoFitWidthApproach.TITLE);
        fields.add(conditionField);

        ListGridField priorityField = new ListGridField("priority", MSG.view_alerts_field_priority());
        priorityField.setType(ListGridFieldType.IMAGE);
        priorityField.setWidth("5%");
        priorityField.setAutoFitWidth(true);
        priorityField.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
        priorityField.setAlign(Alignment.CENTER);
        priorityField.setShowHover(true);
        priorityField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String prio = record.getAttribute("priority");
                if (PRIORITY_ICON_HIGH.equals(prio)) {
                    return MSG.common_alert_high();
                } else if (PRIORITY_ICON_MEDIUM.equals(prio)) {
                    return MSG.common_alert_medium();
                } else if (PRIORITY_ICON_LOW.equals(prio)) {
                    return MSG.common_alert_low();
                } else {
                    return ""; // will never get here
                }
            }
        });
        fields.add(priorityField);

        ListGridField statusField = new ListGridField("status", MSG.common_title_status());
        statusField.setWidth("15%");
        statusField.setAutoFitWidth(true);
        statusField.setAutoFitWidthApproach(AutoFitWidthApproach.TITLE);
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String ackSubject = listGridRecord.getAttribute("acknowledgingSubject");
                if (ackSubject == null) {
                    return MSG.view_alerts_field_ack_status_empty();
                } else {
                    Date ackTime = listGridRecord.getAttributeAsDate("acknowledgeTime");
                    String formattedTime = TimestampCellFormatter.format(ackTime);
                    return MSG.view_alerts_field_ack_status_filled(ackSubject, formattedTime);
                }
            }
        });
        fields.add(statusField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            // TODO need to disambiguate this
            ListGridField resourceNameField = new ListGridField("resourceName", MSG.view_alerts_field_resource());
            resourceNameField.setWidth("10%");
            resourceNameField.setAutoFitWidth(true);
            resourceNameField.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Integer resourceId = listGridRecord.getAttributeAsInt("resourceId");
                    return "<a href=\"" + LinkManager.getResourceLink(resourceId) + "\">" + o + "</a>";
                }
            });
            fields.add(resourceNameField);
        }

        return fields;
    }

    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = getCriteria(request);

        this.alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                Log.info(result.size() + " alerts fetched in: " + fetchTime + "ms");
                response.setData(buildRecords(result));
                // For paging to work, we have to specify size of full result set.
                response.setTotalRows(result.getTotalSize());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    protected AlertCriteria getCriteria(DSRequest request) {
        AlertCriteria criteria = new AlertCriteria();
        criteria.setPageControl(getPageControl(request));

        criteria.addFilterPriorities(getArrayFilter(request, "severities", AlertPriority.class));
        criteria.addFilterEntityContext(entityContext);
        criteria.fetchConditionLogs(true);

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
        record.setAttribute("ctime", new Date(from.getCtime()));
        if (from.getAcknowledgeTime() != null && from.getAcknowledgeTime().longValue() > 0) {
            record.setAttribute("acknowledgeTime", new Date(from.getAcknowledgeTime().longValue()));
        }
        record.setAttribute("acknowledgingSubject", from.getAcknowledgingSubject());

        record.setAttribute("resourceId", from.getAlertDefinition().getResource().getId());
        record.setAttribute("resourceName", from.getAlertDefinition().getResource().getName());
        record.setAttribute("name", from.getAlertDefinition().getName());
        record.setAttribute("priority", ImageManager.getAlertIcon(from.getAlertDefinition().getPriority()));

        Set<AlertConditionLog> conditionLogs = from.getConditionLogs();
        String conditionText;
        String conditionValue;
        if (conditionLogs.size() > 1) {
            conditionText = MSG.view_alerts_field_condition_text_many();
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
            conditionText = MSG.view_alerts_field_condition_text_none();
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

    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        // TODO
        Window.alert(String.valueOf(recordToRemove.getAttributeAsInt("id")));
    }

    public AlertGWTServiceAsync getAlertService() {
        return alertService;
    }
}