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


import com.smartgwt.client.widgets.HTMLFlow;

/**
 * @author Denis Krusko
 * @author Mike Thompson
 */
public class D3GraphCanvasBuilder extends com.smartgwt.client.widgets.Canvas
{

    HTMLFlow flow;

    private int width;
    private int height;
    private int serverDelay = 0;
    private int clientDelay = 0;
    private int step = 1000;
    private GraphDataProvider dataProvider;
    private String chartId;
    private boolean initialized = false;

    public D3GraphCanvasBuilder(String chartId)
    {
        this.chartId = chartId;
        init();
    }


    private void init()
    {
        flow = new HTMLFlow();
        flow.setContents("<div id = \"" + chartId + "\" />");
        this.setHeight100();
        this.setWidth100();
        this.addChild(flow);
    }

    public D3GraphCanvasBuilder(String chartId, int serverDelay, int clientDelay, int step)
    {
        this(chartId);
        this.serverDelay = serverDelay;
        this.clientDelay = clientDelay;
        this.step = step;
    }

    @Override
    public void setWidth(int width)
    {
        this.width = width;
        flow.setWidth(width);
    }

    @Override
    public void setHeight(int height)
    {
        this.height = height;
        flow.setHeight(height);
    }

    public void setDataProvider(GraphDataProvider dataProvider)
    {
        this.dataProvider = dataProvider;
        //dataProvider.initDataProvider(this, step);
        initialized = true;
    }

    private String getPoints(int metricIndex, double start, double stop)
    {
        return dataProvider.getPointsAsJson(metricIndex, Math.round(start), Math.round(stop));
    }

    private String getMetrics()
    {
        return dataProvider.getMetricsAsJson();
    }

    @Override
    public void redraw()
    {
        super.redraw();
        if (initialized)
        {
            drawCharts();
        }
    }

    public native void drawCharts() /*-{
        var context = $wnd.cubism.context()
                .serverDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::serverDelay)// allow seconds of collection lag
                .clientDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::clientDelay)
                .step(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::step)
                .size(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::width);

        var chartDiv = "#" + this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::chartId;
        var jsonMetrics = eval(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::getMetrics()());


        var metrics = [];
        var self = this;

        function createMetric(name, metricIndex)
        {
            var metric = context.metric(function (start, stop, step, callback)
            {

                var jsonPoints = self.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.D3GraphCanvasBuilder::getPoints(IDD)(metricIndex, start.getTime(), stop.getTime());

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


        // create the json metrics from real data
        for (var i = 0; i < jsonMetrics.length; i++)
        {
            var jsonMetric = jsonMetrics[i];
            var aMetric = createMetric(jsonMetric.label, jsonMetric.metricIndex);
            var theHorizon;

            if (jsonMetric.metricUnit != 'PERCENTAGE')
            {
                theHorizon = context.horizon();
            }
            else
            {
                theHorizon = context.horizon()
                        .format($wnd.d3.format(".2%"));
            }
            selection.call(function (div)
            {
                div.datum(aMetric);
                div.append("div")
                        .attr("class", "horizon")
                        .call(theHorizon);
            });
        }
        // On mousemove, reposition the chart values to match the rule.
        context.on("focus", function (i)
        {
            $wnd.d3.selectAll(".value").style("right", i == null ? null : context.size() - i + "px");
        });
    }-*/;
}


