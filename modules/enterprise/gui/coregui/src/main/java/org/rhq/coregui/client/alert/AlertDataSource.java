/*
 * RHQ Management Platform
 * Copyright (C) 2010-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.alert;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.core.DataClass;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ImageStyle;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.templates.AlertDefinitionTemplateTypeView;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Ian Springer
 * @author Joseph Marques
 * @author John Mazzitelli
 */
public class AlertDataSource extends RPCDataSource<Alert, AlertCriteria> {
    
    private static final String FIELD_PARENT = "parent"; // may be template or group alert def parent

    public static final String PRIORITY_ICON_HIGH = ImageManager.getAlertIcon(AlertPriority.HIGH);
    public static final String PRIORITY_ICON_MEDIUM = ImageManager.getAlertIcon(AlertPriority.MEDIUM);
    public static final String PRIORITY_ICON_LOW = ImageManager.getAlertIcon(AlertPriority.LOW);

    public static final String FILTER_PRIORITIES = "priorities";
    public static final String FILTER_RESOURCE_IDS = "resourceIds";

    private AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();

    private EntityContext entityContext;

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
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceField idField = new DataSourceIntegerField("id", MSG.common_title_id(), 50);
        idField.setPrimaryKey(true);
        idField.setCanEdit(false);
        idField.setHidden(true);
        fields.add(idField);

