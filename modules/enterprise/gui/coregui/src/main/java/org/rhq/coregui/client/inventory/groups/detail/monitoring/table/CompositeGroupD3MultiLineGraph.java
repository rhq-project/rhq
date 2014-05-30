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
package org.rhq.coregui.client.inventory.groups.detail.monitoring.table;

import org.rhq.core.domain.common.EntityContext;

/**
 * D3 rendition of group composite graphs for single metric multiple resources.
 *
 * @author Mike Thompson
 */
public class CompositeGroupD3MultiLineGraph extends CompositeGroupD3GraphListView {

    public CompositeGroupD3MultiLineGraph(EntityContext context, int defId) {
        super(context, defId);
    }

    @Override
    public native void drawJsniChart() /*-{
        //console.log("Draw d3 MultiLine jsni chart");

        var MultiLineChartContext = function (chartId, chartHeight, metricsData, xAxisLabel, chartTitle, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, chartHoverTimeFormat, chartHoverDateFormat, isPortalGraph, portalId, buttonBarDateTimeFormat, chartXaxisTimeFormatHours, chartXaxisTimeFormatHoursMinutes) {
            "use strict";
            if (!(this instanceof MultiLineChartContext)) {
                throw new Error("MultiLineChartContext function cannot be called as a function.")
            }
            this.chartId = chartId;
            this.chartHeight = chartHeight;
            if(typeof metricsData !== 'undefined' && metricsData.length > 0){
                this.data = $wnd.jQuery.parseJSON(metricsData);
            }
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

        },
        global = this;

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
        chartContext = new MultiLineChartContext(global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartId()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHeight()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getJsonMetrics()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTitle()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitle()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getYAxisUnits()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitleMinLabel()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitleAvgLabel()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTitlePeakLabel()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartDateLabel()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartTimeLabel()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHoverTimeFormat()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getChartHoverDateFormat()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getButtonBarDateTimeFormat()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTimeFormatHours()(),
                        global.@org.rhq.coregui.client.inventory.groups.detail.monitoring.table.CompositeGroupD3GraphListView::getXAxisTimeFormatHoursMinutes()()
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
                    colorScale = $wnd.d3.scale.category20(),
                    chart,
                    svg;


            function determineScale() {
                var xTicks, xTickSubDivide;
                //console.log("DetermineScale for # resources: "+ chartContext.data.length);

                if (chartContext.data.length > 0) {
                    xTicks = 8;
                    xTickSubDivide = 5;
                    var myExtent = getExtentFromNestedValues(chartContext.data);

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([myExtent[0],myExtent[1]]);

                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(1)
                            .ticks(5)
                            .tickSize(4, 4, 0)
                            .orient("left");

                    var firstDataset = chartContext.data[0].value;
                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(firstDataset, function(d){
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

            function getExtentFromNestedValues(data){
                var tempArray = [],
                        mergedArray = [],
                        resultArray = [],
                        max = 0,
                        min = 0;

                for(var i=0; i< data.length;i++){
                    tempArray.push(data[i].value);
                }
                mergedArray = $wnd.d3.merge(tempArray, function(d){ return d.y;});
                max = $wnd.d3.max(mergedArray, function(d){ return d.y});
                min = $wnd.d3.min(mergedArray, function(d){ return d.y});
                resultArray.push(min);
                resultArray.push(max);
                return resultArray;
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

                //xAxis.tickFormat($wnd.rhqCommon.getD3CustomTimeFormat(chartContext.chartXaxisTimeFormatHours, chartContext.chartXaxisTimeFormatHoursMinutes));

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

            function createLegend() {

                // add legend
                var legend = svg.append("g")
                        .attr("class", "legend")
                        .attr("x", width + 100)
                        .attr("y", 70)
                        .attr("height", 240)
                        .attr("width", 150);

                legend.selectAll('g').data(chartContext.data)
                        .enter()

                        .append('g')
                        .each(function (d, i) {
                            var g = $wnd.d3.select(this);
                            g.append("rect")
                                    .attr("x", width + 10)
                                    .attr("y", (i * 15) - 8)
                                    .attr("width", 10)
                                    .attr("height", 10)
                                    .style("fill", function(){return colorScale(i);});

                            g.append("text")
                                    .attr("x", width + 30)
                                    .attr("y", i * 15)
                                    .attr("height", 10)
                                    .attr("width", 135)
                                    .style("font-size", "10px")
                                    .style("font-family", "Arial, Helvetica, sans-serif")
                                    .style("fill", "#50505A")
                                    .text(function (d) {
                                        return d.key;
                                    });


                        });
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

                chartContext.data.sort(function(a,b){return ((a.key < b.key) ? -1 : ((a.key > b.key) ? 1 : 0));});

                 svg.selectAll(".multiLine")
                        .data(chartContext.data)
                        .enter()
                        .append('path')
                        .attr("class", "multiLine")
                        .attr("fill", "none")
                        .attr("stroke", function(d,i){ return colorScale(i);})
                        .attr("stroke-width", "2")
                        .attr("stroke-opacity", ".9")
                        .attr("d", function(d) { return graphLine(d.value);});

            }


            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    // Guard condition that can occur when a portlet has not been configured yet
                    //console.log("multi-resource chart handle:" + chartContext.chartHandle);
                    if (chartContext.data.length > 0) {
                        //console.log("Creating MultiLine Chart: " + chartContext.chartSelection + " --> " + chartContext.chartTitle);
                        determineScale();
                        createHeader(chartContext.chartTitle);
                        createYAxisGridLines();
                        createMultiLines(chartContext);
                        createXandYAxes();
                        createLegend();
                        //console.log("finished drawing multi-line graph");
                    }
                }
            }; // end public closure
        }();

        if (typeof chartContext.data !== 'undefined' && chartContext.data.length > 0) {
            multiLineGraph.draw(chartContext);
        }

    }-*/;


}
