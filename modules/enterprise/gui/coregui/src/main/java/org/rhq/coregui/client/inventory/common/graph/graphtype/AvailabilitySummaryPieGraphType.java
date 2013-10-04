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

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.coregui.client.GraphMarker;
import org.rhq.coregui.client.util.Log;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, unknown=grey, warn=yellow.
 * This version of the availability graph shows continuous intervals.
 *
 * @author Mike Thompson
 */
public class AvailabilitySummaryPieGraphType implements GraphMarker{

    public static final int HEIGHT = 75;
    public static final int WIDTH = 75;

    private List<AvailabilitySummary> availabilitySummaries;

    public AvailabilitySummaryPieGraphType() {
    }

    public HTMLFlow addGraphMarkerMember(){
        HTMLFlow graphFlow = new HTMLFlow(createGraphMarker());
        graphFlow.setWidth(WIDTH);
        graphFlow.setHeight(HEIGHT);
        return graphFlow;

    }
    public String createGraphMarker() {
        Log.debug("drawGraph marker in AvailabilitySummaryPieGraph");

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"availSummaryChart\" >");
        divAndSvgDefs.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:" + HEIGHT
            + "px;\" ></svg>");
        divAndSvgDefs.append("</div>");
        return divAndSvgDefs.toString();
    }

    public void setAvailabilityData(String upLabel, double upPercent, String downLabel, double downPercent,
        String disabledLabel, double disabledPercent) {
        availabilitySummaries = new ArrayList<AvailabilitySummary>();
        availabilitySummaries.add(new AvailabilitySummary(upLabel, upPercent));
        availabilitySummaries.add(new AvailabilitySummary(downLabel, downPercent));
        availabilitySummaries.add(new AvailabilitySummary(disabledLabel, disabledPercent));

    }

    public String getAvailabilitySummaryJson() {
        StringBuilder sb = new StringBuilder("[");
        if (null != availabilitySummaries) {
            // loop through the avail intervals
            for (AvailabilitySummary availabilitySummary : availabilitySummaries) {
                sb.append("{ \"label\":\"" + availabilitySummary.getLabel() + "\", ");
                sb.append(" \"value\": \"" + availabilitySummary.getValue() *  100 + "\" },");
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
        //console.log("Draw Availability Summary Pie Chart");
        var global = this;

        var availPieGraph = (function () {
            "use strict";

            var w = @org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType::WIDTH,
                h = @org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType::HEIGHT,
                    outerRadius = w / 2,
                    innerRadius = 0,
                    data = $wnd.jQuery.parseJSON(global.@org.rhq.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType::getAvailabilitySummaryJson()());

            function drawPieGraph() {

                var arc = $wnd.d3.svg.arc()
                                .innerRadius(innerRadius)
                                .outerRadius(outerRadius),

                        pie = $wnd.d3.layout.pie()
                                .value(function (d) {
                                    return d.value;
                                }),

                        colorScale = $wnd.d3.scale.ordinal().range(["#8cbe89", "#c5888b", "#d8d8d8"]),

                        svg = $wnd.d3.select("#availSummaryChart svg")
                                .append("g")
                                .attr("width", w)
                                .attr("height", h),

                        arcs = svg.selectAll("g.arc")
                                .data(pie(data))
                                .enter()
                                .append("g")
                                .attr("class", "arc")
                                .attr("transform", "translate(" + outerRadius + "," + outerRadius + ")");

                arcs.append("path")
                        .attr("fill", function (d, i) {
                            return colorScale(i);
                        })
                        .attr("d", arc).append("title").text(function (d) {
                            return d.label;
                        });

                arcs.append("text")
                        .attr("transform", function (d) {
                            return "translate(" + arc.centroid(d) + ")";
                        })
                        .attr("text-anchor", "middle")
                        .attr("fill", "#FFF")
                        .style("font-size", "12px")
                        .style("font-family", "Arial, Verdana, sans-serif;")
                        .text(function (d) {
                            return d.value;
                        });
            }

            return {
                drawGraph: function () {
                    return drawPieGraph();
                }

            };

        })();

        availPieGraph.drawGraph();
        //console.log("done with avail summary pie graph drawing");

    }-*/;

    private static class AvailabilitySummary {
        final private String label;
        final private double value;

        private AvailabilitySummary(String label, double value) {
            this.label = label;
            this.value = value;
        }

        private String getLabel() {
            return label;
        }

        private double getValue() {
            return value;
        }
    }
}
