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
package org.rhq.enterprise.gui.coregui.client.admin.storage;

import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ADDRESS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ALERTS;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CQL_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_CTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_DISK;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ERROR_MESSAGE;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_FAILED_OPERATION;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_ID;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_JMX_PORT;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MEMORY;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_MTIME;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_OPERATION_MODE;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_RESOURCE_ID;
import static org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.FIELD_STATUS;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNode.OperationMode;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite;
import org.rhq.core.domain.cloud.StorageNodeLoadComposite.MeasurementAggregateWithUnits;
import org.rhq.core.domain.criteria.StorageNodeCriteria;
import org.rhq.core.domain.measurement.MeasurementAggregate;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.admin.storage.StorageNodeDatasourceField.StorageNodeLoadCompositeDatasourceField;
import org.rhq.enterprise.gui.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;

/**
 * Datasource for @see StorageNodeDatasource + heap and disk usage.
 *
 * @author Jirka Kremser
 */
public class StorageNodeDatasource extends RPCDataSource<StorageNodeLoadComposite, StorageNodeCriteria> {
    public static final String OK_COLOR = "color: #26aa26;";
    public static final String WARN_COLOR = "color: #ed9b26;";
    public static final String DONT_MISS_ME_COLOR = "font-weight: bold; color: #d64949;";
    
    // filters
    public static final String FILTER_ADDRESS = FIELD_ADDRESS.propertyName();
    public static final String FILTER_OPERATION_MODE = FIELD_OPERATION_MODE.propertyName();
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
        fields.add(FIELD_ALERTS.getListGridField("120"));

        ListGridField field = FIELD_MEMORY.getListGridField("120");
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                return "Average memory taken for last 8 hours.";
            }
        });
        fields.add(field);

        field = FIELD_DISK.getListGridField("120");
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                return "Average disk Ratio of (Free Disk)/(Data File Size) for last 8 hours. A value below 1 is not "
                    + "recommended since a compaction or repair process could double the amount of disk "
                    + "space used by data files. If multiple data locations are specified then the "
                    + "aggregate accross all the partitions that contain data files is reported.";
            }
        });
        fields.add(field);
        
