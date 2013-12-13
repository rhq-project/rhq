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
package org.rhq.coregui.client.inventory.groups.detail.monitoring.metric;

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
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.inventory.common.detail.AbstractTwoLevelTabSetView;
import org.rhq.coregui.client.inventory.common.graph.CustomDateRangeState;
import org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.ExpandedRowsMomento;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.coregui.client.inventory.resource.detail.monitoring.table.MetricAvailabilityView;
import org.rhq.coregui.client.util.BrowserUtility;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;

/**
 * The consolidated metrics view showing metric graphs and availability data both in graphical and tabular form.
 * @author Mike Thompson
 */
public class MetricsGroupView extends AbstractD3GraphListView implements
    AbstractTwoLevelTabSetView.ViewRenderedListener {

    private static final String COLLAPSED_TOOLTIP = MSG.chart_metrics_collapse_tooltip();
    private static final String EXPANDED_TOOLTIP = MSG.chart_metrics_expand_tooltip();

    private final ResourceGroup resourceGroup;
    private EnhancedHLayout expandCollapseHLayout;
    private MetricsGroupTableView metricsTableView;
    private static Integer lastResourceGroupId = 0;

    /**
     * Encapsulate the creation logic and not let it leak out into other objects.
     * Clear the expanded rows set when changing resources as well.
     * @see ExpandedRowsMomento
     * @param group
     * @return MetricsGroupView
     */
    public static MetricsGroupView create(ResourceGroup group ){

        boolean isDifferentResource = (group.getId() != lastResourceGroupId);

        if(isDifferentResource){
            ExpandedRowsMomento.getInstance().clear();
        }

        return  new MetricsGroupView(group,  ExpandedRowsMomento.getInstance().getExpandedRows());

    }

    private MetricsGroupView(ResourceGroup resourceGroup, Set<Integer> expandedRows) {
        super();
        setOverflow(Overflow.AUTO);
        setWidth100();
        setHeight100();
        this.resourceGroup = resourceGroup;
        metricsTableView = new MetricsGroupTableView(resourceGroup, this, expandedRows);

        final MetricAvailabilityView availabilityDetails = new MetricAvailabilityView(resourceGroup.getId());
        availabilityDetails.hide();

        metricsTableView.setHeight100();

        availabilityGraph = AvailabilityD3GraphView.create( new AvailabilityOverUnderGraphType(resourceGroup.getId()));

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
        lastResourceGroupId = resourceGroup.getId();
    }


    private void addAvailabilityGraph() {
            expandCollapseHLayout.removeMember(availabilityGraph);
            availabilityGraph.destroy();

            availabilityGraph = AvailabilityD3GraphView.create(new AvailabilityOverUnderGraphType(resourceGroup.getId()));

            expandCollapseHLayout.addMember(availabilityGraph);

            queryAvailability(EntityContext.forGroup(resourceGroup.getId()), CustomDateRangeState.getInstance().getStartTime(),
                CustomDateRangeState.getInstance().getEndTime(), null);
    }


    @Override
    protected void queryAvailability(final EntityContext context, Long startTime, Long endTime, CountDownLatch notUsed) {

        // now return the availability
        GWTServiceLookup.getAvailabilityService().getAvailabilitiesForResource(context.getGroupId(), startTime,
                endTime, new AsyncCallback<List<Availability>>() {
            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_resource_monitor_availability_loadFailed(), caught);
            }

            @Override
            public void onSuccess(List<Availability> availList) {
                availabilityGraph.setAvailabilityList(availList);
                new Timer() {
                    @Override
                    public void run() {
                        buttonBarDateTimeRangeEditor.updateTimeRangeToNow();
                        availabilityGraph.drawJsniChart();

                    }
                }.schedule(150);
            }
        });
    }

    private void drawAvailabilityGraphAndSparklines() {
        new Timer() {
            @Override
            public void run() {
                availabilityGraph.drawJsniChart();
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);
    }

    @Override
    public void refreshData() {
        addAvailabilityGraph();
        metricsTableView.refresh();
    }

    @Override
    public void onViewRendered() {
        // refresh the graphs on subtab nav because we are a cached view not new
        refreshData();
    }
}
