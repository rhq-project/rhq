/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.common.detail.summary;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.util.Moment;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceTypeFacet;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.measurement.GwtMonitorUtils;

/**
 * @author Simeon Pinder
 */
public abstract class AbstractActivityView extends EnhancedVLayout implements RefreshableView {

    //contains the activity display region
    private EnhancedHLayout columnSection = new EnhancedHLayout();

    protected VLayout leftPane = new VLayout();
    protected VLayout rightPane = new VLayout();

    protected Canvas recentMeasurementsContent = new Canvas();
    protected Canvas recentAlertsContent = new Canvas();
    protected Canvas recentOobContent = new Canvas();
    protected Canvas recentConfigurationContent = new Canvas();
    protected Canvas recentOperationsContent = new Canvas();
    protected Canvas recentEventsContent = new Canvas();
    protected Canvas recentPkgHistoryContent = new Canvas();
    protected Canvas recentBundleDeployContent = new Canvas();

    //retrieve localized text
    public static final String RECENT_MEASUREMENTS = MSG.common_title_recent_measurements();
    public static final String RECENT_MEASUREMENTS_NONE = MSG.view_resource_inventory_activity_no_recent_metrics();
    public static final String RECENT_MEASUREMENTS_GROUP_NONE = MSG.view_group_inventory_activity_no_recent_metrics();
    public static final String RECENT_ALERTS = MSG.common_title_recent_alerts();
    public static final String RECENT_OOB = MSG.common_title_recent_oob_metrics();
    public static final String RECENT_OOB_NONE = MSG.view_resource_inventory_activity_no_recent_oob();
    public static final String RECENT_CONFIGURATIONS = MSG.common_title_recent_configuration_updates();
    public static final String RECENT_OPERATIONS = MSG.common_title_recent_operations();
    public static final String RECENT_EVENTS = MSG.view_portlet_defaultName_recentEvents();
    public static final String RECENT_PKG_HISTORY = MSG.common_title_recent_pkg_history();
    public static final String RECENT_BUNDLE_DEPLOY = MSG.common_title_recent_bundle_deployments();
    public static final String SEE_MORE = MSG.common_msg_see_more();
    public static final String RECENT_CRITERIA_EVENTS_NONE = MSG.view_resource_inventory_activity_criteria_no_recent_events();
    public static final String CHART_TITLE = MSG.common_title_metric_chart();

    private ResourceGroupComposite groupComposite = null;
    private ResourceComposite resourceComposite = null;
    private HLayout recentBundleDeployTitle;
    private ToolStrip footer;
    private boolean firstRightPanePortletLoaded = false;

    public AbstractActivityView(ResourceGroupComposite groupComposite, ResourceComposite resourceComposite) {
        super();
        if (groupComposite != null) {
            this.groupComposite = groupComposite;
        }
        if (resourceComposite != null) {
            this.resourceComposite = resourceComposite;
        }
        addMember(columnSection);
        initializeUi();
    }

