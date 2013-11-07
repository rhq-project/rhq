/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;

/**
 * The consolidated metrics view showing metric graphs and availability data both in graphical and tabular form.
 *
 * @author Mike Thompson
 */
public class MetricsResourceView extends AbstractD3GraphListView implements
    AbstractTwoLevelTabSetView.ViewRenderedListener {

    private static final String COLLAPSED_TOOLTIP = MSG.chart_metrics_collapse_tooltip();
    private static final String EXPANDED_TOOLTIP = MSG.chart_metrics_expand_tooltip();

    private final Resource resource;
    private EnhancedHLayout expandCollapseHLayout;
    private MetricsTableView metricsTableView;
    private static MetricsResourceView INSTANCE = null;
    private static Integer lastResourceId = 0;

    /**
     * Encapsulate the creation logic and not let it leak out into other objects.
     * @param resource
     * @param expandedRows
     * @return MetricsResourceView
     */
    public static MetricsResourceView create(Resource resource, Set<Integer> expandedRows){

        boolean isDifferentResource = (resource.getId() != lastResourceId);

        if(INSTANCE == null ||  isDifferentResource ){
            if(null != expandedRows){
                expandedRows.clear();
            }
            INSTANCE =  new MetricsResourceView(resource,  expandedRows);
        }

        return INSTANCE;
    }

    private MetricsResourceView(Resource resource, Set<Integer> expandedRows) {
        super();
        setOverflow(Overflow.AUTO);
        setWidth100();
        setHeight100();
        this.resource = resource;
        metricsTableView = new MetricsTableView(resource, this, expandedRows);

        final ResourceMetricAvailabilityView availabilityDetails = new ResourceMetricAvailabilityView(resource);
        availabilityDetails.hide();

        metricsTableView.setHeight100();

        availabilityGraph = new AvailabilityD3GraphView<AvailabilityOverUnderGraphType>(
                new AvailabilityOverUnderGraphType(resource.getId()));

        expandCollapseHLayout = new EnhancedHLayout();
        //add expand/collapse icon
        final Img expandCollapseArrow = new Img(IconEnum.COLLAPSED_ICON.getIcon16x16Path(), 16, 16);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.setLayoutAlign(VerticalAlignment.BOTTOM);
        expandCollapseArrow.addClickHandler(new ClickHandler() {
            private boolean collapsed = true;

            @Override
            public void onClick(ClickEvent event) {
                collapsed = !collapsed;
                if (collapsed) {
                    expandCollapseArrow.setSrc(IconEnum.COLLAPSED_ICON.getIcon16x16Path());
                    expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
                    availabilityDetails.hide();
                } else {
                    expandCollapseArrow.setSrc(IconEnum.EXPANDED_ICON.getIcon16x16Path());
                    expandCollapseArrow.setTooltip(EXPANDED_TOOLTIP);
                    availabilityDetails.show();

                }
                drawAvailabilityGraphAndSparklines();
            }
        });
        expandCollapseHLayout.addMember(expandCollapseArrow);
        addAvailabilityGraph();

        addMember(buttonBarDateTimeRangeEditor);
        addMember(expandCollapseHLayout);
        addMember(availabilityDetails);
        addMember(metricsTableView);
    }



    private void addAvailabilityGraph() {
        expandCollapseHLayout.removeMember(availabilityGraph);
        availabilityGraph.destroy();

        availabilityGraph = new AvailabilityD3GraphView<AvailabilityOverUnderGraphType>(
            new AvailabilityOverUnderGraphType(resource.getId()));

        expandCollapseHLayout.addMember(availabilityGraph);

        queryAvailability(EntityContext.forResource(resource.getId()), buttonBarDateTimeRangeEditor.getStartTime(),
            buttonBarDateTimeRangeEditor.getEndTime(), null);
    }


    @Override
    protected void queryAvailability(final EntityContext context, Long startTime, Long endTime, CountDownLatch notUsed) {

        final long timerStart = System.currentTimeMillis();

        // now return the availability
        GWTServiceLookup.getAvailabilityService().getAvailabilitiesForResource(context.getResourceId(), startTime,
                endTime, new AsyncCallback<List<Availability>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
            }

            @Override
            public void onSuccess(List<Availability> availList) {
                Log.debug("\nSuccessfully queried availability in: " + (System.currentTimeMillis() - timerStart)
                        + " ms.");
                availabilityGraph.setAvailabilityList(availList);
                new Timer() {
                    @Override
                    public void run() {
                        availabilityGraph.drawJsniChart();
                        buttonBarDateTimeRangeEditor.updateTimeRangeToNow();

                    }
                }.schedule(150);
            }
        });
    }

    private void drawAvailabilityGraphAndSparklines() {
        new Timer() {
            @Override
            public void run() {
                Log.debug("MetricResourceView.drawAvailabilityGraphAndSparkLines() for: " + resource.getName() + " id: " + resource.getId());
                availabilityGraph.drawJsniChart();
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);
    }

    @Override
    public void refreshData() {
        Log.debug("MetricResourceView.refreshData() for: " + resource.getName() + " id: " + resource.getId());
        addAvailabilityGraph();
        metricsTableView.refresh();
    }

    @Override
    public void onViewRendered() {
        // refresh the graphs on subtab nav because we are a cached view not new
        refreshData();
    }
}
