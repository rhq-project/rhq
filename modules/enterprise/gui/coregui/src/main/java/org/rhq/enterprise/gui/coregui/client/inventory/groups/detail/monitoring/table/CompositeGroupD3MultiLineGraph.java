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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table;

/**
 * D3 rendition of group composite graphs for single metric multiple resources.
 *
 * @author Mike Thompson
 */
public class CompositeGroupD3MultiLineGraph extends CompositeGroupD3GraphListView {

    public CompositeGroupD3MultiLineGraph(int groupId, int defId, boolean isAutogroup) {
        super(groupId, defId, isAutogroup);
    }

    @Override
    public native void drawJsniChart() /*-{
        console.log("Draw d3 MultiLine jsni chart");

        var MultiLineChartContext = function (chartId, chartHeight, metricsData, xAxisLabel, chartTitle, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, chartHoverTimeFormat, chartHoverDateFormat, isPortalGraph, portalId, buttonBarDateTimeFormat, chartXaxisTimeFormatHours, chartXaxisTimeFormatHoursMinutes) {
            "use strict";
            if (!(this instanceof MultiLineChartContext)) {
                throw new Error("MultiLineChartContext function cannot be called as a function.")
            }
            this.chartId = chartId;
            this.chartHeight = chartHeight;
            this.data = $wnd.jQuery.parseJSON(metricsData); // make into json
            this.xAxisLabel = xAxisLabel;
            this.chartTitle = chartTitle;
            this.yAxisUnits = yAxisUnits;
            this.minChartTitle = minChartTitle;
            this.avgChartTitle = avgChartTitle;
            this.peakChartTitle = peakChartTitle;
            this.dateLabel = dateLabel;
            this.timeLabel = timeLabel;
            this.chartHoverTimeFormat = chartHoverTimeFormat;
            this.chartHoverDateFormat = chartHoverDateFormat;
            this.chartHandle = "mChart-" + chartId;
            this.chartSelection = this.chartHandle + " svg";
            this.buttonBarDateTimeFormat = buttonBarDateTimeFormat;
            this.chartXaxisTimeFormatHours = chartXaxisTimeFormatHours;
            this.chartXaxisTimeFormatHoursMinutes = chartXaxisTimeFormatHoursMinutes;

        };

        var global = this,

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
                chartContext = new MultiLineChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartId()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHeight()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getJsonMetrics()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitle()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getYAxisUnits()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitleMinLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitleAvgLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitlePeakLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartDateLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTimeLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHoverTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHoverDateFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getButtonBarDateTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTimeFormatHours()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTimeFormatHoursMinutes()()
                );


        // Define the Stacked Bar Graph function using the module pattern
        var multiLineGraph = function () {
            "use strict";
            // privates
            var margin = {top: 10, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    adjustedChartHeight = chartContext.chartHeight - 50,
                    height = adjustedChartHeight - margin.top - margin.bottom,
                    titleHeight = 30, titleSpace = 10,
                    yScale,
                    yAxis,
                    timeScale,
                    xAxis,
                    chart,
                    svg;


            function determineScale() {
                var xTicks, xTickSubDivide;
                console.log("DetermineScale for # resources: "+ chartContext.data.length);

                if (chartContext.data.length > 0) {
                    xTicks = 8;
                    xTickSubDivide = 5;

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([$wnd.d3.min(chartContext.data[0], function (d) {
                                return d.y;
                            }), $wnd.d3.max(chartContext.data[0], function (d) {
                                return d.y;
                            })]);

                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(1)
                            .ticks(5)
                            .tickSize(4, 4, 0)
                            .orient("left");


                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(chartContext.data[0], function (d) {
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

                }

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
                var customTimeFormat = timeFormat([
                    [$wnd.d3.time.format("%Y"), function () {
                        return true;
                    }],
                    [$wnd.d3.time.format("%B"), function (d) {
                        return d.getMonth();
                    }],
                    [$wnd.d3.time.format("%b %d"), function (d) {
                        return d.getDate() != 1;
                    }],
                    [$wnd.d3.time.format("%a %d"), function (d) {
                        return d.getDay() && d.getDate() != 1;
                    }],
                    [$wnd.d3.time.format(chartContext.chartXaxisTimeFormatHours), function (d) {
                        return d.getHours();
                    }],
                    [$wnd.d3.time.format(chartContext.chartXaxisTimeFormatHoursMinutes), function (d) {
                        return d.getMinutes();
                    }],
                    [$wnd.d3.time.format(":%S"), function (d) {
                        return d.getSeconds();
                    }],
                    [$wnd.d3.time.format(".%L"), function (d) {
                        return d.getMilliseconds();
                    }]
                ]);
                xAxis.tickFormat(customTimeFormat);

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

            function timeFormat(formats) {
                return function (date) {
                    var i = formats.length - 1, f = formats[i];
                    while (!f[1](date)) f = formats[--i];
                    return f[0](date);
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

            function createMultiLines(chartContext) {
                var  graphLine = $wnd.d3.svg.line()
                                .interpolate("linear")
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                        return yScale(d.y);
                                });

                 chart.selectAll(".multiLine")
                        .data(chartContext.data)
                        .enter()
                        .append('path')
                        .attr("class", "multiLine")
                        .attr("fill", "none")
                        .attr("stroke", "#2e376a")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-opacity", ".9")
                        .attr("d", function(d) { return graphLine(d.value);});

            }


            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    // Guard condition that can occur when a portlet has not been configured yet
                    console.log("multi-resource chart handle:" + chartContext.chartHandle);
                    //console.dir(chartContext.data);
                    if (chartContext.data.length > 0) {
                        console.log("Creating MultiLine Chart: " + chartContext.chartSelection + " --> " + chartContext.chartTitle);
                        determineScale();
                        createHeader(chartContext.chartTitle);
                        console.log("created multi-header");
                        createYAxisGridLines();
                        createMultiLines(chartContext);
                        createXandYAxes();
                    }
                }
            }; // end public closure
        }();

        if (chartContext.data !== undefined && chartContext.data.length > 0) {
            multiLineGraph.draw(chartContext);
        }

    }-*/;


}
