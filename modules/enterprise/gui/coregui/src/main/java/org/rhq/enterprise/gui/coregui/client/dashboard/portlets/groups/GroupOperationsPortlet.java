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
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.GroupOperationHistoryCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.operation.GroupOperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.operation.history.GroupOperationHistoryListView;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Simeon Pinder
 */
public class GroupOperationsPortlet extends LocatableVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupOperations";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_operations();

    protected static final String ID = "id";

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
    protected DashboardPortlet storedPortlet;
    protected Configuration portletConfig;
    protected int groupId;
    protected boolean portletConfigInitialized = false;
    private ResourceGroupComposite groupComposite;
    protected String baseViewPath = "";
    protected LocatableCanvas recentOperationsContent = new LocatableCanvas(extendLocatorId("RecentOperations"));
    protected String locatorId;

    public GroupOperationsPortlet(String locatorId) {
        super(locatorId);
        this.locatorId = locatorId;
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int groupId = AbstractActivityView.groupIdLookup(currentPage);
        this.groupId = groupId;
        //populate basepath
        baseViewPath = AbstractActivityView.groupPathLookup(currentPage);
    }

    @Override
    protected void onInit() {
        super.onInit();
        initializeUi();
        loadData();
    }

    private void loadData() {
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
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> results) {
                    if (!results.isEmpty()) {
                        groupComposite = results.get(0);
                        //instantiate view
                        //populated GWT criteria objects
                        Criteria criteria = new Criteria(GroupOperationHistoryDataSource.CriteriaField.GROUP_ID, String
                            .valueOf(groupComposite.getResourceGroup().getId()));

                        groupOperations = new GroupOperationsCriteriaHistoryListView(locatorId,
                            new GroupOperationsCriteriaDataSource(portletConfig), null, criteria, groupComposite);
                    } else {
                        Criteria criteria = new Criteria();
                        ResourceGroup emptyGroup = new ResourceGroup("");
                        emptyGroup.setId(-1);
                        Long zero = new Long(0);
                        groupComposite = new ResourceGroupComposite(zero, zero, zero, zero, emptyGroup);
                        groupOperations = new GroupOperationsCriteriaHistoryListView(locatorId,
                            new GroupOperationsCriteriaDataSource(portletConfig), null, criteria, groupComposite);
                    }

                    //cleanup
                    for (Canvas child : recentOperationsContent.getChildren()) {
                        child.destroy();
                    }
                    recentOperationsContent.addChild(groupOperations);
                    recentOperationsContent.markForRedraw();
                }
            });
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
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
        this.storedPortlet = storedPortlet;
        portletConfig = storedPortlet.getConfiguration();

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
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
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
                portletConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector, portletConfig);

                //time range configuration
                portletConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    portletConfig);

                //operation priority
                portletConfig = AbstractActivityView.saveOperationStatusSelectorSettings(operationStatusSelector,
                    portletConfig);

                //persist and reload portlet
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                //resynch the config object in the datasource
                groupOperations.setDatasource(new GroupOperationsCriteriaDataSource(portletConfig));
                //apply latest settings to the visible result set
                redraw();
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

        public final Portlet getInstance(String locatorId) {
            return new GroupOperationsPortlet(locatorId);
        }
    }

    @Override
    public void startRefreshCycle() {
        //current setting
        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        //cancel any existing timer
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        if (refreshInterval >= MeasurementUtility.MINUTES) {

            refreshTimer = new Timer() {
                public void run() {
                    redraw();
                }
            };

            refreshTimer.scheduleRepeating(refreshInterval);
        }
    }

    @Override
    protected void onDestroy() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        super.onDestroy();
    }

    private boolean isAutoGroup() {
        return ResourceGroupDetailView.AUTO_GROUP_VIEW_PATH.equals(getBaseViewPath());
    }

    private boolean isAutoCluster() {
        return ResourceGroupDetailView.AUTO_CLUSTER_VIEW_PATH.equals(getBaseViewPath());
    }

    public String getBaseViewPath() {
        return baseViewPath;
    }

    @Override
    public void redraw() {
        super.redraw();
        loadData();
    }
}

/** Provide implementation of GroupOperationsHistoryListView using datasource
 *  that customizes fetch based on Configuration parameters.
 *
 * @author spinder
 */
class GroupOperationsCriteriaHistoryListView extends GroupOperationHistoryListView {

    private ResourceGroupComposite composite;

    public GroupOperationsCriteriaHistoryListView(String locatorId, GroupOperationsCriteriaDataSource dataSource,
        String title, Criteria criteria, ResourceGroupComposite composite) {
        super(locatorId, composite);
        super.setDataSource(dataSource);
        this.composite = composite;
        setShowFooterRefresh(false); //disable footer refresh
    }

    public void setDatasource(GroupOperationsCriteriaDataSource datasource) {
        super.setDataSource(datasource);
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
    protected String getBasePath() {
        return "ResourceGroup/" + composite.getResourceGroup().getId() + "/Operations/History";
    }

    @Override
    protected void onInit() {
        super.onInit();
        getListGrid().setEmptyMessage(MSG.view_portlet_results_empty());
    }
}

/** Provide implementation of GroupOperationHistoryDataSource that dynamically
 *  configures fetch requests for this table view.
 *
 * @author spinder
 */
class GroupOperationsCriteriaDataSource extends GroupOperationHistoryDataSource {

    public GroupOperationsCriteriaDataSource(Configuration portletConfig) {
        this.portletConfig = portletConfig;
    }

    private Configuration portletConfig;

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
            property = portletConfig.getSimple(Constant.OPERATION_STATUS);
            if (property != null) {
                String currentSetting = property.getStringValue();
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