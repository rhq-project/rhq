/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.operation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.OperationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHTMLPane;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jay Shaughnessy
 * @author Ian Springer 
 */
public class OperationHistoryDataSource extends
    RPCDataSource<ResourceOperationHistory, ResourceOperationHistoryCriteria> {

    private EntityContext entityContext;

    private ResourceOperationHistory currentOperationHistory;

    public static abstract class Field {
        public static final String ID = "id";
        public static final String OPERATION_NAME = "operationName";
        public static final String STATUS = "status";
        public static final String STARTED_TIME = "startedTime";
        public static final String CREATED_TIME = "createdTime";
        public static final String DURATION = "duration";
        public static final String SUBJECT = "subject";
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

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        // for some reason, the client seems to crash if you don't specify any data source fields
        // even though we know we defined override ListGridFields for all columns.
        List<DataSourceField> fields = super.addDataSourceFields();
        fields.add(new DataSourceTextField(Field.OPERATION_NAME));
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

        ListGridField startTimeField = createStartedTimeField();
        fields.add(startTimeField);

        ListGridField opNameField = new ListGridField(Field.OPERATION_NAME, MSG
            .view_operationHistoryDetails_operation());
        fields.add(opNameField);

        ListGridField subjectField = new ListGridField(Field.SUBJECT, MSG.view_operationHistoryDetails_requestor());
        fields.add(subjectField);

        ListGridField statusField = createStatusField();
        fields.add(statusField);

        if (this.entityContext.type != EntityContext.Type.Resource) {
            ListGridField resourceNameField = new ListGridField(AncestryUtil.RESOURCE_NAME, MSG.common_title_resource());
            resourceNameField.setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    String url = LinkManager
                        .getResourceLink(listGridRecord.getAttributeAsInt(AncestryUtil.RESOURCE_ID));
                    return SeleniumUtility.getLocatableHref(url, o.toString(), null);
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
        ListGridField startedTimeField = new ListGridField(Field.STARTED_TIME, MSG
            .view_operationHistoryDetails_dateSubmitted());
        startedTimeField.setAlign(Alignment.LEFT);
        startedTimeField.setCellAlign(Alignment.LEFT);
        startedTimeField.setCellFormatter(new TimestampCellFormatter() {
            public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (value != null) {
                    String timestamp = super.format(value, record, rowNum, colNum);
                    Integer resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
                    Integer opHistoryId = record.getAttributeAsInt("id");
                    String url = LinkManager.getSubsystemResourceOperationHistoryLink(resourceId, opHistoryId);
                    return SeleniumUtility.getLocatableHref(url, timestamp, null);
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
                    final Window winModal = new LocatableWindow("StatusDetailsWin");
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
                        public void onCloseClick(CloseClientEvent event) {
                            winModal.markForDestroy();
                        }
                    });

                    LocatableHTMLPane htmlPane = new LocatableHTMLPane("StatusDetailsPane");
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
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    /**
     * Additional processing to support entity-specific or cross-resource views, and something that can be overidden.
     */
    protected void dataRetrieved(final PageList<ResourceOperationHistory> result, final DSResponse response,
        final DSRequest request) {
        switch (entityContext.type) {

        // no need to disambiguate the history for a single resource
        case Resource:
            response.setData(buildRecords(result));
            // for paging to work we have to specify size of full result set
            response.setTotalRows(result.getTotalSize());
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
                        record
                            .setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }
                    response.setData(records);
                    // for paging to work we have to specify size of full result set
                    response.setTotalRows(result.getTotalSize());
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

        switch (entityContext.type) {
        case Resource:
            criteria.addFilterResourceIds(entityContext.resourceId);
            break;

        case ResourceGroup:
            criteria.addFilterGroupOperationHistoryId(entityContext.groupId);
            break;
        }

        criteria.setPageControl(getPageControl(request));
        return criteria;
    }

    @Override
    protected void executeRemove(Record recordToRemove, DSRequest request, DSResponse response) {
        final OperationHistory operationHistoryToRemove = copyValues(recordToRemove);
        Boolean forceValue = request.getAttributeAsBoolean("force");
        boolean force = ((forceValue != null) && forceValue);
        operationService.deleteOperationHistory(operationHistoryToRemove.getId(), force, new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                return;
            }

            public void onFailure(Throwable caught) {
                throw new RuntimeException("Failed to delete " + operationHistoryToRemove + ".", caught);
            }
        });
    }

    @Override
    public ResourceOperationHistory copyValues(Record from) {
        return this.currentOperationHistory;
    }

    @Override
    public ListGridRecord copyValues(ResourceOperationHistory from) {
        this.currentOperationHistory = from;

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
