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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;

/**This portlet allows the end user to customize the metric display
 *
 * @author Simeon Pinder
 */
public class ResourceMetricsPortlet extends GroupMetricsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceMetrics";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_metrics();

    private int resourceId = -1;

    private ChartViewWindow window;
    private D3GraphListView graphView;

    public ResourceMetricsPortlet(int resourceId) {
        super(EntityContext.forResource(-1));
        this.resourceId = resourceId;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.Resource != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new ResourceMetricsPortlet(context.getResourceId());
        }
    }

    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
     * digits.
     */
    @Override
    protected void getRecentMetrics() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();
        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack
        column.setWidth100();

        //initialize to defaults
        end = -1;
        start = -1;
        lastN = -1;
        units = -1;

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
                    lastN = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                        Constant.METRIC_RANGE_LASTN_DEFAULT));
                    units = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                        Constant.METRIC_RANGE_UNIT_DEFAULT));
                }
            }
        }

        //locate resource reference
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(this.resourceId);

        //locate the resource
        GWTServiceLookup.getResourceService().findResourceCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving resource resource composite for resource [" + resourceId + "]:"
                        + caught.getMessage());
                    setRefreshing(false);
                }

                @Override
                public void onSuccess(PageList<ResourceComposite> results) {
                    if (!results.isEmpty()) {
                        final ResourceComposite resourceComposite = results.get(0);
                        final Resource resource = resourceComposite.getResource();
                        // Load the fully fetched ResourceType.
                        ResourceType resourceType = resource.getResourceType();
                        ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                            resourceType.getId(),
                            EnumSet.of(ResourceTypeRepository.MetadataType.content,
                                ResourceTypeRepository.MetadataType.operations,
                                ResourceTypeRepository.MetadataType.measurements,
                                ResourceTypeRepository.MetadataType.events,
                                ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                            new ResourceTypeRepository.TypeLoadedCallback() {
                                public void onTypesLoaded(ResourceType type) {
                                    resource.setResourceType(type);
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

                                    AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>> callback = new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            Log.debug("Error retrieving recent metrics charting data for resource ["
                                                + resourceId + "]:" + caught.getMessage());
                                            setRefreshing(false);
                                        }

                                        @Override
                                        public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                                            if (!results.isEmpty()) {
                                                boolean someChartedData = false;
                                                //iterate over the retrieved charting data
                                                for (int index = 0; index < displayOrder.length; index++) {
                                                    //retrieve the correct measurement definition
                                                    final MeasurementDefinition md = measurementDefMap
                                                        .get(displayOrder[index]);

                                                    //load the data results for the given metric definition
                                                    List<MeasurementDataNumericHighLowComposite> data = results
                                                        .get(index);

                                                    //locate last and minimum values.
                                                    double lastValue = -1;
                                                    double minValue = Double.MAX_VALUE;//
                                                    for (MeasurementDataNumericHighLowComposite d : data) {
                                                        if ((!Double.isNaN(d.getValue()))
                                                            && (!String.valueOf(d.getValue()).contains("NaN"))) {
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
                                                            && (!String.valueOf(d.getValue()).contains("NaN"))) {
                                                            commaDelimitedList += d.getValue() + ",";
                                                        }
                                                    }
                                                    DynamicForm row = new DynamicForm();
                                                    row.setNumCols(4);
                                                    row.setColWidths(65, "*", 20, 100);
                                                    row.setWidth100();
                                                    row.setAutoHeight();
                                                    row.setOverflow(Overflow.VISIBLE);
                                                    HTMLFlow sparklineGraph = new HTMLFlow();
                                                    String contents = "<span id='sparkline_" + index
                                                        + "' class='dynamicsparkline' width='0' " + "values='"
                                                        + commaDelimitedList + "'>...</span>";
                                                    sparklineGraph.setContents(contents);
                                                    sparklineGraph.setContentsType(ContentsType.PAGE);
                                                    //disable scrollbars on span
                                                    sparklineGraph.setScrollbarSize(0);

                                                    CanvasItem sparklineContainer = new CanvasItem();
                                                    sparklineContainer.setShowTitle(false);
                                                    sparklineContainer.setHeight(16);
                                                    sparklineContainer.setWidth(60);
                                                    sparklineContainer.setCanvas(sparklineGraph);

                                                    //Link/title element
                                                    final String title = md.getDisplayName();
                                                    LinkItem link = AbstractActivityView.newLinkItem(title, null);
                                                    link.setTooltip(title);
                                                    link.setTitleVAlign(VerticalAlignment.TOP);
                                                    link.setAlign(Alignment.LEFT);
                                                    link.setClipValue(true);
                                                    link.setWrap(true);
                                                    link.setHeight(26);
                                                    link.setWidth("100%");
                                                    if(!BrowserUtility.isBrowserPreIE9()){
                                                        link.addClickHandler(new ClickHandler() {
                                                            @Override
                                                            public void onClick(ClickEvent event) {
                                                                window = new ChartViewWindow(title);

                                                                graphView = D3GraphListView
                                                                    .createSingleGraph(resourceComposite.getResource(),
                                                                        md.getId(), true);
                                                                graphView.addSetButtonClickHandler(new ClickHandler() {
                                                                    @Override
                                                                    public void onClick(ClickEvent event) {
                                                                        graphView.redrawGraphs();
                                                                    }
                                                                });

                                                                window.addItem(graphView);
                                                                window.show();
                                                            }
                                                        });
                                                    }else{
                                                        link.disable();
                                                    }

                                                    //@todo: this goes away once we have validated charts
                                                    final String chartTitle = md.getDisplayName();
                                                    final String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
                                                        + resourceId + "&m=" + md.getId();

                                                    //have link launch modal window on click
                                                    LinkItem oldLink = AbstractActivityView.newLinkItem("*",
                                                        destination);
                                                    oldLink.setTooltip("Link to test Old Chart");
                                                    oldLink.setTitleVAlign(VerticalAlignment.TOP);
                                                    oldLink.setAlign(Alignment.LEFT);
                                                    oldLink.setClipValue(true);
                                                    oldLink.setWrap(true);
                                                    oldLink.setHeight(26);
                                                    oldLink.setWidth("100%");
                                                    oldLink.addClickHandler(new ClickHandler() {
                                                        @Override
                                                        public void onClick(ClickEvent event) {
                                                            ChartViewWindow window = new ChartViewWindow(chartTitle);
                                                            //generate and include iframed content
                                                            FullHTMLPane iframe = new FullHTMLPane(destination);
                                                            window.addItem(iframe);
                                                            window.show();
                                                        }
                                                    });

                                                    //Value
                                                    String convertedValue;
                                                    convertedValue = AbstractActivityView.convertLastValueForDisplay(
                                                        lastValue, md);
                                                    StaticTextItem value = AbstractActivityView
                                                        .newTextItem(convertedValue);
                                                    value.setVAlign(VerticalAlignment.TOP);
                                                    value.setAlign(Alignment.RIGHT);

                                                    row.setItems(sparklineContainer, link, oldLink, value);
                                                    row.setWidth100();

                                                    //if graph content returned
                                                    if ((!md.getName().trim().contains("Trait.")) && (lastValue != -1)) {
                                                        column.addMember(row);
                                                        someChartedData = true;
                                                    }
                                                }
                                                if (!someChartedData) {// when there are results but no chartable entries.
                                                    DynamicForm row = AbstractActivityView.createEmptyDisplayRow(

                                                    AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                                    column.addMember(row);
                                                } else {
                                                    //insert see more link
                                                    DynamicForm row = new DynamicForm();
                                                    String link = LinkManager
                                                        .getResourceMonitoringGraphsLink(resourceId);
                                                    AbstractActivityView.addSeeMoreLink(row, link, column);
                                                }
                                                //call out to 3rd party javascript lib
                                                BrowserUtility.graphSparkLines();
                                            } else {
                                                DynamicForm row = AbstractActivityView
                                                    .createEmptyDisplayRow(AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                                column.addMember(row);
                                            }
                                            setRefreshing(false);
                                        }
                                    };

                                    //make the asynchronous call for all the measurement data
                                    if (end != -1 && start != -1) {
                                        GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId,
                                            definitionArrayIds, start, end, 60, callback);
                                    } else if (lastN != -1 && units != -1) {
                                        GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(
                                            resourceId, definitionArrayIds, lastN, units, 60, callback);
                                    } else {
                                        GWTServiceLookup.getMeasurementDataService().findDataForResourceForLast(
                                            resourceId, definitionArrayIds, 8, MeasurementUtility.UNIT_HOURS, 60,
                                            callback);
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
}