    /**Defines layout for the Activity page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        //dividers definition
        HTMLFlow divider1 = new HTMLFlow("<hr/>");
        HTMLFlow divider2 = new HTMLFlow("<hr/>");
        HTMLFlow divider3 = new HTMLFlow("<hr/>");
        HTMLFlow divider4 = new HTMLFlow("<hr/>");
        HTMLFlow divider5 = new HTMLFlow("<hr/>");
        divider1.setWidth("50%");
        divider2.setWidth("50%");
        divider3.setWidth("50%");
        divider4.setWidth("50%");
        divider5.setWidth("50%");

        //leftPane
        leftPane.setWidth("50%");
        leftPane.setPadding(5);
        leftPane.setMembersMargin(5);
        leftPane.setAutoHeight();

        Resource resource = null;
        ResourceGroup group = null;
        GroupCategory groupCategory = null;
        Set<ResourceTypeFacet> facets = null;
        Set<ResourceTypeFacet> resourceFacets = null;
        if ((groupComposite != null) && (groupComposite.getResourceGroup() != null)) {
            group = groupComposite.getResourceGroup();
            groupCategory = groupComposite.getResourceGroup().getGroupCategory();
            facets = groupComposite.getResourceFacets().getFacets();
        }
        if (resourceComposite != null) {
            resource = resourceComposite.getResource();
            resourceFacets = this.resourceComposite.getResourceFacets().getFacets();
        }

        //recentMetrics.xhtml
        HLayout recentMetricsTitle = new TitleWithIcon(IconEnum.RECENT_MEASUREMENTS.getIcon24x24Path(),
            RECENT_MEASUREMENTS);
        if ((resource != null) || ((group != null) && (groupCategory.equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            leftPane.addMember(recentMetricsTitle);
            leftPane.addMember(recentMeasurementsContent);
            recentMeasurementsContent.setHeight(20);
            leftPane.addMember(divider1);
        }
        //recentAlerts.xhtml
        HLayout recentAlertsTitle = new TitleWithIcon(IconEnum.RECENT_ALERTS.getIcon24x24Path(), RECENT_ALERTS);
        leftPane.addMember(recentAlertsTitle);
        leftPane.addMember(recentAlertsContent);
        recentAlertsContent.setHeight(20);
        //recentOOBs.xhtml
        HLayout recentOobsTitle = new TitleWithIcon(IconEnum.SUSPECT_METRICS.getIcon24x24Path(), RECENT_OOB);
        recentOobContent.setHeight(20);
        if ((resource != null) || ((group != null) && (groupCategory.equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            leftPane.addMember(divider2);
            leftPane.addMember(recentOobsTitle);
            leftPane.addMember(recentOobContent);
        }
        //rightPane
        rightPane.setWidth("50%");
        rightPane.setPadding(5);
        rightPane.setMembersMargin(5);
        rightPane.setAutoHeight();
        firstRightPanePortletLoaded = false;
        //recentConfigUpdates.xhtml
        HLayout recentConfigUpdatesTitle = new TitleWithIcon(IconEnum.CONFIGURE.getIcon24x24Path(),
            RECENT_CONFIGURATIONS);
        recentConfigurationContent.setHeight(20);
        if (((resource != null) && (resourceFacets.contains(ResourceTypeFacet.CONFIGURATION)))
            || (displayGroupConfigurationUpdates(groupCategory, facets))) {//resource
            rightPane.addMember(recentConfigUpdatesTitle);
            rightPane.addMember(recentConfigurationContent);
            firstRightPanePortletLoaded = true;
        }

        //recentOperations.xhtml
        HLayout recentOperationsTitle = new TitleWithIcon(IconEnum.RECENT_OPERATIONS.getIcon24x24Path(),
            RECENT_OPERATIONS);
        recentOperationsContent.setHeight(20);
        if (((resource != null) && (resourceFacets.contains(ResourceTypeFacet.OPERATION)))
            || (displayGroupOperations(groupCategory, facets))) {//resource
            if (firstRightPanePortletLoaded) {
                rightPane.addMember(divider3);
            }
            rightPane.addMember(recentOperationsTitle);
            rightPane.addMember(recentOperationsContent);
            firstRightPanePortletLoaded = true;
        }
        //recentEventCounts.xhtml
        HLayout recentEventsTitle = new TitleWithIcon(IconEnum.EVENTS.getIcon24x24Path(), RECENT_EVENTS);
        recentEventsContent.setHeight(20);
        if (((resource != null) && (resourceFacets.contains(ResourceTypeFacet.EVENT)))
            || displayGroupEvents(groupCategory, facets)) {//resource
            if (firstRightPanePortletLoaded) {
                rightPane.addMember(divider4);
            }
            rightPane.addMember(recentEventsTitle);
            rightPane.addMember(recentEventsContent);
            firstRightPanePortletLoaded = true;
        }
        //recentPackageHistory.xhtml
        HLayout recentPkgHistoryTitle = new TitleWithIcon(IconEnum.CONTENT.getIcon24x24Path(), RECENT_PKG_HISTORY);
        recentPkgHistoryContent.setHeight(20);
        if ((resource != null) || ((group != null) && (groupCategory.equals(GroupCategory.COMPATIBLE)))) {//resource,CompatibleGroup
            if (firstRightPanePortletLoaded) {
                rightPane.addMember(divider5);
            }
            rightPane.addMember(recentPkgHistoryTitle);
            rightPane.addMember(recentPkgHistoryContent);
        }

        //recent bundle deployments
        recentBundleDeployTitle = new TitleWithIcon(IconEnum.CONTENT.getIcon24x24Path(), RECENT_BUNDLE_DEPLOY);
        recentBundleDeployTitle.setHeight(20);
        deployBundleViewIfApplicable(resource, group);

        columnSection.addMember(leftPane);
        columnSection.addMember(rightPane);

        //Add footer region
        this.footer = new ToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);
        footer.addMember(new LayoutSpacer());
        IButton refreshButton = new EnhancedIButton(MSG.common_button_refresh());
        refreshButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                loadData();
                refresh();
            }
        });
        footer.addMember(refreshButton);
        addMember(footer);

    }

    private void deployBundleViewIfApplicable(Resource resource, ResourceGroup group) {
        if (displayBundlesForResource(resource)) {
            enableBundleArea();
        } else {//necessarily need to check group membership for platforms
            if (group != null) {
                //displays bundles region if group is compatible and contains platform resources
                displayBundleDeploymentsForPlatformGroups(group);
            }
        }

    }

    protected boolean displayBundlesForResource(Resource resource) {
        boolean display = false;
        if ((resource != null) && (resource.getResourceType().getCategory().equals(ResourceCategory.PLATFORM))) {
            display = true;
        }
        return display;
    }

    protected void displayBundleDeploymentsForPlatformGroups(final ResourceGroup group) {
        if (group != null) {
            ResourceGroupCriteria criteria = new ResourceGroupCriteria();
            criteria.addFilterId(group.getId());
            criteria.fetchExplicitResources(true);
            GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroup>>() {
                    @Override
                    public void onSuccess(PageList<ResourceGroup> results) {
                        if (!results.isEmpty()) {
                            ResourceGroup gp = results.get(0);
                            Set<Resource> explicitMembers = gp.getExplicitResources();
                            Resource[] currentResources = new Resource[explicitMembers.size()];
                            explicitMembers.toArray(currentResources);
                            if (group.getGroupCategory().equals(GroupCategory.COMPATIBLE)) {
                                if (currentResources[0].getResourceType().getCategory()
                                    .equals(ResourceCategory.PLATFORM)) {
                                    enableBundleArea();
                                    getRecentBundleDeployments();
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        Log.debug("Error retrieving information for group [" + group.getId() + "]:"
                            + caught.getMessage());
                    }
                });

        }
    }

    /** Implement to define calls to asynchronous calls out to UI display data.
     */
    protected abstract void loadData();

