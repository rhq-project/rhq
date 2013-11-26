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
package org.rhq.coregui.client.dashboard.portlets.resource;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
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
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.D3GraphListView;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;

/**
 * This portlet allows the end user to customize the metric display
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
        public static final PortletViewFactory INSTANCE = new Factory();

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
        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack
        column.setWidth100();

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
                        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resourceType.getId(),
                            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
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

                                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId,
                                        definitionArrayIds, CustomDateRangeState.getInstance().getStartTime(),
                                        CustomDateRangeState.getInstance().getEndTime(), 60,
                                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                Log.debug("Error retrieving recent metrics charting data for resource ["
                                                    + resourceId + "]:" + caught.getMessage());
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
                                                        row.setNumCols(3);
                                                        row.setColWidths(65, "*", 100);
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
                                                        if (!BrowserUtility.isBrowserPreIE9()) {
                                                            link.addClickHandler(new ClickHandler() {
                                                                @Override
                                                                public void onClick(ClickEvent event) {
                                                                    window = new ChartViewWindow(title);

                                                                    graphView = D3GraphListView.createSingleGraph(
                                                                            resourceComposite.getResource(), md.getId(),
                                                                            true);

                                                                    window.addItem(graphView);
                                                                    window.show();
                                                                }
                                                            });
                                                        } else {
                                                            link.disable();
                                                        }

                                                        //Value
                                                        String convertedValue;
                                                        convertedValue = AbstractActivityView
                                                            .convertLastValueForDisplay(lastValue, md);
                                                        StaticTextItem value = AbstractActivityView
                                                            .newTextItem(convertedValue);
                                                        value.setVAlign(VerticalAlignment.TOP);
                                                        value.setAlign(Alignment.RIGHT);

                                                        row.setItems(sparklineContainer, link, value);
                                                        row.setWidth100();

                                                        //if graph content returned
                                                        if ((!md.getName().trim().contains("Trait."))
                                                            && (lastValue != -1)) {
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
                                                    new Timer() {
                                                        @Override
                                                        public void run() {
                                                            BrowserUtility.graphSparkLines();
                                                        }
                                                    }.schedule(200);
                                                } else {
                                                    DynamicForm row = AbstractActivityView
                                                        .createEmptyDisplayRow(AbstractActivityView.RECENT_MEASUREMENTS_NONE);
                                                    column.addMember(row);
                                                }
                                                setRefreshing(false);
                                            }
                                        }

                                    );

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
