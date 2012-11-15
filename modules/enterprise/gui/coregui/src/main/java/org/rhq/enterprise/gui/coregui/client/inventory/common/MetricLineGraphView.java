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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.HasD3JsniChart;

/**
 * Contains the chart definition for a Multi-Line graph chart.
 *
 * @author Mike Thompson
 */
public abstract class MetricLineGraphView extends AbstractMetricD3GraphView implements HasD3JsniChart
{
   public MetricLineGraphView(String locatorId){
            super(locatorId);
   }


    public MetricLineGraphView(String locatorId, int entityId, MeasurementDefinition def,
                               List<MeasurementDataNumericHighLowComposite> data) {
        super(locatorId,entityId, def,data);

    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw Line jsni chart");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()(),
            chartHandle = "#rChart-"+chartId,
            chartSelection = chartHandle + " svg",
            json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()()),
            yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()(),
            yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()(),
            xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()();

        console.log("chart id: "+chartSelection );
        console.log(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        //var jsonData = eval([{ x:1352204720548, high:0.016642348035599646, low:0.016642348035599646, y:0.016642348035599646},{ x:1352211680548, high:12.000200003333388, low:0.0, y:3.500050000833347},{ x:1352211920548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352212160548, high:5.0, low:1.999966667222213, y:2.750000000277778},{ x:1352212400548, high:4.0, low:2.0, y:2.5000083334722243},{ x:1352212640548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352212880548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352213120548, high:3.000050000833347, low:1.999966667222213, y:2.2500041672916677},{ x:1352213360548, high:4.0, low:1.999966667222213, y:2.7499916668055535},{ x:1352213600548, high:2.000033333888898, low:1.999966667222213, y:2.000008333750002},{ x:1352213840548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352214080548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352214320548, high:4.0, low:2.0, y:2.5},{ x:1352214560548, high:3.0, low:1.999966667222213, y:2.250000000833347},{ x:1352214800548, high:2.000033333888898, low:1.999966667222213, y:2.000000000277778},{ x:1352215040548, high:4.0, low:2.0, y:2.5},{ x:1352215280548, high:3.0, low:2.0, y:2.2500083334722243},{ x:1352215520548, high:2.0, low:1.999966667222213, y:1.9999916668055533},{ x:1352215760548, high:3.0, low:1.999966667222213, y:2.250000000277778},{ x:1352216000548, high:4.0, low:2.0, y:2.5},{ x:1352216240548, high:2.000066668888963, low:1.999966667222213, y:2.000008334027794},{ x:1352216480548, high:3.0, low:1.999966667222213, y:2.2499916668055535}]);

        draw(json);

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

//d3.select("y axis").append().text(" Metric Label")
//        .attr("transform", "rotate (90, "+margin.right + ", 0)")
//        .attr("x",20).attr("y",0);

            var interpolation = "basis";
            //var interpolation = "step-after";

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

            var svg = $wnd.d3.select(chartSelection).append("svg")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    .append("g")
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
                    .text(yAxisUnits !== "" ? "("+yAxisUnits+")" : "");

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

//            svg.selectAll("circle")
//                    .data(data)
//                    .enter()
//                    .append("circle")
//                    .attr("class", "tooltip")
//                    .attr("cx", function(d){ return timeScale(d.x);  })
//                    .attr("cy", function(d){ return yScale(d.y);  })
//                    .attr("r", 2)
//                    .attr("stroke", "black")
//                    .attr("fill", "none");

//            svg.selectAll("circle")
//                    .on("mouseover",  function(d){
//                        $wnd.d3.select(this)
//                                .transition().attr("r",7).attr("stroke", "red").attr("fill","red");
//                    })
//                    .on("mouseout",  function(d){
//                        $wnd.d3.select(this)
//                                .transition().attr("r",2).attr("stroke","black").attr("fill","none");
//                    });
//
//            svg.selectAll("circle")
//                    .on("mouseover.tooltip", function(d){
//               $wnd.d3.select("text#" + d.x).remove();
//               $wnd.d3.select(chartHandle)
//                       .append("text")
//                       .text("Value: "+ d.y)
//                       .attr("x", timeScale(d.x) + 10)
//                       .attr("y", yScale(d.y) - 10)
//                       .attr("id", d.x);
//            });
//            svg.selectAll("circle")
//                    .on("mouseout.tooltip", function(d){
//                        $wnd.d3.select("text#" + d.x)
//                                .transition()
//                                .duration(500)
//                                .style("opacity",0)
//                                .style("transform","translate(10, -10)")
//                                .remove();
//                    });

            console.log("finished paths");
        }

    }-*/;

}
