/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.operations.OperationsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * Responsible for defining and populating the Smart GWT datasource details and
 * translating the deserialized content into specific record entries for display
 * 
 * @author Simeon Pinder
 * @author Jay Shaughnessy
 */
public class RecentOperationsDataSource extends RPCDataSource<ResourceOperationLastCompletedComposite> {

    public enum Field {

        ANCESTRY("ancestry", MSG.common_title_ancestry()),

        OPERATION("operationName", MSG.dataSource_operationSchedule_field_operationName()),

        RESOURCE("resource", MSG.common_title_resource()),

        STATUS("operationStatus", MSG.common_title_status()),

        TIME("operationStartTime", MSG.common_title_timestamp()),

        TYPE("typeId", MSG.common_title_type());

        /**
         * Corresponds to a property name of Resource (e.g. resourceType.name).
         */
        private String propertyName;

        /**
         * The table header for the field or property (e.g. Type).
         */
        private String title;

        private Field(String propertyName, String title) {
            this.propertyName = propertyName;
            this.title = title;
        }

        public String propertyName() {
            return propertyName;
        }

        public String title() {
            return title;
        }

        public ListGridField getListGridField() {
            return new ListGridField(propertyName, title);
        }

        public ListGridField getListGridField(int width) {
            return new ListGridField(propertyName, title, width);
        }

    }

    private Portlet portlet;

    //config attributes
    private boolean operationsRangeLastEnabled = false;
    private int operationsRangeCompleted = -1;

    /** Build list of fields for the datasource and then adds them to it.
     */
    public RecentOperationsDataSource(Portlet portlet) {
        this.portlet = portlet;

        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField resourceField = new DataSourceTextField(Field.RESOURCE.propertyName, Field.RESOURCE.title);
        fields.add(resourceField);

        DataSourceTextField ancestryField = new DataSourceTextField(Field.ANCESTRY.propertyName(), Field.ANCESTRY
            .title());
        fields.add(ancestryField);

        DataSourceTextField operationField = new DataSourceTextField(Field.OPERATION.propertyName(), Field.OPERATION
            .title());
        fields.add(operationField);

        DataSourceTextField timeField = new DataSourceTextField(Field.TIME.propertyName(), Field.TIME.title());
        fields.add(timeField);

        DataSourceTextField statusField = new DataSourceTextField(Field.STATUS.propertyName, Field.STATUS.title(), 20);
        fields.add(statusField);

        return fields;
    }

    /** Fetch the ProblemResource data, and populate the response object appropriately.
     * 
     * @param request incoming request
     * @param response outgoing response
     */
    public void executeFetch(final DSRequest request, final DSResponse response) {
        PageControl pageControl = new PageControl();
        //retrieve current portlet display settings
        if ((this.portlet != null) && (this.portlet instanceof OperationsPortlet)) {
            OperationsPortlet operationsPortlet = (OperationsPortlet) this.portlet;
            //populate criteria with portlet preferences defined.
            if (operationsPortlet != null) {
                if (isOperationsRangeCompletedEnabled()) {
                    pageControl.setPageSize(getOperationsRangeCompleted());
                    operationsPortlet.getCompletedOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE_DEFAULT);
                } else {//show the component, return no results and indicate that you've disabled this display
                    pageControl.setPageSize(0);
                    operationsPortlet.getCompletedOperationsGrid().setEmptyMessage(
                        OperationsPortlet.RANGE_DISABLED_MESSAGE);
                    response.setData(null);
                    response.setTotalRows(0);
                    //pass off for processing
                    processResponse(request.getRequestId(), response);
                    return;
                }
            }
        }

        int resourceId = -1;

        GWTServiceLookup.getOperationService().findRecentCompletedOperations(resourceId, pageControl,
            new AsyncCallback<PageList<ResourceOperationLastCompletedComposite>>() {

                public void onFailure(Throwable throwable) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_recentOperations_error_fetchFailure(),
                        throwable);
                }

