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

import org.rhq.enterprise.gui.coregui.client.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the javascript chart definition for an implementation of the d3 availability chart. This implementation is
 * just a line that changes color based on availability type: up=green, down=red, orange=disabled, unknown=grey,
 * empty=grey, warn=yellow.  This version of the availability graph shows continuous intervals.
 *
 * @author Mike Thompson
 */
public class AvailabilitySummaryPieGraphType {

    private List<AvailabilitySummary> availabilitySummaries;

    public AvailabilitySummaryPieGraphType() {
    }

    public void setAvailabilityData(String upLabel, double upPercent, String downLabel, double downPercent, String disabledLabel, double disabledPercent ){
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
        console.log("Draw Availability Summary Pie Chart");

        var data = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.AvailabilitySummaryPieGraphType::getAvailabilitySummaryJson()();

        var w = 100,
            h = 100,
            r = 30,
            color = $wnd.d3.scale.category10();

        var vis = $wnd.d3.select("#availSummaryChart svg")
            .append("g")
            .data(data)
            .attr("width", w)
            .attr("height", h)
            .attr("transform", "translate(" + r + "," + r + ")");

        var arc = $wnd.d3.svg.arc()
            .outerRadius(r);

        var pie = $wnd.d3.layout.pie();

        var arcs = vis.selectAll("g.slice")
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
            .text(function (d, i) {
                return data[i].value;
            });
        console.warn("done with avail summary pie graph");

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
