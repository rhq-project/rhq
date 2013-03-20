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

import java.util.Date;
import java.util.List;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, orange=disabled, unknown=grey,
 * empty=grey, warn=yellow.  This version of the availability graph shows continuous intervals.
 *
 * @author Mike Thompson
 */
public class AvailabilityLineGraphType {

    private static Messages MSG = CoreGUI.getMessages();
    private List<Availability> availabilityList;
    private List<ResourceGroupAvailability> groupAvailabilityList;
    private Integer entityId;

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public AvailabilityLineGraphType(Integer entityId) {
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
                sb.append("{ \"availType\":\"" + groupAvailability.getGroupAvailabilityType() + "\", ");
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

        console.groupCollapsed("AvailabilityChart");
        console.time("availabilityChart");
        var global = this,

        // tidy up all of our interactions with java (via JSNI) thru AvailChartContext class
        // NOTE: rhq.js has the javascript object constructors in it.
                availChartContext = new $wnd.AvailChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartId()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getAvailabilityJson()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartDateLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartTimeLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverStartLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverEndLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverBarLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverAvailabilityLabel()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverTimeFormat()(),
                        global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType::getChartHoverDateFormat()()
                );


        function draw(availChartContext) {
            "use strict";

            var margin = {top: 5, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    height = 20 - margin.top - margin.bottom,
                    pixelsOffHeight = 0,
                    UP = 4, DOWN = 3, UNKNOWN = 2, DISABLED = 1, WARN = 0,
                    xAxisMin = $wnd.d3.min(availChartContext.data, function (d) {
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

                    svg = $wnd.d3.select(availChartContext.chartSelection).append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");


            // The gray bars at the bottom leading up
            svg.selectAll("rect.availBars")
                    .data(availChartContext.data)
                    .enter().append("rect")
                    .attr("class", "availBars")
                    .attr("x", function (d) {
                        return timeScale(+d.availStart);
                    })
                    .attr("y", function (d) {
                        return yScale(0);
                    })
                    .attr("height", function (d) {
                        return height - yScale(4) - pixelsOffHeight;
                    })
                    .attr("width", function (d) {
                        return timeScale(+d.availEnd) - timeScale(+d.availStart);
                    })

                    .attr("opacity", ".9")
                    .attr("fill", function (d) {
                        if (d.availType === 'DOWN') {
                            return "#FF1919"; // red
                        }
                        else if (d.availType === 'DISABLED') {
                            return "#FF9933"; // orange
                        }
                        else if (d.availType === 'UNKNOWN') {
                            return "#CCC"; // gray
                        }
                        else if (d.availType === 'UP') {
                            return "#198C19"; // green
                        }
                        else if (d.availType === 'WARN') {
                            return "#FFFF00"; // yellow
                        }
                        else if (d.availType === 'EMPTY') {
                            return "#CCC"; // gray
                        }
                        else {
                            // should not ever happen, but...
                            console.warn("AvailabilityType not valid.");
                            return "#000"; //black
                        }
                    });

            createHovers();

        }

        function createHovers() {
            $wnd.jQuery('svg rect.availBars').tipsy({
                gravity: 'n',
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
            var hoverString,
                    timeFormatter = $wnd.d3.time.format(availChartContext.chartHoverTimeFormat),
                    dateFormatter = $wnd.d3.time.format(availChartContext.chartHoverDateFormat),
                    availType = d.availType,
                    availStart = new Date(+d.availStart),
                    availEnd = new Date(+d.availEnd),
                    availDuration = d.availDuration;

            hoverString =
                    '<div class="chartHoverEnclosingDiv">' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverBarAvailabilityLabel + ': </span><span style="width:50px;">' + availType + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverStartLabel + ': </span><span style="width:50px;">' + timeFormatter(availStart) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + ' </span><span style="width:50px;">' + dateFormatter(availStart) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverEndLabel + ': </span><span style="width:50px;">' + timeFormatter(availEnd) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + ' </span><span style="width:50px;">' + dateFormatter(availEnd) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverBarLabel + ': </span><span style="width:50px;">' + availDuration + '</span></div>' +
                            '</div>';
            return hoverString;

        }

        if (availChartContext.data.length > 0) {
            draw(availChartContext);
        }

        console.timeEnd("availabilityChart");
        console.groupEnd("AvailabilityChart");
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

    public String getChartHoverEndLabel() {
        return MSG.chart_hover_end_label();
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
}
