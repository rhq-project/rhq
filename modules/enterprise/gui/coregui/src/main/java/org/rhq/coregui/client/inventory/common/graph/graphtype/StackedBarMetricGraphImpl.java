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
package org.rhq.coregui.client.inventory.common.graph.graphtype;

import org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph;

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 * The purpose of this class is the fill the div element with id: '#rChart-[resourceId]-[metricId]'.
 * with SVG instructions to draw a d3 graph (in this case a stacked bar graph).
 *
 * @author Mike Thompson
 */
public class StackedBarMetricGraphImpl extends AbstractMetricGraph {


    public StackedBarMetricGraphImpl() {
        super();
    }


    /**
     * The magic JSNI to draw the charts with $wnd.d3.js
     */
    @Override
    public native void drawJsniChart() /*-{
        //console.log("Draw Stacked Bar jsni chart");
        var global = this,

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
                chartContext = new $wnd.ChartContext(global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartId()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHeight()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getJsonMetrics()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getXAxisTitle()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitle()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getYAxisUnits()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitleMinLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitleAvgLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTitlePeakLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartDateLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartTimeLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartDownLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartUnknownLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartNoDataLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverStartLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverEndLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverPeriodLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverBarLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverTimeFormat()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartHoverDateFormat()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::isPortalGraph()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getPortalId()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getButtonBarDateTimeFormat()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartSingleValueLabel()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getXAxisTimeFormatHours()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getXAxisTimeFormatHoursMinutes()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::isHideLegend()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartAverage()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartMin()(),
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::getChartMax()()
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
                    tooltipTimeout = 15000,
                    barOffset = 2,
                    chartData,
                    interpolation = "basis",
                    avgFiltered, avg, minFiltered, min, peakFiltered, peak,
                    oobMax,
                    legendUnDefined,
                    lowBound,
                    newLow = 0,
                    highBound,
                    calcBarWidth,
                    yScale,
                    yAxis,
                    timeScale,
                    xAxis,
                    chart,
                    svg;

            // adjust the min scale so blue low line is not in axis
            function determineLowBound(min) {
                newLow = min;
                if (newLow < 0) {
                    return 0;
                }
                else {
                    return newLow;
                }
            }

            function getChartWidth() {
                return $wnd.jQuery("#" + chartContext.chartHandle).width();
            }

            function useSmallCharts() {
                //console.log("getChartWidth: "+ getChartWidth());
                return  getChartWidth() <= smallChartThresholdInPixels;
            }

            function determineScale() {
                var xTicks, xTickSubDivide, numberOfBarsForSmallGraph = 20;
                if (chartContext.data.length > 0) {

                    // if window is too small server up small chart
                    if (useSmallCharts()) {
                        //console.log("Using Small Charts Profile for width: "+getChartWidth());
                        width = 250;
                        xTicks = 3;
                        xTickSubDivide = 2;
                        chartData = chartContext.data.slice(chartContext.data.length - numberOfBarsForSmallGraph, chartContext.data.length);
                    }
                    else {
                        //console.log("Using Large Charts Profile, width: "+ width);
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
                    lowBound = determineLowBound(min);
                    highBound = peak + ((peak - min) * 0.1);
                    oobMax = $wnd.d3.max(chartContext.data.map(function (d) {
                        if (typeof d.baselineMax === 'undefined') {
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

                    legendUnDefined = (chartContext.chartAverage === "");
                    if ((!chartContext.hideLegend &&  !useSmallCharts() && !legendUnDefined )) {
                        createMinAvgPeakSidePanel(chartContext.minChartTitle, chartContext.chartMin, chartContext.avgChartTitle, chartContext.chartAverage, chartContext.peakChartTitle, chartContext.chartMax, chartContext.yAxisUnits);
                    }
                }

            }


            function createMinAvgPeakSidePanel(minLabel, minValue, avgLabel, avgValue, highLabel, highValue ) {
                var xLabel = 772,
                        xValue = 820,
                        yBase = 100,
                        yInc = 25;

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

                if(typeof highValue !== 'undefined'){
                    chart.append("text")
                            .attr("class", "highText")
                            .attr("x", xValue)
                            .attr("y", yBase)
                            .text(highValue);
                }


                //avg
                chart.append("text")
                        .attr("class", "avgLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + yInc)
                        .text(avgLabel + " - ");

                if(typeof avgValue !== 'undefined'){
                    chart.append("text")
                            .attr("class", "avgText")
                            .attr("x", xValue)
                            .attr("y", yBase + yInc)
                            .text(avgValue);
                }

                // min
                chart.append("text")
                        .attr("class", "minLabel")
                        .attr("x", xLabel)
                        .attr("y", yBase + 2 * yInc)
                        .text(minLabel + " - ");

                if(typeof minValue !== 'undefined'){
                    chart.append("text")
                            .attr("class", "minText")
                            .attr("x", xValue)
                            .attr("y", yBase + 2 * yInc)
                            .text(minValue);
                }

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

            function showFullMetricBarHover(d){

                var timeFormatter = $wnd.d3.time.format(chartContext.chartHoverTimeFormat),
                        dateFormatter = $wnd.d3.time.format(chartContext.chartHoverDateFormat),
                        startDate = new Date(+d.x),
                        metricGraphTooltipDiv =  $wnd.d3.select("#metricGraphTooltip");

                metricGraphTooltipDiv.style("left", + ($wnd.d3.event.pageX) +15 + "px")
                        .style("top",  ($wnd.d3.event.pageY)+"px");

                metricGraphTooltipDiv.select("#metricGraphTooltipTimeLabel")
                        .text(chartContext.timeLabel);
                metricGraphTooltipDiv.select("#metricGraphTooltipTimeValue")
                        .text(timeFormatter(startDate));

                metricGraphTooltipDiv.select("#metricGraphTooltipDateLabel")
                        .text(chartContext.dateLabel);
                metricGraphTooltipDiv.select("#metricGraphTooltipDateValue")
                        .text(dateFormatter(startDate));

                metricGraphTooltipDiv.select("#metricGraphTooltipDurationLabel")
                        .text(chartContext.hoverBarLabel);
                metricGraphTooltipDiv.select("#metricGraphTooltipDurationValue")
                        .text(d.barDuration);

                metricGraphTooltipDiv.select("#metricGraphTooltipMaxLabel")
                        .text(chartContext.peakChartTitle);
                metricGraphTooltipDiv.select("#metricGraphTooltipMaxValue")
                        .text(d.high.toFixed(1));

                metricGraphTooltipDiv.select("#metricGraphTooltipAvgLabel")
                        .text(chartContext.avgChartTitle);
                metricGraphTooltipDiv.select("#metricGraphTooltipAvgValue")
                        .text(d.y.toFixed(1));


                metricGraphTooltipDiv.select("#metricGraphTooltipMinLabel")
                        .text(chartContext.minChartTitle);
                metricGraphTooltipDiv.select("#metricGraphTooltipMinValue")
                        .text(d.low.toFixed(1));


                //Show the tooltip
                $wnd.jQuery('#metricGraphTooltip').show();
                setTimeout(function(){$wnd.jQuery('#metricGraphTooltip').hide();},tooltipTimeout);

            }
            function showNoDataBarHover(d){
                var timeFormatter = $wnd.d3.time.format(chartContext.chartHoverTimeFormat),
                        dateFormatter = $wnd.d3.time.format(chartContext.chartHoverDateFormat),
                        startDate = new Date(+d.x),
                        noDataTooltipDiv =  $wnd.d3.select("#noDataTooltip");

                noDataTooltipDiv.style("left", + ($wnd.d3.event.pageX) + 15 + "px")
                        .style("top",  ($wnd.d3.event.pageY)+"px");

                noDataTooltipDiv.select("#noDataTooltipTimeLabel")
                        .text(chartContext.timeLabel);
                noDataTooltipDiv.select("#noDataTooltipTimeValue")
                        .text(timeFormatter(startDate));

                noDataTooltipDiv.select("#noDataTooltipDateLabel")
                        .text(chartContext.dateLabel);
                noDataTooltipDiv.select("#noDataTooltipDateValue")
                        .text(dateFormatter(startDate));

                noDataTooltipDiv.select("#noDataLabel")
                        .text(chartContext.noDataLabel);

                //Show the tooltip
                $wnd.jQuery('#noDataTooltip').show();
                setTimeout(function(){$wnd.jQuery('#noDataTooltip').hide();},tooltipTimeout);

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
                        }).on("mouseover",function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                showNoDataBarHover(d);
                            }
                            else {
                                if(+d.high === +d.low){
                                    showSingleValueMetricBarHover(d);
                                } else {
                                    showFullMetricBarHover(d);
                                }
                            }
                        }).on("mouseout", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                $wnd.jQuery('#noDataTooltip').hide();
                            }else {
                                if(+d.high === +d.low){
                                    $wnd.jQuery('#singleValueTooltip').hide();
                                } else {
                                    $wnd.jQuery('#metricGraphTooltip').hide();
                                }
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
                                return 0;
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
                        .attr("opacity", 0.9)
                        .on("mouseover",function (d) {
                            showFullMetricBarHover(d);
                        }).on("mouseout", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                $wnd.jQuery('#noDataTooltip').hide();
                            }else {
                                $wnd.jQuery('#metricGraphTooltip').hide();
                            }
                        });



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
                                return 0;
                            }
                            else {
                                return  yScale(d.low) - yScale(d.y);
                            }
                        })
                        .attr("width", function () {
                            return  calcBarWidth();
                        })
                        .attr("opacity", 0.9)
                        .on("mouseover",function (d) {
                            showFullMetricBarHover(d);
                        }).on("mouseout", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                $wnd.jQuery('#noDataTooltip').hide();
                            }else {
                                $wnd.jQuery('#metricGraphTooltip').hide();
                            }
                        });

                function showSingleValueMetricBarHover(d){
                    var timeFormatter = $wnd.d3.time.format(chartContext.chartHoverTimeFormat),
                            dateFormatter = $wnd.d3.time.format(chartContext.chartHoverDateFormat),
                            startDate = new Date(+d.x),
                            singleValueGraphTooltipDiv =  $wnd.d3.select("#singleValueTooltip");

                    singleValueGraphTooltipDiv.style("left", + ($wnd.d3.event.pageX) + 15 + "px")
                            .style("top",  ($wnd.d3.event.pageY)+"px");

                    singleValueGraphTooltipDiv.select("#singleValueTooltipTimeLabel")
                            .text(chartContext.timeLabel);
                    singleValueGraphTooltipDiv.select("#singleValueTooltipTimeValue")
                            .text(timeFormatter(startDate));

                    singleValueGraphTooltipDiv.select("#singleValueTooltipDateLabel")
                            .text(chartContext.dateLabel);
                    singleValueGraphTooltipDiv.select("#singleValueTooltipDateValue")
                            .text(dateFormatter(startDate));

                    singleValueGraphTooltipDiv.select("#singleValueTooltipValueLabel")
                            .text(chartContext.singleValueLabel);
                    singleValueGraphTooltipDiv.select("#singleValueTooltipValue")
                            .text(d.y.toFixed(1));


                    //Show the tooltip
                    $wnd.jQuery('#singleValueTooltip').show();
                    setTimeout(function(){$wnd.jQuery('#singleValueTooltip').hide();},tooltipTimeout);

                }


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
                                return 0;
                            }
                            else {
                                if (d.low === d.high) {
                                    return  yScale(d.low) - yScale(d.y) + 2;
                                }
                                else {
                                    return  0;
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
                        }).on("mouseover",function (d) {
                            showSingleValueMetricBarHover(d);
                        }).on("mouseout", function () {
                            $wnd.jQuery('#singleValueTooltip').hide();
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

                xAxis.tickFormat($wnd.rhqCommon.getD3CustomTimeFormat(chartContext.chartXaxisTimeFormatHours, chartContext.chartXaxisTimeFormatHoursMinutes));

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
                        .attr("transform", "rotate(-90),translate( -60,0)")
                        .attr("y", -30)
                        .style("text-anchor", "end")
                        .text(chartContext.yAxisUnits === "NONE" ? "" : chartContext.yAxisUnits);

            }

            function createAvgLines() {
                var showBarAvgTrendline =
                                global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::showBarAvgTrendLine()(),
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
                var unitsPercentMultiplier = chartContext.yAxisUnits === '%' ? 100 : 1,
                        minBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMin * unitsPercentMultiplier);
                                }),
                        maxBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMax * unitsPercentMultiplier);
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

                var brush = $wnd.d3.svg.brush()
                                .x(timeScale)
                                .on("brushstart", brushstart)
                                .on("brush", brushmove)
                                .on("brushend", brushend),
                        brushg = svg.append("g")
                                .attr("class", "brush")
                                .call(brush);

                brushg.selectAll(".resize").append("path");

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
                    var startTime = Math.round(s[0].getTime());
                    var endTime = Math.round(s[1].getTime() );
                    svg.classed("selecting", !$wnd.d3.event.target.empty());
                    // ignore selections less than 1 minute
                    if(endTime - startTime >= 60000){
                        global.@org.rhq.coregui.client.inventory.common.graph.AbstractMetricGraph::dragSelectionRefresh(DD)(startTime, endTime);
                    }
                }

                function updateDateRangeDisplay(startDate, endDate ) {
                    var formattedDateRange = startDate.format(chartContext.buttonBarDateTimeFormat) + '  -  ' + endDate.format(chartContext.buttonBarDateTimeFormat);
                    var timeRange = endDate.from(startDate,true);
                    $wnd.jQuery('.graphDateTimeRangeLabel').text(formattedDateRange+'('+timeRange+')');
                }


            }

            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    // Guard condition that can occur when a portlet has not been configured yet
                    if (chartContext.data.length > 0) {
                        //console.log("Creating Chart: "+ chartContext.chartSelection + " --> "+ chartContext.chartTitle);

                        determineScale();
                        createHeader(chartContext.chartTitle);

                        createYAxisGridLines();
                        if(!chartContext.isPortalGraph){
                            createBrush();
                        }
                        createStackedBars();
                        createXandYAxes();
                        createAvgLines();
                        if (oobMax > 0) {
                            //console.log("OOB Data Exists!");
                            createOOBLines();
                        }
                    }
                }
            }; // end public closure
        }();

        if(typeof chartContext.data !== 'undefined' && chartContext.data !== null && chartContext.data.length > 0){
            metricStackedBarGraph.draw(chartContext);
        }

    }-*/;

}
