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

import java.util.List;

import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Mike Thompson
 */
public abstract class AbstractMetricD3GraphView extends LocatableVLayout {

    protected HTMLFlow resourceTitle;

    private int entityId;
    private int definitionId;

    private MeasurementDefinition definition;
    private List<MeasurementDataNumericHighLowComposite> data;

    private String chartHeight;
    private String svgText;

    public AbstractMetricD3GraphView(String locatorId) {
        super(locatorId);
    }


    public AbstractMetricD3GraphView(String locatorId, int entityId, MeasurementDefinition def,
                                     List<MeasurementDataNumericHighLowComposite> data) {
        this(locatorId);

        this.entityId = entityId;
        this.definition = def;
        this.data = data;
        setHeight100();
        setWidth100();
    }

    public abstract AbstractMetricD3GraphView getInstance(String locatorId, int entityId, MeasurementDefinition def,
        List<MeasurementDataNumericHighLowComposite> data);

    protected abstract void renderGraph();

    protected HTMLFlow getEntityTitle(){
        return resourceTitle;
    }

    public int getEntityId() {
        return this.entityId;
    }


    public void setEntityId(int entityId) {
        this.entityId = entityId;
        this.definition = null;
    }
    public int getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(int definitionId) {
        this.definitionId = definitionId;
        this.definition = null;
    }


    public MeasurementDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(MeasurementDefinition definition) {
        this.definition = definition;
    }

    public String getChartId(){
        return entityId + "-" + definition.getId();
    }
    public List<MeasurementDataNumericHighLowComposite> getData() {
        return data;
    }

    public void setData(List<MeasurementDataNumericHighLowComposite> data) {
        this.data = data;
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

    protected void drawGraph() {
        Log.debug("drawGraph in AbstractMetricD3GraphView for: "+ definition + ","+definitionId);

        HLayout titleHLayout = new LocatableHLayout(extendLocatorId("HTitle"));

        if (definition != null) {
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

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            title.setWidth100();
            addMember(title);
            chartHeight = (chartHeight != null) ? chartHeight : "100%";
            HTMLFlow graph = new HTMLFlow("<div id=\"rChart-"+getChartId()+"\" ><svg style=\"height:"+ chartHeight +";\"></svg></div>");
            graph.setWidth100();
            graph.setHeight100();
            addMember(graph);

            if(BrowserUtility.isBrowserIE8()){
                // @todo: drawIE8Charts()
            }else {
                drawJsniCharts();
                //svgText = graph.getContents(); //just gets the  div and svg tags nothing below
                //Log.debug("svgText set by JSNI: "+ svgText);
            }
        }



    }

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

    public void setChartHeight(String height) {
        this.chartHeight = height;
    }

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

    public void setSvgText(String svgText) {
        this.svgText = svgText;
    }

    public String getYAxisTitle(){
       return definition.getName();
    }
    public String getYAxisUnits(){
       return definition.getUnits().getName();
    }

    public String getXAxisTitle(){
       //@todo: i18n
       return "Time";
    }

    public String getJsonMetrics(){
        StringBuilder sb = new StringBuilder("[");
        for (MeasurementDataNumericHighLowComposite measurement : data) {
            sb.append("{ x:"+measurement.getTimestamp()+",");
            sb.append(" y:"+measurement.getValue()+"},");
        }
        sb.setLength(sb.length()-1); // delete the last ','
        sb.append("]");
        return sb.toString();
    }

    public native void drawJsniCharts() /*-{
        console.log("Draw nvd3 charts");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()();
        var chartHandle = "#rChart-"+chartId;
        var chartSelection = "#rChart-"+chartId + " svg";
        var yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()();
        var yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()();
        var xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()();
        var json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        var data = function() {
            return [
                {
                    values: json,
                    key: yAxisLabel ,
                    color: '#ff7f0e'
                }
            ];
        };
        $wnd.nv.addGraph(function() {
            var chart = $wnd.nv.models.multiBarChart()
                    .showControls(false)
                    .tooltips(true);

            chart.xAxis.axisLabel(xAxisLabel)
                    .tickFormat(function(d) { return $wnd.d3.time.format('%I %p')(new Date(d)) });

            chart.yAxis
                    .axisLabel(yAxisUnits)
                    .tickFormat($wnd.d3.format('.02f'));

            $wnd.d3.select(chartSelection)
                    .datum(data())
                    .transition().duration(300)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            //var aChart = $doc.getElementById(chartHandle);
            //console.log(" *** rChart id: "+ aChart.innerHTML.toString());

            //this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::setSvgText(Ljava/lang/String;)("<h>Hi Mike</h>");

            return chart;
        });

    }-*/;

}