//        fields.add(FIELD_JMX_PORT.getListGridField("90"));
//        ListGridField cqlField = FIELD_CQL_PORT.getListGridField("90");
//        cqlField.setHidden(true);
//        fields.add(cqlField);
        
        field = FIELD_STATUS.getListGridField("90");
        field.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                if (listGridRecord.getAttribute(FIELD_ERROR_MESSAGE.propertyName()) != null
                    || listGridRecord.getAttribute(FIELD_FAILED_OPERATION.propertyName()) != null) {
                    return "<span style='" + DONT_MISS_ME_COLOR + "'>" + value.toString() + "</span>";
                } else
                    return value.toString();
            }
        });
        
        field.setShowHover(true);
        field.setHoverCustomizer(new HoverCustomizer() {
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                if (record.getAttribute(FIELD_ERROR_MESSAGE.propertyName()) != null
                    || record.getAttribute(FIELD_FAILED_OPERATION.propertyName()) != null) {
                    return value.toString() + ": Something went wrong. Please double click on the storage node to show the detail page to know more.";
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

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response, StorageNodeCriteria criteria) {
        GWTServiceLookup.getStorageService().getStorageNodeComposites(new AsyncCallback<PageList<StorageNodeLoadComposite>>() {
            public void onSuccess(PageList<StorageNodeLoadComposite> result) {                
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }

            public void onFailure(Throwable t) {
                CoreGUI.getErrorHandler().handleError("td(i18n) Unable to fetch storage nodes.", t);
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
            record.setAttribute(FIELD_ADDRESS.propertyName(), node.getAddress());
            record.setAttribute(FIELD_JMX_PORT.propertyName(), node.getJmxPort());
            record.setAttribute(FIELD_CQL_PORT.propertyName(), node.getCqlPort());
            record.setAttribute(FIELD_OPERATION_MODE.propertyName(), node.getOperationMode());
            record.setAttribute(FIELD_STATUS.propertyName(), node.getStatus());
            record.setAttribute(FIELD_ERROR_MESSAGE.propertyName(), node.getErrorMessage());
            if (node.getFailedOperation() != null && node.getFailedOperation().getResource() != null) {
                ResourceOperationHistory operationHistory = node.getFailedOperation();
                String value = LinkManager.getSubsystemResourceOperationHistoryLink(operationHistory.getResource().getId(), operationHistory.getId());
//                String value = "#Resource/" + operationHistory.getResource().getId() + "/Operations/History/" + operationHistory.getId());
                record.setAttribute(FIELD_FAILED_OPERATION.propertyName(), value);
            }
            record.setAttribute(FIELD_CTIME.propertyName(), node.getCtime());
            record.setAttribute(FIELD_MTIME.propertyName(), node.getMtime());
            if (node.getResource() != null) {
                record.setAttribute(FIELD_RESOURCE_ID.propertyName(), node.getResource().getId());
            }
        }
        int value = from.getUnackAlerts();
        record.setAttribute(FIELD_ALERTS.propertyName(),
            node.getResource() != null ? StorageNodeAdminView.getAlertsString("New Alerts", node.getId(), value)
                : "New Alerts (0)");
        String memory = null;
        if (from.getHeapPercentageUsed() != null && from.getHeapPercentageUsed().getAggregate().getAvg() != null)
            memory = MeasurementConverterClient.format(from.getHeapPercentageUsed().getAggregate().getAvg(), from
                .getHeapPercentageUsed().getUnits(), true);
        record.setAttribute(FIELD_MEMORY.propertyName(), memory);
        if (from.getFreeDiskToDataSizeRatio() != null) {
            if (from.getFreeDiskToDataSizeRatio().getMax() < 0.7) {
                record.setAttribute(FIELD_DISK.propertyName(),
                    "<span style='" + DONT_MISS_ME_COLOR + "'>Insufficient</span>");
            } else if (from.getFreeDiskToDataSizeRatio().getMax() < 1.5) {
                record.setAttribute(FIELD_DISK.propertyName(), "<span style='" + WARN_COLOR + "'>Warning</span>");
            } else {
                record.setAttribute(FIELD_DISK.propertyName(),
                    "<span style='" + OK_COLOR + "'>Sufficient</span>");
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

    public static class StorageNodeLoadCompositeDatasource extends RPCDataSource<StorageNodeLoadComposite, StorageNodeCriteria> {
        public static final String HEAP_PERCENTAGE_KEY = "heapPercentage";
        public static final String DATA_DISK_SPACE_PERCENTAGE_KEY = "dataDiskSpacePercentage";
        public static final String TOTAL_DISK_SPACE_PERCENTAGE_KEY = "totalDiskSpacePercentage";
        public static final String FREE_DISK_TO_DATA_SIZE_RATIO_KEY = "freeDiskToDataSizeRatio";
        
        private int id;

        public static StorageNodeLoadCompositeDatasource getInstance(int id) {
            return new StorageNodeLoadCompositeDatasource(id);
        }

        public StorageNodeLoadCompositeDatasource(int id) {
            super();
            this.id = id;
            setID("storageNodeLoad");
            List<DataSourceField> fields = addDataSourceFields();
            addFields(fields);
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
            node.setId(id);
            executeFetch(node, new AsyncCallback<StorageNodeLoadComposite>() {
                    public void onSuccess(final StorageNodeLoadComposite loadComposite) {
                        ListGridRecord[] records = makeListGridRecords(loadComposite);
                        response.setData(records);
                        response.setTotalRows(records.length);
                        StorageNodeLoadCompositeDatasource.this.processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("td(i18n) Unable to fetch storage node load details.", caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        StorageNodeLoadCompositeDatasource.this.processResponse(request.getRequestId(), response);
                    }
                });
        }

        private static void executeFetch(final StorageNode node, final AsyncCallback<StorageNodeLoadComposite> callback) {
            GWTServiceLookup.getStorageService().getLoad(node, 8, MeasurementUtils.UNIT_HOURS, callback);
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
//            recordsList.add(makeListGridRecord(loadComposite.getHeapCommitted(), "Heap Maximum", "The limit the RHQ storage node was started with. This corresponds with the -Xmx JVM option.", "heapMax"));
            recordsList.add(makeListGridRecord(loadComposite.getHeapUsed(), "Heap Used", "Amount of memory actually used by the RHQ storage node", "heapUsed"));
            recordsList.add(makeListGridRecord(loadComposite.getHeapPercentageUsed(), "Heap Percent Used", "This value is calculated by dividing Heap Used by Heap Maximum.", HEAP_PERCENTAGE_KEY));
  
            // disk related metrics
            recordsList.add(makeListGridRecord(loadComposite.getDataDiskUsed(), "Disk Space Used by Storage Node", "Total space used on disk by all data files, commit logs, and saved caches.", "totaldisk"));
            recordsList.add(makeListGridRecord(loadComposite.getTotalDiskUsedPercentage(),"Total Disk Space Percent Used", "Percentage of total disk space used (system and Storage Node) on the partitions that contain the data files. If multiple data locations are specified then the aggregate accross all the partitions that contain data files is reported.", TOTAL_DISK_SPACE_PERCENTAGE_KEY));
            recordsList.add(makeListGridRecord(loadComposite.getDataDiskUsedPercentage(), "Data Disk Space Percent Used","Percentage of disk space used by data files on the partitions that contain the data files. If multiple data locations are specified then the aggregate accross all the partitions that contain data files is reported.", DATA_DISK_SPACE_PERCENTAGE_KEY));
            
            if (loadComposite.getFreeDiskToDataSizeRatio() != null){
                MeasurementAggregate aggregate = loadComposite.getFreeDiskToDataSizeRatio();
                NumberFormat nf = NumberFormat.getFormat("0.0");
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("id", FREE_DISK_TO_DATA_SIZE_RATIO_KEY);
                record.setAttribute("name", "Free Disk To Data Size Ratio");
                record.setAttribute("hover", "Ratio of (Free Disk)/(Data File Size). A value below 1 is not recommended since a compaction or repair process could double the amount of disk space used by data files. If multiple data locations are specified then the aggregate accross all the partitions that contain data files is reported.");
                record.setAttribute("min", nf.format(aggregate.getMin()));
                record.setAttribute("avg", nf.format(aggregate.getAvg()));
                record.setAttribute("avgFloat", aggregate.getAvg());
                record.setAttribute("max", nf.format(aggregate.getMax()));
                recordsList.add(record);
            }
//            recordsList.add(makeListGridRecord(loadComposite.getLoad(), "Load", "Data stored on the node", "load"));

            // other metrics
            recordsList.add(makeListGridRecord(loadComposite.getActuallyOwns(), "Ownership", "Refers to the percentage of keys that a node owns.", "ownership"));
            if (loadComposite.getTokens() != null) {
                ListGridRecord tokens = new ListGridRecord();
                tokens.setAttribute("id", "tokens");
                tokens.setAttribute("name", "Number of Tokens");
                tokens.setAttribute("hover", "Number of partitions of the ring that a node owns.");
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
            if (aggregateWithUnits == null) return null;
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
//            throw new UnsupportedOperationException("StorageNodeDatasource.StorageNodeLoadCompositeDatasource.getFetchCriteria()");
        }

        @Override
        public StorageNodeLoadComposite copyValues(Record from) {
            throw new UnsupportedOperationException("StorageNodeDatasource.StorageNodeLoadCompositeDatasource.copyValues(Record from)");
        }

        @Override
        public ListGridRecord copyValues(StorageNodeLoadComposite from) {
            throw new UnsupportedOperationException("StorageNodeDatasource.StorageNodeLoadCompositeDatasource.copyValues(StorageNodeLoadComposite from)");
        }

    }
}
