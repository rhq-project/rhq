/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;



/**
 * A MultiLine version of the Composite group single metric multiple resource charts.
 *
 * @author Mike Thompson
 */
@Deprecated
public final class CompositeGroupNvD3MultiLineGraph extends CompositeGroupD3GraphListView
{

    public CompositeGroupNvD3MultiLineGraph(int groupId, int defId, boolean isAutogroup)
    {
        super(groupId, defId, isAutogroup);
    }



    @Override
    public native void drawJsniChart() /*-{
        console.log("Draw nvd3 charts for composite multiline graph");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartId()(),
            chartHandle = "#mChart-"+chartId,
            chartSelection = chartHandle + " svg",
            yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getYAxisUnits()(),
            xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTitle()(),
            xAxisTimeFormat =  this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3MultiLineGraph::getXAxisTimeFormatHoursMinutes()();
            json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getJsonMetrics()());

        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.lineChart();

            chart.xAxis.axisLabel(xAxisLabel)
                    .tickFormat(function(d) { return $wnd.d3.time.format(xAxisTimeFormat)(new Date(d)) });

            chart.yAxis
                    .axisLabel(yAxisUnits)
                    .tickFormat($wnd.d3.format('.02f'));

            $wnd.d3.select(chartSelection)
                    .datum(json)
                    .transition().duration(300)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            return chart;
        });

    }-*/;



}
