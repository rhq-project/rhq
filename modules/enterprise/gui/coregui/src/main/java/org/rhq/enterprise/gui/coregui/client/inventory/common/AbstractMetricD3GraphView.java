/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Provide common functionality for graph views. Such as setting up the divs with
 * specific ids that the jsni svn graphs can bind to and place their svg definitions
 * of the charts directly under this chart div. Essentially, it creates the placeholder
 * for the graphs to later render themselves.
 *
 * @author Mike Thompson
 */
public abstract class AbstractMetricD3GraphView extends LocatableVLayout {

    protected HTMLFlow resourceTitle;
    protected MetricGraphData metricGraphData;

    //private String chartHeight;

    public AbstractMetricD3GraphView(String locatorId) {
        super(locatorId);
    }


    public AbstractMetricD3GraphView(String locatorId, MetricGraphData metricGraphData){
        this(locatorId);
        this.metricGraphData = metricGraphData;
        setHeight100();
        setWidth100();
    }

    protected abstract void renderGraph();

    protected HTMLFlow getEntityTitle(){
        return resourceTitle;
    }



    @Override
    protected void onDraw() {
        super.onDraw();
        removeMembers(getMembers());
        renderGraph();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        removeMembers(getMembers());
        renderGraph();
    }

    /**
     * Setup the page elements especially the div and svg elements that serve as
     * placeholders for the d3 stuff to grab onto and add svg tags to render the chart.
     * Later the drawJsniGraph() is called to actually fill in the div/svg element
     * created here with the actual svg element.
     *
     */
    protected void drawGraph() {
        Log.debug("drawGraph marker in AbstractMetricD3GraphView for: " + metricGraphData.getChartId());

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        //if (definition != null) {
            titleHLayout.setAutoHeight();
            titleHLayout.setWidth100();

            HTMLFlow entityTitle = getEntityTitle();
            if (null != entityTitle) {
                entityTitle.setWidth("*");
                titleHLayout.addMember(entityTitle);
            }

            if (supportsLiveGraphViewDialog()) {
                Img liveGraph = createLiveGraphImage();
                titleHLayout.addMember(liveGraph);
            }

            addMember(titleHLayout);

            StringBuilder divAndSvgDefs = new StringBuilder();
            divAndSvgDefs.append("<div id=\"rChart-" + metricGraphData.getChartId() + "\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:320px;\">");
            divAndSvgDefs.append(getSvgDefs());
            divAndSvgDefs.append("</svg></div>");
            HTMLFlow graph = new HTMLFlow(divAndSvgDefs.toString());
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            new Timer() {
                @Override
                public void run() {
                    //@todo: this is a hack around timing issue of jsni not seeing the DOM
                    drawJsniChart();
                }
            }.schedule(100);
        //}
    }

    /**
     * Svg definitions for patterns and gradients to use on SVG shapes.
     * @return xml String
     */
    private static String getSvgDefs(){
      return " <defs>" +
              "               <linearGradient id=\"headerGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#707883;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#425b64;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"leaderBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#d3d3d6;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#d3d3d6;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"heavyLeaderBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#a7a7ac;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#a7a7ac;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"topBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#067aba;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#1278a8;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <linearGradient id=\"bottomBarGrad\" x1=\"0%\" y1=\"0%\" x2=\"0%\" y2=\"100%\">" +
              "                   <stop offset=\"0%\" style=\"stop-color:#6bc0e6;stop-opacity:1\"/>" +
              "                   <stop offset=\"100%\" style=\"stop-color:#388bb0;stop-opacity:1\"/>" +
              "               </linearGradient>" +
              "               <pattern id=\"grayStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\"" +
              "                        width=\"6\" height=\"3\">" +
              "                   <path d=\"M 0 0 6 0\" style=\"stroke:#dfdfdf; fill:none;\"/>" +
              "               </pattern>" +
              "               <pattern id=\"redStripes\" patternUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\"" +
              "                        width=\"6\" height=\"3\">" +
              "                   <path d=\"M 0 0 6 0\" style=\"stroke:#ff8a9a; fill:none;\"/>" +
              "               </pattern>" +
              "</defs>";
    }

    public abstract  void drawJsniChart();

    private Img createLiveGraphImage() {
        Img liveGraph = new LocatableImg(extendLocatorId("Live"), IconEnum.RECENT_MEASUREMENTS.getIcon16x16Path(), 16, 16);
        liveGraph.setTooltip(MSG.view_resource_monitor_graph_live_tooltip());

        liveGraph.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                displayLiveGraphViewDialog();
            }
        });
        return liveGraph;
    }

//    public void setChartHeight(String height) {
//        this.chartHeight = height;
//    }

    protected boolean supportsLiveGraphViewDialog() {
        return false;
    }

    protected void displayLiveGraphViewDialog() {
        return;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    @Override
    public void hide() {
        super.hide();
    }

}
