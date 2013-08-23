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

import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AvailabilityGraphType;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, orange=disabled, unknown=grey,
 * empty=grey, warn=yellow.  This version of the availability graph shows continuous intervals.
 *
 * @author Mike Thompson
 */
public class AvailabilityOverUnderGraphType implements AvailabilityGraphType {

    private static final Messages MSG = CoreGUI.getMessages();
    private List<Availability> availabilityList;
    private List<ResourceGroupAvailability> groupAvailabilityList;
    private Integer entityId;

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public AvailabilityOverUnderGraphType(Integer entityId) {
        this.entityId = entityId;
    }

    public void setAvailabilityList(List<Availability> availabilityList) {
        this.availabilityList = availabilityList;
    }

    public void setGroupAvailabilityList(List<ResourceGroupAvailability> groupAvailabilityList) {
        this.groupAvailabilityList = groupAvailabilityList;
    }

    public String getAvailabilityJson() {
        StringBuilder sb = new StringBuilder("[");
        if (null != availabilityList) {
            // loop through the avail intervals
            for (Availability availability : availabilityList) {
                sb.append("{ \"availType\":\"" + availability.getAvailabilityType() + "\", ");
                sb.append(" \"availTypeMessage\":\"" + availability.getAvailabilityType() + "\", ");
                sb.append(" \"availStart\":" + availability.getStartTime() + ", ");
                // last record will be null
                long endTime = availability.getEndTime() != null ? availability.getEndTime() : (new Date()).getTime();
                sb.append(" \"availEnd\":" + endTime + ", ");

                long availDuration = endTime - availability.getStartTime();
                String availDurationString = MeasurementConverterClient.format((double) availDuration,
                    MeasurementUnits.MILLISECONDS, true);
                sb.append(" \"availDuration\": \"" + availDurationString + "\" },");

            }
            sb.setLength(sb.length() - 1);

        } else if (null != groupAvailabilityList) {
            // loop through the group avail down intervals
            for (ResourceGroupAvailability groupAvailability : groupAvailabilityList) {
                // allows substitution for situations like WARN=MIXED for easier terminology
                String availabilityTypeMessage = (groupAvailability.getGroupAvailabilityType()
                    .equals(ResourceGroupComposite.GroupAvailabilityType.WARN)) ? MSG
                    .chart_hover_availability_type_warn() : groupAvailability.getGroupAvailabilityType().name();

                sb.append("{ \"availType\":\"" + groupAvailability.getGroupAvailabilityType() + "\", ");
                sb.append(" \"availTypeMessage\":\"" + availabilityTypeMessage + "\", ");
                sb.append(" \"availStart\":" + groupAvailability.getStartTime() + ", ");
                // last record will be null
                long endTime = groupAvailability.getEndTime() != null ? groupAvailability.getEndTime() : (new Date())
                    .getTime();
                sb.append(" \"availEnd\":" + endTime + ", ");

                long availDuration = endTime - groupAvailability.getStartTime();
                String availDurationString = MeasurementConverterClient.format((double) availDuration,
                    MeasurementUnits.MILLISECONDS, true);
                sb.append(" \"availDuration\": \"" + availDurationString + "\" },");

            }
            sb.setLength(sb.length() - 1);
        }

        sb.append("]");
        Log.debug(sb.toString());
        return sb.toString();
    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw Enhanced Availability chart");

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
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getAvailChartDownLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartXaxisTimeFormatHours()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilityOverUnderGraphType::getChartXaxisTimeFormatHoursMinutes()()
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
                            var ABOVE = -6,
                                    BELOW = 0,
                                    STRADDLE = -3,
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
                                return "#c5888b"; // red
                            }
                            else if (d.availType === 'DISABLED') {
                                return "url(#diagonalHatchFill)"; // grey diagonal hatches
                            }
                            else if (d.availType === 'UNKNOWN') {
                                return "#d8d8d8"; // gray
                            }
                            else if (d.availType === 'UP') {
                                return "#8cbe89"; // green
                            }
                            else if (d.availType === 'WARN') {
                                return "#e1b36b"; // orange
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
                            return 6;
                        })
                        .attr("width", function (d) {
                            return timeScale(+d.availEnd) - timeScale(+d.availStart);
                        })
                        .attr("opacity", ".75")
                        .attr("fill", function (d) {
                            return calcBarFill(d);
                        }).on("mouseover",function (d) {

                            var timeFormatter = $wnd.d3.time.format(availChartContext.chartHoverTimeFormat),
                                    dateFormatter = $wnd.d3.time.format(availChartContext.chartHoverDateFormat),
                                    availStart = new Date(+d.availStart),
                                    xPosition = parseFloat($wnd.d3.select(this).attr("x")),
                                    xWidth = parseFloat($wnd.d3.select(this).attr("width")),
                                    xMidPoint = xPosition + (xWidth/2),
                                    availTooltipDiv =  $wnd.d3.select("#availTooltip")
                                    .style("left", + xMidPoint + "px")
                                    .style("top",  "0px");

                            availTooltipDiv.select("#availTooltipLabel")
                                    .text(availChartContext.hoverBarAvailabilityLabel);

                            availTooltipDiv
                                    .select("#availTooltipType")
                                    .text(d.availTypeMessage);

                            availTooltipDiv
                                    .select("#availTooltipStartDate")
                                    .text(dateFormatter(availStart));

                            availTooltipDiv
                                    .select("#availTooltipStartTime")
                                    .text(timeFormatter(availStart));

                            availTooltipDiv
                                    .select("#availTooltipDurationLabel")
                                    .text(availChartContext.hoverBarLabel);

                            availTooltipDiv
                                    .select("#availTooltipDuration")
                                    .text(d.availDuration);

                            //Show the tooltip
                            $wnd.d3.select("#availTooltip").classed("hidden", false);
                        }).on("mouseout", function () {
                            //Hide the tooltip
                            $wnd.d3.select("#availTooltip").classed("hidden", true);
                        });

                xAxis.tickFormat($wnd.rhqCommon.getD3CustomTimeFormat(availChartContext.chartXaxisTimeFormatHours, availChartContext.chartXaxisTimeFormatHoursMinutes));

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("fill", "#b0b0b0")
                        .attr("stroke-width", "0.5")
                        .attr("transform", "translate(0," + height + ")")
                        .call(xAxis);

                svg.append("text")
                        .attr("class", "availabilityLabel")
                        .attr("x", -40)
                        .attr("y", 10)
                        .style("font-size", "12px")
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-weight", "bold")
                        .attr("fill", "#545454")
                        .text(availChartContext.chartTitle);

                svg.append("text")
                        .attr("class", "upLabel")
                        .attr("x", -5)
                        .attr("y", 28)
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-size", "9px")
                        .attr("fill", "#545454")
                        .style("text-anchor", "end")
                        .text(availChartContext.chartUpLabel);

                svg.append("text")
                        .attr("class", "downLabel")
                        .attr("x", -5)
                        .attr("y", 39)
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .style("font-size", "9px")
                        .attr("fill", "#545454")
                        .style("text-anchor", "end")
                        .text(availChartContext.chartDownLabel);

            }


            return {
                // Public API
                draw: function (availChartContext) {
                    "use strict";
                    drawBars(availChartContext);
                }
            }; // end public closure


        }();

        if (typeof availChartContext.data !== 'undefined' && availChartContext.data.length > 0) {
            availabilityGraph.draw(availChartContext);
            console.log("Availability Chart Drawn");
        }

    }-*/;

    public String getChartId() {
        return String.valueOf(entityId);
    }

    public String getChartTimeLabel() {
        return MSG.chart_time_label();
    }

    public String getChartDateLabel() {
        return MSG.chart_date_label();
    }

    public String getChartHoverAvailabilityLabel() {
        return MSG.chart_hover_availability_label();
    }

    public String getChartHoverStartLabel() {
        return MSG.chart_hover_start_label();
    }

    public String getAvailChartDownLabel() {
        return MSG.avail_chart_down_label();
    }

    public String getAvailChartUpLabel() {
        return MSG.avail_chart_up_label();
    }

    public String getAvailChartTitleLabel() {
        return MSG.avail_chart_title_label();
    }

    public String getChartHoverBarLabel() {
        return MSG.chart_hover_bar_label();
    }

    public String getChartHoverTimeFormat() {
        return MSG.chart_hover_time_format();
    }

    public String getChartHoverDateFormat() {
        return MSG.chart_hover_date_format();
    }
    public String getChartXaxisTimeFormatHours() {
        return MSG.chart_xaxis_time_format_hours();
    }
    public String getChartXaxisTimeFormatHoursMinutes() {
        return MSG.chart_xaxis_time_format_hours_minutes();
    }
}
