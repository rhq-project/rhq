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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail;

import java.util.List;

import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.enterprise.gui.coregui.client.GraphMarker;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.AvailabilityGraphType;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A special graph implementation just for representing Availability (with hovers) for
 * a resource or resource group.
 *
 * @author Mike Thompson
 */
public class AvailabilityD3GraphView<T extends AvailabilityGraphType> extends EnhancedVLayout implements GraphMarker {

    protected T availabilityGraphType;

    public AvailabilityD3GraphView(T availabilityGraphType) {
        super();
        this.availabilityGraphType = availabilityGraphType;
        setHeight(65);
        setWidth100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        addGraphMarkerComponent();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        addGraphMarkerComponent();
        drawJsniChart();
    }

    public void setAvailabilityList(List<Availability> availabilityList) {
        availabilityGraphType.setAvailabilityList(availabilityList);
    }

    public void setGroupAvailabilityList(List<ResourceGroupAvailability> groupAvailabilityList) {
        availabilityGraphType.setGroupAvailabilityList(groupAvailabilityList);
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     *
     */

    public String createGraphMarker() {
        Log.debug("drawGraph marker in AvailabilityD3Graph for: " + availabilityGraphType.getChartId());

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"availChart-" + availabilityGraphType.getChartId()
            + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:65px;\">");
        divAndSvgDefs.append(getSvgDefs());
        divAndSvgDefs.append("</svg>");
        createTooltip(divAndSvgDefs);
        divAndSvgDefs.append("</div>");
        return divAndSvgDefs.toString();
    }

    private void createTooltip(StringBuilder divAndSvgDefs) {
        divAndSvgDefs.append("<div id=\"availTooltip\" class=\"hidden\" >");
        divAndSvgDefs.append("<div>");
        divAndSvgDefs.append("<span id=\"availTooltipLabel\" class=\"availTooltipLabel\"></span>");
        divAndSvgDefs.append("<span> : </span>");
        divAndSvgDefs.append("<span id=\"availTooltipType\" style=\"width:40px;font-weight:bold;\"></span>");
        divAndSvgDefs.append("<span> - </span>");
        divAndSvgDefs.append("<span id=\"availTooltipDuration\" ></span>");
        divAndSvgDefs.append("<div/>");
        divAndSvgDefs.append("<div>");
        divAndSvgDefs.append("<span id=\"availTooltipStartDate\" ></span>");
        divAndSvgDefs.append("<span>  </span>");
        divAndSvgDefs.append("<span id=\"availTooltipStartTime\" ></span>");
        divAndSvgDefs.append("</div>");
        divAndSvgDefs.append("</div>");   // end availTooltipDiv
    }

    public void addGraphMarkerComponent(){
        HTMLFlow graph = new HTMLFlow(createGraphMarker());
        graph.setWidth100();
        graph.setHeight(65);
        addMember(graph);
    }

    /**
     * Svg definitions for patterns and gradients to use on SVG shapes.
     * @return xml String
     */
    private static String getSvgDefs() {
        return " <defs>"+
                "<pattern id=\"diagonalHatch\" patternUnits=\"userSpaceOnUse\" width=\"4\" height=\"4\">" +
                "<path d=\"M-1,1 l2,-2 M0,4 l4,-4 M3,5 l2,-2\" />" +
                "</pattern>" +
                 "</defs>";
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
     * Delegate the call to rendering the JSNI chart.
     * This way the chart type can be swapped out at any time.
     */
    public void drawJsniChart() {
        availabilityGraphType.drawJsniChart();
    }

}
