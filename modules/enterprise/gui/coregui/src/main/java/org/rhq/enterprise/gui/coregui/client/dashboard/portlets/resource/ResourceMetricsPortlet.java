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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupMetricsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

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

    public ResourceMetricsPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        int currentResourceIdentifier = Integer.valueOf(elements[1]);
        this.resourceId = currentResourceIdentifier;
        baseViewPath = elements[0];
    }

    @Override
    protected void onInit() {
        super.onInit();
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

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ResourceMetricsPortlet(locatorId);
        }
    }

    protected void loadData() {
        currentlyLoading = true;
        getRecentMetrics();
    }

    /** Fetches recent metric information and updates the DynamicForm instance with i)sparkline information,
     * ii) link to recent metric graph for more details and iii) last metric value formatted to show significant
     * digits.
     */
    private void getRecentMetrics() {
        //display container
        final VLayout column = new VLayout();
        column.setHeight(10);//pack

        //initialize to defaults
        end = System.currentTimeMillis();
        start = end - (1000L * 60 * 60 * 8);//last 8 hrs

        //result timeframe if enabled
        PropertySimple property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
        if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting
            property = portletConfig.getSimple(Constant.METRIC_RANGE);
            if (property != null) {
                String currentSetting = property.getStringValue();
                String[] range = currentSetting.split(",");
                start = Long.valueOf(range[0]);
                end = Long.valueOf(range[1]);
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

                                    //make the asynchronous call for all the measurement data
                                    GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId,
                                        definitionArrayIds, start, end, 60,
                                        new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                Log
                                                    .debug("Error retrieving recent metrics charting data for resource ["
                                                        + resourceId + "]:" + caught.getMessage());
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
                                                        String destination = "/resource/common/monitor/Visibility.do?mode=chartSingleMetricSingleResource&id="
                                                            + resourceId + "&m=" + md.getId();
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
                                                            ReportDecorator.GWT_RESOURCE_URL + resourceId
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
                                            }
                                        });
                                }
                            });
                        //                        }
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