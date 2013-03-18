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
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityGraph;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AvailabilityLineGraphType;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * A special graph implementation just for representing Availability (with hovers) for
 * a resource or resource group.
 *
 * @author Mike Thompson
 */
public class AvailabilityD3Graph extends EnhancedVLayout implements AvailabilityGraph {

    protected AvailabilityLineGraphType availabilityGraphType;

    public AvailabilityD3Graph(AvailabilityLineGraphType graphType) {
        super();
        this.availabilityGraphType = graphType;
        setHeight(25);
        setWidth100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        createGraphMarker();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        createGraphMarker();
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

    public void createGraphMarker() {
        Log.debug("drawGraph marker in AvailabilityD3Graph for: " + availabilityGraphType.getChartId());

        StringBuilder divAndSvgDefs = new StringBuilder();
        divAndSvgDefs.append("<div id=\"availChart-" + availabilityGraphType.getChartId()
            + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:20px;\">");
        divAndSvgDefs.append("</svg></div>");
        HTMLFlow graph = new HTMLFlow(divAndSvgDefs.toString());
        graph.setWidth100();
        graph.setHeight(25);
        addMember(graph);

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
