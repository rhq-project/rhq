/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.admin.storage;

import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ALERTS;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_AVAILABILITY;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_DISK;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ERROR_MESSAGE;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_FAILED_OPERATION;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ID;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MEMORY;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;
import static org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_STATUS;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite.MeasurementAggregateWithUnits;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.admin.storage.StorageNodeDatasourceField.StorageNodeLoadCompositeDatasourceField;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementConverterClient;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * Datasource for @see StorageNodeDatasource + heap and disk usage.
 *
 * @author Jirka Kremser
 */
public class StorageNodeDatasource extends RPCDataSource<StorageNodeLoadComposite, StorageNodeCriteria> {
    public static final String OK_CLASS = "storageNodeOk";
    public static final String WARN_CLASS = "storageNodeWarn";
    public static final String DONT_MISS_ME_CLASS = "storageNodeProblem";

    // filters
    public static final String FILTER_ADDRESS = FIELD_ADDRESS.propertyName();
    public static final String FILTER_OPERATION_MODE = FIELD_OPERATION_MODE.propertyName();

    public static final int AGGREGATE_FOR_LAST_N_HOURS = 8;
    private static StorageNodeDatasource instance;

    private StorageNodeDatasource() {
        super();
        setID("storageNode");
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    public static StorageNodeDatasource instance() {
        if (instance == null) {
            instance = new StorageNodeDatasource();
        }
        return instance;
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();
        DataSourceField idField = new DataSourceIntegerField(FIELD_ID.propertyName(), FIELD_ID.title(), 50);
        idField.setPrimaryKey(true);
        idField.setHidden(true);
        fields.add(idField);
        return fields;
    }

    public List<ListGridField> getListGridFields() {
        List<ListGridField> fields = new ArrayList<ListGridField>();

        ListGridField idField = FIELD_ID.getListGridField();
        idField.setHidden(true);
        fields.add(idField);

        fields.add(FIELD_ADDRESS.getListGridField("*"));
        fields.add(FIELD_ALERTS.getListGridField("170"));

        ListGridField field = FIELD_MEMORY.getListGridField("120");
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                return MSG.view_adminTopology_storageNodes_memoryHover(String.valueOf(AGGREGATE_FOR_LAST_N_HOURS));
            }
        });
        fields.add(field);

        field = FIELD_DISK.getListGridField("120");
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                return MSG.view_adminTopology_storageNodes_diskHover(value.toString(),
                    String.valueOf(AGGREGATE_FOR_LAST_N_HOURS));
            }
        });
        fields.add(field);

        field = FIELD_STATUS.getListGridField("90");
        field.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                if (listGridRecord.getAttribute(FIELD_ERROR_MESSAGE.propertyName()) != null
                    || listGridRecord.getAttribute(FIELD_FAILED_OPERATION.propertyName()) != null) {
                    return "<span class='" + DONT_MISS_ME_CLASS + "'>" + value.toString() + "</span>";
                } else
                    return value.toString();
            }
        });

        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (record.getAttribute(FIELD_ERROR_MESSAGE.propertyName()) != null
                    || record.getAttribute(FIELD_FAILED_OPERATION.propertyName()) != null) {
                    return value.toString() + ": " + MSG.view_adminTopology_storageNodes_statusHoverError();
                } else
                    return value.toString();
            }
        });
        fields.add(field);

        ListGridField createdTimeField = FIELD_CTIME.getListGridField("120");
        TimestampCellFormatter.prepareDateField(createdTimeField);
        fields.add(createdTimeField);

        ListGridField resourceIdField = FIELD_RESOURCE_ID.getListGridField("120");
        //        resourceIdField.setHidden(true);
        fields.add(resourceIdField);

        field = FIELD_AVAILABILITY.getListGridField("65");
        field.setAlign(Alignment.CENTER);
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                Object availability = record.getAttribute(FIELD_AVAILABILITY.propertyName());
                return "Storage Node is " + availability == null ? AvailabilityType.UNKNOWN.toString() : availability
                    .toString();
            }
        });
        fields.add(field);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, StorageNodeCriteria criteria) {
        GWTServiceLookup.getStorageService().getStorageNodeComposites(
            new AsyncCallback<PageList<StorageNodeLoadComposite>>() {
                public void onSuccess(PageList<StorageNodeLoadComposite> result) {
                    response.setData(buildRecords(result));
                    response.setTotalRows(result.size());
                    processResponse(request.getRequestId(), response);
                }

                public void onFailure(Throwable t) {
                    CoreGUI.getErrorHandler().handleError("Unable to fetch storage nodes.", t);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    /**
     * Returns a prepopulated PageControl based on the provided DSRequest. This will set sort fields,
     * pagination, but *not* filter fields.
     *
     * @param request the request to turn into a page control
     * @return the page control for passing to criteria and other queries
     */
    @Override
    protected PageControl getPageControl(DSRequest request) {
        // Initialize paging.
        PageControl pageControl = new PageControl(0, getDataPageSize());

        // Initialize sorting.
        String sortBy = request.getAttribute("sortBy");
        if (sortBy != null) {
            String[] sorts = sortBy.split(",");
            for (String sort : sorts) {
                PageOrdering ordering = (sort.startsWith("-")) ? PageOrdering.DESC : PageOrdering.ASC;
                String columnName = (ordering == PageOrdering.DESC) ? sort.substring(1) : sort;
                pageControl.addDefaultOrderingField(columnName, ordering);
            }
        }

        return pageControl;
    }

    @Override
    public StorageNodeLoadComposite copyValues(Record from) {
        throw new UnsupportedOperationException("StorageNodeDatasource.copyValues(Record from)");
    }

    @Override
    public ListGridRecord copyValues(StorageNodeLoadComposite from) {
        ListGridRecord record = new ListGridRecord();
        StorageNode node = from.getStorageNode();
        if (node != null) {
            record.setAttribute(FIELD_ID.propertyName(), node.getId());
            record.setAttribute(FIELD_ADDRESS.propertyName(), from.getHostname());
            record.setAttribute(FIELD_CQL_PORT.propertyName(), node.getCqlPort());
            record.setAttribute(FIELD_OPERATION_MODE.propertyName(), node.getOperationMode());
            record.setAttribute(FIELD_STATUS.propertyName(), node.getStatus());
            record.setAttribute(FIELD_CTIME.propertyName(), node.getCtime());
            record.setAttribute(FIELD_ERROR_MESSAGE.propertyName(), node.getErrorMessage());
            if (node.getFailedOperation() != null && node.getFailedOperation().getResource() != null) {
                ResourceOperationHistory operationHistory = node.getFailedOperation();
                String value = LinkManager.getSubsystemResourceOperationHistoryLink(operationHistory.getResource()
                    .getId(), operationHistory.getId());
                record.setAttribute(FIELD_FAILED_OPERATION.propertyName(), value);
            }
            if (node.getResource() != null) {
                record.setAttribute(FIELD_RESOURCE_ID.propertyName(), node.getResource().getId());
                record.setAttribute(FIELD_AVAILABILITY.propertyName(), node.getResource().getCurrentAvailability()
                    .getAvailabilityType());
            }
        }
        int value = from.getUnackAlerts();
        record.setAttribute(
            FIELD_ALERTS.propertyName(),
            node.getResource() != null ? StorageNodeAdminView.getAlertsString(
                MSG.view_adminTopology_storageNodes_unackAlerts(), node.getId(), value) : MSG
                .view_adminTopology_storageNodes_unackAlerts() + " (0)");
        String memory = null;
        if (from.getHeapPercentageUsed() != null && from.getHeapPercentageUsed().getAggregate().getAvg() != null)
            memory = MeasurementConverterClient.format(from.getHeapPercentageUsed().getAggregate().getAvg(), from
                .getHeapPercentageUsed().getUnits(), true);
        record.setAttribute(FIELD_MEMORY.propertyName(), memory);
        if (from.getFreeDiskToDataSizeRatio() != null) {
            if (from.getFreeDiskToDataSizeRatio().getMax() < 0.7) {
                record.setAttribute(
                    FIELD_DISK.propertyName(),
                    "<span class='" + DONT_MISS_ME_CLASS + "'>"
                        + MSG.view_adminTopology_storageNodes_diskInsufficient() + "</span>");
            } else if (from.getFreeDiskToDataSizeRatio().getMax() < 1.5) {
                record
                    .setAttribute(FIELD_DISK.propertyName(),
                        "<span class='" + WARN_CLASS + "'>" + MSG.view_adminTopology_storageNodes_diskWarning()
                            + "</span>");
            } else {
                record.setAttribute(FIELD_DISK.propertyName(),
                    "<span class='" + OK_CLASS + "'>" + MSG.view_adminTopology_storageNodes_diskSufficient()
                        + "</span>");
            }
        }
        return record;
    }

    @Override
    protected StorageNodeCriteria getFetchCriteria(DSRequest request) {
        OperationMode[] modesFilter = getArrayFilter(request, FILTER_OPERATION_MODE, OperationMode.class);
        if (modesFilter == null || modesFilter.length == 0) {
            return null; // user didn't select any modes - return null to indicate no data should be displayed
        }
        StorageNodeCriteria criteria = new StorageNodeCriteria();
        criteria.addFilterId(getFilter(request, FIELD_ID.propertyName(), Integer.class));
        criteria.addFilterAddress(getFilter(request, FILTER_ADDRESS, String.class));
        criteria.addFilterOperationMode(modesFilter);

        //@todo: Remove me when finished debugging search expression
        Log.debug(" *** StorageNodeCriteria Search String: " + getFilter(request, "search", String.class));
        criteria.setSearchExpression(getFilter(request, "search", String.class));

        return criteria;
    }

    public static class StorageNodeLoadCompositeDatasource extends
        RPCDataSource<StorageNodeLoadComposite, StorageNodeCriteria> {
        public static final String KEY_HEAP_USED = "{HeapMemoryUsage.used}";
        public static final String KEY_HEAP_PERCENTAGE = "Calculated.HeapUsagePercentage";
        public static final String KEY_DATA_DISK_SPACE_PERCENTAGE = "Calculated.DataDiskUsedPercentage";
        public static final String KEY_TOTAL_DISK_SPACE_PERCENTAGE = "Calculated.TotalDiskUsedPercentage";
        public static final String KEY_FREE_DISK_TO_DATA_SIZE_RATIO = "Calculated.FreeDiskToDataSizeRatio";
        public static final String KEY_TOTAL_DISK = "Load"; // todo: calculation for sparkline graphs
        public static final String KEY_OWNERSHIP = "Ownership";
        public static final String KEY_TOKENS = "Tokens";

        private int storageNodeId;

        public static StorageNodeLoadCompositeDatasource getInstance(int storageNodeId) {
            return new StorageNodeLoadCompositeDatasource(storageNodeId);
        }

        public StorageNodeLoadCompositeDatasource(int storageNodeId) {
            super("storageNodeLoad" + storageNodeId + "-" + Random.nextDouble());
            this.storageNodeId = storageNodeId;
            List<DataSourceField> fields = addDataSourceFields();
            super.addFields(fields);
        }

        @Override
        protected List<DataSourceField> addDataSourceFields() {
            List<DataSourceField> fields = super.addDataSourceFields();
            DataSourceField idField = new DataSourceIntegerField(FIELD_ID.propertyName(), FIELD_ID.title(), 50);
            idField.setPrimaryKey(true);
            idField.setHidden(true);
            DataSourceTextField parentField = new DataSourceTextField(
                StorageNodeLoadCompositeDatasourceField.FIELD_PARENT_ID.propertyName(), null);
            parentField.setHidden(true);
            parentField.setRequired(true);
            parentField.setRootValue("root");
            parentField.setForeignKey("storageNode." + FIELD_ID);

            fields.add(idField);
            return fields;
        }

        public List<ListGridField> getListGridFields() {
            List<ListGridField> fields = new ArrayList<ListGridField>();
            ListGridField idField = FIELD_ID.getListGridField();
            idField.setHidden(true);
            fields.add(idField);
            ListGridField nameField = StorageNodeLoadCompositeDatasourceField.FIELD_NAME.getListGridField("*");
            nameField.setWidth("40%");
            nameField.setShowHover(true);
            nameField.setHoverCustomizer(new HoverCustomizer() {
                @Override
                public String hoverHTML(Object o, ListGridRecord listGridRecord, int i, int i2) {
                    return listGridRecord.getAttribute("hover");
                }
            });
            fields.add(nameField);
            fields.add(StorageNodeLoadCompositeDatasourceField.FIELD_MIN.getListGridField("130"));
            fields.add(StorageNodeLoadCompositeDatasourceField.FIELD_AVG.getListGridField("130"));
            fields.add(StorageNodeLoadCompositeDatasourceField.FIELD_MAX.getListGridField("130"));
            ListGridField hoverField = new ListGridField("hover", "hover");
            hoverField.setHidden(true);
            fields.add(hoverField);
            return fields;
        }

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response, StorageNodeCriteria criteria) {
            final StorageNode node = new StorageNode();
            node.setId(storageNodeId);
            // set dummy address because StorageNode.equals method ignores id field
            node.setAddress(String.valueOf(storageNodeId));
            Log.debug("Executing fetch for storage node [id=" + storageNodeId + "]");
            executeFetch(node, new AsyncCallback<StorageNodeLoadComposite>() {
                public void onSuccess(final StorageNodeLoadComposite loadComposite) {
                    Log.debug("Data for storage node [id=" + storageNodeId + "] arrived: " + loadComposite);
                    ListGridRecord[] records = makeListGridRecords(loadComposite);
                    response.setData(records);
                    response.setTotalRows(records.length);
                    StorageNodeLoadCompositeDatasource.this.processResponse(request.getRequestId(), response);
                }

                @Override
                public void onFailure(Throwable caught) {
                    Log.warn("Failed to execute fetch for storage node [id=" + storageNodeId + "]");
                    CoreGUI.getErrorHandler().handleError(MSG.view_adminTopology_storageNodes_fetchFail(), caught);
                    response.setStatus(DSResponse.STATUS_FAILURE);
                    StorageNodeLoadCompositeDatasource.this.processResponse(request.getRequestId(), response);
                }
            });
        }

        private void executeFetch(final StorageNode node, final AsyncCallback<StorageNodeLoadComposite> callback) {
            GWTServiceLookup.getStorageService().getLoad(node, AGGREGATE_FOR_LAST_N_HOURS, MeasurementUtils.UNIT_HOURS,
                callback);
        }

        private ListGridRecord[] makeListGridRecords(StorageNodeLoadComposite loadComposite) {
            List<ListGridRecord> recordsList = new ArrayList<ListGridRecord>(6) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean add(ListGridRecord record) {
                    if (record != null)
                        return super.add(record);
                    return false;
                }
            };

            // heap related metrics
            recordsList.add(makeListGridRecord(loadComposite.getHeapUsed(),
                MSG.view_adminTopology_storageNodes_load_heapUsedName(),
                MSG.view_adminTopology_storageNodes_load_heapUsedHover(), KEY_HEAP_USED));
            recordsList.add(makeListGridRecord(loadComposite.getHeapPercentageUsed(),
                MSG.view_adminTopology_storageNodes_load_heapPercentUsedName(),
                MSG.view_adminTopology_storageNodes_load_heapPercentUsedHover(), KEY_HEAP_PERCENTAGE));

            // disk related metrics
            recordsList.add(makeListGridRecord(loadComposite.getDataDiskUsed(),
                MSG.view_adminTopology_storageNodes_load_dataDiskUsedName(),
                MSG.view_adminTopology_storageNodes_load_dataDiskUsedHover(), KEY_TOTAL_DISK));
            recordsList.add(makeListGridRecord(loadComposite.getTotalDiskUsedPercentage(),
                MSG.view_adminTopology_storageNodes_load_totalDiskUsedPercentageName(),
                MSG.view_adminTopology_storageNodes_load_totalDiskUsedPercentageHover(),
                KEY_TOTAL_DISK_SPACE_PERCENTAGE));
            recordsList
                .add(makeListGridRecord(loadComposite.getDataDiskUsedPercentage(),
                    MSG.view_adminTopology_storageNodes_load_dataDiskUsedPercentageName(),
                    MSG.view_adminTopology_storageNodes_load_dataDiskUsedPercentageHover(),
                    KEY_DATA_DISK_SPACE_PERCENTAGE));

            if (loadComposite.getFreeDiskToDataSizeRatio() != null) {
                MeasurementAggregate aggregate = loadComposite.getFreeDiskToDataSizeRatio();
                NumberFormat nf = NumberFormat.getFormat("0.0");
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("id", KEY_FREE_DISK_TO_DATA_SIZE_RATIO);
                record.setAttribute("name", MSG.view_adminTopology_storageNodes_load_freeDiskToDataSizeRatioName());
                record.setAttribute("hover", MSG.view_adminTopology_storageNodes_load_freeDiskToDataSizeRatioHover());
                record.setAttribute("min", nf.format(aggregate.getMin()));
                record.setAttribute("avg", nf.format(aggregate.getAvg()));
                record.setAttribute("avgFloat", aggregate.getAvg());
                record.setAttribute("max", nf.format(aggregate.getMax()));
                recordsList.add(record);
            }

            // other metrics
            recordsList.add(makeListGridRecord(loadComposite.getActuallyOwns(),
                MSG.view_adminTopology_storageNodes_load_actuallyOwnsName(),
                MSG.view_adminTopology_storageNodes_load_actuallyOwnsHover(), KEY_OWNERSHIP));
            if (loadComposite.getTokens() != null) {
                ListGridRecord tokens = new ListGridRecord();
                tokens.setAttribute("id", KEY_TOKENS);
                tokens.setAttribute("name", MSG.view_adminTopology_storageNodes_load_tokensName());
                tokens.setAttribute("hover", MSG.view_adminTopology_storageNodes_load_tokensHover());
                tokens.setAttribute("min", loadComposite.getTokens().getMin());
                tokens.setAttribute("avg", loadComposite.getTokens().getAvg());
                tokens.setAttribute("max", loadComposite.getTokens().getMax());
                recordsList.add(tokens);
            }

            ListGridRecord[] records = recordsList.toArray(new ListGridRecord[recordsList.size()]);
            return records;
        }

        private ListGridRecord makeListGridRecord(MeasurementAggregateWithUnits aggregateWithUnits, String name,
            String hover, String id) {
            if (aggregateWithUnits == null)
                return null;
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("id", id);
            record.setAttribute(StorageNodeLoadCompositeDatasourceField.FIELD_NAME.propertyName(), name);
            record.setAttribute(
                StorageNodeLoadCompositeDatasourceField.FIELD_MIN.propertyName(),
                MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getMin(),
                    aggregateWithUnits.getUnits(), true));
            record.setAttribute("avgFloat", aggregateWithUnits.getAggregate().getAvg());
            record.setAttribute(
                StorageNodeLoadCompositeDatasourceField.FIELD_AVG.propertyName(),
                MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getAvg(),
                    aggregateWithUnits.getUnits(), true));
            record.setAttribute(
                StorageNodeLoadCompositeDatasourceField.FIELD_MAX.propertyName(),
                MeasurementConverterClient.format(aggregateWithUnits.getAggregate().getMax(),
                    aggregateWithUnits.getUnits(), true));
            record.setAttribute("hover", hover);
            return record;
        }

        @Override
        protected StorageNodeCriteria getFetchCriteria(DSRequest request) {
            return new StorageNodeCriteria();
        }

        @Override
        public StorageNodeLoadComposite copyValues(Record from) {
            throw new UnsupportedOperationException(
                "StorageNodeDatasource.StorageNodeLoadCompositeDatasource.copyValues(Record from)");
        }

        @Override
        public ListGridRecord copyValues(StorageNodeLoadComposite from) {
            throw new UnsupportedOperationException(
                "StorageNodeDatasource.StorageNodeLoadCompositeDatasource.copyValues(StorageNodeLoadComposite from)");
        }
    }
}
