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
package org.rhq.coregui.client.dashboard.portlets.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.util.Instant;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.PortletWindow;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationDataSource;
import org.rhq.coregui.client.inventory.groups.detail.configuration.HistoryGroupResourceConfigurationTable;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Simeon Pinder
 */
public class GroupConfigurationUpdatesPortlet extends EnhancedVLayout implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupConfigurationUpdates";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_config_updates();

    // context provides whether this is a standard group, autocluster or autogroup
    private EntityContext context;

    protected Canvas recentConfigurationContent = new Canvas();

    public static final String ID = "id";

    //defines the list of configuration elements to load/persist for this portlet
    protected static List<String> CONFIG_INCLUDE = new ArrayList<String>();
    static {
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_BEGIN_END_FLAG);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_ENABLE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_LASTN);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_UNIT);
        CONFIG_INCLUDE.add(Constant.RESULT_COUNT);
        CONFIG_INCLUDE.add(Constant.CONFIG_UPDATE_STATUS);
        //        CONFIG_INCLUDE.add(Constant.RESULT_SORT_ORDER);
    }

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;

    //instance ui widgets
    protected Canvas containerCanvas;

    protected Timer refreshTimer;
    private ResourceGroupComposite groupComposite;
    protected boolean portletConfigInitialized = false;
    protected boolean currentlyLoading = false;
    private GroupConfigurationHistoryCriteriaTable groupHistoryTable;

    protected static HashMap<String, String> updatedMapping = new HashMap<String, String>();
    static {
        updatedMapping.putAll(PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION);
    }

    public GroupConfigurationUpdatesPortlet(EntityContext context) {
        super();
        this.context = context;
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setHeight("*");
        setWidth100();

        //tell canvas to fill it's component
        recentConfigurationContent.setHeight100();
        addMember(recentConfigurationContent);
        markForRedraw();
    }

    /** Responsible for initialization and lazy configuration of the portlet values
     */
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        //populate portlet configuration details
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        Configuration portletConfig = storedPortlet.getConfiguration();

        //lazy init any elements not yet configured.
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if ((portletConfig.getSimple(key) == null) && CONFIG_INCLUDE.contains(key)) {
                portletConfig.put(new PropertySimple(key,
                    PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_config_updates());
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();
        DynamicForm customSettings = new DynamicForm();
        EnhancedVLayout page = new EnhancedVLayout();
        //build editor form container
        final DynamicForm form = new DynamicForm();
        form.setMargin(5);

        //add sort priority selector
        //        final SelectItem resultSortSelector = PortletConfigurationEditorComponent
        //            .getResulSortOrderEditor(portletConfig);
        //add result status selector
        final SelectItem resultStatusSelector = PortletConfigurationEditorComponent
            .getConfigurationUpdateStatusEditor(portletConfig);

        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        //        form.setItems(alertPrioritySelector, resultCountSelector);
        form.setItems(resultStatusSelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //                //result sort order
                //                selectedValue = resultSortSelector.getValue().toString();
                //                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(PageOrdering.DESC.name()))) {//then desc
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.DESC));
                //                } else {
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.ASC));
                //                }
                //config status
                Configuration updatedConfig = AbstractActivityView.saveConfigUpdateStatusSelectorSettings(
                    resultStatusSelector, portletConfig);

                //result count
                updatedConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector, updatedConfig);

                //time range settings
                updatedConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    updatedConfig);

                //persist and reload portlet
                storedPortlet.setConfiguration(updatedConfig);
                configure(portletWindow, storedPortlet);
                refresh();
            }
        });
        form.markForRedraw();
        page.addMember(measurementRangeEditor);
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.ResourceGroup != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new GroupConfigurationUpdatesPortlet(context);
        }
    }

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    protected void onInit() {
        //disable the refresh timer for this run
        currentlyLoading = true;
        initializeUi();
        loadData();
    }

    @Override
    public boolean isRefreshing() {
        return this.currentlyLoading;
    }

    protected void setRefreshing(boolean currentlyRefreshing) {
        this.currentlyLoading = currentlyRefreshing;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            loadData();
        }
    }

    protected void loadData() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        //populate composite data
        //locate resourceGroupRef
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(context.getGroupId());
        criteria.fetchConfigurationUpdates(false);
        criteria.fetchExplicitResources(false);
        criteria.fetchGroupDefinition(false);
        criteria.fetchOperationHistories(false);

        // for autoclusters and autogroups we need to add more criteria
        if (context.isAutoCluster()) {
            criteria.addFilterVisible(false);
        } else if (context.isAutoGroup()) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        //locate the resource group
        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource group composite for group [" + context.getGroupId() + "]:"
                        + caught.getMessage());
                    currentlyLoading = false;
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> results) {
                    if (!results.isEmpty()) {
                        groupComposite = results.get(0);
                        //instantiate view

                        PageControl lastFive = new PageControl(0, 5);
                        GroupResourceConfigurationUpdateCriteria criteria = new GroupResourceConfigurationUpdateCriteria();
                        criteria.setPageControl(lastFive);
                        //TODO: spinder: move this up into the pageControl.
                        criteria.addSortStatus(PageOrdering.DESC);
                        List<Integer> filterResourceGroupIds = new ArrayList<Integer>();
                        filterResourceGroupIds.add(context.getGroupId());
                        criteria.addFilterResourceGroupIds(filterResourceGroupIds);

                        groupHistoryTable = new GroupConfigurationHistoryCriteriaTable(groupComposite);
                    } else {
                        ResourceGroup emptyGroup = new ResourceGroup("");
                        emptyGroup.setId(-1);
                        Long zero = new Long(0);
                        groupComposite = new ResourceGroupComposite(zero, zero, zero, zero, zero, zero, zero, zero,
                            emptyGroup);
                        groupHistoryTable = new GroupConfigurationHistoryCriteriaTable(groupComposite);
                    }

                    //update table for portlet display.                    
                    groupHistoryTable.setDataSource(new GroupConfigurationUdpatesCriteriaDataSource(portletConfig,
                        context.getGroupId()));
                    groupHistoryTable.setShowHeader(false);
                    groupHistoryTable.setShowFooterRefresh(false);

                    //cleanup
                    for (Canvas child : recentConfigurationContent.getChildren()) {
                        child.destroy();
                    }
                    recentConfigurationContent.addChild(groupHistoryTable);
                    recentConfigurationContent.markForRedraw();
                    currentlyLoading = false;
                }
            });
    }

    class GroupConfigurationHistoryCriteriaTable extends HistoryGroupResourceConfigurationTable {

        public GroupConfigurationHistoryCriteriaTable(ResourceGroupComposite groupComposite) {
            super(groupComposite);
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

        @Override
        protected void onInit() {
            super.onInit();
            getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
        }

    }

    class GroupConfigurationUdpatesCriteriaDataSource extends GroupResourceConfigurationDataSource {

        public GroupConfigurationUdpatesCriteriaDataSource(Configuration portletConfig, int groupId) {
            super(groupId);
            this.portletConfig = portletConfig;
            this.groupId = groupId;
        }

        private int groupId;
        private Configuration portletConfig;

        @Override
        protected void executeFetch(final DSRequest request, final DSResponse response,
            final GroupResourceConfigurationUpdateCriteria criteria) {
            ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

            configurationService.findGroupResourceConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<GroupResourceConfigurationUpdate>>() {

                    @Override
                    public void onSuccess(PageList<GroupResourceConfigurationUpdate> result) {
                        response.setData(buildRecords(result));
                        //adjust for portlets that restrict result size
                        response.setTotalRows(result.size());
                        processResponse(request.getRequestId(), response);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_group_resConfig_table_failFetch(), caught);
                        response.setStatus(DSResponse.STATUS_FAILURE);
                        processResponse(request.getRequestId(), response);
                    }
                });
        }

        @Override
        protected GroupResourceConfigurationUpdateCriteria getFetchCriteria(final DSRequest request) {
            //initialize criteria
            GroupResourceConfigurationUpdateCriteria criteria = new GroupResourceConfigurationUpdateCriteria();

            criteria.addFilterResourceGroupIds(Arrays.asList(groupId));

            PageControl pageControl = new PageControl(0, Integer.valueOf(Constant.RESULT_COUNT_DEFAULT));

            //customize query with latest configuration selections

            //retrieve previous settings from portlet config
            if (portletConfig != null) {
                //            //result sort order
                //            PropertySimple property = portletConfig.getSimple(Constant.RESULT_SORT_ORDER);
                //            if (property != null) {
                //                String currentSetting = property.getStringValue();
                //                if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase(PageOrdering.DESC.name())) {
                //                    criteria.addSortStatus(PageOrdering.DESC);
                //                } else {
                //                    criteria.addSortStatus(PageOrdering.ASC);
                //                }
                //            }
                //result timeframe if enabled
                PropertySimple property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
                if (null != property && Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

                    boolean isAdvanced = Boolean.valueOf(portletConfig.getSimpleValue(
                        Constant.METRIC_RANGE_BEGIN_END_FLAG, Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT));
                    if (isAdvanced) {
                        //Advanced time settings
                        String currentSetting = portletConfig.getSimpleValue(Constant.METRIC_RANGE,
                            Constant.METRIC_RANGE_DEFAULT);
                        String[] range = currentSetting.split(",");
                        if (range.length == 2) {
                            criteria.addFilterStartTime(Long.valueOf(range[0]));
                            criteria.addFilterEndTime(Long.valueOf(range[1]));
                        }
                    } else {
                        //Simple time settings
                        property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                        if (property != null) {
                            int lastN = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                                Constant.METRIC_RANGE_LASTN_DEFAULT));
                            int units = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                                Constant.METRIC_RANGE_UNIT_DEFAULT));
                            ArrayList<Instant> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                            criteria.addFilterStartTime(beginEnd.get(0).toDate().getTime());
                            criteria.addFilterEndTime(beginEnd.get(1).toDate().getTime());
                        }
                    }
                }

                //result count
                String currentSetting = portletConfig.getSimpleValue(Constant.RESULT_COUNT,
                    Constant.RESULT_COUNT_DEFAULT);
                if (currentSetting.trim().isEmpty()) {
                    pageControl.setPageSize(Integer.valueOf(Constant.RESULT_COUNT_DEFAULT));
                } else {
                    pageControl.setPageSize(Integer.valueOf(currentSetting));
                }

                criteria.setPageControl(pageControl);

                //detect operation status filter
                property = portletConfig.getSimple(Constant.CONFIG_UPDATE_STATUS);
                if (property != null) {
                    currentSetting = portletConfig.getSimpleValue(Constant.CONFIG_UPDATE_STATUS,
                        Constant.CONFIG_UPDATE_STATUS_DEFAULT);
                    String[] parsedValues = currentSetting.trim().split(",");
                    if (currentSetting.trim().isEmpty()
                        || parsedValues.length == ConfigurationUpdateStatus.values().length) {
                        //all operation stati assumed
                    } else {
                        ConfigurationUpdateStatus[] updateStatus = new ConfigurationUpdateStatus[parsedValues.length];
                        int indx = 0;
                        for (String priority : parsedValues) {
                            ConfigurationUpdateStatus s = ConfigurationUpdateStatus.valueOf(priority);
                            updateStatus[indx++] = s;
                        }
                        criteria.addFilterStatuses(updateStatus);
                    }
                }
            }
            return criteria;
        }
    }
}
