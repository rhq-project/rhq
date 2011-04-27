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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.group.GroupResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.GroupResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.GroupResourceConfigurationDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration.HistoryGroupResourceConfigurationTable;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Simeon Pinder
 */
public class GroupConfigurationUpdatesPortlet extends LocatableVLayout implements CustomSettingsPortlet,
    AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupConfigurationUpdates";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_config_updates();

    private int groupId = -1;
    protected LocatableCanvas recentConfigurationContent = new LocatableCanvas(
        extendLocatorId("RecentConfigurationUpdates"));

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
    protected String baseViewPath = "";
    private GroupConfigurationHistoryCriteriaTable groupHistoryTable;

    protected static HashMap<String, String> updatedMapping = new HashMap<String, String>();
    static {
        updatedMapping.putAll(PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION);
    }

    public GroupConfigurationUpdatesPortlet(String locatorId, int groupId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.groupId = groupId;
        baseViewPath = elements[0];
    }

    public GroupConfigurationUpdatesPortlet(String locatorId) {
        super(locatorId);
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
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
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
                storedPortlet.setConfiguration(portletConfig);
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
        public static PortletViewFactory INSTANCE = new Factory();

        /* (non-Javadoc)
         * TODO:  This factory ASSUMES the user is currently navigated to a group detail view, and generates a portlet
         *        for that group.  It will fail in other scenarios.  This mechanism should be improved such that the
         *        factory method can take an EntityContext explicitly indicating, in this case, the group.
         * @see org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory#getInstance(java.lang.String)
         */
        public final Portlet getInstance(String locatorId) {

            String currentPage = History.getToken();
            int groupId = -1;
            String[] elements = currentPage.split("/");
            // process for groups and auto groups Ex. ResourceGroup/10111 or ResourceGroup/AutoCluster/10321
            try {
                groupId = Integer.valueOf(elements[1]);
            } catch (NumberFormatException nfe) {
                groupId = Integer.valueOf(elements[2]);
            }

            return new GroupConfigurationUpdatesPortlet(locatorId, groupId);
        }
    }

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

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
        criteria.addFilterId(this.groupId);
        criteria.fetchConfigurationUpdates(false);
        criteria.fetchExplicitResources(false);
        criteria.fetchGroupDefinition(false);
        criteria.fetchOperationHistories(false);

        // for autoclusters and autogroups we need to add more criteria
        final boolean isAutoCluster = isAutoCluster();
        final boolean isAutoGroup = isAutoGroup();
        if (isAutoCluster) {
            criteria.addFilterVisible(false);
        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        //locate the resource group
        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource group composite for group [" + groupId + "]:"
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
                        filterResourceGroupIds.add(groupId);
                        criteria.addFilterResourceGroupIds(filterResourceGroupIds);

                        groupHistoryTable = new GroupConfigurationHistoryCriteriaTable(extendLocatorId("Table"),
                            groupComposite);
                    } else {
                        ResourceGroup emptyGroup = new ResourceGroup("");
                        emptyGroup.setId(-1);
                        Long zero = new Long(0);
                        groupComposite = new ResourceGroupComposite(zero, zero, zero, zero, emptyGroup);
                        groupHistoryTable = new GroupConfigurationHistoryCriteriaTable(extendLocatorId("Table"),
                            groupComposite);
                    }

                    //update table for portlet display.                    
                    groupHistoryTable.setDataSource(new GroupConfigurationUdpatesCriteriaDataSource(portletConfig,
                        groupId));
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

    private boolean isAutoGroup() {
        return ResourceGroupDetailView.AUTO_GROUP_VIEW.equals(getBaseViewPath());
    }

    private boolean isAutoCluster() {
        return ResourceGroupDetailView.AUTO_CLUSTER_VIEW.equals(getBaseViewPath());
    }

    public String getBaseViewPath() {
        return baseViewPath;
    }

    class GroupConfigurationHistoryCriteriaTable extends HistoryGroupResourceConfigurationTable {

        public GroupConfigurationHistoryCriteriaTable(String locatorId, ResourceGroupComposite groupComposite) {
            super(locatorId, groupComposite);
        }

        @Override
        protected void refreshTableInfo() {
            super.refreshTableInfo();
            if (getTableInfo() != null) {
                int count = getListGrid().getSelection().length;
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

            //initialize to only five for quick queries.
            PageControl pageControl = new PageControl(0, 5);//default to displaying five
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
                if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

                    boolean isAdvanced = false;
                    //detect type of widget[Simple|Advanced]
                    property = portletConfig.getSimple(Constant.METRIC_RANGE_BEGIN_END_FLAG);
                    if (property != null) {
                        isAdvanced = property.getBooleanValue();
                    }
                    if (isAdvanced) {
                        //Advanced time settings
                        property = portletConfig.getSimple(Constant.METRIC_RANGE);
                        if (property != null) {
                            String currentSetting = property.getStringValue();
                            String[] range = currentSetting.split(",");
                            criteria.addFilterStartTime(Long.valueOf(range[0]));
                            criteria.addFilterEndTime(Long.valueOf(range[1]));
                        }
                    } else {
                        //Simple time settings
                        property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                        if (property != null) {
                            int lastN = property.getIntegerValue();
                            property = portletConfig.getSimple(Constant.METRIC_RANGE_UNIT);
                            int lastUnits = property.getIntegerValue();
                            ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, Integer
                                .valueOf(lastUnits));
                            criteria.addFilterStartTime(Long.valueOf(beginEnd.get(0)));
                            criteria.addFilterEndTime(Long.valueOf(beginEnd.get(1)));
                        }
                    }
                }

                //result count
                property = portletConfig.getSimple(Constant.RESULT_COUNT);
                if (property != null) {
                    String currentSetting = property.getStringValue();
                    if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                        pageControl.setPageSize(5);
                    } else {
                        pageControl = new PageControl(0, Integer.valueOf(currentSetting));
                    }
                }
                criteria.setPageControl(pageControl);

                //detect operation status filter
                property = portletConfig.getSimple(Constant.CONFIG_UPDATE_STATUS);
                if (property != null) {
                    String currentSetting = property.getStringValue();
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