    protected abstract void getRecentBundleDeployments();

    @Override
    protected void onDraw() {
        super.onDraw();
        refresh();
    }

    @Override
    public void destroy() {
        // destroy members of non-enhanced layouts
        EnhancedUtility.destroyMembers(leftPane);
        EnhancedUtility.destroyMembers(rightPane);

        super.destroy();
    }

    @Override
    public void refresh() {
        markForRedraw();
        //call out to 3rd party javascript lib
        BrowserUtility.graphSparkLines();
    }

    private void enableBundleArea() {
        HTMLFlow divider6 = new HTMLFlow("<hr/>");
        divider6.setWidth("50%");

        if (firstRightPanePortletLoaded) {
            rightPane.addMember(divider6);
        }
        rightPane.addMember(recentBundleDeployTitle);
        rightPane.addMember(recentBundleDeployContent);
        rightPane.markForRedraw();
    }

    /**Creates the section top titles with icon for regions of Activity page.
     */
    public class TitleWithIcon extends HLayout {

        public TitleWithIcon(String imageUrl, String title) {
            super();
            Img titleImage = new Img(imageUrl, 24, 24);
            HTMLFlow titleElement = new HTMLFlow();
            titleElement.setWidth("*");
            titleElement.setContents(title);
            titleElement.setStyleName("HeaderLabel");
            addMember(titleImage);
            addMember(titleElement);
            setMembersMargin(10);
        }

