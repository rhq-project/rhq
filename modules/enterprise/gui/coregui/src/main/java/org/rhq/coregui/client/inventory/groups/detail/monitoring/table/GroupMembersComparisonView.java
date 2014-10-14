/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.CATEGORY;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.INVENTORY_STATUS;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.NAME;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE;
import static org.rhq.coregui.client.inventory.resource.ResourceDataSourceField.TYPE_ID;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.GroupStartOpen;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.GroupValueFunction;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.measurement.ui.MetricDisplayValue;
import org.rhq.core.domain.measurement.util.Instant;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.table.IconField;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.table.GroupMembersComparisonView.GroupMembersComparisonDataSource;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository.TypesLoadedCallback;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * @author Jay Shaughnessy
 */
public class GroupMembersComparisonView extends Table<GroupMembersComparisonDataSource> implements Refreshable { //, AutoRefresh {

    private static SortSpecifier DEFAULT_SORT_SPECIFIER_1 = new SortSpecifier(
        GroupMembersComparisonDataSource.FIELD_METRIC_NAME, SortDirection.ASCENDING);
    private static SortSpecifier DEFAULT_SORT_SPECIFIER_2 = new SortSpecifier(NAME.propertyName(),
        SortDirection.ASCENDING);

    protected ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;

    private final ResourceGroupComposite groupComposite;
    private final int[] resourceIds;
    private final GroupMembersComparisonDataSource dataSource;

    public GroupMembersComparisonView(ResourceGroupComposite groupComposite, int[] resourceIds) {
        super(null, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER_1, DEFAULT_SORT_SPECIFIER_2 });

        this.groupComposite = groupComposite;
        this.resourceIds = resourceIds;

