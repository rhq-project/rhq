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


import org.rhq.enterprise.gui.coregui.client.util.Log;

/**
 * @author Denis Krusko
 */
public class GraphCanvas extends AbstractGraphCanvas
{
    String chartId;
    public int serverDelay = 5000;
    public int clientDelay = 5000;
    public int step = 3000;
    protected int width = 900;
    protected int height = 700;

    public GraphCanvas(String chartId)
    {
        super(chartId);
    }

    @Override
    public void initOverride() {
        // TODO: Implement this method.
    }

    @Override
    public void copyShadowProperties() {
        chartId = parentChartId;
    }

    public GraphCanvas(String chartId, int serverDelay, int clientDelay, int step)
    {
        this(chartId);
        this.serverDelay = serverDelay;
        this.clientDelay = clientDelay;
        this.step = step;
    }


    private String getPoints(int metricIndex, double start, double stop)
    {
        Log.info(" *** GraphCanvas.getPoints");
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

        var chartDiv = "#" + this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::chartId;
        var jsonMetrics = eval(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::getMetrics()());


        var metrics = [];
        var self = this;

        function createMetric(name, metricIndex)
        {
            var metric = context.metric(function (start, stop, step, callback)
            {

                var jsonPoints = self.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::getPoints(IDD)(metricIndex, start.getTime(), stop.getTime());

                try
                {
                    var json = jsonPoints ? eval("tmp=" + jsonPoints) : null;
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
            var metricI = createMetric(jsonMetric.label, jsonMetric.metricIndex);
            var horizonI;

            if (jsonMetric.metricUnit != 'PERCENTAGE')
            {
                horizonI = context.horizon();
            }
            else
            {
                horizonI = context.horizon()
                        .format($wnd.d3.format(".2%"));
            }
            selection.call(function (div)
            {
                div.datum(metricI);
                div.append("div")
                        .attr("class", "horizon")
                        .call(horizonI);
            });
        }
        // On mousemove, reposition the chart values to match the rule.
        context.on("focus", function (i)
        {
            $wnd.d3.selectAll(".value").style("right", i == null ? null : context.size() - i + "px");
        });
    }-*/;
}