        @Override
        public void destroy() {
            EnhancedUtility.destroyMembers(this);
            super.destroy();
        }
    }

    /** Takes last double value returned and the relevant MeasurementDefinition and formats
     *  the results for display in the UI.  'Formatting' refers to relevant rounding,
     *  number format for significant digits depending upon the measurement definition
     *  details.
     *
     * @param lastValue
     * @param md MeasurementDefinition
     * @return formatted String representation of the last value retrieved.
     */
    public static String convertLastValueForDisplay(double lastValue, MeasurementDefinition md) {
        String convertedValue = "";
        String[] convertedValues = GwtMonitorUtils.formatSimpleMetrics(new double[] { lastValue }, md);
        convertedValue = convertedValues[0];

        return convertedValue;
    }

    /** Create empty display row(DynamicForm) that is constantly defined and displayed.
     *
     * @param emptyMessage Contents of the empty region
     * @return
     */
    public static DynamicForm createEmptyDisplayRow(String emptyMessage) {
        DynamicForm row = null;
        row = new DynamicForm();

        row.setNumCols(3);
        StaticTextItem none = new StaticTextItem();
        none.setShowTitle(false);
        none.setDefaultValue(emptyMessage);
        none.setWrap(false);
        row.setItems(none);
        return row;
    }

    public static StaticTextItem newTextItemIcon(String imageSrc, String mouseOver) {
        StaticTextItem iconItem = new StaticTextItem();
        FormItemIcon img = new FormItemIcon();
        img.setSrc(imageSrc);
        img.setWidth(16);
        img.setHeight(16);
        if (mouseOver != null) {
            img.setPrompt(mouseOver);
        }
        iconItem.setIcons(img);
        iconItem.setShowTitle(false);
        return iconItem;
    }

    public static LinkItem newLinkItem(String title, String destination) {
        LinkItem link = new LinkItem();
        link.setLinkTitle(title);
        link.setTitle(title);
        link.setValue(destination);
        link.setTarget("_self");
        link.setShowTitle(false);
        return link;
    }

    public static StaticTextItem newTextItem(String contents) {
        StaticTextItem item = new StaticTextItem();
        item.setDefaultValue(contents);
        item.setShowTitle(false);
        item.setShowPickerIcon(false);
        item.setWrap(false);
        return item;
    }

    /** Generates a "See more.." link item, using the dynamic form passed in and appends to the VLayout passed in.
     *
     * @param form
     * @param linkDestination
     * @param column
     */
    public static void addSeeMoreLink(DynamicForm form, String linkDestination, VLayout column) {
        if ((form != null) && (column != null)) {
            form.setNumCols(1);
            LinkItem link = newLinkItem(SEE_MORE, linkDestination);
            form.setItems(link);
            column.addMember(form);
        }
    }