        dataSource = new GroupMembersComparisonDataSource();
        setDataSource(dataSource);
        //disable full-screen fields, just use auto refresh
        setShowFooterRefresh(false);
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(this);
    }

    @Override
    public void refreshData() {
        if (isVisible()) {
            refreshDateTimeRangeEditor();
            refresh();
        }
    }

    private void refreshDateTimeRangeEditor() {
        Date now = new Date();
        long timeRange = CustomDateRangeState.getInstance().getTimeRange();
        Date newStartDate = new Date(now.getTime() - timeRange);
        buttonBarDateTimeRangeEditor.showUserFriendlyTimeRange(newStartDate.getTime(), now.getTime());
    }

    @Override
    public GroupMembersComparisonDataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    protected void configureTableFilters() {
        // currently no table filters
    }

    @Override
    protected ListGrid createListGrid() {
        return new GroupMembersComparisonListGrid();
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));

        addTopWidget(buttonBarDateTimeRangeEditor);
    }

    /**
     * The list grid with metric per row and embedded component for the selected resources
     */
    private class GroupMembersComparisonListGrid extends ListGrid {

        public GroupMembersComparisonListGrid() {
            super();

            setShowAllRecords(true);
            setGroupByField(GroupMembersComparisonDataSource.FIELD_METRIC_NAME);
            setGroupStartOpen(GroupStartOpen.ALL);
            setCanCollapseGroup(false);

            // this grouped view can't display a useful total rows value, so show nothing
            setShowFooter(false);
        }
    }

    public class GroupMembersComparisonDataSource extends RPCDataSource<MetricDisplaySummary, Criteria> {
        public static final String FIELD_MIN_VALUE = "min";
        public static final String FIELD_MAX_VALUE = "max";
        public static final String FIELD_AVG_VALUE = "avg";
        public static final String FIELD_LAST_VALUE = "last";

        public static final String FIELD_METRIC_NAME = "metricName";
        public static final String FIELD_ICON = "icon";

        public static final String ATTR_RESOURCE_ID = "resourceId";
        public static final String ATTR_DEFINITION_ID = "definitionId";

        private final MeasurementUserPreferences measurementUserPrefs;

        public GroupMembersComparisonDataSource() {
            measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        }

        public ArrayList<ListGridField> getListGridFields() {
            ArrayList<ListGridField> fields = new ArrayList<ListGridField>();

            ListGridField metricNameField = new ListGridField(FIELD_METRIC_NAME, MSG.common_title_name());
            metricNameField.setHidden(true);

            metricNameField.setGroupValueFunction(new GroupValueFunction() {
                public Object getGroupValue(Object value, ListGridRecord record, ListGridField field, String fieldName,
                    ListGrid grid) {
                    // just create a group for each metric display name
                    return value;
                }
            });

            fields.add(metricNameField);

            IconField iconField = new IconField(FIELD_ICON);
            iconField.setWidth(25);

            // click an icon, win a chart
            iconField.addRecordClickHandler(new RecordClickHandler() {
                @Override
                public void onRecordClick(RecordClickEvent event) {
                    Record record = event.getRecord();

                    String title = record.getAttribute(NAME.propertyName());
                    ChartViewWindow window = new ChartViewWindow("", title);
                    int defId = record.getAttributeAsInt(GroupMembersComparisonDataSource.ATTR_DEFINITION_ID);

                    ResourceGroup group = groupComposite.getResourceGroup();
                    EntityContext context = EntityContext.forGroup(group);
                    CompositeGroupD3GraphListView graph = new CompositeGroupD3MultiLineGraph(context, defId);
                    window.addItem(graph);
                    graph.populateData();
                    window.show();
                }
            });

            fields.add(iconField);

            ListGridField nameField = new ListGridField(NAME.propertyName(), NAME.title(), 250);
            nameField.setCellFormatter(new CellFormatter() {
                public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                    String invStatus = record.getAttribute(INVENTORY_STATUS.propertyName());
                    if (InventoryStatus.COMMITTED == InventoryStatus.valueOf(invStatus)) {
                        String url = LinkManager.getResourceLink(record.getAttributeAsInt(ATTR_RESOURCE_ID));
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
        protected void executeFetch(final DSRequest request, final DSResponse response, final Criteria unused) {
            final ResourceGroup group = groupComposite.getResourceGroup();
            // Load the fully fetched ResourceType.
            ResourceType groupType = group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(groupType.getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        group.setResourceType(type);
                        //metric definitions
                        Set<MeasurementDefinition> definitions = type.getMetricDefinitions();
                        ArrayList<MeasurementDefinition> filteredDefinitions = new ArrayList<MeasurementDefinition>();

                        // TODO: make this a filter to allow summary or detail
                        boolean summaryOnly = false;

                        for (MeasurementDefinition d : definitions) {
                            if (DataType.MEASUREMENT == d.getDataType()
                                && (!summaryOnly || (d.getDisplayType() != DisplayType.SUMMARY))) {
                                filteredDefinitions.add(d);
                            }
                        }

                        int[] definitionIds = new int[filteredDefinitions.size()];
                        int i = 0;
                        for (MeasurementDefinition d : filteredDefinitions) {
                            definitionIds[i++] = d.getId();
                        }

                        Instant begin = measurementUserPrefs.getMetricRangePreferences().begin;
                        Instant end = measurementUserPrefs.getMetricRangePreferences().end;
                        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForMetricsCompare(
                            resourceIds, definitionIds, begin, end,
                            new AsyncCallback<Map<MeasurementDefinition, List<MetricDisplaySummary>>>() {

                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Cannot load comparison data", caught);
                                }

                                public void onSuccess(Map<MeasurementDefinition, List<MetricDisplaySummary>> result) {
                                    List<MetricDisplaySummary> all = new ArrayList<MetricDisplaySummary>();
                                    for (MeasurementDefinition key : result.keySet()) {
                                        all.addAll(result.get(key));
                                    }
                                    dataRetrieved(all, response, request);
                                }
                            });
                    }
                });
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
        protected Criteria getFetchCriteria(DSRequest request) {
            return null;
        }

        @Override
        public ListGridRecord copyValues(MetricDisplaySummary from) {

            ListGridRecord record = new ListGridRecord();
            record.setAttribute(FIELD_ID, from.getMetricName() + "_" + from.getResourceId());
            record.setAttribute(FIELD_METRIC_NAME, from.getLabel());

            Resource resource = from.getResource();

            record.setAttribute(NAME.propertyName(), resource.getName());
            record.setAttribute(INVENTORY_STATUS.propertyName(), resource.getInventoryStatus());
            record.setAttribute(CATEGORY.propertyName(), resource.getResourceType().getCategory().name());
            record.setAttribute(AncestryUtil.RESOURCE_ANCESTRY, resource.getAncestry());
            record.setAttribute(AncestryUtil.RESOURCE_TYPE_ID, resource.getResourceType().getId());

            MeasurementUtility.formatSimpleMetrics(from);

            record.setAttribute(FIELD_MIN_VALUE, getMetricStringValue(from.getMinMetric()));
            record.setAttribute(FIELD_MAX_VALUE, getMetricStringValue(from.getMaxMetric()));
            record.setAttribute(FIELD_AVG_VALUE, getMetricStringValue(from.getAvgMetric()));
            record.setAttribute(FIELD_LAST_VALUE, getMetricStringValue(from.getLastMetric()));

            record.setAttribute(FIELD_ICON, ImageManager.getMonitorIcon());

            record.setAttribute(ATTR_RESOURCE_ID, from.getResourceId());
            record.setAttribute(ATTR_DEFINITION_ID, from.getDefinitionId());

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
