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
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
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
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.util.Moment;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
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
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryDataSource;
import org.rhq.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryListView;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Simeon Pinder
 */
public class GroupOperationsPortlet extends EnhancedVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupOperations";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_operations();

    protected static final String ID = "id";
    protected boolean currentlyRefreshing = false;

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;

    private GroupOperationsCriteriaHistoryListView groupOperations;

    //defines the list of configuration elements to load/persist for this portlet
    protected static List<String> CONFIG_INCLUDE = new ArrayList<String>();
    static {
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_BEGIN_END_FLAG);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_ENABLE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_LASTN);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_UNIT);
        CONFIG_INCLUDE.add(Constant.RESULT_COUNT);
        //        CONFIG_INCLUDE.add(Constant.RESULT_SORT_ORDER);
        //        CONFIG_INCLUDE.add(Constant.RESULT_SORT_PRIORITY);
        CONFIG_INCLUDE.add(Constant.OPERATION_STATUS);
    }

    //instance ui widgets
    protected Canvas containerCanvas;

    protected Timer refreshTimer;
    protected int groupId;
    protected boolean portletConfigInitialized = false;
    private ResourceGroupComposite groupComposite;
    protected Canvas recentOperationsContent = new Canvas();

    private boolean isAutoGroup;
    private boolean isAutoCluster;

    public GroupOperationsPortlet(EntityContext context) {
        super();
        this.groupId = context.getGroupId();
        this.isAutoGroup = context.isAutoGroup();
        this.isAutoCluster = context.isAutoCluster();
    }

    @Override
    protected void onInit() {
        super.onInit();
        //disable the refresh timer for this run
        currentlyRefreshing = true;
        initializeUi();
        loadData();
    }

    private void loadData() {
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
                    currentlyRefreshing = false;
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> results) {
                    if (!results.isEmpty()) {
                        groupComposite = results.get(0);
                        //instantiate view
                        //populated GWT criteria objects
                        Criteria criteria = new Criteria(GroupOperationHistoryDataSource.CriteriaField.GROUP_ID, String
                            .valueOf(groupComposite.getResourceGroup().getId()));

                        groupOperations = new GroupOperationsCriteriaHistoryListView(
                            new GroupOperationsCriteriaDataSource(portletConfig), null, criteria, groupComposite,
                            isAutoGroup);
                    } else {
                        Criteria criteria = new Criteria();
                        ResourceGroup emptyGroup = new ResourceGroup("");
                        emptyGroup.setId(-1);
                        Long zero = new Long(0);
                        groupComposite = new ResourceGroupComposite(zero, zero, zero, zero, zero, zero, zero, zero,
                            emptyGroup);
                        groupOperations = new GroupOperationsCriteriaHistoryListView(
                            new GroupOperationsCriteriaDataSource(portletConfig), null, criteria, groupComposite,
                            isAutoGroup);
                    }

                    //cleanup
                    for (Canvas child : recentOperationsContent.getChildren()) {
                        child.destroy();
                    }
                    recentOperationsContent.addChild(groupOperations);
                    currentlyRefreshing = false;
                    recentOperationsContent.markForRedraw();
                }
            });
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setHeight("*");
        setWidth100();

        //tell canvas to fill it's component
        recentOperationsContent.setHeight100();
        addMember(recentOperationsContent);
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
        return new HTMLFlow(MSG.view_portlet_help_operations_criteria());
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

        //add label about what configuration affects? redundant?
        //add filter operation status type selector
        final SelectItem operationStatusSelector = PortletConfigurationEditorComponent
            .getOperationStatusEditor(portletConfig);
        //        //add sort priority selector
        //        final SelectItem resultSortSelector = PortletConfigurationEditorComponent
        //            .getResulSortOrderEditor(portletConfig);
        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);

        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        form.setItems(operationStatusSelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                //result count
                Configuration updatedConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector,
                    portletConfig);

                //time range configuration
                updatedConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    portletConfig);

                //operation priority
                updatedConfig = AbstractActivityView.saveOperationStatusSelectorSettings(operationStatusSelector,
                    portletConfig);

                //persist and reload portlet
                storedPortlet.setConfiguration(updatedConfig);
                configure(portletWindow, storedPortlet);
                //resynch the config object in the datasource
                ((GroupOperationsCriteriaDataSource) groupOperations.getDataSource()).setPortletConfig(updatedConfig);
                //apply latest settings to the visible result set
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

            return new GroupOperationsPortlet(context);
        }
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return currentlyRefreshing;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            if (groupOperations != null) {
                groupOperations.refresh();
            }
        }
    }

    private boolean isAutoGroup() {
        return this.isAutoGroup;
    }

    private boolean isAutoCluster() {
        return this.isAutoCluster;
    }

    protected void setCurrentlyRefreshing(boolean currentlyRefreshing) {
        this.currentlyRefreshing = currentlyRefreshing;
    }
}

