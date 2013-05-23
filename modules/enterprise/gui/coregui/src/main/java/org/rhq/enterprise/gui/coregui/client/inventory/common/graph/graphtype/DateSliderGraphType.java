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
package org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype;


/**
 * The simplest of a all graph types as this d3 graph does not even require data, just x axis range so we can plot an
 * x-axis. No y data values needed. The purpose of this graph is to provide an x axis and a way to drag a
 * range over it (via d3 brushes).
 *
 * @author Mike Thompson
 */
public class DateSliderGraphType {

    private Long startDateTime;
    private Long endDateTime;

    public DateSliderGraphType(Long startTime, Long endTime) {
        this.startDateTime = startTime;
        this.endDateTime = endTime;
    }


    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart(double start, double end) /*-{
        console.log("Draw Date Range graph");

        var dateSliderGraph = function () {
            "use strict";
            // privates

            var margin = {top: 5, right: 5, bottom: 5, left: 40},
                    barOffset = 10,
                    width = 750 - margin.left - margin.right + barOffset,
                    height = 40 - margin.top - margin.bottom,
                    svg;


            function drawBars(startTime, endTime) {

                var timeScale = $wnd.d3.time.scale()
                                .range([0, width])
                                .domain([startTime, endTime]),

                        xAxis = $wnd.d3.svg.axis()
                                .scale(timeScale)
                                .ticks(8)
                                .tickSize(13, 0, 0)
                                .orient("bottom"),

                        svg = $wnd.d3.select("#dateSlider svg").append("g")
                                .attr("width", width + margin.left + margin.right)
                                .attr("height", height + margin.top + margin.bottom)
                                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("fill", "#b0b0b0")
                        .attr("stroke-width", "0.5")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .call(xAxis);


                // here is all the brush related stuff
                var brush = $wnd.d3.svg.brush()
                        .x(timeScale)
                        .extent([startTime, endTime])
                        .on("brushstart", brushstart)
                        .on("brush", brushmove)
                        .on("brushend", brushend);


                var brushg = svg.append("g")
                        .attr("class", "brush")
                        .call(brush);

                brushg.selectAll(".resize").append("path")
                        .attr("transform", "translate(0," + height / 2 + ")")
                        .attr("d", resizePath);

                brushg.selectAll("rect")
                        .attr("height", 20);

                brushstart();
                brushmove();

                function brushstart() {
                    svg.classed("selecting", true);
                }

                function brushmove() {
                    var s = brush.extent();
                    //circle.classed("selected", function(d) { return s[0] <= d && d <= s[1]; });
                    updateDateRangeDisplay($wnd.moment(s[0]), $wnd.moment(s[1]));
                }

                function brushend() {
                    var s = brush.extent();
                    svg.classed("selecting", !$wnd.d3.event.target.empty());
                    updateDateRangeDisplay($wnd.moment(s[0]), $wnd.moment(s[1]));
                }

                function updateDateRangeDisplay(startDate, endDate ) {
                    var formattedDateRange = startDate.format('MM/DD/YYYY h:mm a') + ' - ' + endDate.format('MM/DD/YYYY h:mm a');
                    $wnd.jQuery('#dateRange').val(formattedDateRange);
                }

                // Taken from crossfilter (http://square.github.com/crossfilter/)
                function resizePath(d) {
                    var e = +(d == 'e'),
                            x = e ? 1 : -1,
                            y = height / 3;
                    return 'M' + (.5 * x) + ',' + y
                            + 'A6,6 0 0 ' + e + ' ' + (6.5 * x) + ',' + (y + 6)
                            + 'V' + (2 * y - 6)
                            + 'A6,6 0 0 ' + e + ' ' + (.5 * x) + ',' + (2 * y)
                            + 'Z'
                            + 'M' + (2.5 * x) + ',' + (y + 8)
                            + 'V' + (2 * y - 8)
                            + 'M' + (4.5 * x) + ',' + (y + 8)
                            + 'V' + (2 * y - 8);
                }
            }

            return {
                // Public API
                draw: function (startTime, endTime) {
                    "use strict";
                    console.log("DateSliderChart");
                    drawBars(startTime, endTime);
                }
            }; // end public closure


        }();

        dateSliderGraph.draw(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.DateSliderGraphType::getStartTime()(), this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.DateSliderGraphType::getEndTime()());

    }-*/;

    public Long getStartTime() {
        return startDateTime;
    }

    public void setStartDateTime(Long startDateTime) {
        this.startDateTime = startDateTime;
    }


    public Long getEndTime() {
        return endDateTime;
    }


    public void setEndDateTime(Long endDateTime) {
        this.endDateTime = endDateTime;
    }


}
