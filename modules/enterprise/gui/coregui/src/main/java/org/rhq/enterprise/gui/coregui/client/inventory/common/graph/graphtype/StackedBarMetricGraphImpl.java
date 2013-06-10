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

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 * The purpose of this class is the fill the div element with id: '#rChart-[resourceId]-[metricId]'.
 * with SVG instructions to draw a d3 graph (in this case a stacked bar graph).
 *
 * @author Mike Thompson
 */
public class StackedBarMetricGraphImpl extends AbstractMetricGraph {

    /**
     * GWT constructor for GWT.create() deferred instantiation.
     */
    public StackedBarMetricGraphImpl() {
        super();
    }


    /**
     * The magic JSNI to draw the charts with $wnd.d3.js
     */
    @Override
    public native void drawJsniChart() /*-{
        console.log("Draw Stacked Bar jsni chart");
        var global = this,

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
                chartContext = new $wnd.ChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartId()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHeight()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getJsonMetrics()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getXAxisTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getYAxisUnits()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitleMinLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitleAvgLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitlePeakLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartDateLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTimeLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartDownLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartUnknownLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartNoDataLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverStartLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverEndLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverPeriodLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverBarLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverDateFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::isPortalGraph()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::getPortalId()()
                );


        // Define the Stacked Bar Graph function using the module pattern
        var metricStackedBarGraph = function () {
            "use strict";
            // privates
            var margin = {top: 10, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    adjustedChartHeight = chartContext.chartHeight - 50,
                    height = adjustedChartHeight - margin.top - margin.bottom,
                    smallChartThresholdInPixels = 600,
                    titleHeight = 30, titleSpace = 10,
                    barOffset = 2,
                    chartData,
                    interpolation = "basis",
                    avgFiltered, avg, minFiltered, min, peakFiltered, peak,
                    oobMax,
                    legendDefined,
                    lowBound,
                    highBound,
                    calcBarWidth,
                    yScale,
                    yAxis,
                    timeScale,
                    xAxis,
                    chart,
                    svg;


            function getChartWidth() {
                return $wnd.jQuery("#" + chartContext.chartHandle).width();
            }

            function useSmallCharts() {
                return  getChartWidth() <= smallChartThresholdInPixels;
            }

            function determineScale() {
                var xTicks, xTickSubDivide, numberOfBarsForSmallGraph = 20;
                if (chartContext.data.length > 0) {

                    // if window is too small server up small chart
                    if (useSmallCharts()) {
                        console.log("Using Small Charts Profile for width: "+getChartWidth());
                        width = 250;
                        xTicks = 3;
                        xTickSubDivide = 2;
                        chartData = chartContext.data.slice(chartContext.data.length - numberOfBarsForSmallGraph, chartContext.data.length - 1);
                    }
                    else {
                        console.log("Using Large Charts Profile");
                        //  we use the width already defined above
                        xTicks = 8;
                        xTickSubDivide = 5;
                        chartData = chartContext.data;
                    }

                    avgFiltered = chartContext.data.filter(function (d) {
                        if (d.nodata !== 'true') {
                            return d.y;
                        }
                    });
                    avg = $wnd.d3.mean(avgFiltered.map(function (d) {
                        return d.y;
                    }));
                    peakFiltered = chartContext.data.filter(function (d) {
                        if (d.nodata !== 'true') {
                            return d.high;
                        }
                    });
                    peak = $wnd.d3.max(peakFiltered.map(function (d) {
                            return d.high;
                    }));
                    minFiltered = chartContext.data.filter(function (d) {
                        if (d.nodata !== 'true') {
                            return d.low;
                        }
                    });
                    min = $wnd.d3.min(minFiltered.map(function (d) {
                            return d.low;
                    }));
                    highBound = peak + ((peak - min) * 0.1);
                    oobMax = $wnd.d3.max(chartContext.data.map(function (d) {
                        if (d.baselineMax == undefined) {
                            return 0;
                        }
                        else {
                            return +d.baselineMax;
                        }
                    }));
                    calcBarWidth = function () {
                        return (width / chartData.length - barOffset  )
                    };

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([$wnd.d3.min(chartContext.data, function(d) {return d.low;}),$wnd.d3.max(chartContext.data, function(d) {return d.high;})]);

                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(1)
                            .ticks(5)
                            .tickSize(4, 4, 0)
                            .orient("left");


                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(chartData, function (d) {
                                return d.x;
                            }));

                    xAxis = $wnd.d3.svg.axis()
                            .scale(timeScale)
                            .ticks(xTicks)
                            .tickSubdivide(xTickSubDivide)
                            .tickSize(4, 4, 0)
                            .orient("bottom");

                    // create the actual chart group
                    chart = $wnd.d3.select("#" + chartContext.chartSelection);

                    svg = chart.append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top - titleHeight - titleSpace + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + (+titleHeight + titleSpace + margin.top) + ")");

                    legendDefined = (typeof min !== "undefined") || (typeof avg !== "undefined") || (typeof peak !== "undefined");
                    if (!useSmallCharts() && legendDefined) {
                        createMinAvgPeakSidePanel(chartContext.minChartTitle, min, chartContext.avgChartTitle, avg, chartContext.peakChartTitle, peak, chartContext.yAxisUnits);
                    }
                }

            }


            function createMinAvgPeakSidePanel(minLabel, minValue, avgLabel, avgValue, highLabel, highValue, uom) {
                var xLabel = 772,
                        xValue = 820,
                        yBase = 100,
                        yInc = 25,
                        decimalPlaces = 0;

                // title/header
                chart.append("g").append("rect")
                        .attr("class", "rightSidePanel")
                        .attr("x", xLabel - 10)
                        .attr("y", margin.top + 70)
                        .attr("rx", 10)
                        .attr("ry", 10)
                        .attr("height", 80)
                        .attr("width", 135)
                        .attr("opacity", "0.3")
                        .attr("fill", "#E8E8E8");

                // high
                chart.append("text")
                        .attr("class", "highLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase)
                        .text(highLabel + " - ");

                chart.append("text")
                        .attr("class", "highText")
                        .attr("x", xValue)
                        .attr("y", yBase)
                        .text(highValue.toFixed(decimalPlaces) + " " + uom);


                //avg
                chart.append("text")
                        .attr("class", "avgLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + yInc)
                        .text(avgLabel + " - ");

                chart.append("text")
                        .attr("class", "avgText")
                        .attr("x", xValue)
                        .attr("y", yBase + yInc)
                        .text(avgValue.toFixed(decimalPlaces) + " " + uom);

                // min
                chart.append("text")
                        .attr("class", "minLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + 2 * yInc)
                        .text(minLabel + " - ");

                chart.append("text")
                        .attr("class", "minText")
                        .attr("x", xValue)
                        .attr("y", yBase + 2 * yInc)
                        .text(minValue.toFixed(decimalPlaces) + " " + uom);


            }

            function createHeader(titleName) {
                var title = chart.append("g").append("rect")
                        .attr("class", "title")
                        .attr("x", 10)
                        .attr("y", margin.top)
                        .attr("height", titleHeight)
                        .attr("width", width + 30 + margin.left)
                        .attr("fill", "none");

                chart.append("text")
                        .attr("class", "titleName")
                        .attr("x", 40)
                        .attr("y", 37)
                        .attr("font-size", "12")
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(titleName)
                        .attr("fill", "#003168");

                return title;

            }

            function createStackedBars() {

                var pixelsOffHeight = 0;

                // The gray bars at the bottom leading up
                svg.selectAll("rect.leaderBar")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "leaderBar")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return yScale(highBound);
                            }
                            else {
                                return yScale(d.low);
                            }
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(highBound) - pixelsOffHeight;
                            }
                            else {
                                return height - yScale(d.low) - pixelsOffHeight;
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })

                        .attr("opacity", ".9")
                        .attr("fill", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return  "url(#noDataStripes)";
                            }
                            else {
                                return  "#d3d3d6";
                            }
                        });


                // upper portion representing avg to high
                svg.selectAll("rect.high")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "high")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.high) ? yScale(lowBound) : yScale(d.high);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.y) - yScale(d.high);
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("data-rhq-value", function (d) {
                            return d.y;
                        })
                        .attr("opacity", 0.9);


                // lower portion representing avg to low
                svg.selectAll("rect.low")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "low")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.low) - yScale(d.y);
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("opacity", 0.9);

                // if high == low put a "cap" on the bar to show non-aggregated bar
                svg.selectAll("rect.singleValue")
                        .data(chartData)
                        .enter().append("rect")
                        .attr("class", "singleValue")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y) - 2;
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                if (d.low === d.high) {
                                    return  yScale(d.low) - yScale(d.y) + 2;
                                }
                                else {
                                    return  yScale(d.low) - yScale(d.y);
                                }
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", function (d) {
                            if (d.low === d.high) {
                                return  "#50505a";
                            }
                            else {
                                return  "#70c4e2";
                            }
                        });
            }

            function createYAxisGridLines() {
                // create the y axis grid lines
                svg.append("g").classed("grid y_grid", true)
                        .call($wnd.d3.svg.axis()
                                .scale(yScale)
                                .orient("left")
                                .ticks(10)
                                .tickSize(-width, 0, 0)
                                .tickFormat("")
                        );
            }

            function createXandYAxes() {

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .call(xAxis);


                // create y-axis
                svg.append("g")
                        .attr("class", "y axis")
                        .call(yAxis)
                        .append("text")
                        .attr("transform", "rotate(-90),translate( -60,0)")
                        .attr("y", -30)
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .text(chartContext.yAxisUnits === "NONE" ? "" : chartContext.yAxisUnits);

            }

            function createAvgLines() {
                var showBarAvgTrendline =
                                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::showBarAvgTrendLine()(),
                        barAvgLine = $wnd.d3.svg.line()
                                .interpolate("linear")
                                .defined(function (d) {
                                    return !d.nodata;
                                })
                                .x(function (d) {
                                    return timeScale(d.x) + (calcBarWidth() / 2);
                                })
                                .y(function (d) {
                                    if (showBarAvgTrendline) {
                                        return yScale(d.y);
                                    }
                                    else {
                                        return NaN;
                                    }
                                });

                // Bar avg line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "barAvgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#2e376a")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-opacity", ".7")
                        .attr("d", barAvgLine);

            }

            function createOOBLines() {

                var minBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMin);
                                }),
                        maxBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMax);
                                });

                // min baseline Line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "minBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "purple")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minBaselineLine);

                // max baseline Line
                svg.append("path")
                        .datum(chartData)
                        .attr("class", "maxBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "orange")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".7")
                        .attr("d", maxBaselineLine);

            }

            function createBrush(){
                // slightly modified but originally from crossfilter (http://square.github.com/crossfilter/)
              var resizePath = function (d) {
                      var e1 = +(d === "e"),
                           e = e1 ? 1 : 0 ,
                            x = e ? 1 : -1,
                            y = height / 3;
                    return "M" + (.5 * x) + "," + y
                            + "A6,6 0 0 " + e + " " + (6.5 * x) + "," + (y + 6)
                            + "V" + (2 * y - 6)
                            + "A6,6 0 0 " + e + " " + (.5 * x) + "," + (2 * y)
                            + "Z"
                            + "M" + (2.5 * x) + "," + (y + 8)
                            + "V" + (2 * y - 8)
                            + "M" + (4.5 * x) + "," + (y + 8)
                            + "V" + (2 * y - 8);
                },
                brush = $wnd.d3.svg.brush()
                        .x(timeScale)
                        .extent($wnd.d3.extent(chartData, function (d) {
                            return d.x;
                        }))
                        .on("brushstart", brushstart)
                        .on("brush", brushmove)
                        .on("brushend", brushend),
                brushg = svg.append("g")
                        .attr("class", "brush")
                        .call(brush);

                brushg.selectAll(".resize").append("path")
                        .attr("d", resizePath);

                brushg.selectAll("rect")
                        .attr("height", height);

                function brushstart() {
                    svg.classed("selecting", true);
                }

                function brushmove() {
                    var s = brush.extent();
                    updateDateRangeDisplay($wnd.moment(s[0]), $wnd.moment(s[1]));
                }

                function brushend() {
                    var s = brush.extent();
                    svg.classed("selecting", !$wnd.d3.event.target.empty());
                    //global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::saveDateRange(DD)($wnd.moment(s[0]).unix(),$wnd.moment(s[0]).unix());
                    //$wnd.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.D3GraphListView::redrawGraphs()();
                    global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AbstractMetricGraph::redrawGraphs()();
                }

                function updateDateRangeDisplay(startDate, endDate ) {
                    //@todo: i18n the date format
                    var formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + '  -  ' + endDate.format('MM/DD/YYYY h:mm a');
                    $wnd.jQuery('.graphDateTimeRangeLabel').text(formattedDateRange);
                }


            }

            function formatHovers(chartContext, d) {
                var hoverString,
                        xValue = (d.x == undefined) ? 0 : +d.x,
                        date = new Date(+xValue),
                        barDuration = d.barDuration,
                        timeFormatter = $wnd.d3.time.format(chartContext.chartHoverTimeFormat),
                        dateFormatter = $wnd.d3.time.format(chartContext.chartHoverDateFormat),
                        highValue = d.high.toFixed(2),
                        lowValue = d.low.toFixed(2),
                        avgValue = d.y.toFixed(2);


                // our special condition to indicate no data because avg lines dont like discontinuous data
                if (d.y === 0 && d.high === 0 && d.low === 0) {
                    // no data
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel" >' + chartContext.timeLabel + ': </span>' + timeFormatter(date) +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr class="chartHoverDivider" ></hr>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">' + chartContext.noDataLabel + '</span></div>' +
                                    '</div>';


                }
                else {
                    // regular bar hover
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + chartContext.timeLabel + ':  </span><span style="width:50px;">' + timeFormatter(date) + '</span>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ':  </span><span style="width:50px;">' + dateFormatter(date) + '</span></div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverLabelSpan">' + chartContext.hoverBarLabel + ": " + barDuration + '</span></div>' +
                                    '<hr  class="chartHoverDivider"></hr>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverPeakValue" >' + chartContext.peakChartTitle + ': </span><span style="width:50px;">' + highValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverAvgValue" >' + chartContext.avgChartTitle + ':  </span><span style="width:50px;">' + avgValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverLowValue" >' + chartContext.minChartTitle + ': </span><span style="width:50px;">' + lowValue + '</span></div>' +
                                    '</div>';
                }
                return hoverString;

            }

            function createHovers(chartContext) {
                $wnd.jQuery('svg rect.leaderBar, svg rect.high, svg rect.low, svg rect.singleValue').tipsy({
                    gravity: 'w',
                    html: true,
                    trigger: 'hover',
                    title: function () {
                        var d = this.__data__;
                        return formatHovers(chartContext, d);
                    },
                    show: function (e, el) {
                        el.css({ 'z-index': '990000'})
                    }
                });
            }

            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    // Guard condition that can occur when a portlet has not been configured yet
                    if (chartContext.data.length > 0) {
                        console.log("Creating Chart: "+ chartContext.chartSelection + " --> "+ chartContext.chartTitle);

                        determineScale();
                        createHeader(chartContext.chartTitle);

                        createYAxisGridLines();
                        createStackedBars();
                        createXandYAxes();
                        createAvgLines();
                        if (oobMax > 0) {
                            console.log("OOB Data Exists!");
                            createOOBLines();
                        }
                        createHovers(chartContext);
                        createBrush();
                    }
                }
            }; // end public closure
        }();

        if(chartContext.data !== undefined && chartContext.data.length > 0){
            metricStackedBarGraph.draw(chartContext);
        }

    }-*/;

}
