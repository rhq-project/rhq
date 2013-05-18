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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * An implementation using JQuery UI components because smartGWT doesn't contain buttonsets or dual range sliders.
 *
 * @author Mike Thompson
 */
@Deprecated
public class JQueryDateTimeRangeEditor {
    private static Messages MSG = CoreGUI.getMessages();

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw GraphDateTimeRangeEditor");

        var global = this,
        // tidy up all of our interactions with java (via JSNI) thru AvailChartContext class
        // NOTE: rhq.js has the javascript object constructors in it.
                availChartContext = new $wnd.AvailChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartId()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getAvailabilityJson()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartDateLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartTimeLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartHoverStartLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartHoverBarLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartHoverAvailabilityLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartHoverTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartHoverDateFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getAvailChartTitleLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getAvailChartUpLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getAvailChartDownLabel()()
                );


        var availabilityGraph = function () {
            "use strict";
            // privates

            var margin = {top: 5, right: 5, bottom: 5, left: 40},
                    barOffset = 10,
                    width = 750 - margin.left - margin.right + barOffset,
                    height = 40 - margin.top - margin.bottom,
                    svg;


            function drawBars(availChartContext) {
                var xAxisMin = $wnd.d3.min(availChartContext.data, function (d) {
                            return +d.availStart;
                        }),
                        xAxisMax = $wnd.d3.max(availChartContext.data, function (d) {
                            return +d.availEnd;
                        }),

                        timeScale = $wnd.d3.time.scale()
                                .range([0, width])
                                .domain([xAxisMin, xAxisMax]),

                        yScale = $wnd.d3.scale.linear()
                                .clamp(true)
                                .rangeRound([height, 0])
                                .domain([0, 4]),

                        xAxis = $wnd.d3.svg.axis()
                                .scale(timeScale)
                                .ticks(8)
                                .tickSize(13, 0, 0)
                                .orient("bottom"),

                        calcBarY = function (d) {
                            var ABOVE = -10,
                                    BELOW = 0,
                                    STRADDLE = -5,
                                    offset;

                            if (d.availType === 'DOWN') {
                                offset = BELOW;
                            }
                            else if (d.availType === 'DISABLED') {
                                offset = STRADDLE;
                            }
                            else if (d.availType === 'UNKNOWN') {
                                offset = STRADDLE;
                            }
                            else if (d.availType === 'UP') {
                                offset = ABOVE;
                            }
                            else if (d.availType === 'WARN') {
                                offset = STRADDLE;
                            }
                            else if (d.availType === 'EMPTY') {
                                offset = STRADDLE;
                            }
                            return yScale(0) + offset;

                        },

                        calcBarFill = function (d) {
                            if (d.availType === 'DOWN') {
                                return "#FF1919"; // red
                            }
                            else if (d.availType === 'DISABLED') {
                                return "url(#diagonalHatchFill)"; // grey diagonal hatches
                            }
                            else if (d.availType === 'UNKNOWN') {
                                return "#CCC"; // gray
                            }
                            else if (d.availType === 'UP') {
                                return "#198C19"; // green
                            }
                            else if (d.availType === 'WARN') {
                                return "#FFA500"; // orange
                            }
                            else if (d.availType === 'EMPTY') {
                                return "#CCC"; // gray
                            }
                            else {
                                // should not ever happen, but...
                                console.log("AvailabilityType not valid.");
                                return "#000"; //black
                            }
                        },
                        svg = $wnd.d3.select(availChartContext.chartSelection).append("g")
                                .attr("width", width + margin.left + margin.right)
                                .attr("height", height + margin.top + margin.bottom)
                                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


                svg.selectAll("rect.availBars")
                        .data(availChartContext.data)
                        .enter().append("rect")
                        .attr("class", "availBars")
                        .attr("x", function (d) {
                            return timeScale(+d.availStart);
                        })
                        .attr("y", function (d) {
                            return calcBarY(d);
                        })
                        .attr("height", function (d) {
                            return 10;
                        })
                        .attr("width", function (d) {
                            return timeScale(+d.availEnd) - timeScale(+d.availStart);
                        })
                        .attr("opacity", ".9")
                        .attr("fill", function (d) {
                            return calcBarFill(d);
                        });

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("fill", "#50505a")
                        .attr("stroke-width", "0.5")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .call(xAxis);

                svg.append("text")
                        .attr("class", "availabilityLabel")
                        .attr("x", -40)
                        .attr("y", 10)
                        .style("font-size", "12px")
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-weight", "bold")
                        .attr("fill", "#003168")
                        .text(availChartContext.chartTitle);

                svg.append("text")
                        .attr("class", "upLabel")
                        .attr("x", -5)
                        .attr("y", 28)
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-size", "9px")
                        .attr("fill", "#50505a")
                        .style("text-anchor", "end")
                        .text(availChartContext.chartUpLabel);

                svg.append("text")
                        .attr("class", "downLabel")
                        .attr("x", -5)
                        .attr("y", 39)
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-size", "9px")
                        .attr("fill", "#50505a")
                        .style("text-anchor", "end")
                        .text(availChartContext.chartDownLabel);

            }

            function createHovers() {
                $wnd.jQuery('svg rect.availBars').tipsy({
                    gravity: 's',
                    html: true,
                    trigger: 'hover',
                    title: function () {
                        var d = this.__data__;
                        return formatHovers(d);
                    },
                    show: function (e, el) {
                        el.css({ 'z-index': '990000'})
                    }
                });
            }

            function formatHovers(d) {
                var  timeFormatter = $wnd.d3.time.format(availChartContext.chartHoverTimeFormat),
                        dateFormatter = $wnd.d3.time.format(availChartContext.chartHoverDateFormat),
                        availStart = new Date(+d.availStart);

                return '<div class="chartHoverEnclosingDiv">' +
                        '<div class="chartHoverAlignLeft"><span >' + availChartContext.hoverBarAvailabilityLabel + ': </span><span style="width:50px;">' + d.availTypeMessage + '</span></div>' +
                        '<div class="chartHoverAlignLeft"><span>' + dateFormatter(availStart) + ' ' + timeFormatter(availStart) + '</span></div>' +
                        '<div class="chartHoverAlignLeft"><span >' + availChartContext.hoverBarLabel + ': </span><span style="width:50px;">' + d.availDuration + '</span></div>' +
                        '</div>';

            }

            return {
                // Public API
                draw: function (availChartContext) {
                    "use strict";
                    console.log("AvailabilityChart");
                    drawBars(availChartContext);
                    createHovers();
                }
            }; // end public closure


        }();

        if (availChartContext.data !== undefined && availChartContext.data.length > 0) {
            availabilityGraph.draw(availChartContext);
        }

    }-*/;


}
