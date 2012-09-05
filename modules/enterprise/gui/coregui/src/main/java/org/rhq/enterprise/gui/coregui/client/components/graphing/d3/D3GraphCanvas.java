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
package org.rhq.enterprise.gui.coregui.client.components.graphing.d3;

/**
 * @author Denis Krusko
 * @author Mike Thompson
 */
//@todo: add Generics
public class D3GraphCanvas extends AbstractGraphCanvas
{
    String chartId;
    public int serverDelay = 5000;
    public int clientDelay = 5000;
    public int step = 1000;
    //protected int width = 900;
    //protected int height = 700;

    public D3GraphCanvas(String chartId)
    {
        super(chartId);
    }

    @Override
    /**
     * Place any client overrides here.
     */
    public void initOverride() {
    }

    @Override
    public void copyShadowProperties() {
        chartId = parentChartId;
    }

    private String getPoints(int metricIndex, double start, double stop)
    {
        return dataProvider.getPointsAsJson(metricIndex, Math.round(start), Math.round(stop));
    }

    private String getMetrics()
    {
        return dataProvider.getMetricsAsJson();
    }


    public native void drawCharts() /*-{
        var context = $wnd.cubism.context()
                .serverDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::serverDelay)// allow seconds of collection lag
                .clientDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::clientDelay)
                .step(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::step)
                .size(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::width);

        var chartDiv = "#" + this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvas::chartId;
        var jsonMetrics = eval(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvas::getMetrics()());

        var metrics = [];
        var self = this;

        var selection = $wnd.d3.select(chartDiv)
                .call(function (div)
                {
                    div.append("div")
                            .attr("class", "axis")
                            .call(context.axis().orient("top"));
                    div.append("div")
                            .attr("class", "rule")
                            .call(context.rule());
                });
        for (var i = 0; i < jsonMetrics.length; i++)
        {
            var jsonMetric = jsonMetrics[i];
            var graphMetric = createMetric(jsonMetric.label, jsonMetric.metricIndex);
            var horizonContext;

            if (jsonMetric.metricUnit != 'PERCENTAGE')
            {
                horizonContext = context.horizon();
            }
            else
            {
                horizonContext = context.horizon()
                        .format($wnd.d3.format(".2%"));
            }
            selection.call(function (div)
            {
                div.datum(graphMetric);
                div.append("div")
                        .attr("class", "horizon")
                        .call(horizonContext);
            });
        }

        function createMetric(name, metricIndex)
        {
            var metric = context.metric(function (start, stop, step, callback)
            {

                var jsonPoints = self.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvas::getPoints(IDD)(metricIndex, start.getTime(), stop.getTime());

                try
                {
                    var json = jsonPoints ? eval("json=" + jsonPoints) : null;
                }
                catch (e)
                {
                    console.log(e + jsonPoints);
                }
                callback(null, json);
            }, name);
            metrics.push(metric);
            return metric;
        }

    }-*/;
}

