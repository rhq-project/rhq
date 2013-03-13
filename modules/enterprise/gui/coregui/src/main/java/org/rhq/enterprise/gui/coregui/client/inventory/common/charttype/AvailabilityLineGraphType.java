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
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, orange=disabled, yellow=warn, unknown=grey.
 *
 * @author Mike Thompson
 */
public class AvailabilityLineGraphType {

    private static Messages MSG = CoreGUI.getMessages();
    private List<MeasurementDataNumericHighLowComposite> metricData;
    private PageList<Availability> availabilityList;
    private Integer entityId;

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public AvailabilityLineGraphType(Integer entityId) {
        this.entityId = entityId;
    }

    public void setAvailabilityList(PageList<Availability> availabilityList) {
        this.availabilityList = availabilityList;
    }

    public void setMetricData(List<MeasurementDataNumericHighLowComposite> metricData) {
        this.metricData = metricData;
    }

    public String getAvailabilityJson() {
        StringBuilder sb = new StringBuilder("[");
        if (null != metricData) {

            for (MeasurementDataNumericHighLowComposite measurement : metricData) {
                sb.append("{ \"x\":" + measurement.getTimestamp() + ",");

                if (null != availabilityList) {
                    // loop through the avail down intervals
                    for (Availability availability : availabilityList) {

                        boolean hasValidTimestamps = availability.getStartTime() != null
                                && availability.getEndTime() != null;
                        // we know we are in an interval
                        if (hasValidTimestamps && measurement.getTimestamp() >= availability.getStartTime()
                                && measurement.getTimestamp() <= availability.getEndTime()) {

                            sb.append(" \"availType\":\"" + availability.getAvailabilityType() + "\", ");
                            sb.append(" \"availStart\":" + availability.getStartTime() + ", ");
                            sb.append(" \"availEnd\":" + availability.getEndTime() + ", ");
                            long availDuration = availability.getEndTime() - availability.getStartTime();
                            String availDurationString = MeasurementConverterClient.format((double)availDuration,
                                    MeasurementUnits.MILLISECONDS, true);
                            sb.append(" \"availDuration\": \"" + availDurationString + "\" ");
                            break;
                        }
                        else if (availability.getEndTime() == null) {
                            // we are in the last, unbounded avail interval so assume that the end is now
                            Date now = new Date();
                            sb.append(" \"availType\":\"" + availability.getAvailabilityType() + "\", ");
                            sb.append(" \"availStart\":" + availability.getStartTime() + ", ");
                            sb.append(" \"availEnd\":" + now.getTime() + ",");
                            long availDuration = (new Date()).getTime() - availability.getStartTime();
                            String availDurationString = MeasurementConverterClient.format((double)availDuration,
                                    MeasurementUnits.MILLISECONDS, true);
                            sb.append(" \"availDuration\": \"" + availDurationString + " +\" ");
                            break;
                        }
                    }
                }

                if (sb.toString().endsWith(",")) {
                    sb.setLength(sb.length() - 1); // delete the last ','
                }
                if (!sb.toString().endsWith("},")) {
                    sb.append(" },");
                }
            }
            sb.setLength(sb.length() - 1); // delete the last ','
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
                    xAxisMin = $wnd.d3.min(availChartContext.data, function(d){
                        return +d.x;
                    }),
                    xAxisMax = $wnd.d3.max(availChartContext.data,function(d){
                                return +d.x;
                            }),

                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(availChartContext.data, function(d){
                              return +d.x;
                            }));

                    var yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([0, 1]);


                    var svg = $wnd.d3.select(availChartContext.chartSelection).append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // The gray bars at the bottom leading up
            svg.selectAll("rect.availBar")
                    .data(availChartContext.data)
                    .enter().append("rect")
                    .attr("class", "availBar")
                    .attr("x", function (d) {
                        return timeScale(d.x);
                    })
                    .attr("y", function (d) {
                        return yScale(0);
                    })
                    .attr("height", function (d) {
                        return 8;
                    })
                    .attr("width", function (d) {
                        return  (width / availChartContext.data.length);
                    })

                    .attr("opacity", ".9")
                    .attr("fill", function (d) {
                        if (d.availType === 'DOWN') {
                            return "#FF1919"; // red
                        }
                        else if (d.availType === 'WARN') {
                            return "#FFFF00"; // yellow
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
                        else {
                            // should not ever happen, but...
                            console.warn("AvailabilityType not valid.");
                            return "#000"; //black
                        }
                    });

            createHovers();

        }

        function createHovers() {
            //console.log("Create Hovers");
            $wnd.jQuery('svg rect.availBar').tipsy({
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
                    xValue = (d.x == undefined) ? 0 : +d.x,
                    date = new Date(+xValue),
                    timeFormatter = $wnd.d3.time.format(availChartContext.chartHoverTimeFormat),
                    dateFormatter = $wnd.d3.time.format(availChartContext.chartHoverDateFormat),
                    availType = d.availType,
                    availStart = new Date(+d.availStart),
                    availEnd = new Date(+d.availEnd),
                    availDuration = d.availDuration;

            hoverString =
                    '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + availChartContext.timeLabel + ':  </span><span style="width:50px;">' + timeFormatter(date) + '</span></div>' +
                            '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + availChartContext.dateLabel + ':  </span><span style="width:50px;">' + dateFormatter(date) + '</span></div>' +
                            '<hr  class="chartHoverDivider"></hr>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverBarAvailabilityLabel + ': </span><span style="width:50px;">' + availType + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverStartLabel + ': </span><span style="width:50px;">' + timeFormatter(availStart) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' +  ' </span><span style="width:50px;">' + dateFormatter(availStart) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverEndLabel + ': </span><span style="width:50px;">' + timeFormatter(availEnd) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' +  ' </span><span style="width:50px;">' + dateFormatter(availEnd) + '</span></div>' +
                            '<div class="chartHoverAlignRight"><span >' + availChartContext.hoverBarLabel + ': </span><span style="width:50px;">' + availDuration + '</span></div>' +
                            '</div>';
            return hoverString;

        }

        if(availChartContext.data.length > 0){
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
