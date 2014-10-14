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
package org.rhq.coregui.client.dashboard.portlets.recent.events;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.common.EntityContext.Type;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.EventCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.measurement.util.Moment;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.inventory.common.event.EventCompositeDatasource;
import org.rhq.coregui.client.inventory.common.event.EventCompositeHistoryView;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A base class for deriving recent event portlets for different entity contexts.  In this way the
 * basic plumbing is shared, giving a consistent behavior and configuration for the concrete portlets.
 *
 * @author Jirka Kremser
 */
public abstract class AbstractRecentEventsPortlet extends EventCompositeHistoryView implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;

    private EventsPortletDataSource dataSource;

    // autorefresh timer
    private Timer refreshTimer;

    public AbstractRecentEventsPortlet(EntityContext entityContext) {
        super(null, entityContext, false);

        setShowFilterForm(false); //disable filter form for portlet
        setOverflow(Overflow.VISIBLE);
        setShowFooterRefresh(false); //disable footer refresh button as redundant for portlets
        setShowHeader(false);//disable header for portlets
        setMinHeight(400);
    }

    @Override
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                String url = getEventDetailLink(record);
                String formattedValue = TimestampCellFormatter.format(value);
                return LinkManager.getHref(url, formattedValue);
            }
        };
    }

    @Override
    public void showDetails(ListGridRecord record) {
        String url = getEventDetailLink(record);
        CoreGUI.goToView(url);
    }

    private String getEventDetailLink(ListGridRecord record) {
        Integer eventId = getId(record);
        Integer resourceId = record.getAttributeAsInt(AncestryUtil.RESOURCE_ID);
        return LinkManager.getEventDetailLink(resourceId, eventId);
    }

    @Override
    protected boolean canSupportDeleteAndPurgeAll() {
        return false;
    }

    public Timer getRefreshTimer() {
        return refreshTimer;
    }

    public PortletWindow getPortletWindow() {
        return portletWindow;
    }

    @Override
    public EventsPortletDataSource getDataSource() {
        if (this.dataSource == null) {
            this.dataSource = new EventsPortletDataSource(getContext());
        }
        return this.dataSource;
    }

    @Override
    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_events());
    }
    
    @Override
    protected void configureTable() {
        super.configureTable();
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields(false);
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
    }

    @Override
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        // the portletWindow does not change, so we can hold onto it locally
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        // if there is no configuration there is nothing to set
        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        Configuration config = storedPortlet.getConfiguration();

        // not sure I love the fact that this common portlet config assigns some irrelevant/unused config props,
        // may be better to prune the common set and add the specific properties locally in this method
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if (config.getSimple(key) == null) {
                config.put(new PropertySimple(key, PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION
                    .get(key)));
            }
        }

        getDataSource().setConfiguration(config);
    }

    @Override
    public DynamicForm getCustomSettingsForm() {

        DynamicForm customSettingsForm = new DynamicForm();
        EnhancedVLayout page = new EnhancedVLayout();
        DynamicForm filterForm = new DynamicForm();
        filterForm.setMargin(5);

        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        List<FormItem> items = new ArrayList<FormItem>(4);
        // resource name
        final TextItem eventResourceFilter = getContext().type == Type.SubsystemView ? PortletConfigurationEditorComponent
            .getEventResourceEditor(portletConfig) : null;
        if (eventResourceFilter != null) {
            items.add(eventResourceFilter);
        }
        
        // event source
        final TextItem eventSourceFilter = PortletConfigurationEditorComponent.getEventSourceEditor(portletConfig);
        items.add(eventSourceFilter);
        
        // event severity
        final SelectItem eventSeveritySelector = PortletConfigurationEditorComponent
            .getEventSeverityEditor(portletConfig);
        items.add(eventSeveritySelector);

        // result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);
        items.add(resultCountSelector);

        // range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);
        
        filterForm.setItems(items.toArray(new FormItem[items.size()]));

        //submit handler
        customSettingsForm.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                // resource name
                if (eventResourceFilter != null) {
                    String selectedValue = (null == eventResourceFilter.getValue()) ? "" : eventResourceFilter.getValue()
                        .toString();
                    portletConfig.put(new PropertySimple(Constant.EVENT_RESOURCE, selectedValue));
                }
                
                // event source
                String selectedValue = (null == eventSourceFilter.getValue()) ? "" : eventSourceFilter.getValue()
                    .toString();
                portletConfig.put(new PropertySimple(Constant.EVENT_SOURCE, selectedValue));
                
                // event severity
                selectedValue = (null == eventSeveritySelector.getValue()) ? "" : eventSeveritySelector
                    .getValue().toString();
                if ((selectedValue.trim().isEmpty())
                    || (selectedValue.split(",").length == EventSeverity.values().length)) {
                    // no severity filter
                    selectedValue = Constant.EVENT_SEVERITY_DEFAULT;
                }
                portletConfig.put(new PropertySimple(Constant.EVENT_SEVERITY, selectedValue));

                // result count
                selectedValue = resultCountSelector.getValue().toString();
                if (selectedValue.trim().isEmpty()) {
                    selectedValue = Constant.RESULT_COUNT_DEFAULT;
                }
                portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, selectedValue));

                // time range settings
                saveMeasurementRangeEditorSettings(measurementRangeEditor, portletConfig);

                // persist and reload portlet
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                //apply latest settings to the visible result set
                refresh();
            }
        });

        page.addMember(measurementRangeEditor);
        page.addMember(filterForm);
        customSettingsForm.addChild(page);

        return customSettingsForm;
    }

    /**
     * Takes the current value of the widget and persists it into the configuration object passed in.
     *
     * @param measurementRangeEditor metric range editor widget
     * @param portletConfig - the config to be updated
     */
    private void saveMeasurementRangeEditorSettings(final CustomConfigMeasurementRangeEditor measurementRangeEditor,
        Configuration portletConfig) {
        String selectedValue;
        if ((measurementRangeEditor != null) && (portletConfig != null)) {
            //time range filter. Check for enabled and then persist property. Dealing with compound widget.
            FormItem item = measurementRangeEditor.getItem(CustomConfigMeasurementRangeEditor.ENABLE_RANGE_ITEM);
            CheckboxItem itemC = (CheckboxItem) item;
            boolean persistTimeRangeSettings = itemC.getValueAsBoolean();
            if (persistTimeRangeSettings) {//retrieve values and persist
                selectedValue = String.valueOf(itemC.getValueAsBoolean());
                if (!selectedValue.trim().isEmpty()) {//then call
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_ENABLE, selectedValue));
                }

                //time advanced time filter enabled.
                boolean isAdvanceTimeSetting = false;
                selectedValue = String.valueOf(measurementRangeEditor.isAdvanced());
                if ((selectedValue != null) && (!selectedValue.trim().isEmpty())) {
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_BEGIN_END_FLAG, selectedValue));
                    isAdvanceTimeSetting = Boolean.valueOf(selectedValue);
                }

                //time frame
                List<Moment> begEnd = measurementRangeEditor.getBeginEndTimes();
                if (isAdvanceTimeSetting) {//advanced settings
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE, (begEnd.get(0) + "," + begEnd.get(1))));
                } else {
                    //save not advanced time range
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_LASTN, measurementRangeEditor
                        .getMetricRangePreferences().lastN));
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_UNIT, measurementRangeEditor
                        .getMetricRangePreferences().unit));
                }
            } else {//if disabled, reset time defaults
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_ENABLE, false));
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_BEGIN_END_FLAG, false));
                List<Moment> rangeArray = MeasurementUtility.calculateTimeFrame(
                    Integer.valueOf(Constant.METRIC_RANGE_LASTN_DEFAULT),
                    Integer.valueOf(Constant.METRIC_RANGE_UNIT_DEFAULT));
                //                String[] range = {String.valueOf(rangeArray.get(0)),String.valueOf(rangeArray.get(1))};
                portletConfig.put(new PropertySimple(Constant.METRIC_RANGE,
                    (String.valueOf(rangeArray.get(0)) + "," + rangeArray.get(1))));
            }
        }
    }

    @Override
    public void refreshTableInfo() {
        super.refreshTableInfo();
        if (getTableInfo() != null) {
            int count = getListGrid().getSelectedRecords().length;
            getTableInfo().setContents(
                MSG.view_table_matchingRows(String.valueOf(getListGrid().getTotalRows()), String.valueOf(count)));
        }
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    public boolean isRefreshing() {
        return false;
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            super.refresh();
        }
    }

    static public class EventsPortletDataSource extends EventCompositeDatasource {
        private Configuration configuration;

        public EventsPortletDataSource(EntityContext entityContext) {
            this(entityContext, null);
        }

        public EventsPortletDataSource(EntityContext entityContext, Configuration configuration) {
            super(entityContext);
            this.configuration = configuration;
        }

        public Configuration getConfiguration() {
            return configuration;
        }

        public void setConfiguration(Configuration configuration) {
            this.configuration = configuration;
        }

        /* (non-Javadoc)
         * This override allows us to set the total rows to the number of recent op history configured for
         * the portlet. This sets the counter appropriately and stops further queries to the server.
         *
         * @see org.rhq.coregui.client.operation.OperationHistoryDataSource#getTotalRows(org.rhq.core.domain.util.PageList, com.smartgwt.client.data.DSResponse, com.smartgwt.client.data.DSRequest)
         */
        @Override
        protected int getTotalRows(final PageList<EventComposite> result, final DSResponse response, final DSRequest request) {
            return result.size();
        }

        @Override
        protected EventCriteria getFetchCriteria(DSRequest request) {
            EventCriteria criteria = new EventCriteria();
            
            // event source filter
            String currentSetting = this.configuration.getSimpleValue(Constant.EVENT_SOURCE, "");
            criteria.addFilterSourceName(currentSetting);

            // result count
            currentSetting = this.configuration.getSimpleValue(Constant.RESULT_COUNT,
                Constant.RESULT_COUNT_DEFAULT);

            int pageNumber = 0;
            int pageSize = Integer.valueOf(currentSetting);
            OrderingField orderingField = new OrderingField("timestamp", PageOrdering.DESC);
            criteria.setPageControl(new PageControl(pageNumber, pageSize, orderingField));

            // filter severity
            currentSetting = this.configuration
                .getSimpleValue(Constant.EVENT_SEVERITY, Constant.EVENT_SEVERITY_DEFAULT);
            String[] parsedValues = currentSetting.trim().split(",");
            if (!(currentSetting.trim().isEmpty() || parsedValues.length == EventSeverity.values().length)) {
                EventSeverity[] filterSeverities = new EventSeverity[parsedValues.length];
                int index = 0;
                for (String severity : parsedValues) {
                    EventSeverity p = EventSeverity.valueOf(severity);
                    filterSeverities[index++] = p;
                }
                criteria.addFilterSeverities(filterSeverities);
            }

            //result timeframe if enabled
            PropertySimple property = configuration.getSimple(Constant.METRIC_RANGE_ENABLE);
            if (null != property && Boolean.valueOf(property.getBooleanValue())) { //then proceed setting

                boolean isAdvanced = Boolean.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_BEGIN_END_FLAG,
                    Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT));
                if (isAdvanced) {
                    //Advanced time settings
                    currentSetting = configuration.getSimpleValue(Constant.METRIC_RANGE, Constant.METRIC_RANGE_DEFAULT);
                    String[] range = currentSetting.split(",");
                    if (range.length == 2) {
                        criteria.addFilterStartTime(Long.valueOf(range[0]));
                        criteria.addFilterEndTime(Long.valueOf(range[1]));
                    }
                } else {
                    //Simple time settings
                    property = configuration.getSimple(Constant.METRIC_RANGE_LASTN);
                    if (property != null) {
                        Integer lastN = Integer.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                            Constant.METRIC_RANGE_LASTN_DEFAULT));
                        Integer units = Integer.valueOf(configuration.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                            Constant.METRIC_RANGE_UNIT_DEFAULT));
                        ArrayList<Moment> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                        criteria.addFilterStartTime(beginEnd.get(0).toDate().getTime());
                        criteria.addFilterEndTime(beginEnd.get(1).toDate().getTime());
                    }
                }
            }

            // add any context related filters
            switch (getEntityContext().type) {
            case Resource:
                criteria.addFilterResourceId(getEntityContext().getResourceId());
                break;
            case ResourceGroup:
                criteria.addFilterResourceGroupId(getEntityContext().getGroupId());
            case SubsystemView:
                // event resource name filter
                currentSetting = this.configuration.getSimpleValue(Constant.EVENT_RESOURCE, "");
                criteria.addFilterResourceName(currentSetting);
                break;
            default:
                // no default
                break;
            }
            criteria.fetchSource(true);

            return criteria;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();
        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }
}