/** Provide implementation of GroupOperationsHistoryListView using datasource
 *  that customizes fetch based on Configuration parameters.
 *
 * @author spinder
 */
class GroupOperationsCriteriaHistoryListView extends GroupOperationHistoryListView {

    private ResourceGroupComposite composite;
    private boolean isAutogroup;

    public GroupOperationsCriteriaHistoryListView(GroupOperationsCriteriaDataSource dataSource, String title,
        Criteria criteria, ResourceGroupComposite composite, boolean isAutogroup) {
        super(composite);
        super.setDataSource(dataSource);
        this.composite = composite;
        this.isAutogroup = isAutogroup;
        setShowFooterRefresh(false); //disable footer refresh
    }

    public void setDatasource(GroupOperationsCriteriaDataSource datasource) {
        super.setDataSource(datasource);
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
    protected String getBasePath() {
        return (isAutogroup ? ResourceGroupDetailView.AUTO_GROUP_VIEW + '/' : "ResourceGroup/") + composite.getResourceGroup().getId() + "/Operations/History";
    }

    @Override
    protected void onInit() {
        super.onInit();
        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }
    
    @Override
    protected boolean showNewScheduleButton() {
        return false; //hide the "new schedule" button for group portlets
    }
}

/** Provide implementation of GroupOperationHistoryDataSource that dynamically
 *  configures fetch requests for this table view.
 *
 * @author spinder
 */
class GroupOperationsCriteriaDataSource extends GroupOperationHistoryDataSource {

    private Configuration portletConfig;

    public GroupOperationsCriteriaDataSource(Configuration portletConfig) {
        this.portletConfig = portletConfig;
    }

    public Configuration getPortletConfig() {
        return portletConfig;
    }

    public void setPortletConfig(Configuration portletConfig) {
        this.portletConfig = portletConfig;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response,
        final GroupOperationHistoryCriteria criteria) {
        operationService.findGroupOperationHistoriesByCriteria(criteria,
            new AsyncCallback<PageList<GroupOperationHistory>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.dataSource_operationHistory_error_fetchFailure(), caught);
                }

                public void onSuccess(PageList<GroupOperationHistory> result) {
                    response.setData(buildRecords(result));
                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    protected GroupOperationHistoryCriteria getFetchCriteria(final DSRequest request) {
        //initialize criteria
        GroupOperationHistoryCriteria criteria = new GroupOperationHistoryCriteria();

        //retrieve group identifier
        if (request.getCriteria().getValues().containsKey(CriteriaField.GROUP_ID)) {
            int groupId = Integer.parseInt((String) request.getCriteria().getValues().get(CriteriaField.GROUP_ID));
            criteria.addFilterResourceGroupIds(Arrays.asList(groupId));
        }

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

                boolean isAdvanced = Boolean.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_BEGIN_END_FLAG,
                    Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT));
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
                        Integer lastN = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                            Constant.METRIC_RANGE_LASTN_DEFAULT));
                        Integer units = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                            Constant.METRIC_RANGE_UNIT_DEFAULT));
                        ArrayList<Moment> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                        criteria.addFilterStartTime(beginEnd.get(0).toDate().getTime());
                        criteria.addFilterEndTime(beginEnd.get(1).toDate().getTime());
                    }
                }
            }

            //result count
            String currentSetting = portletConfig.getSimpleValue(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
            if (currentSetting.trim().isEmpty()) {
                pageControl.setPageSize(Integer.valueOf(Constant.RESULT_COUNT_DEFAULT));
            } else {
                pageControl.setPageSize(Integer.valueOf(currentSetting));
            }
            criteria.setPageControl(pageControl);

            //detect operation status filter
            property = portletConfig.getSimple(Constant.OPERATION_STATUS);
            if (property != null) {
                currentSetting = portletConfig.getSimpleValue(Constant.OPERATION_STATUS,
                    Constant.OPERATION_STATUS_DEFAULT);
                String[] parsedValues = currentSetting.trim().split(",");
                if (currentSetting.trim().isEmpty() || parsedValues.length == OperationRequestStatus.values().length) {
                    //all operation stati assumed
                } else {
                    OperationRequestStatus[] operationStati = new OperationRequestStatus[parsedValues.length];
                    int indx = 0;
                    for (String priority : parsedValues) {
                        OperationRequestStatus s = OperationRequestStatus.valueOf(priority);
                        operationStati[indx++] = s;
                    }
                    criteria.addFilterStatuses(operationStati);
                }
            }
        }
        return criteria;
    }
}