    /** Takes the current value of the widget and persists it into the configuration object passed in.
     *
     * @param resultCountSelector
     * @param portletConfig
     * returns populated configuration object.
     */
    public static Configuration saveResultCounterSettings(final SelectItem resultCountSelector,
        final Configuration portletConfig) {
        if ((resultCountSelector != null) && (portletConfig != null)) {
            String selectedValue = (null == resultCountSelector.getValue()) ? "" : resultCountSelector.getValue()
                .toString();
            if (selectedValue.trim().isEmpty()) {
                portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT));
            } else {
                portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, selectedValue));
            }
        }
        return portletConfig;
    }

    /** Takes the current value of the widget and persists it into the configuration object passed in.
    *
    * @param operationStatusSelector
    * @param portletConfig
    * returns populated configuration object.
    */
    public static Configuration saveOperationStatusSelectorSettings(final SelectItem operationStatusSelector,
        final Configuration portletConfig) {
        String selectedValue = (null == operationStatusSelector.getValue()) ? "" : operationStatusSelector.getValue()
            .toString();
        if (selectedValue.trim().isEmpty()
            || (selectedValue.split(",").length == OperationRequestStatus.values().length)) {
            portletConfig.put(new PropertySimple(Constant.OPERATION_STATUS, Constant.OPERATION_STATUS_DEFAULT));
        } else {
            portletConfig.put(new PropertySimple(Constant.OPERATION_STATUS, selectedValue));
        }
        return portletConfig;
    }

    /** Takes the current value of the widget and persists it into the configuration object passed in.
    *
    * @param configStatusSelector
    * @param portletConfig
    * returns populated configuration object.
    */
    public static Configuration saveConfigUpdateStatusSelectorSettings(final SelectItem configStatusSelector,
        final Configuration portletConfig) {
        String selectedValue = (null == configStatusSelector.getValue()) ? "" : configStatusSelector.getValue()
            .toString();
        if (selectedValue.trim().isEmpty()
            || (selectedValue.split(",").length == ConfigurationUpdateStatus.values().length)) {
            portletConfig.put(new PropertySimple(Constant.CONFIG_UPDATE_STATUS, Constant.CONFIG_UPDATE_STATUS_DEFAULT));
        } else {//some subset of available configUpdate statuses will be used
            portletConfig.put(new PropertySimple(Constant.CONFIG_UPDATE_STATUS, selectedValue));
        }
        return portletConfig;
    }

    /** Takes the current value of the widget and persists it into the configuration object passed in.
    *
    * @param measurementRangeEditor
    * @param portletConfig
    * returns populated configuration object.
    */
    public static Configuration saveMeasurementRangeEditorSettings(
        final CustomConfigMeasurementRangeEditor measurementRangeEditor, Configuration portletConfig) {
        String selectedValue = null;
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
        return portletConfig;
    }

    /** Takes the current value of the widget and persists it into the configuration object passed in.
    *
    * @param alertPrioritySelector
    * @param portletConfig
    * returns populated configuration object.
    */
    public static Configuration saveAlertPrioritySettings(SelectItem alertPrioritySelector, Configuration portletConfig) {
        String selectedValue = (String) alertPrioritySelector.getValue();
        if (null == selectedValue || selectedValue.trim().isEmpty()
            || (selectedValue.split(",").length == AlertPriority.values().length)) {//then no alertPriority specified
            portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, Constant.ALERT_PRIORITY_DEFAULT));
        } else {//some subset of available alertPriorities will be used
            portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, selectedValue));
        }
        return portletConfig;
    }

    protected boolean displayGroupConfigurationUpdates(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if ((groupCategory == null) || facets == null) {
            return false;
        }
        return (groupCategory == GroupCategory.COMPATIBLE && facets.contains(ResourceTypeFacet.CONFIGURATION));
    }

    protected boolean displayGroupOperations(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if ((groupCategory == null) || facets == null) {
            return false;
        }
        return ((groupCategory == GroupCategory.COMPATIBLE) && facets.contains(ResourceTypeFacet.OPERATION));
    }

    protected boolean displayGroupEvents(GroupCategory groupCategory, Set<ResourceTypeFacet> facets) {
        if ((groupCategory == null) || facets == null) {
            return false;
        }
        return ((groupCategory == GroupCategory.MIXED) || (groupCategory == GroupCategory.COMPATIBLE && facets
            .contains(ResourceTypeFacet.EVENT)));
    }

    public static class ChartViewWindow extends Window {

        public ChartViewWindow(String title) {
            this(title, null);
        }

        public ChartViewWindow(String title, String windowTitle) {
            super();
            if ((windowTitle != null) && (!windowTitle.trim().isEmpty())) {
                setTitle(windowTitle + ": " + title);
            } else {
                setTitle(CHART_TITLE + ": " + title);
            }
            setShowMinimizeButton(false);
            setShowMaximizeButton(false);
            setShowCloseButton(true);
            setIsModal(true);
            setShowModalMask(true);
            setWidth(950);
            setHeight(420);
            setShowResizer(true);
            setCanDragResize(true);
            centerInPage();

            addCloseClickHandler(new CloseClickHandler() {
                @Override
                public void onCloseClick(CloseClickEvent event) {
                    try {
                        ChartViewWindow.this.destroy();
                    } catch (Throwable e) {
                        Log.warn("Cannot destroy chart display window.", e);
                    }
                }
            });

        }
    }

}
