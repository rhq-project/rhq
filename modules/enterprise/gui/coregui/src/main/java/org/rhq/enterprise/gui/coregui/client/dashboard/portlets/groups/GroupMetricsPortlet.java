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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
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
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableCanvas;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**This portlet allows the end user to customize the metric display
 *
 * @author Simeon Pinder
 */
public class GroupMetricsPortlet extends LocatableVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    private int groupId = -1;
    protected LocatableCanvas recentMeasurementsContent = new LocatableCanvas(extendLocatorId("RecentMetrics"));
    private boolean currentlyLoading = false;
    private Configuration portletConfig = null;
    private DashboardPortlet storedPortlet;

    public GroupMetricsPortlet(String locatorId) {
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

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentMeasurementsContent);
    }

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupMetrics";
    // A default displayed, persisted name for the portlet
    public static final String NAME = "Group: Metrics";
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
        //TODO: spinder change.
        return new HTMLFlow(MSG.view_portlet_help_recentAlerts());
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new GroupMetricsPortlet(locatorId);
        }
    }

    protected void loadData() {
        currentlyLoading = true;
        getRecentMetrics();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                //alert time range filter. Check for enabled and then persist property. Dealing with compound widget.
                FormItem item = measurementRangeEditor.getItem(CustomConfigMeasurementRangeEditor.ENABLE_RANGE_ITEM);
                CheckboxItem itemC = (CheckboxItem) item;
                String selectedValue = String.valueOf(itemC.getValueAsBoolean());
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
        page.addMember(measurementRangeEditor);
        customSettings.addChild(page);
        return customSettings;
    }

    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
     * digits.
     */
    private void getRecentMetrics() {

        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack
        //        final int groupId = this.groupComposite.getResourceGroup().getId();
        final int groupId = this.groupId;
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterIds(groupId);
        criteria.fetchGroupDefinition(true);
        criteria.fetchResourceType(true);
        criteria.fetchExplicitResources(true);
        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource group composite for group [" + groupId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (!result.isEmpty()) {
                        //retrieve all relevant measurement definition ids.
                        //                    Set<MeasurementDefinition> definitions = this.groupComposite.getResourceGroup().getResourceType()
                        ResourceGroupComposite composite = result.get(0);
                        ResourceGroup group = composite.getResourceGroup();
                        ResourceType type = group.getResourceType();
                        Set<MeasurementDefinition> defs = type.getMetricDefinitions();
                        //                        Log.debug("------------- Composite:" + composite);
                        //                        Log.debug("------------- Group:" + group);
                        //                        Log.debug("------------- Type:" + type);
                        //                        Log.debug("------------- Defs:" + defs);

                        Set<MeasurementDefinition> definitions = result.get(0).getResourceGroup().getResourceType()
                            .getMetricDefinitions();

                        //build id mapping for measurementDefinition instances Ex. Free Memory -> MeasurementDefinition[100071]
                        final HashMap<String, MeasurementDefinition> measurementDefMap = new HashMap<String, MeasurementDefinition>();
                        for (MeasurementDefinition definition : definitions) {
                            measurementDefMap.put(definition.getDisplayName(), definition);
                        }
                        //bundle definition ids for asynch call.
                        int[] definitionArrayIds = new int[definitions.size()];
                        final String[] displayOrder = new String[definitions.size()];
                        measurementDefMap.keySet().toArray(displayOrder);
                        //sort the charting data ex. Free Memory, Free Swap Space,..System Load
                        Arrays.sort(displayOrder);

                        //organize definitionArrayIds for ordered request on server.
                        int index = 0;
                        for (String definitionToDisplay : displayOrder) {
                            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay).getId();
                        }

                        //make the asynchronous call for all the measurement data
                        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(groupId,
                            definitionArrayIds, System.currentTimeMillis() - (1000L * 60 * 60 * 8),
                            System.currentTimeMillis(), 60,
                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    Log.debug("Error retrieving recent metrics charting data for group [" + groupId
                                        + "]:" + caught.getMessage());
                                }

                                @Override
                                public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                                    if (!results.isEmpty()) {
                                        boolean someChartedData = false;
                                        //iterate over the retrieved charting data
                                        for (int index = 0; index < displayOrder.length; index++) {
                                            //retrieve the correct measurement definition
                                            MeasurementDefinition md = measurementDefMap.get(displayOrder[index]);

                                            //load the data results for the given metric definition
                                            List<MeasurementDataNumericHighLowComposite> data = results.get(index);

                                            //locate last and minimum values.
                                            double lastValue = -1;
                                            double minValue = Double.MAX_VALUE;//
                                            for (MeasurementDataNumericHighLowComposite d : data) {
                                                if ((!Double.isNaN(d.getValue()))
                                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                                    if (d.getValue() < minValue) {
                                                        minValue = d.getValue();
                                                    }
                                                    lastValue = d.getValue();
                                                }
                                            }

                                            //collapse the data into comma delimited list for consumption by third party javascript library(jquery.sparkline)
                                            String commaDelimitedList = "";

                                            for (MeasurementDataNumericHighLowComposite d : data) {
                                                if ((!Double.isNaN(d.getValue()))
                                                    && (String.valueOf(d.getValue()).indexOf("NaN") == -1)) {
                                                    commaDelimitedList += d.getValue() + ",";
                                                }
                                            }
                                            LocatableDynamicForm row = new LocatableDynamicForm(
                                                recentMeasurementsContent.extendLocatorId(md.getName()));
                                            row.setNumCols(3);
                                            HTMLFlow graph = new HTMLFlow();
                                            //                        String contents = "<span id='sparkline_" + index + "' class='dynamicsparkline' width='0'>"
                                            //                            + commaDelimitedList + "</span>";
                                            String contents = "<span id='sparkline_" + index
                                                + "' class='dynamicsparkline' width='0' " + "values='"
                                                + commaDelimitedList + "'>...</span>";
                                            graph.setContents(contents);
                                            graph.setContentsType(ContentsType.PAGE);
                                            //diable scrollbars on span
                                            graph.setScrollbarSize(0);

                                            CanvasItem graphContainer = new CanvasItem();
                                            graphContainer.setShowTitle(false);
                                            graphContainer.setHeight(16);
                                            graphContainer.setWidth(60);
                                            graphContainer.setCanvas(graph);

                                            //Link/title element
                                            //TODO: spinder, change link whenever portal.war/graphing is removed.
                                            String title = md.getDisplayName() + ":";
                                            //                            String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
                                            //                                + resourceId + "&m=" + md.getId();
                                            String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricMultiResource&groupId="
                                                + groupId + "&m=" + md.getId();
                                            LinkItem link = AbstractActivityView.newLinkItem(title, destination);

                                            //Value
                                            String convertedValue = lastValue + " " + md.getUnits();
                                            convertedValue = AbstractActivityView.convertLastValueForDisplay(lastValue,
                                                md);
                                            StaticTextItem value = AbstractActivityView.newTextItem(convertedValue);

                                            row.setItems(graphContainer, link, value);
                                            //if graph content returned
                                            if ((md.getName().trim().indexOf("Trait.") == -1) && (lastValue != -1)) {
                                                column.addMember(row);
                                                someChartedData = true;
                                            }
                                        }
                                        if (!someChartedData) {// when there are results but no chartable entries.
                                            LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(
                                                recentMeasurementsContent.extendLocatorId("None"),
                                                AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                            column.addMember(row);
                                        } else {
                                            //insert see more link
                                            LocatableDynamicForm row = new LocatableDynamicForm(
                                                recentMeasurementsContent
                                                    .extendLocatorId("RecentMeasurementsContentSeeMore"));
                                            AbstractActivityView.addSeeMoreLink(row, ReportDecorator.GWT_GROUP_URL
                                                + groupId + "/Monitoring/Graphs/", column);
                                        }
                                        //call out to 3rd party javascript lib
                                        BrowserUtility.graphSparkLines();
                                    } else {
                                        LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(
                                            recentMeasurementsContent.extendLocatorId("None"),
                                            AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                        column.addMember(row);
                                    }
                                }
                            });
                    }

                }
            });

        //cleanup
        for (Canvas child : recentMeasurementsContent.getChildren()) {
            child.destroy();
        }
        recentMeasurementsContent.addChild(column);
        recentMeasurementsContent.markForRedraw();
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
                    if (!currentlyLoading) {
                        loadData();
                        redraw();
                    }
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

    @Override
    public void redraw() {
        super.redraw();
        loadData();
    }

}