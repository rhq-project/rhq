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
 * Contains the chart definition for a Area/Bar graph chart.
 *
 * @author Mike Thompson
 */
public abstract class MetricAreaBarGraphView extends AbstractMetricD3GraphView implements HasD3JsniChart
{
   public MetricAreaBarGraphView(String locatorId){
            super(locatorId);
   }


    public MetricAreaBarGraphView(String locatorId, int entityId, MeasurementDefinition def,
                                  List<MeasurementDataNumericHighLowComposite> data) {
        super(locatorId,entityId, def,data);

    }

    /**
     * The magic JSNI to draw the charts with $wnd.d3.
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


        function draw(data) {
            "use strict";

            var margin = {top:10, right:5, bottom:30, left:70},
                    width = 500 - margin.left - margin.right,
                    height = 150 - margin.top - margin.bottom;

            var avg = $wnd.d3.mean(data.map(function (d) {
                return d.y;
            }));
            var peak = $wnd.d3.max(data.map(function (d) {
                return d.high;
            }));
            var min = $wnd.d3.min(data.map(function (d) {
                return d.low;
            }));

            var timeScale = $wnd.d3.time.scale()
                    .range([0, width])
                    .domain($wnd.d3.extent(data, function (d) {
                        return d.x;
                    }));

            // adjust the min scale so blue low line is not in axis
            var lowBound = min - ((peak - min)* 0.1);
            var highBound = peak + ((peak - min)* 0.1);

            var yScale = $wnd.d3.scale.linear()
                    .rangeRound([height, 0])
                    .domain([lowBound,highBound]);

            var xAxis = $wnd.d3.svg.axis()
                    .scale(timeScale)
                    .ticks(5)
                    .tickSubdivide(5)
                    .orient("bottom");

            var yAxis = $wnd.d3.svg.axis()
                    .scale(yScale)
                    .ticks(5)
                    .orient("left");

            var interpolation = "basis";
            var avgLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((avg));
                    });
            var peakLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((peak));
                    });
            var minLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((min));
                    });
            var line = $wnd.d3.svg.line()
                    .interpolate("linear")
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale((+d.y));
                    });

            var highLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale(+d.high);
                    });

            var lowLine = $wnd.d3.svg.line()
                    .interpolate(interpolation)
                    .x(function (d) {
                        return timeScale(d.x);
                    })
                    .y(function (d) {
                        return yScale(+d.low);
                    });

            var areaHigh = $wnd.d3.svg.area()
                    .defined(function(d) { return !isNaN(d.y); })
                    .x(function(d) { return timeScale(d.x); })
                    .y0(function(d) { return yScale(+d.y); })
                    .y1(function(d) { return yScale(+d.high); });

            var areaLow = $wnd.d3.svg.area()
                    .x(function(d) { return timeScale(d.x); })
                    .y0(function(d) { return yScale(+d.low); })
                    .y1(function(d) { return yScale(+d.y); });

            // create the actual chart group
            var chart = $wnd.d3.select(chartSelection);

            // add the gradient background
//    chart.append("rect")
//            .attr("class", "frame")
//            .attr("x", margin.left)
//            .attr("y", margin.top)
//            .attr("height", height)
//            .attr("width", width+10)
//            .attr("fill", "url(#gradBackground)");

            var svg = chart.append("g")
                    .attr("width", width + margin.left + margin.right)
                    .attr("height", height + margin.top + margin.bottom)
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // The bars of the bar graph
            svg.selectAll("rect.bar")
                    .data(data)
                    .enter().append("rect")
                    .attr("class", "bar")
                    .attr("x", function (d) {
                        return timeScale(d.x);
                    })
                    .attr("y", function (d) {
                        return yScale(d.high);
                    })
                    .attr("height", function (d) {
                        return height - yScale(d.high);
                    })
                    .attr("width", function (d) {
                        return  (width / data.length -12  );
                    })
                    .attr("data-rhq-value", function (d) {
                        return d.y;
                    })
                    .attr("data-rhq-high-value", function (d) {
                        return d.high;
                    })
                    .attr("data-rhq-low-value", function (d) {
                        return d.low;
                    })
                    .attr("data-rhq-time", function (d) {
                        var myDate = new Date(d.x);
                        return myDate.getHours() + ":" + myDate.getMinutes();
                    })
                //.attr("opacity", 0.3)
                    .attr("fill", "#dadde0");


            svg.append("path")
                    .datum(data)
                    .attr("class", "areaHigh")
                    .attr("fill", "#41cdfb")
                    .attr("opacity", ".4")
                    .attr("stroke", "black")
                    .attr("stroke-width", ".5")
                    .attr("stroke-opacity", ".6")
                    .attr("d", areaHigh);

            svg.append("path")
                    .datum(data)
                    .attr("class", "areaLow")
                    .attr("opacity", ".4")
                    .attr("fill", "#075ef4")
                    .attr("stroke", "black")
                    .attr("stroke-width", ".5")
                    .attr("stroke-opacity", ".6")
                    .attr("d", areaLow);


            // create x-axis
            svg.append("g")
                    .attr("class", "x axis")
                    .attr("transform", "translate(0," + height + ")")
                    .call(xAxis);


            // create y-axis
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




//    var radius = 2;
//    var edgeToCenter = 2;


            // The bars of the bar graph
//    svg.selectAll("rect.candle")
//            .data(data)
//            .enter().append("rect")
//            .attr("class", "candle")
//            .attr("x", function (d) {
//                return timeScale(d.x);
//            })
//            .attr("y", function (d) {
//                return yScale(d.high);
//            })
//            .attr("height", function (d) {
//                return (height - yScale(d.high)) - (height - yScale(d.low));
//            })
//            .attr("width", function (d) {
//                return  (width / data.length -12  );
//            })
//            .attr("data-rhq-value", function (d) {
//                return d.y;
//            })
//            .attr("data-rhq-high-value", function (d) {
//                return d.high;
//            })
//            .attr("data-rhq-low-value", function (d) {
//                return d.low;
//            })
//            .attr("data-rhq-time", function (d) {
//                var myDate = new Date(d.x);
//                return myDate.getHours() + ":" + myDate.getMinutes();
//            })
//            .attr("opacity", 0.6)
//            .attr("fill", "grey");



            // peak Line (must be before line.high to look right
            svg.append("path")
                    .datum(data)
                    .attr("class", "peakLine")
                    .attr("fill", "none")
                    .attr("stroke", "red")
                    .attr("stroke-width", "2")
                //.attr("stroke-dasharray", "5,5")
                    .attr("stroke-dasharray", "20,10,5,5,5,10")
                    .attr("stroke-opacity", ".4")
                    .attr("d", peakLine);

            // min Line
            svg.append("path")
                    .datum(data)
                    .attr("class", "minLine")
                    .attr("fill", "none")
                    .attr("stroke", "steelblue")
                    .attr("stroke-width", "2")
                //.attr("stroke-dasharray", "5,5")
                    .attr("stroke-dasharray", "20,10,5,5,5,10")
                    .attr("stroke-opacity", ".6")
                    .attr("d", minLine);

            svg.append("path")
                    .datum(data)
                    .attr("class", "avgLine")
                    .attr("fill", "none")
                    .attr("stroke", "green")
                    .attr("stroke-width", "2")
                    .attr("stroke-dasharray", "5,5")
                    .attr("stroke-opacity", ".6")
                    .attr("d", avgLine);

            svg.append("path")
                    .datum(data)
                    .attr("class", "yLine")
                    .attr("fill", "none")
                    .attr("stroke", "black")
                    .attr("stroke-width", "2")
                    .attr("stroke-opacity", ".6")
                    .attr("d", line);


//    svg.selectAll("line.middleWhisker")
//            .data(data)
//            .enter().append("line")
//            .attr("class", "middleWhisker")
//            .attr("x1", function (d) {
//                return timeScale(d.x);
//            })
//            .attr("x2", function (d) {
//                return timeScale(d.x) + ( 4 * edgeToCenter);
//            })
//            .attr("y1", function (d) {
//                return yScale(d.y);
//            })
//            .attr("y2", function (d) {
//                return yScale(d.y);
//            })
//            .attr("stroke-width", "2")
//            .attr("stroke-opacity", ".5")
//            .attr("stroke", "black");

//    svg.selectAll("line.highWhisker")
//            .data(data)
//            .enter().append("line")
//            .attr("class", "highWhisker")
//            .attr("x1", function (d) {
//                return timeScale(d.x);
//            })
//            .attr("x2", function (d) {
//                return timeScale(d.x) + ( 4 * edgeToCenter);
//            })
//            .attr("y1", function (d) {
//                return yScale(d.high);
//            })
//            .attr("y2", function (d) {
//                return yScale(d.high);
//            })
//            .attr("stroke-width", "1")
//            .attr("stroke-opacity", ".7")
//            .attr("stroke", "magenta");

//    svg.selectAll("line.lowWhisker")
//            .data(data)
//            .enter().append("line")
//            .attr("class", "lowWhisker")
//            .attr("x1", function (d) {
//                return timeScale(d.x);
//            })
//            .attr("x2", function (d) {
//                return timeScale(d.x) + ( 4 * edgeToCenter);
//            })
//            .attr("y1", function (d) {
//                return yScale(d.low);
//            })
//            .attr("y2", function (d) {
//                return yScale(d.low);
//            })
//            .attr("stroke-width", "1")
//            .attr("stroke-opacity", ".7")
//            .attr("stroke", "magenta");


            // Whisker stem
//    svg.selectAll("line.stem")
//            .data(data)
//            .enter().append("line")
//            .attr("class", "stem")
//            .attr("x1", function (d) {
//                return timeScale(d.x) + (2 * edgeToCenter);
//            })
//            .attr("x2", function (d) {
//                return timeScale(d.x) + (2 * edgeToCenter);
//            })
//            .attr("y1", function (d) {
//                return yScale(d.high) -2;
//            })
//            .attr("y2", function (d) {
//                return yScale(d.low) +2;
//            })
//            .attr("stroke-width", "3")
//            .attr("stroke-opacity", ".7")
//            //.attr("stroke-dasharray", "2,2")
//            .attr("stroke", "magenta");
//

//    svg.selectAll("line.base.stem")
//            .data(data)
//            .enter().append("line")
//            .attr("class", "base.stem")
//            .attr("x1", function (d) {
//                return timeScale(d.x) + (2 * edgeToCenter);
//            })
//            .attr("x2", function (d) {
//                return timeScale(d.x) + (2 * edgeToCenter);
//            })
//            .attr("y1", function (d) {
//                return yScale(d.low) +2;
//            })
//            .attr("y2", function (d) {
//                return height ;
//            })
//            .attr("stroke-opacity", function (d,i) {
//                return i % 5 == 0 ? 1 : .6;
//            })
//            .attr("stroke", function (d,i) {
//                return i % 5 == 0 ? "#919191" : "#cccccc";
//            })
//            .attr("stroke-width", "1");



            console.log("finished drawing paths");
        }
     draw(json);
    }-*/;

}
