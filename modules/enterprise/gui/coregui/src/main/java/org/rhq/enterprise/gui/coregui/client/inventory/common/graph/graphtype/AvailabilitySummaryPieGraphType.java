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

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementConverterClient;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, orange=disabled, unknown=grey,
 * empty=grey, warn=yellow.  This version of the availability graph shows continuous intervals.
 *
 * @author Mike Thompson
 */
public class AvailabilitySummaryPieGraphType {

    public static final int HEIGHT = 100;
    public static final int WIDTH = 100;

    private List<AvailabilitySummary> availabilitySummaries;

    public AvailabilitySummaryPieGraphType() {
    }

    public HTMLFlow createGraphMarker() {
        Log.debug("drawGraph marker in AvailabilitySummaryPieGraph");

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"availSummaryChart\" >");
        divAndSvgDefs.append("<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:" + HEIGHT
            + "px;\" ></svg>");
        divAndSvgDefs.append("</div>");
        HTMLFlow graphFlow = new HTMLFlow(divAndSvgDefs.toString());
        graphFlow.setWidth(WIDTH);
        graphFlow.setHeight(HEIGHT);
        return graphFlow;
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
                sb.append(" \"value\": \""
                    + MeasurementConverterClient.format(availabilitySummary.getValue(), MeasurementUnits.PERCENTAGE,
                        true) + "\" },");
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
        console.log("Draw Availability Summary Pie Chart");

        var global = this,
                w = 100,
                h = 100,
                r = h / 2,
                color = $wnd.d3.scale.category10(),
                data = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType::getAvailabilitySummaryJson()(),
                vis = $wnd.d3.select("#availSummaryChart svg")
                        .append("g")
                        .data(data)
                        .attr("width", w)
                        .attr("height", h)
                        .attr("transform", "translate(" + r + "," + r + ")"),
                arc = $wnd.d3.svg.arc()
                        .outerRadius(r),
                pie = $wnd.d3.layout.pie(),
                arcs = vis.selectAll("g.slice")
                        .data(pie)
                        .enter()
                        .append("g")
                        .attr("class", "slice");

        arcs.append("path")
                .attr("fill", function (d, i) {
                    return color(i);
                })
                .attr("d", arc);

        arcs.append("text")
                .attr("transform", function (d) {
                    d.innerRadius = 0;
                    d.outerRadius = r;
                    return "translate(" + arc.centroid(d) + ")";
                })
                .attr("text-anchor", "middle")
                .style("font-size", "9px")
                .style("font-family", "Arial, Verdana, sans-serif;")
                .attr("fill", "#000")
                .text(function (d, i) {
                    return data[i].value;
                });
        console.log("done with avail summary pie graph");

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
