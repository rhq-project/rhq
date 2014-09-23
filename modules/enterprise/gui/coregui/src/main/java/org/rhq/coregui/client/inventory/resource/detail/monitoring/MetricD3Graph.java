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
package org.rhq.coregui.client.inventory.resource.detail.monitoring;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.coregui.client.GraphMarker;
import org.rhq.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A D3 graph implementation for graphing Resource metrics.
 * Just the graph only. No avail graph no buttons just he graph.
 */
public class MetricD3Graph<T extends AbstractD3GraphListView> extends EnhancedVLayout implements Refreshable,GraphMarker {

    protected StackedBarMetricGraphImpl graph;
    private HTMLFlow graphDiv = null;
    protected Timer refreshTimer;
    private T d3GraphListView;

    /**
     * This constructor is for the use case in the Dashboard where we dont actually
     * have a entity or measurement yet.
     */
    public MetricD3Graph() {
        super();
    }

    public MetricD3Graph(StackedBarMetricGraphImpl graph, T graphListView) {
        super();
        this.graph = graph;
        this.d3GraphListView = graphListView;
        setHeight100();
        setWidth100();
    }

    /**
     * Svg definitions for patterns and gradients to use on SVG shapes.
     * @return xml String
     */
    private static String getSvgDefs() {
        return " <defs>"
            + " <linearGradient id=\"headerGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">"
            + "   <stop offset=\"0%\" style=\"stop-color:#E6E6E6;stop-opacity:1\"/>"
            + "   <stop offset=\"100%\" style=\"stop-color:#F0F0F0;stop-opacity:1\"/>"
            + " </linearGradient>"
            + " <pattern id=\"noDataStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + " width=\"6\" height=\"3\">"
            + "   <path d=\"M 0 0 6 0\" style=\"stroke:#CCCCCC; fill:none;\"/>"
            + " </pattern>"
            + " <pattern id=\"unknownStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + " width=\"6\" height=\"3\">"
            + "   <path d=\"M 0 0 6 0\" style=\"stroke:#2E9EC2; fill:none;\"/>"
            + " </pattern>"
            + "<pattern id=\"diagonalHatchFill\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"105\" height=\"105\">"
            + "<g style=\"fill:none; stroke:black; stroke-width:1\">"
            + "<path d=\"M0 90 l15,15\"/>"
            + "<path d=\"M0 75 l30,30\"/>"
            + "<path d=\"M0 60 l45,45\"/>"
            + "<path d=\"M0 45 l60,60\"/>"
            + "<path d=\"M0 30 l75,75\"/>"
            + "<path d=\"M0 15 l90,90\"/>"
            + "<path d=\"M0 0 l105,105\"/>"
            + "<path d=\"M15 0 l90,90\"/>"
            + "<path d=\"M30 0 l75,75\"/>"
            + "<path d=\"M45 0 l60,60\"/>"
            + "<path d=\"M60 0 l45,45\"/>"
            + "<path d=\"M75 0 l30,30\"/>"
            + "<path d=\"M90 0 l15,15\"/>"
            + "</g>"
            + "</pattern>"
            + "<pattern id=\"diagonalHatch\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"105\" height=\"105\">"
            + "<g style=\"fill:none; stroke:black; stroke-width:1\">" + "<path d=\"M0 90 l15,15\"/>"
            + "<path d=\"M0 75 l30,30\"/>" + "<path d=\"M0 60 l45,45\"/>" + "<path d=\"M0 45 l60,60\"/>"
            + "<path d=\"M0 30 l75,75\"/>" + "<path d=\"M0 15 l90,90\"/>" + "<path d=\"M0 0 l105,105\"/>"
            + "<path d=\"M15 0 l90,90\"/>" + "<path d=\"M30 0 l75,75\"/>" + "<path d=\"M45 0 l60,60\"/>"
            + "<path d=\"M60 0 l45,45\"/>" + "<path d=\"M75 0 l30,30\"/>" + "<path d=\"M90 0 l15,15\"/>" + "</g>"
            + "</pattern>"
            + "<pattern id=\"downStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\""
            + "      width=\"6\" height=\"3\">"
            + "<path d=\"M 0 0 6 0\" style=\"stroke:#ff8a9a; fill:none;\"/>"
            + "</pattern>" + "</defs>";
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (graph.getDefinition() != null) {
            drawGraph();
        }
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        drawGraph();
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniChart() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     *
     */
    protected void drawGraph() {
        final String divAndSvgDefs = createGraphMarker();

        if (null != graphDiv) {
            removeMember(graphDiv);
        }

        graphDiv = new HTMLFlow(divAndSvgDefs);
        setupGraphDiv(graphDiv);
        addMember(graphDiv);

        drawJsniChart();

    }

    /**
     * for subclasses to apply additional styling to graphDiv right before it get's drawn with SVG chart 
     * @param graphDiv
     */
    protected void setupGraphDiv(HTMLFlow graphDiv) {
        graphDiv.setWidth100();
        graphDiv.setHeight100();
    }

    /**
     * This is used to explicitly use the 2 phase creation of a graph separately.
     * Used to add the chart to something custom.
     * @return String Graph Marker to be filled in with drawJsniChart() later
     */
    public String createGraphMarker() {
      return createGraphMarkerTemplate(getFullChartId(), getHeight());
    }

    /**
     * A static version of createGraphMarker that can be used when a MetricD3Graph is not
     * instantiated.
     * @param fullChartId
     * @param height
     * @return String chart marker
     */
    public static String createGraphMarkerTemplate(String fullChartId, Integer height){
        Log.debug("drawGraph marker in MetricD3Graph for: " + fullChartId);

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs
                .append("<div id=\""
                        + fullChartId
                        + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" style=\"height:"
                        + height + "px;\">");
        divAndSvgDefs.append(getSvgDefs());
        divAndSvgDefs.append("</svg>");
        divAndSvgDefs.append("</div>");

        return divAndSvgDefs.toString();
    }


    /**
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart() {
        new Timer() {
            @Override
            public void run() {
                graph.drawJsniChart();
            }
        }.schedule(200);
    }

    public String getFullChartId() {
        String graphMarkerPrefix = graph.getMetricGraphData().isSummaryGraph() ? "sChart-" : "rChart-";
        return graphMarkerPrefix + graph.getMetricGraphData().getChartId();

    }

    public Integer getChartHeight() {
        return graph.getMetricGraphData().getChartHeight();
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
    }

    /**
     * Allow the graph to refresh  the whole d3GraphListView.
     */
    public void refreshData(){
        d3GraphListView.refreshData();
    }

}