                public void onSuccess(PageList<ResourceOperationLastCompletedComposite> result) {
                    dataRetrieved(result, response, request);
                }
            });
    }

    protected void dataRetrieved(final PageList<ResourceOperationLastCompletedComposite> result,
        final DSResponse response, final DSRequest request) {
        HashSet<Integer> typesSet = new HashSet<Integer>();
        HashSet<String> ancestries = new HashSet<String>();
        for (ResourceOperationLastCompletedComposite composite : result) {
            typesSet.add(composite.getResourceTypeId());
            ancestries.add(composite.getAncestry());
        }

        // In addition to the types of the result resources, get the types of their ancestry
        // NOTE: this may be too labor intensive in general, but since this datasource is a singleton I couldn't
        //       make it easily optional.
        typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

        ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
        typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
            @Override
            public void onTypesLoaded(Map<Integer, ResourceType> types) {

                Record[] records = buildRecords(result);
                for (Record record : records) {
                    // enhance resource name
                    int resourceId = record.getAttributeAsInt("id");
                    int resourceTypeId = record.getAttributeAsInt(Field.TYPE.propertyName);
                    String resourceName = record.getAttributeAsString(Field.RESOURCE.propertyName);
                    ResourceType type = types.get(resourceTypeId);
                    record.setAttribute(Field.RESOURCE.propertyName, AncestryUtil.getResourceLongName(resourceId,
                        resourceName, type));

                    // decode ancestry
                    String ancestry = record.getAttributeAsString(Field.ANCESTRY.propertyName());
                    if (null == ancestry) {
                        continue;
                    }
                    String[] decodedAncestry = AncestryUtil.decodeAncestry(resourceId, ancestry, types);
                    // Preserve the encoded ancestry for special-case formatting at higher levels. Set the
                    // decoded strings as different attributes. 
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_RESOURCES, decodedAncestry[0]);
                    record.setAttribute(ResourceDatasource.ATTR_ANCESTRY_TYPES, decodedAncestry[1]);
                }
                response.setData(records);
                response.setTotalRows(result.getTotalSize()); // for paging to work we have to specify size of full result set
                processResponse(request.getRequestId(), response);
            }
        });
    }

    @Override
    public ListGridRecord copyValues(ResourceOperationLastCompletedComposite from) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("id", from.getResourceId());
        record.setAttribute(Field.ANCESTRY.propertyName, from.getAncestry());
        record.setAttribute(Field.OPERATION.propertyName, from.getOperationName());
        record.setAttribute(Field.RESOURCE.propertyName, from.getResourceName());
        record.setAttribute(Field.STATUS.propertyName, getStatusIconLink(from));
        record.setAttribute(Field.TIME.propertyName, new Date(from.getOperationStartTime()));
        record.setAttribute(Field.TYPE.propertyName, from.getResourceTypeId());

        record.setAttribute("entity", from);
        return record;
    }

    @Override
    public ResourceOperationLastCompletedComposite copyValues(Record from) {
        throw new UnsupportedOperationException("ResourceOperationLastCompletedComposite data is read only");
    }

    private String getStatusIconLink(ResourceOperationLastCompletedComposite composite) {

        OperationRequestStatus opStatus = composite.getOperationStatus();
        String statusStr = "";
        switch (opStatus) {
        case SUCCESS: {
            statusStr = MSG.common_status_success();
            break;
        }
        case FAILURE: {
            statusStr = MSG.common_status_failed();
            break;
        }
        case INPROGRESS: {
            statusStr = MSG.common_status_inprogress();
            break;
        }
        case CANCELED: {
            statusStr = MSG.common_status_canceled();
            break;
        }
        }

        String iconLink = ImageManager.getOperationResultsIcon(opStatus);
        StringBuilder sbImg = new StringBuilder();
        sbImg.append("<img alt='");
        sbImg.append(statusStr);
        sbImg.append("' title='");
        sbImg.append(statusStr);
        sbImg.append("' src='");
        sbImg.append(ImageManager.getFullImagePath(iconLink));
        sbImg.append("'></img>");

        String url = LinkManager.getSubsystemResourceOperationHistoryLink(composite.getResourceId(), composite
            .getOperationHistoryId());

        return "<a href='" + url + "'>" + sbImg.toString() + "</a>";
    }

    public boolean isOperationsRangeCompletedEnabled() {
        return operationsRangeLastEnabled;
    }

    public void setOperationsRangeCompleteEnabled(boolean operationsRangeLastEnabled) {
        this.operationsRangeLastEnabled = operationsRangeLastEnabled;
    }

    public int getOperationsRangeCompleted() {
        return operationsRangeCompleted;
    }

    public void setOperationsRangeCompleted(int operationsRangeCompleted) {
        this.operationsRangeCompleted = operationsRangeCompleted;
    }

}
