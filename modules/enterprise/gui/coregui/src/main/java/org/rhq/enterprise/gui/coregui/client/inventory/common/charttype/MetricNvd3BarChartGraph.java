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
package org.rhq.enterprise.gui.coregui.client.inventory.common.charttype;

import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView;

/**
 * Contains the chart definition for a Bar Chart Graph.
 *
 * @author Mike Thompson
 */
public class MetricNvd3BarChartGraph extends AbstractMetricD3GraphView implements HasD3JsniChart
{

    /**
     * Constructor for dashboard portlet view as chart definition and data are deferred to later
     * in the portlet configuration.
     * @param locatorId
     */
    public MetricNvd3BarChartGraph(String locatorId) {
       super(locatorId);
    }

    /**
     * General constructor for stacked bar graph when you have all the data needed to
     * produce the graph. (This is true for all cases but the dashboard portlet).
     * @param locatorId
     * @param entityId
     * @param entityName
     * @param def
     * @param data
     */
    public MetricNvd3BarChartGraph(String locatorId, int entityId, String entityName, MeasurementDefinition def,
                                   List<MeasurementDataNumericHighLowComposite> data) {
        super(locatorId,entityId, entityName, def,data);

    }

    @Override
    protected void renderGraph()
    {
        drawJsniChart();
    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw NVD3 Bar jsni chart");
        var global = this,
            chartId =  global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()(),
            chartHandle = "#rChart-"+chartId,
            chartSelection = chartHandle + " svg",
            json = eval(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()()),
            yAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()(),
            yAxisUnits = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()(),
            xAxisLabel = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()(),
            displayDayOfWeek = global.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::shouldDisplayDayOfWeekInXAxisLabel()(),
            xAxisTimeFormat = (displayDayOfWeek) ? "%a %I %p" : "%I %p";

        // nvd3 defines their json models a standard way (same model for other graphs)
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
                    .tickFormat(function(d) { return $wnd.d3.time.format(xAxisTimeFormat)(new Date(d)) });

            chart.yAxis
                    .axisLabel(yAxisUnits)
                    .tickFormat($wnd.d3.format(',f'));

            $wnd.d3.select(chartSelection)
                    .datum(data())
                    .transition().duration(300)
                    .call(chart);

            $wnd.nv.utils.windowResize(chart.update);

            return chart;
        });

    }-*/;

}
