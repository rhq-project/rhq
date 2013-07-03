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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype;

import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData;

/**
 * Contains the javascript chart definition for a d3 Line graph chart.
 * NOTE: this class isn't used just provided as an example as how to create
 * other graph types.
 *
 * @author Mike Thompson
 */
public final class LineMetricGraph extends AbstractMetricGraph {

    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     */
    public LineMetricGraph(MetricGraphData metricGraphData) {
        setMetricGraphData(metricGraphData);
    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
       console.log("Draw Metric Line jsni chart");
       var global = this,
       chartId =  global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getChartId()(),
       chartHandle = "#rChart-"+chartId,
       chartSelection = chartHandle + " svg",
       json = $wnd.jQuery.parseJSON(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getJsonMetrics()()),
       yAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getChartTitle()(),
       yAxisUnits = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getYAxisUnits()(),
       xAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getXAxisTitle()();

       console.log("chart id: "+chartSelection );
       console.log(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData::getJsonMetrics()());


       function draw(data){
       "use strict";

       var margin = {top: 10, right: 5, bottom: 30, left: 70},
       width = 400 - margin.left - margin.right,
       height = 150 - margin.top - margin.bottom;

               var timeScale = $wnd.d3.time.scale()
               .range([0, width])
               .domain($wnd.d3.extent(data, function(d) { return d.x; }));

               var yScale = $wnd.d3.scale.linear()
               .rangeRound([height, 0])
               .domain([$wnd.d3.min(data.map(function(x) {return x.low;})), $wnd.d3.max(data.map(function(x){return x.high;}))]);

               var xAxis = $wnd.d3.svg.axis()
               .scale(timeScale)
               .ticks(5)
               .orient("bottom");

               var yAxis = $wnd.d3.svg.axis()
               .scale(yScale)
               .ticks(5)
               .orient("left");

               var interpolation = "basis";

               var line = $wnd.d3.svg.line()
               .interpolate(interpolation)
               .x(function(d) { return timeScale(d.x); })
               .y(function(d) { return yScale(+d.y); });

               var highLine = $wnd.d3.svg.line()
               .interpolate(interpolation)
               .x(function(d) { return timeScale(d.x); })
               .y(function(d) { return yScale(+d.high); });

               var lowLine = $wnd.d3.svg.line()
               .interpolate(interpolation)
               .x(function(d) { return timeScale(d.x); })
               .y(function(d) { return yScale(+d.low); });

               var svg = $wnd.d3.select(chartSelection).append("g")
               .attr("width", width + margin.left + margin.right)
               .attr("height", height + margin.top + margin.bottom)
               .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

               svg.append("g")
               .attr("class", "x axis")
               .attr("transform", "translate(0," + height + ")")
               .call(xAxis);


               svg.append("g")
               .attr("class", "y axis")
               .call(yAxis)
               .append("text")
               .attr("transform", "rotate(-90)")
               .attr("y", -60)
               .attr("dy", ".71em")
               .style("text-anchor", "end")
               .text(yAxisUnits === "NONE" ? "" : yAxisUnits);

               console.log("finished axes");

               svg.append("path")
               .datum(data)
               .attr("class", "line")
               .attr("fill", "none")
               .attr("stroke", "steelblue")
               .attr("stroke-width", "2")
               .attr("d", line);

               svg.append("path")
               .datum(data)
               .attr("class", "highLine")
               .attr("fill", "none")
               .attr("stroke", "red")
               .attr("stroke-width", "1.5")
               //.attr("stroke-dasharray", "20,10,5,5,5,10")
               .attr("stroke-dasharray", "5,5")
               .attr("stroke-opacity", ".3")
               .attr("d", highLine);

               svg.append("path")
               .datum(data)
               .attr("class", "lowLine")
               .attr("fill", "none")
               .attr("stroke", "blue")
               .attr("stroke-width", "1.5")
               .attr("stroke-dasharray", "5,5")
               .attr("stroke-opacity", ".3")
               .attr("d", lowLine);

               console.log("finished paths");
               }(data);

                                       }-*/;

}
