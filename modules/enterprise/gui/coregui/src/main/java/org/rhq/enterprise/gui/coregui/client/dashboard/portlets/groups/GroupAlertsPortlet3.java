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

import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.AlertGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**This portlet allows the end user to customize the:
 * i)range
 * ii)priority
 * iii)etc.
 * of alerts to display for the given group
 *
 * @author Simeon Pinder
 */
public class GroupAlertsPortlet3 extends LocatableVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {
    private int groupId = -1;
    protected LocatableCanvas recentAlertsContent = new LocatableCanvas(extendLocatorId("RecentAlerts"));
    private static AlertGWTServiceAsync alertService = GWTServiceLookup.getAlertService();
    private boolean currentlyLoading = false;
    private Configuration portletConfig = null;
    private DashboardPortlet storedPortlet;

    public GroupAlertsPortlet3(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int currentGroupIdentifier = Integer.valueOf(elements[1]);
        this.groupId = currentGroupIdentifier;
        initializeUi();
    }

    @Override
    protected void onInit() {
        super.onInit();
        loadData();
    }

    /**Defines layout for the Activity page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentAlertsContent);
    }

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupAlerts";
    // A default displayed, persisted name for the portlet
    public static final String NAME = "Group: Alerts";
    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    private PortletWindow portletWindow;
    //instance ui widgets

    private Timer refreshTimer;

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
            if (portletConfig.getSimple(key) == null) {
                portletConfig.put(new PropertySimple(key,
                    PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new GroupAlertsPortlet3(locatorId);
        }
    }

    /** Fetches alerts and updates the DynamicForm instance with the latest
     *  alert information.
     */
    private void getRecentAlerts() {
        final int groupId = this.groupId;
        currentlyLoading = false;
        //fetches last five alerts for this resource
        AlertCriteria criteria = new AlertCriteria();
        //filter priority
        PropertySimple property = portletConfig.getSimple(Constant.ALERT_PRIORITY);
        if (property != null) {
            String currentSetting = property.getStringValue();
            String[] parsedValues = currentSetting.trim().split(",");
            if (currentSetting.trim().isEmpty() || parsedValues.length == 3) {
                //all alert priorities assumed
            } else {
                AlertPriority[] filterPriorities = new AlertPriority[parsedValues.length];
                int indx = 0;
                for (String priority : parsedValues) {
                    AlertPriority p = AlertPriority.valueOf(priority);
                    filterPriorities[indx++] = p;
                }
                criteria.addFilterPriorities(filterPriorities);
            }
        }
        PageControl pc = new PageControl();
        //result sort order
        property = portletConfig.getSimple(Constant.RESULT_SORT_ORDER);
        if (property != null) {
            String currentSetting = property.getStringValue();
            if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase(PageOrdering.DESC.name())) {
                criteria.addSortCtime(PageOrdering.DESC);
                pc.setPrimarySortOrder(PageOrdering.DESC);
            } else {
                criteria.addSortCtime(PageOrdering.ASC);
                pc.setPrimarySortOrder(PageOrdering.ASC);
            }
        }
        //result timeframe if enabled
        property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
        if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting
            property = portletConfig.getSimple(Constant.METRIC_RANGE);
            if (property != null) {
                String currentSetting = property.getStringValue();
                String[] range = currentSetting.split(",");
                criteria.addFilterStartTime(Long.valueOf(range[0]));
                criteria.addFilterEndTime(Long.valueOf(range[1]));
            }
        }

        //result count
        property = portletConfig.getSimple(Constant.RESULT_COUNT);
        if (property != null) {
            String currentSetting = property.getStringValue();
            if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                //PageControl pageControl = new PageControl(0, 5);
                pc.setPageSize(5);
            } else {
                //PageControl pageControl = new PageControl(0, Integer.valueOf(currentSetting));
                pc.setPageSize(Integer.valueOf(currentSetting));
            }
        }
        criteria.setPageControl(pc);
        criteria.addFilterResourceGroupIds(groupId);
        alertService.findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {
            @Override
            public void onSuccess(PageList<Alert> result) {
                VLayout column = new VLayout();
                column.setHeight(10);
                if (!result.isEmpty()) {
                    int rowNum = 0;
                    for (Alert alert : result) {
                        // alert history records do not have a usable locatorId, we'll use rownum, which is unique and
                        // may be repeatable.
                        LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                            .valueOf(rowNum++)));
                        row.setNumCols(3);

                        StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(ImageManager.getAlertIcon(alert
                            .getAlertDefinition().getPriority()), alert.getAlertDefinition().getPriority()
                            .getDisplayName());
                        LinkItem link = AbstractActivityView.newLinkItem(alert.getAlertDefinition().getName() + ": ",
                            ReportDecorator.GWT_GROUP_URL + groupId + "/Alerts/History/" + alert.getId());
                        StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                            .format(alert.getCtime()));
                        row.setItems(iconItem, link, time);

                        column.addMember(row);
                    }
                    //link to more details
                    LocatableDynamicForm row = new LocatableDynamicForm(recentAlertsContent.extendLocatorId(String
                        .valueOf(rowNum++)));
                    AbstractActivityView.addSeeMoreLink(row, ReportDecorator.GWT_GROUP_URL + groupId
                        + "/Alerts/History/", column);
                } else {
                    LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentAlertsContent
                    //                        .extendLocatorId("None"), AbstractActivityView.RECENT_ALERTS_NONE);
                        .extendLocatorId("None"), "No results found using criteria specified.");
                    column.addMember(row);
                }
                for (Canvas child : recentAlertsContent.getChildren()) {
                    child.destroy();
                }
                recentAlertsContent.addChild(column);
                recentAlertsContent.markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving recent alerts for group [" + groupId + "]:" + caught.getMessage());
            }
        });
    }

    protected void loadData() {
        currentlyLoading = true;
        getRecentAlerts();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        //build editor form container
        final LocatableDynamicForm form = new LocatableDynamicForm(page.extendLocatorId("alert-filter"));
        form.setMargin(5);

        //add label about what configuration affects

        //add alert priority selector
        final SelectItem alertPrioritySelector = PortletConfigurationEditorComponent
            .getAlertPriorityEditor(portletConfig);
        //add sort priority selector
        //        final SelectItem resultSortSelector = PortletConfigurationEditorComponent
        //            .getResulSortOrderEditor(portletConfig);
        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);
        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);
        //TODO: spinder 3/10/11 renable sort selector once it's working in criteria
        //        form.setItems(alertPrioritySelector, resultSortSelector, resultCountSelector);
        form.setItems(alertPrioritySelector, resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //alert severity
                String selectedValue = alertPrioritySelector.getValue().toString();
                if ((selectedValue.trim().isEmpty()) || (selectedValue.split(",").length == 3)) {//then no alertPriority specified
                    portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, ""));
                } else {//some subset of available alertPriorities will be used
                    portletConfig.put(new PropertySimple(Constant.ALERT_PRIORITY, selectedValue));
                }
                //                //result sort order
                //                selectedValue = resultSortSelector.getValue().toString();
                //                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(PageOrdering.DESC.name()))) {//then desc
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.DESC));
                //                } else {
                //                    portletConfig.put(new PropertySimple(Constant.RESULT_SORT_ORDER, PageOrdering.ASC));
                //                }
                //result count
                selectedValue = resultCountSelector.getValue().toString();
                if ((selectedValue.trim().isEmpty()) || (selectedValue.equalsIgnoreCase(Constant.RESULT_COUNT_DEFAULT))) {//then 5
                    portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT));
                } else {
                    portletConfig.put(new PropertySimple(Constant.RESULT_COUNT, selectedValue));
                }

                //alert time range filter. Check for enabled and then persist property. Dealing with compound widget.
                FormItem item = measurementRangeEditor.getItem(CustomConfigMeasurementRangeEditor.ENABLE_RANGE_ITEM);
                CheckboxItem itemC = (CheckboxItem) item;
                selectedValue = String.valueOf(itemC.getValueAsBoolean());
                if (!selectedValue.trim().isEmpty()) {//then call
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_ENABLE, selectedValue));
                }

                //alert time advanced time filter enabled.
                selectedValue = String.valueOf(measurementRangeEditor.isAdvanced());
                if ((selectedValue != null) && (!selectedValue.trim().isEmpty())) {
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE_BEGIN_END_FLAG, selectedValue));
                }

                //alert time frame
                List<Long> begEnd = measurementRangeEditor.getBeginEndTimes();
                if (begEnd.get(0) != 0) {//advanced settings
                    portletConfig.put(new PropertySimple(Constant.METRIC_RANGE, (begEnd.get(0) + "," + begEnd.get(1))));
                }

                //persist
                storedPortlet.setConfiguration(portletConfig);
                configure(portletWindow, storedPortlet);
                loadData();
            }
        });
        form.markForRedraw();
        page.addMember(measurementRangeEditor);
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return this.currentlyLoading;
    }

    @Override
    public void redraw() {
        super.redraw();
        loadData();
    }
}