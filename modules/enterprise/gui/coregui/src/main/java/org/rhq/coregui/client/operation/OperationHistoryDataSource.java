/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.coregui.client.operation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.form.DateFilterItem;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.RPCDataSource;

/**
 * @author Jay Shaughnessy
 * @author Ian Springer
 */
public class OperationHistoryDataSource extends
    RPCDataSource<ResourceOperationHistory, ResourceOperationHistoryCriteria> {

    private EntityContext entityContext;

    public static abstract class Field {
        public static final String ID = "id";
        public static final String OPERATION_NAME = "operationName";
        public static final String STATUS = "status";
        public static final String STARTED_TIME = "startedTime";
        public static final String CREATED_TIME = "createdTime";
        public static final String DURATION = "duration";
        public static final String SUBJECT = "subjectName";
        public static final String OPERATION_DEFINITION = "operationDefinition";
        public static final String ERROR_MESSAGE = "errorMessage";
        public static final String PARAMETERS = "parameters";
    }

    protected OperationGWTServiceAsync operationService = GWTServiceLookup.getOperationService();

    public OperationHistoryDataSource() {
        this(EntityContext.forSubsystemView());
    }

    public OperationHistoryDataSource(EntityContext context) {
        super();
        this.entityContext = context;

        addDataSourceFields();
    }

    /**
     * The view that contains the list grid which will display this datasource's data will call this
     * method to get the field information which is used to control the display of the data.
     *
     * @return list grid fields used to display the datasource data
     */
    public ArrayList<ListGridField> getListGridFields() {
        ArrayList<ListGridField> fields = new ArrayList<ListGridField>(7);

        ListGridField startTimeField = createStartedTimeField();
        fields.add(startTimeField);

        ListGridField opNameField = new ListGridField(Field.OPERATION_NAME,
            MSG.view_operationHistoryDetails_operation());
        fields.add(opNameField);

        ListGridField subjectField = new ListGridField(Field.SUBJECT, MSG.view_operationHistoryDetails_requestor());
        fields.add(subjectField);

        ListGridField statusField = createStatusField();
        fields.add(statusField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager.getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return LinkManager.getHref(url, o.toString());
                }
            });
            resourceNameField.setShowHover(true);
            resourceNameField.setHoverCustomizer(new HoverCustomizer() {

                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(resourceNameField);

            ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
            fields.add(ancestryField);

            startTimeField.setWidth(200);
            opNameField.setWidth("25%");
            subjectField.setWidth("15%");
            statusField.setWidth(100);
            resourceNameField.setWidth("25%");
            ancestryField.setWidth("35%");
        } else {
            startTimeField.setWidth(200);
            opNameField.setWidth("*");
            subjectField.setWidth("*");
            statusField.setWidth(100);
        }

        return fields;
    }

    protected ListGridField createStartedTimeField() {
        ListGridField startedTimeField = new ListGridField(Field.STARTED_TIME,
            MSG.view_operationHistoryDetails_dateSubmitted());
        startedTimeField.setAlign(Alignment.LEFT);
        startedTimeField.setCellAlign(Alignment.LEFT);
        startedTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value != null) {
                    String timestamp = super.format(value, record, rowNum, colNum);
                    Integer opHistoryId = record.getAttributeAsInt("id");
                    String url = LinkManager.getEntityTabLink(entityContext, "Operations", "History") + "/"
                        + opHistoryId;
                    return LinkManager.getHref(url, timestamp);
                } else {
                    return "<i>" + MSG.view_operationHistoryList_notYetStarted() + "</i>";
                }
            }
        });
        startedTimeField.setShowHover(true);
        startedTimeField.setHoverCustomizer(TimestampCellFormatter.getHoverCustomizer(Field.STARTED_TIME));

        return startedTimeField;
    }

    protected ListGridField createStatusField() {
        ListGridField statusField = new ListGridField(Field.STATUS, MSG.view_operationHistoryDetails_status());
        statusField.setAlign(Alignment.CENTER);
        statusField.setCellAlign(Alignment.CENTER);
        statusField.setShowHover(true);
        statusField.setHoverCustomizer(new HoverCustomizer() {
            @Override
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                String statusStr = record.getAttribute(Field.STATUS);
                OperationRequestStatus status = OperationRequestStatus.valueOf(statusStr);
                switch (status) {
                case SUCCESS: {
                    return MSG.common_status_success();
                }
                case FAILURE: {
                    return MSG.common_status_failed();
                }
                case INPROGRESS: {
                    return MSG.common_status_inprogress();
                }
                case CANCELED: {
                    return MSG.common_status_canceled();
                }
                }
                // should never get here
                return MSG.common_status_unknown();
            }
        });
        statusField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                OperationRequestStatus status = OperationRequestStatus.valueOf((String) o);
                String icon = ImageManager.getOperationResultsIcon(status);
                return Canvas.imgHTML(icon, 16, 16);
            }
        });
        statusField.addRecordClickHandler(new RecordClickHandler() {
            @Override
            public void onRecordClick(RecordClickEvent event) {
                Record record = event.getRecord();
                String statusStr = record.getAttribute(Field.STATUS);
                OperationRequestStatus status = OperationRequestStatus.valueOf(statusStr);
                if (status == OperationRequestStatus.FAILURE) {
                    final Window winModal = new Window();
                    winModal.setTitle(MSG.common_title_details());
                    winModal.setOverflow(Overflow.VISIBLE);
                    winModal.setShowMinimizeButton(false);
                    winModal.setShowMaximizeButton(true);
                    winModal.setIsModal(true);
                    winModal.setShowModalMask(true);
                    winModal.setAutoSize(true);
                    winModal.setAutoCenter(true);
                    winModal.setShowResizer(true);
                    winModal.setCanDragResize(true);
                    winModal.centerInPage();
                    winModal.addCloseClickHandler(new CloseClickHandler() {
                        @Override
                        public void onCloseClick(CloseClickEvent event) {
                            winModal.markForDestroy();
                        }
                    });

                    HTMLPane htmlPane = new HTMLPane();
                    htmlPane.setMargin(10);
                    htmlPane.setDefaultWidth(500);
                    htmlPane.setDefaultHeight(400);
                    String errorMsg = record.getAttribute(Field.ERROR_MESSAGE);
                    if (errorMsg == null) {
                        errorMsg = MSG.common_status_failed();
                    }
                    htmlPane.setContents("<pre>" + errorMsg + "</pre>");
                    winModal.addItem(htmlPane);
                    winModal.show();
                }
            }
        });

        return statusField;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final ResourceOperationHistoryCriteria criteria) {

        if (criteria == null) {
            // the user selected no statuses in the filter - it makes sense from the UI perspective to show 0 rows
            response.setTotalRows(0);
            processResponse(request.getRequestId(), response);
            return;
        }

        final long start = System.currentTimeMillis();

        this.operationService.findResourceOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceOperationHistory>>() {

                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler()
                        .handleError(MSG.view_operationHistoryDetails_error_fetchFailure(), caught);
                    response.setStatus(RPCResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }

                public void onSuccess(PageList<ResourceOperationHistory> result) {
                    long fetchTime = System.currentTimeMillis() - start;
                    Log.info(result.size() + " operation histories fetched in: " + fetchTime + "ms");

                    dataRetrieved(result, response, request);
                }
            });
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
    protected int getTotalRows(final PageList<ResourceOperationHistory> result, final DSResponse response,
        final DSRequest request) {

        return result.getTotalSize();
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    private void dataRetrieved(final PageList<ResourceOperationHistory> result, final DSResponse response,
        final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate the history for a single resource
        case Resource:
            response.setData(buildRecords(result));
            setPagingInfo(response, result);
            processResponse(request.getRequestId(), response);
            break;

        // disambiguate as the results could be cross-resource
        default:
            HashSet<Integer> typesSet = new HashSet<Integer>();
            HashSet<String> ancestries = new HashSet<String>();
            for (ResourceOperationHistory history : result) {
                Resource resource = history.getResource();
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
                    setPagingInfo(response, result);
                    processResponse(request.getRequestId(), response);
                }
            });
        }
    }

    @Override
    protected ResourceOperationHistoryCriteria getFetchCriteria(DSRequest request) {
        OperationRequestStatus[] statusFilter = getArrayFilter(request, Field.STATUS, OperationRequestStatus.class);

        if (statusFilter == null || statusFilter.length == 0) {
            return null; // user didn't select any severities - return null to indicate no data should be displayed
        }

        ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

        criteria.addFilterStatuses(statusFilter);

        Date startDateFilter = getFilter(request, DateFilterItem.START_DATE_FILTER, Date.class);
        if (startDateFilter != null) {
            Date startOfDay = DateFilterItem.adjustTimeToStartOfDay(startDateFilter);
            criteria.addFilterStartTime(startOfDay.getTime());
        }

        Date endDateFilter = getFilter(request, DateFilterItem.END_DATE_FILTER, Date.class);
        if (endDateFilter != null) {
            Date endOfDay = DateFilterItem.adjustTimeToEndOfDay(endDateFilter);
            criteria.addFilterEndTime(endOfDay.getTime());
        }

        switch (entityContext.type) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.resourceId);
            break;

        case ResourceGroup:
            criteria.addFilterGroupOperationHistoryId(entityContext.groupId);
            break;
        }

        return criteria;
    }

    @Override
    protected String getSortFieldForColumn(String columnName) {
        if (AncestryUtil.RESOURCE_ANCESTRY.equals(columnName)) {
            return "resource.ancestry";
        }

        return super.getSortFieldForColumn(columnName);
    }

    @Override
    protected void executeRemove(Record recordToRemove, final DSRequest request, final DSResponse response) {
        final ResourceOperationHistory operationHistoryToRemove = copyValues(recordToRemove);
        Boolean forceValue = request.getAttributeAsBoolean("force");
        boolean force = ((forceValue != null) && forceValue);
        operationService.deleteOperationHistories(new int[] { operationHistoryToRemove.getId() }, force,
            new AsyncCallback<Void>() {
                public void onSuccess(Void result) {
                    sendSuccessResponse(request, response, operationHistoryToRemove, null);
                }

                public void onFailure(Throwable caught) {
                    throw new RuntimeException("Failed to delete " + operationHistoryToRemove + ".", caught);
                }
            });
    }

    @Override
    public ResourceOperationHistory copyValues(Record from) {
        Resource resource = new Resource();
        resource.setId(from.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
        ResourceOperationHistory resourceOperationHistory = new ResourceOperationHistory(null, null,
            from.getAttribute(Field.SUBJECT),
            (OperationDefinition) from.getAttributeAsObject(Field.OPERATION_DEFINITION),
            (Configuration) from.getAttributeAsObject(Field.PARAMETERS), resource, null);
        resourceOperationHistory.setId(from.getAttributeAsInt(Field.ID));
        return resourceOperationHistory;
    }

    @Override
    public ListGridRecord copyValues(ResourceOperationHistory from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute(Field.ID, from.getId());
        record.setAttribute(Field.CREATED_TIME, convertTimestampToDate(from.getCreatedTime()));
        record.setAttribute(Field.STARTED_TIME, convertTimestampToDate(from.getStartedTime()));
        record.setAttribute(Field.DURATION, from.getDuration());
        record.setAttribute(Field.SUBJECT, from.getSubjectName());
        record.setAttribute(Field.OPERATION_DEFINITION, from.getOperationDefinition());
        record.setAttribute(Field.OPERATION_NAME, from.getOperationDefinition().getDisplayName());
        record.setAttribute(Field.ERROR_MESSAGE, from.getErrorMessage());
        record.setAttribute(Field.STATUS, from.getStatus().name());
        record.setAttribute(Field.PARAMETERS, from.getParameters());

        // for ancestry handling
        Resource resource = from.getResource();
        record.setAttribute(AncestryUtil.RESOURCE_ID, resource.getId());
        record.setAttribute(AncestryUtil.RESOURCE_NAME, resource.getName());
        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
        record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

        return record;
    }

    protected EntityContext getEntityContext() {
        return entityContext;
    }

    protected void setEntityContext(EntityContext entityContext) {
        this.entityContext = entityContext;
    }

}
