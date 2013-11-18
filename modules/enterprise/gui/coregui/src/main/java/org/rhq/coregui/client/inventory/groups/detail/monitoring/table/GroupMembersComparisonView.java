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

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.EmbeddedPosition;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.ui.MetricDisplaySummary;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.inventory.groups.detail.monitoring.table.GroupMembersComparisonView.GroupMembersComparisonDataSource;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.RPCDataSource;
import org.rhq.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * @author Jay Shaughnessy
 */
public class GroupMembersComparisonView extends Table<GroupMembersComparisonDataSource> implements Refreshable { //, AutoRefresh {

    private static SortSpecifier DEFAULT_SORT_SPECIFIER = new SortSpecifier(
        GroupMembersComparisonDataSource.FIELD_NAME, SortDirection.ASCENDING);

    protected ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;
    //protected Timer refreshTimer;

    private final ResourceGroupComposite groupComposite;
    private final int[] resourceIds;
    private final GroupMembersComparisonDataSource dataSource;

    private Map<MeasurementDefinition, List<MetricDisplaySummary>> comparisonData;

    public GroupMembersComparisonView(ResourceGroupComposite groupComposite, int[] resourceIds) {
        super(null, new SortSpecifier[] { DEFAULT_SORT_SPECIFIER });

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
        if (isVisible()) { // && !isRefreshing()) {
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
        addTopWidget(buttonBarDateTimeRangeEditor);
        refreshDateTimeRangeEditor();

        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();

        //add cell click handler to execute on Table data entries.
        getListGrid().addCellClickHandler(new CellClickHandler() {
            @Override
            public void onCellClick(CellClickEvent event) {
                Record record = event.getRecord();
                Object source = event.getSource();

                String title = record.getAttribute(GroupMembersComparisonDataSource.FIELD_NAME);
                ChartViewWindow window = new ChartViewWindow("", title);
                int defId = record.getAttributeAsInt(FIELD_ID);

                ResourceGroup group = groupComposite.getResourceGroup();
                boolean isAutogroup = group.getAutoGroupParentResource() != null;
                CompositeGroupD3GraphListView graph = new CompositeGroupD3MultiLineGraph(group.getId(), defId,
                    isAutogroup);
                window.addItem(graph);
                graph.populateData();
                window.show();
            }
        });

        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
    }

    /**
     * The list grid with metric per row and embedded component for the selected resources
     */
    private class GroupMembersComparisonListGrid extends ListGrid {

        public GroupMembersComparisonListGrid() {
            super();

            setShowRecordComponents(true);
            setShowRecordComponentsByCell(false);
            setRecordComponentPosition(EmbeddedPosition.EXPAND);
            setShowAllRecords(true);
        }

        @Override
        protected Canvas createRecordComponent(ListGridRecord record, Integer colNum) {
            MeasurementDefinition measurementDefinition = (MeasurementDefinition) record
                .getAttributeAsObject(GroupMembersComparisonDataSource.FIELD_OBJECT);

            return new GroupMembersComparisonMetricView(comparisonData.get(measurementDefinition));
        }
    }

    public class GroupMembersComparisonDataSource extends RPCDataSource<MeasurementDefinition, Criteria> {

        public static final String FIELD_OBJECT = "object";
        public static final String FIELD_NAME = "name";
        private final MeasurementUserPreferences measurementUserPrefs;

        public GroupMembersComparisonDataSource() {
            measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        }

        /**
         * The view that contains the list grid which will display this datasource's data will call this
         * method to get the field information which is used to control the display of the data.
         *
         * @return list grid fields used to display the datasource data
         */
        public ArrayList<ListGridField> getListGridFields() {
            ArrayList<ListGridField> fields = new ArrayList<ListGridField>(6);

            ListGridField nameField = new ListGridField(FIELD_NAME, MSG.common_title_name());
            nameField.setType(ListGridFieldType.LINK);
            nameField.setTarget("javascript");
            nameField.setWidth("*");
            fields.add(nameField);

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

                        long begin = measurementUserPrefs.getMetricRangePreferences().begin;
                        long end = measurementUserPrefs.getMetricRangePreferences().end;
                        if (null != comparisonData) {
                            comparisonData.clear();
                            comparisonData = null;
                        }
                        GWTServiceLookup.getMeasurementChartsService().getMetricDisplaySummariesForMetricsCompare(
                            resourceIds, definitionIds, begin, end,
                            new AsyncCallback<Map<MeasurementDefinition, List<MetricDisplaySummary>>>() {

                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError("Cannot load comparison data", caught);
                                }

                                public void onSuccess(Map<MeasurementDefinition, List<MetricDisplaySummary>> result) {
                                    comparisonData = result;
                                    response.setData(buildRecords(result.keySet()));
                                    processResponse(request.getRequestId(), response);
                                }
                            });
                    }
                });
        }

        @Override
        protected Criteria getFetchCriteria(DSRequest request) {
            return null;
        }

        @Override
        public ListGridRecord copyValues(MeasurementDefinition from) {

            ListGridRecord record = new ListGridRecord();
            record.setAttribute("object", from);
            record.setAttribute(FIELD_ID, from.getId());
            record.setAttribute(FIELD_NAME, from.getDisplayName());

            return record;
        }

        @Override
        public MeasurementDefinition copyValues(Record from) {
            return null;
        }
    }

}