        return fields;
    }

    
    public ArrayList<ListGridField> getListGridFields() {
        return getListGridFields(true);
    }
    
    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     *
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields(boolean showResourceAncestry) {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

        ListGridField ctimeField = new ListGridField(AlertCriteria.SORT_FIELD_CTIME, MSG.common_title_createTime());
        ctimeField.setCellFormatter(new TimestampCellFormatter(TimestampCellFormatter.DATE_TIME_FORMAT_SHORT));
        ctimeField.setShowHover(true);
        ctimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(AlertCriteria.SORT_FIELD_CTIME));
        fields.add(ctimeField);

        ListGridField nameField = new ListGridField("name", MSG.common_title_name());
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                Integer resourceId = listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                Integer defId = listGridRecord.getAttributeAsInt("definitionId");
                String url = LinkManager.getSubsystemAlertDefinitionLink(resourceId, defId);
                return LinkManager.getHref(url, o.toString());
            }
        });
        fields.add(nameField);

        ListGridField conditionField = new ListGridField("conditionText", MSG.view_alerts_field_condition_text());
        conditionField.setCanSortClientOnly(true);
        fields.add(conditionField);

        ListGridField conditionLogField = new ListGridField("conditionValue", MSG.view_alerts_field_condition_value());
        conditionLogField.setCanSortClientOnly(true);
        fields.add(conditionLogField);

        ListGridField priorityField = new ListGridField("priority", MSG.view_alerts_field_priority());
        priorityField.setType(ListGridFieldType.IMAGE);
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

        ListGridField statusField = new ListGridField("acknowledgingSubject", MSG.common_title_acknowledged());
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                String ackSubject = listGridRecord.getAttribute("acknowledgingSubject");
                if (ackSubject == null) {
                    return "&nbsp;";
                } else {
                    Img checkedImg = new Img(ImageManager.getAlertStatusCheckedIcon(),80,16);
                    checkedImg.setImageType(ImageStyle.CENTER);
                    return checkedImg.getInnerHTML();
                }
            }
        });
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String ackSubject = record.getAttribute("acknowledgingSubject");
                StringBuilder sb = new StringBuilder("<p");
                if (ackSubject == null) {
                    sb.append(" style='width:150px'>");
                    sb.append(MSG.view_alerts_field_ack_status_noAckHover());
                } else {
                    sb.append(" style='width:500px'>");
                    Date ackDateTime = record.getAttributeAsDate("acknowledgeTime");
                    String ackDateTimeString = TimestampCellFormatter.format(ackDateTime,
                        TimestampCellFormatter.DATE_TIME_FORMAT_FULL);
                    sb.append(MSG.view_alerts_field_ack_status_ackHover(ackSubject, ackDateTimeString));
                }
                sb.append("</p>");
                return sb.toString();
            }
        });
        fields.add(statusField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String url = LinkManager.getResourceLink(record.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return LinkManager.getHref(url, value.toString());
                }
            });
            resourceNameField.setShowHover(true);
            resourceNameField.setHoverCustomizer(new HoverCustomizer() {

                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(resourceNameField);

            if (showResourceAncestry) {
                ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
                fields.add(ancestryField);
                ancestryField.setWidth("20%");
            }
            ctimeField.setWidth(100);
            nameField.setWidth("15%");
            conditionField.setWidth("15%");
            conditionLogField.setWidth("25%");
            priorityField.setWidth(50);
            statusField.setWidth(80);
            resourceNameField.setWidth("20%");
        } else {
            ctimeField.setWidth(200);
            nameField.setWidth("15%");
            conditionField.setWidth("20%");
            conditionLogField.setWidth("35%");
            priorityField.setWidth(50);
            statusField.setWidth("25%");
        }

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, final AlertCriteria criteria) {
        if (criteria == null) {
            // the user selected no priorities in the filter - it makes sense from the UI perspective to show 0 rows
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }

        final long start = System.currentTimeMillis();

        this.alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_alerts_loadFailed(), caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                Log.info(result.size() + " alerts fetched in: " + fetchTime + "ms");

                dataRetrieved(result, response, request);
            }
        });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overridden.
     */
    protected void dataRetrieved(final PageList<Alert> result, final DSResponse response, final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate, the alerts are for a single resource
        case Resource:
            response.setData(buildRecords(result));
            setPagingInfo(response, result);
            processResponse(request.getRequestId(), response);
            break;

        // disambiguate as the results could be cross-resource
        default:
            Set<Integer> typesSet = new HashSet<Integer>();
            Set<String> ancestries = new HashSet<String>();
            for (Alert alert : result) {
                Resource resource = alert.getAlertDefinition().getResource();
                typesSet.add(resource.getResourceType().getId());
                ancestries.add(resource.getAncestry());
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // Smartgwt has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    Record[] records = buildRecords(result);
                    for (Record record : records) {
                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                        // Build the decoded ancestry Strings now for display
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }
                    response.setData(records);
                    // for paging to work we have to specify size of full result set
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    /**
     * Sub-classes can override this to add fine-grained control over the result set size. By default the
     * total rows are set to the total result set for the query, allowing proper paging.  But some views (portlets)
     * may want to limit results to a small set (like most recent).
     * @param result
     * @param response
     * @param request
     *
     * @return should not exceed result.getTotalSize().
     */
    protected int getTotalRows(final PageList<Alert> result, final DSResponse response, final DSRequest request) {

        return result.getTotalSize();
    }

    @Override
    protected AlertCriteria getFetchCriteria(DSRequest request) {
        AlertPriority[] prioritiesFilter = getArrayFilter(request, FILTER_PRIORITIES, AlertPriority.class);
        if (prioritiesFilter == null || prioritiesFilter.length == 0) {
            return null; // user didn't select any priorities - return null to indicate no data should be displayed
        }

        AlertCriteria criteria = new AlertCriteria();
        // There's no need to add a priorities filter to the criteria if the user specified all priorities.
        if (prioritiesFilter.length != AlertPriority.values().length) {
            criteria.addFilterPriorities(prioritiesFilter);
        }

        Date startDateFilter = getFilter(request, DateFilterItem.START_DATE_FILTER, Date.class);
        if(startDateFilter != null){
            Date startOfDay =   DateFilterItem.adjustTimeToStartOfDay(startDateFilter);
            criteria.addFilterStartTime(startOfDay.getTime());
        }

        Date endDateFilter = getFilter(request, DateFilterItem.END_DATE_FILTER, Date.class);
        if( endDateFilter!= null){
            Date endOfDay = DateFilterItem.adjustTimeToEndOfDay(endDateFilter);
            criteria.addFilterEndTime(endOfDay.getTime());
        }
        criteria.addFilterResourceIds(getArrayFilter(request, FILTER_RESOURCE_IDS, Integer.class));
        criteria.addFilterEntityContext(entityContext);
        criteria.fetchConditionLogs(true);
//        criteria.fetchGroupAlertDefinition(true);

        return criteria;
    }


    @Override
    protected String getSortFieldForColumn(String columnName) {
        String sortField;
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            sortField = "alertDefinition.resource.ancestry";
        } else if ("status".equals(columnName)) {
            sortField = "acknowledgeTime";
        } else {
            sortField = super.getSortFieldForColumn(columnName);
        }
        return sortField;
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

        AlertDefinition alertDefinition = from.getAlertDefinition();

        record.setAttribute("definitionId", alertDefinition.getId());
        Resource resource = alertDefinition.getResource();
        record.setAttribute("name", alertDefinition.getName());
        record.setAttribute("description", alertDefinition.getDescription());
        record.setAttribute("priority", ImageManager.getAlertIcon(alertDefinition.getPriority()));

        // for ancestry handling
        record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

        AlertDefinition groupAlertDefinition = alertDefinition.getGroupAlertDefinition();
        Integer parentId = alertDefinition.getParentId();
        if (groupAlertDefinition != null && groupAlertDefinition.getGroup() != null) {
            boolean isAutogroup = groupAlertDefinition.getGroup().getAutoGroupParentResource() != null;
            record.setAttribute(FIELD_PARENT, (isAutogroup ? "#Resource/AutoGroup/" : "#ResourceGroup/")
                + groupAlertDefinition.getGroup().getId() + "/Alerts/Definitions/" + groupAlertDefinition.getId());
            record.setLinkText(MSG.view_alert_definition_for_group());
        } else if (parentId != null && parentId.intValue() != 0) {
            record.setAttribute(
                FIELD_PARENT,
                LinkManager.getAdminTemplatesEditLink(AlertDefinitionTemplateTypeView.VIEW_ID.getName(), resource
                    .getResourceType().getId())
                    + "/" + parentId);
            record.setLinkText(MSG.view_alert_definition_for_type());
        }

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
                try {
                    conditionValue = MeasurementConverterClient.format(Double.valueOf(conditionLog.getValue()),
                        condition.getMeasurementDefinition().getUnits(), true);
                } catch (Exception e) {
                    // the condition log value was probably not a number (most likely a trait). Ignore this exception.
                    // even if any other errors occur trying to format the value, ignore this and just use the raw value string
                }
            }
        } else {
            conditionText = MSG.view_alerts_field_condition_text_none();
            conditionValue = "--";
        }
        record.setAttribute("conditionText", conditionText);
        if (conditionValue.contains("extraInfo=")) {
            conditionValue = conditionValue.replaceFirst("extraInfo=\\[","");
            conditionValue = conditionValue.substring(0,conditionValue.length()-1);
        }
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
                try {
                    value = MeasurementConverterClient.format(Double.valueOf(log.getValue()), condition
                        .getMeasurementDefinition().getUnits(), true);
                } catch (Exception e) {
                    // the condition log value was probably not a number (most likely a trait). Ignore this exception.
                    // even if any other errors occur trying to format the value, ignore this and just use the raw value string
                }
            }
            // Remove the extraInfo=[ ] that is added when storing the raw event data in the data base
            if (value.contains("extraInfo=")) {
                value = value.replaceFirst("extraInfo=\\[","");
                value = value.substring(0,value.length()-1);
            }
            dc.setAttribute("value", value);
            conditions[i++] = dc;
        }
        record.setAttribute("conditionLogs", conditions);
        record.setAttribute("conditionExpression", alertDefinition.getConditionExpression());

        String recoveryInfo = AlertFormatUtility.getAlertRecoveryInfo(from);
        record.setAttribute("recoveryInfo", recoveryInfo);

        // Alert notification logs
        DataClass[] notifications = new DataClass[from.getAlertNotificationLogs().size()];
        i = 0;
        for (AlertNotificationLog log : from.getAlertNotificationLogs()) {
            DataClass dc = new DataClass();
            dc.setAttribute("sender", log.getSender());
            dc.setAttribute("status", log.getResultState().name());
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

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}