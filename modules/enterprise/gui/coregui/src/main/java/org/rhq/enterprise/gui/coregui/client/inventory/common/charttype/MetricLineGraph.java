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
 * Contains the javascript chart definition for a d3 Line graph chart.
 *
 * @author Mike Thompson
 */
public final class MetricLineGraph extends MetricGraphData implements HasD3JsniChart
{
    /**
     * Constructor for dashboard portlet view as chart definition and data are deferred to later
     * in the portlet configuration.
     *
     * @param locatorId
     */
    public MetricLineGraph(String locatorId) {
       //super(locatorId);
    }


    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     */
    public MetricLineGraph(MetricGraphData metricGraphData){
        super(metricGraphData.getEntityId(), metricGraphData.getEntityName(),metricGraphData.getDefinition(),metricGraphData.getMetricData());
    }


    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw Metric Line jsni chart");
        var global = this,
            chartId =  global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getChartId()(),
            chartHandle = "#rChart-"+chartId,
            chartSelection = chartHandle + " svg",
            json = eval(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getJsonMetrics()()),
            yAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getYAxisTitle()(),
            yAxisUnits = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getYAxisUnits()(),
            xAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getXAxisTitle()();

        console.log("chart id: "+chartSelection );
        console.log(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData::getJsonMetrics()());

        //var jsonData = eval([{ x:1352204720548, high:0.016642348035599646, low:0.016642348035599646, y:0.016642348035599646},{ x:1352211680548, high:12.000200003333388, low:0.0, y:3.500050000833347},{ x:1352211920548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352212160548, high:5.0, low:1.999966667222213, y:2.750000000277778},{ x:1352212400548, high:4.0, low:2.0, y:2.5000083334722243},{ x:1352212640548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352212880548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352213120548, high:3.000050000833347, low:1.999966667222213, y:2.2500041672916677},{ x:1352213360548, high:4.0, low:1.999966667222213, y:2.7499916668055535},{ x:1352213600548, high:2.000033333888898, low:1.999966667222213, y:2.000008333750002},{ x:1352213840548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352214080548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352214320548, high:4.0, low:2.0, y:2.5},{ x:1352214560548, high:3.0, low:1.999966667222213, y:2.250000000833347},{ x:1352214800548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352215040548, high:4.0, low:2.0, y:2.5},{ x:1352215280548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352215520548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352215760548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352216000548, high:4.0, low:2.0, y:2.5},{ x:1352216240548, high:2.000066668888963, low:1.999966667222213, y:2.000008334027794},{ x:1352216480548, high:3.0, low:1.999966667222213, y:2.2499916668055535}]);


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
