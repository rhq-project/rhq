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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.Iterator;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickEvent;
import com.smartgwt.client.widgets.events.CloseClickHandler;

import org.rhq.core.domain.measurement.MeasurementData;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.MeasurementDataGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.BoundedLinkedList;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableWindow;

/**
 * @author Greg Hinkle
 * @author Mike Thompson
 */
public class LiveGraphD3View extends LocatableVLayout {

    private static final int MAX_BARS_TO_KEEP = 60;
    private static final int DATA_REFRESH_TIME = 2 * 1000;
    private int resourceId;
    private MeasurementDefinition definition;
    private BoundedLinkedList<MeasurementData> data;

    private Timer dataLoaderTimer;


    public LiveGraphD3View(String locatorId, int resourceId, MeasurementDefinition def) {
        super(locatorId);
        this.resourceId = resourceId;
        this.definition = def;
        setHeight100();
        setWidth100();
        // create a sliding window of MAX_BARS_TO_KEEP to live graph
        data = new BoundedLinkedList<MeasurementData>(MAX_BARS_TO_KEEP);
    }

    public String getChartId(){
        return resourceId + "-" + definition.getId();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        for (Canvas c : getChildren()) {
            c.destroy();
        }

        drawGraph();
    }

    @Override
    protected void onDetach() {
        super.onDetach();
    }

    @Override
    protected void onUnload() {
        super.onUnload();
    }

    @Override
    public void parentResized() {
        super.parentResized();
        onDraw();
    }


    private void drawGraph() {

        if (definition != null) {

            HTMLFlow title = new HTMLFlow("<b>" + definition.getDisplayName() + "</b> " + definition.getDescription());
            addMember(title);

        }
        HTMLFlow graph = new HTMLFlow("<div id=\"liveChart-"+getChartId()+"\" ><svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\" style=\"height:100%;\"></svg></div>");
        graph.setWidth100();
        graph.setHeight100();
        addMember(graph);
        loadData();

    }


    private void loadData() {
        final MeasurementDataGWTServiceAsync dataService = GWTServiceLookup.getMeasurementDataService();

        dataLoaderTimer = new Timer() {
            @Override
            public void run() {
                dataService.findLiveData(resourceId, new int[] { definition.getId() },
                    new AsyncCallback<Set<MeasurementData>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler()
                                .handleError(MSG.view_resource_monitor_graphs_loadFailed(), caught);
                        }

                        @Override
                        public void onSuccess(Set<MeasurementData> result) {
                            Iterator<MeasurementData> i = result.iterator();
                            if (!i.hasNext()) {
                                return;
                            }

                            MeasurementDataNumeric measurementDataNumeric= (MeasurementDataNumeric) i.next();
                            data.addLast(measurementDataNumeric);

                            drawJsniCharts();

                        }
                    });
            }
        };

        dataLoaderTimer.scheduleRepeating(DATA_REFRESH_TIME);
    }

    public static void displayAsDialog(String locatorId, int resourceId, MeasurementDefinition def) {
        final LiveGraphD3View graph = new LiveGraphD3View(locatorId, resourceId, def);
        final Window graphPopup = new LocatableWindow(locatorId);
        graphPopup.setTitle(MSG.view_resource_monitor_detailed_graph_label());
        graphPopup.setWidth(800);
        graphPopup.setHeight(400);
        graphPopup.setIsModal(true);
        graphPopup.setShowModalMask(true);
        graphPopup.setCanDragResize(true);
        graphPopup.centerInPage();
        graphPopup.addItem(graph);
        graphPopup.show();

        graphPopup.addCloseClickHandler(new CloseClickHandler() {
            public void onCloseClick(CloseClickEvent closeClientEvent) {
                graphPopup.destroy();
            }
        });
    }

    protected void stop() {
        dataLoaderTimer.cancel();
    }

    @Override
    protected void onDestroy() {
        stop();
        super.onDestroy();
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
        //Log.debug("getJsonMetrics size: "+data.size());
        StringBuilder sb = new StringBuilder("[");
        for (MeasurementData measurement : data) {
            sb.append("{ x:"+measurement.getTimestamp()+",");
            sb.append(" y:"+measurement.getValue()+"},");
        }
        sb.setLength(sb.length()-1); // delete the last ','
        sb.append("]");
        return sb.toString();
    }


    public native void drawJsniCharts() /*-{
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.LiveGraphD3View::getChartId()(),
        chartHandle = "#liveChart-"+chartId,
        chartSelection = chartHandle + " svg",
        yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.LiveGraphD3View::getYAxisTitle()(),
        yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.LiveGraphD3View::getYAxisUnits()(),
        xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.LiveGraphD3View::getXAxisTitle()(),
        json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.LiveGraphD3View::getJsonMetrics()()),
        data = function() {
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
                    .tickFormat(function(d) { return $wnd.d3.time.format('%X')(new Date(d)) });

            chart.yAxis
                    .axisLabel(yAxisUnits)
                    .tickFormat($wnd.d3.format('.02f'));

            $wnd.d3.select(chartSelection)
                    .datum(data())
                    .transition().duration(100)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            return chart;
        });

    }-*/;
}
