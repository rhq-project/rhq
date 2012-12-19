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
public final class CompositeGroupMultiLineGraphListView extends CompositeGroupD3GraphListView
{

    public CompositeGroupMultiLineGraphListView(String locatorId, int groupId, int defId)
    {
        super(locatorId, groupId, defId);
    }



    @Override
    public native void drawJsniChart() /*-{
        console.log("Draw nvd3 charts for composite multiline graph");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartId()(),
            chartHandle = "#mChart-"+chartId,
            chartSelection = chartHandle + " svg",
    //        yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getYAxisTitle()(),
            yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getYAxisUnits()(),
            xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTitle()(),
            displayDayOfWeek = this.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::shouldDisplayDayOfWeekInXAxisLabel()(),
            xAxisTimeFormat = (displayDayOfWeek) ? "%a %I %p" : "%I %p",
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


    public CompositeGroupMultiLineGraphListView getInstance(String locatorId, int groupId, int definitionId) {
        return new CompositeGroupMultiLineGraphListView(locatorId, groupId, definitionId);
    }

}
