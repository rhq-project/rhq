package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.INVENTORY_STATUS;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.Layout;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.table.GroupMembersComparisonMetricView.GroupMembersComparisonMetricDataSource;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.StringUtility;

public class GroupMembersComparisonMetricView extends Table<GroupMembersComparisonMetricDataSource> {

    List<MetricDisplaySummary> summaries;
    private GroupMembersComparisonMetricDataSource dataSource;

    public GroupMembersComparisonMetricView(List<MetricDisplaySummary> summaries) {
        super(null, true);

        this.summaries = summaries;

        setAutoHeight();
        setOverflow(Overflow.VISIBLE);
        setShowFilterForm(false);
        setShowHeader(false);
        setShowFooter(false);

        setDataSource(getDataSource());
    }

    @Override
    public GroupMembersComparisonMetricDataSource getDataSource() {
        if (null == dataSource) {
            dataSource = new GroupMembersComparisonMetricDataSource();
        }

        return dataSource;
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        super.configureTable();
    }

    @Override
    protected void configureTableContents(Layout contents) {
        contents.setWidth100();
        contents.setHeight100();
        contents.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void configureListGrid(ListGrid grid) {
        grid.setDefaultHeight(1);
        grid.setAutoFitData(Autofit.VERTICAL);
    }

    public class GroupMembersComparisonMetricDataSource extends RPCDataSource<MetricDisplaySummary, Criteria> {

        public static final String FIELD_MIN_VALUE = "min";
        public static final String FIELD_MAX_VALUE = "max";
        public static final String FIELD_AVG_VALUE = "avg";
        public static final String FIELD_LAST_VALUE = "last";

        public ArrayList<ListGridField> getListGridFields() {
            ArrayList<ListGridField> fields = new ArrayList<ListGridField>();

            IconField iconField = new IconField();
            iconField.setShowHover(true);
            iconField.setHoverCustomizer(new HoverCustomizer() {
                public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String resCat = record.getAttribute(CATEGORY.propertyName());
                    switch (ResourceCategory.valueOf(resCat)) {
                    case PLATFORM:
                        return MSG.common_title_platform();
                    case SERVER:
                        return MSG.common_title_server();
                    case SERVICE:
                        return MSG.common_title_service();
                    }
                    return null;
                }
            });
            fields.add(iconField);

            ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title(), 250);
            nameField.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String invStatus = record.getAttribute(INVENTORY_STATUS.propertyName());
                    if (InventoryStatus.COMMITTED == InventoryStatus.valueOf(invStatus)) {
                        String url = LinkManager.getResourceLink(record.getAttributeAsInt("id"));
                        String name = StringUtility.escapeHtml(value.toString());
                        return LinkManager.getHref(url, name);
                    } else {
                        return value.toString();
                    }
                }
            });
            nameField.setShowHover(true);
            nameField.setHoverCustomizer(new HoverCustomizer() {
                public String hoverHTML(Object value, ListGridRecord listGridRecord, int rowNum, int colNum) {
                    return AncestryUtil.getResourceHoverHTML(listGridRecord, 0);
                }
            });
            fields.add(nameField);

            ListGridField ancestryField = AncestryUtil.setupAncestryListGridField();
            fields.add(ancestryField);

            ListGridField minField = new ListGridField(FIELD_MIN_VALUE, MSG.common_title_monitor_minimum());
            minField.setWidth("15%");
            fields.add(minField);

            ListGridField maxField = new ListGridField(FIELD_MAX_VALUE, MSG.common_title_monitor_maximum());
            maxField.setWidth("15%");
            fields.add(maxField);

            ListGridField avgField = new ListGridField(FIELD_AVG_VALUE, MSG.common_title_monitor_average());
            avgField.setWidth("15%");
            fields.add(avgField);

            ListGridField lastField = new ListGridField(FIELD_LAST_VALUE, MSG.view_resource_monitor_table_last());
            lastField.setWidth("15%");
            fields.add(lastField);

            return fields;
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            return null;
        }

        @Override
        protected void executeFetch(DSRequest request, DSResponse response, Criteria criteria) {
            dataRetrieved(summaries, response, request);
        }

        protected void dataRetrieved(final List<MetricDisplaySummary> result, final DSResponse response,
            final DSRequest request) {
            HashSet<Integer> typesSet = new HashSet<Integer>();
            HashSet<String> ancestries = new HashSet<String>();
            for (MetricDisplaySummary mds : result) {
                Resource resource = mds.getResource();
                ResourceType type = resource.getResourceType();
                if (type != null) {
                    typesSet.add(type.getId());
                }
                ancestries.add(resource.getAncestry());
            }

            // In addition to the types of the result resources, get the types of their ancestry
            typesSet.addAll(AncestryUtil.getAncestryTypeIds(ancestries));

            ResourceTypeRepository typeRepo = ResourceTypeRepository.Cache.getInstance();
            typeRepo.getResourceTypes(typesSet.toArray(new Integer[typesSet.size()]), new TypesLoadedCallback() {
                @Override
                public void onTypesLoaded(Map<Integer, ResourceType> types) {
                    // SmartGWT has issues storing a Map as a ListGridRecord attribute. Wrap it in a pojo.
                    AncestryUtil.MapWrapper typesWrapper = new AncestryUtil.MapWrapper(types);

                    Record[] records = buildRecords(result);
                    for (Record record : records) {
                        // replace type id with type name
                        Integer typeId = record.getAttributeAsInt(TYPE.propertyName());
                        ResourceType type = types.get(typeId);
                        if (type != null) {
                            record.setAttribute(TYPE.propertyName(), type.getName());
                            record.setAttribute(TYPE_ID.propertyName(), type.getId());
                        }

                        // To avoid a lot of unnecessary String construction, be lazy about building ancestry hover text.
                        // Store the types map off the records so we can build a detailed hover string as needed.
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_TYPES, typesWrapper);

                        // Build the decoded ancestry Strings now for display
                        record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY_VALUE, AncestryUtil.getAncestryValue(record));
                    }
                    response.setData(records);
                    processResponse(request.getRequestId(), response);
                }
            });
        }

        @Override
        public ListGridRecord copyValues(MetricDisplaySummary from) {
            ListGridRecord record = new ListGridRecord();
            Resource resource = from.getResource();

            record.setAttribute("id", resource.getId());
            record.setAttribute(NAME.propertyName(), resource.getName());
            record.setAttribute(INVENTORY_STATUS.propertyName(), resource.getInventoryStatus());
            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

            MeasurementUtility.formatSimpleMetrics(from);

            record.setAttribute(FIELD_MIN_VALUE, getMetricStringValue(from.getMinMetric()));
            record.setAttribute(FIELD_MAX_VALUE, getMetricStringValue(from.getMaxMetric()));
            record.setAttribute(FIELD_AVG_VALUE, getMetricStringValue(from.getAvgMetric()));
            record.setAttribute(FIELD_LAST_VALUE, getMetricStringValue(from.getLastMetric()));

            return record;
        }

        protected String getMetricStringValue(MetricDisplayValue value) {
            return (value != null) ? value.toString() : "";
        }

        @Override
        public MetricDisplaySummary copyValues(Record from) {
            return null;
        }

    }
}
