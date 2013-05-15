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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

/**
 * Contains the chart definition for a MultiLine Graph.
 *
 * @author Mike Thompson
 */
public final class MetricNvd3MultiLineGraph extends AbstractGraph
{
    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     */
    public MetricNvd3MultiLineGraph(MetricGraphData metricGraphData){
        setMetricGraphData(metricGraphData);
    }


    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw NVD3 Bar jsni chart");
        console.time("multiChart");
        var global = this,
            chartId =  global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartId()(),
            chartHandle = "#rChart-"+chartId,
            chartSelection = chartHandle + " svg",
            json = $wnd.jQuery.parseJSON(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getJsonMetrics()()),
            yAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartTitle()(),
            yAxisUnits = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getYAxisUnits()(),
            xAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getXAxisTitle()(),
            displayDayOfWeek = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::shouldDisplayDayOfWeekInXAxisLabel()(),
            xAxisTimeFormat = (displayDayOfWeek) ? "%a %I %p" : "%I : %M %p",

        // nvd3 defines their json models a standard way (same model for other graphs)
        data = function() {
            return [
                {
                    values: json,
                    key: yAxisLabel ,
                    color: '#ff7f0e'
                }
            ];
        };
        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.multiBarChart()
                    .showControls(true)
                    .tooltips(true);

            chart.xAxis.axisLabel(xAxisLabel)
                    .tickFormat(function(d) { return $wnd.d3.time.format(xAxisTimeFormat)(new Date(d)) });

            chart.yAxis
                    .axisLabel(yAxisUnits)
                    .tickFormat($wnd.d3.format(',f'));

            $wnd.d3.select(chartSelection)
                    .datum(data())
                    .transition().duration(300)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            return chart;
        });
        console.timeEnd("multiChart")

    }-*/;

}
