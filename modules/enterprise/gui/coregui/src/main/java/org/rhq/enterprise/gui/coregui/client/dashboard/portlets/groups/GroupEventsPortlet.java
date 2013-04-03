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
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.util.GwtTuple;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**This portlet allows the end user to customize the Events display
 *
 * @author Simeon Pinder
 */
public class GroupEventsPortlet extends EnhancedVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupEvents";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_events();

    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;
    //instance ui widgets

    protected Timer refreshTimer;

    protected static List<String> CONFIG_INCLUDE = new ArrayList<String>();
    static {
        CONFIG_INCLUDE.add(Constant.RESULT_COUNT);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_BEGIN_END_FLAG);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_ENABLE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_LASTN);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_UNIT);
    }

    private int groupId = -1;
    protected Canvas recentEventsContent = new Canvas();
    protected boolean currentlyLoading = false;

    public GroupEventsPortlet(int groupId) {
        super();
        this.groupId = groupId;
    }

    @Override
    protected void onInit() {
        //disable the refresh timer for this run
        currentlyLoading = true;
        initializeUi();
        loadData();
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentEventsContent);
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
        return new HTMLFlow(MSG.view_portlet_help_eventcounts());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.ResourceGroup != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new GroupEventsPortlet(context.getGroupId());
        }
    }

    protected void loadData() {
        getRecentEventUpdates();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        final DynamicForm customSettings = new DynamicForm();
        EnhancedVLayout page = new EnhancedVLayout();

        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        //add range selector
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                //persist the measurement range selections
                Configuration updatedConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(
                    measurementRangeEditor, portletConfig);

                //persist
                storedPortlet.setConfiguration(updatedConfig);
                configure(portletWindow, storedPortlet);
                refresh();
            }
        });
        page.addMember(measurementRangeEditor);
        customSettings.addChild(page);
        return customSettings;
    }

    /** Fetches recent events and updates the DynamicForm instance with the latest
     *  event information over last 24hrs.
     */
    protected void getRecentEventUpdates() {
        final int groupId = this.groupId;
        long end = System.currentTimeMillis();
        long start = end - (24 * 60 * 60 * 1000);

        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

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
                    start = Long.valueOf(range[0]);
                    end = Long.valueOf(range[1]);
                }
            } else {
                //Simple time settings
                property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                if (property != null) {
                    int lastN = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                        Constant.METRIC_RANGE_LASTN_DEFAULT));
                    int units = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                        Constant.METRIC_RANGE_UNIT_DEFAULT));
                    ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                    start = Long.valueOf(beginEnd.get(0));
                    end = Long.valueOf(beginEnd.get(1));
                }
            }
        }

        GWTServiceLookup.getEventService().getEventCountsBySeverityForGroup(groupId, start, end,
            new AsyncCallback<Map<EventSeverity, Integer>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving recent event counts for group [" + groupId + "]:" + caught.getMessage());
                    setCurrentlyRefreshing(false);
                }

                @Override
                public void onSuccess(Map<EventSeverity, Integer> eventCounts) {
                    //Now populated Tuples
                    List<GwtTuple<EventSeverity, Integer>> results = new ArrayList<GwtTuple<EventSeverity, Integer>>();
                    for (EventSeverity severity : eventCounts.keySet()) {
                        int count = eventCounts.get(severity);
                        if (count > 0) {
                            results.add(new GwtTuple<EventSeverity, Integer>(severity, count));
                        }
                    }
                    //build display
                    EnhancedVLayout column = new EnhancedVLayout();
                    column.setHeight(10);

                    if (!results.isEmpty()) {
                        int rowNum = 0;
                        for (GwtTuple<EventSeverity, Integer> tuple : results) {
                            DynamicForm row = new DynamicForm();
                            row.setNumCols(2);
                            row.setWidth(10);//pack.

                            //icon
                            StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(
                                ImageManager.getEventSeverityIcon(tuple.getLefty()), tuple.getLefty().name());
                            //count
                            StaticTextItem count = AbstractActivityView.newTextItem(String.valueOf(tuple.righty));
                            row.setItems(iconItem, count);

                            column.addMember(row);
                        }
                        column.markForRedraw();
                        //insert see more link
                        DynamicForm row = new DynamicForm();
                        String link = LinkManager.getGroupEventHistoryListLink(EntityContext.forGroup(groupId));
                        AbstractActivityView.addSeeMoreLink(row, link, column);
                    } else {
                        DynamicForm row = AbstractActivityView
                            .createEmptyDisplayRow(AbstractActivityView.RECENT_CRITERIA_EVENTS_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentEventsContent.getChildren()) {
                        child.destroy();
                    }
                    recentEventsContent.addChild(column);
                    recentEventsContent.markForRedraw();
                    setCurrentlyRefreshing(false);
                    markForRedraw();
                }
            });
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycle(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    public boolean isRefreshing() {
        return this.currentlyLoading;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            loadData();
        }
    }

    protected void setCurrentlyRefreshing(boolean currentlyRefreshing) {
        this.currentlyLoading = currentlyRefreshing;
    }
}