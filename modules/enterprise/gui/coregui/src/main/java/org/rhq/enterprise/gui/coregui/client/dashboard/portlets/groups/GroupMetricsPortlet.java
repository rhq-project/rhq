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
import java.util.EnumSet;
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
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortletUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.ResourceGroupDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
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
    protected boolean currentlyLoading = false;
    protected Configuration portletConfig = null;
    protected DashboardPortlet storedPortlet;
    protected String baseViewPath = "";
    protected long start = -1;
    protected long end = -1;

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupMetrics";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_metrics();
    public static final String ID = "id";

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;
    //instance ui widgets

    protected Timer refreshTimer;

    //defines the list of configuration elements to load/persist for this portlet
    protected static List<String> CONFIG_INCLUDE = new ArrayList<String>();
    static {
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_BEGIN_END_FLAG);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_ENABLE);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_LASTN);
        CONFIG_INCLUDE.add(Constant.METRIC_RANGE_UNIT);
    }

    public GroupMetricsPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int groupId = AbstractActivityView.groupIdLookup(currentPage);
        this.groupId = groupId;
        baseViewPath = elements[0];
    }

    @Override
    protected void onInit() {
        setRefreshing(true);
        initializeUi();
        loadData();
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentMeasurementsContent);
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
        return new HTMLFlow(MSG.view_portlet_help_metrics());
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

    /** Builds custom config UI, using shared widgets
     */
    @Override
    public DynamicForm getCustomSettingsForm() {
        //root form.
        LocatableDynamicForm customSettings = new LocatableDynamicForm(extendLocatorId("customSettings"));
        //embed range editor in it own container
        LocatableVLayout page = new LocatableVLayout(customSettings.extendLocatorId("page"));
        final CustomConfigMeasurementRangeEditor measurementRangeEditor = PortletConfigurationEditorComponent
            .getMeasurementRangeEditor(portletConfig);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                //retrieve range editor values
                portletConfig = AbstractActivityView.saveMeasurementRangeEditorSettings(measurementRangeEditor,
                    portletConfig);

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
    protected void getRecentMetrics() {
        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack

        //initialize to defaults
        end = System.currentTimeMillis();
        start = end - (1000L * 60 * 60 * 8);//last 8 hrs

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
                    start = Long.valueOf(range[0]);
                    end = Long.valueOf(range[1]);
                }
            } else {
                //Simple time settings
                property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                if (property != null) {
                    int lastN = property.getIntegerValue();
                    property = portletConfig.getSimple(Constant.METRIC_RANGE_UNIT);
                    int lastUnits = property.getIntegerValue();
                    ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, Integer.valueOf(lastUnits));
                    start = Long.valueOf(beginEnd.get(0));
                    end = Long.valueOf(beginEnd.get(1));
                }
            }
        }

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
                    setRefreshing(false);
                }

                @Override
                public void onSuccess(PageList<ResourceGroupComposite> results) {
                    if (!results.isEmpty()) {
                        final ResourceGroupComposite groupComposite = results.get(0);
                        final ResourceGroup group = groupComposite.getResourceGroup();
                        if (group.getGroupCategory() == GroupCategory.COMPATIBLE) {
                            // Load the fully fetched ResourceType.
                            ResourceType groupType = group.getResourceType();
                            ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                                groupType.getId(),
                                EnumSet.of(ResourceTypeRepository.MetadataType.content,
                                    ResourceTypeRepository.MetadataType.operations,
                                    ResourceTypeRepository.MetadataType.measurements,
                                    ResourceTypeRepository.MetadataType.events,
                                    ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                                new ResourceTypeRepository.TypeLoadedCallback() {
                                    public void onTypesLoaded(ResourceType type) {
                                        group.setResourceType(type);
                                        //metric definitions
                                        Set<MeasurementDefinition> definitions = type.getMetricDefinitions();

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
                                            definitionArrayIds[index++] = measurementDefMap.get(definitionToDisplay)
                                                .getId();
                                        }

                                        //make the asynchronous call for all the measurement data
                                        GWTServiceLookup.getMeasurementDataService().findDataForCompatibleGroup(
                                            groupId, definitionArrayIds, start, end, 60,
                                            new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                                @Override
                                                public void onFailure(Throwable caught) {
                                                    Log
                                                        .debug("Error retrieving recent metrics charting data for group ["
                                                            + groupId + "]:" + caught.getMessage());
                                                    setRefreshing(false);
                                                }

                                                @Override
                                                public void onSuccess(
                                                    List<List<MeasurementDataNumericHighLowComposite>> results) {
                                                    if (!results.isEmpty()) {
                                                        boolean someChartedData = false;
                                                        //iterate over the retrieved charting data
                                                        for (int index = 0; index < displayOrder.length; index++) {
                                                            //retrieve the correct measurement definition
                                                            MeasurementDefinition md = measurementDefMap
                                                                .get(displayOrder[index]);

                                                            //load the data results for the given metric definition
                                                            List<MeasurementDataNumericHighLowComposite> data = results
                                                                .get(index);

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
                                                            LinkItem link = AbstractActivityView.newLinkItem(title,
                                                                destination);

                                                            //Value
                                                            String convertedValue = lastValue + " " + md.getUnits();
                                                            convertedValue = AbstractActivityView
                                                                .convertLastValueForDisplay(lastValue, md);
                                                            StaticTextItem value = AbstractActivityView
                                                                .newTextItem(convertedValue);

                                                            row.setItems(graphContainer, link, value);
                                                            //if graph content returned
                                                            if ((md.getName().trim().indexOf("Trait.") == -1)
                                                                && (lastValue != -1)) {
                                                                column.addMember(row);
                                                                someChartedData = true;
                                                            }
                                                        }
                                                        if (!someChartedData) {// when there are results but no chartable entries.
                                                            LocatableDynamicForm row = AbstractActivityView
                                                                .createEmptyDisplayRow(recentMeasurementsContent
                                                                    .extendLocatorId("None"),
                                                                    AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                                            column.addMember(row);
                                                        } else {
                                                            //insert see more link
                                                            LocatableDynamicForm row = new LocatableDynamicForm(
                                                                recentMeasurementsContent
                                                                    .extendLocatorId("RecentMeasurementsContentSeeMore"));
                                                            AbstractActivityView.addSeeMoreLink(row,
                                                                ReportDecorator.GWT_GROUP_URL + groupId
                                                                    + "/Monitoring/Graphs/", column);
                                                        }
                                                        //call out to 3rd party javascript lib
                                                        BrowserUtility.graphSparkLines();
                                                    } else {
                                                        LocatableDynamicForm row = AbstractActivityView
                                                            .createEmptyDisplayRow(recentMeasurementsContent
                                                                .extendLocatorId("None"),
                                                                AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                                        column.addMember(row);
                                                    }
                                                    setRefreshing(false);
                                                }
                                            });
                                    }
                                });
                        }
                    } else {
                        LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentMeasurementsContent
                            .extendLocatorId("None"), AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                        column.addMember(row);
                        setRefreshing(false);
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
        refreshTimer = AutoRefreshPortletUtil.startRefreshCycle(this, this, refreshTimer);
        //call out to 3rd party javascript lib
        BrowserUtility.graphSparkLines();
        recentMeasurementsContent.markForRedraw();
    }

    @Override
    protected void onDestroy() {
        AutoRefreshPortletUtil.onDestroy(this, refreshTimer);

        super.onDestroy();
    }

    @Override
    public boolean isRefreshing() {
        return this.currentlyLoading;
    }

    @Override
    public void redraw() {
        super.redraw();
        if (!isRefreshing()) {
            loadData();
        }
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

    protected void setRefreshing(boolean currentlyRefreshing) {
        this.currentlyLoading = currentlyRefreshing;
    }
}