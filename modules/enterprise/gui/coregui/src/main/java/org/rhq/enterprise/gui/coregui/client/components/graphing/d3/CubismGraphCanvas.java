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
public class CubismGraphCanvas extends AbstractGraphCanvas
{
    String chartId;
    public int serverDelay = 1000;
    public int clientDelay = 1000;
    public int step = 400;
    protected int width = 600;
    protected int height = 700;

    public CubismGraphCanvas(String chartId)
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

    public CubismGraphCanvas(String chartId, int serverDelay, int clientDelay, int step)
    {
        this(chartId);
        this.serverDelay = serverDelay;
        this.clientDelay = clientDelay;
        this.step = step;
    }



    public String getPointsAsJson(int metricIndex, double start, double stop)
    {
        Log.info(" *** GraphCanvas.getPoints");
        // @todo: this guy is not returning data
        return dataProvider.getPointsAsJson(metricIndex, Math.round(start), Math.round(stop));
    }

    private String getMetricsAsJson()
    {
        return dataProvider.getMetricsAsJson();
    }

//    public native void determineAxisLabels(String metricUnit) /*-{
//        var context = $wnd.cubism.context();
//        console.log("Metric Unit: "+ metricUnit);
//        var horizonContext;
//        if (metricUnit != 'PERCENTAGE')
//        {
//            horizonContext = context.horizon();
//        }
//        else
//        {
//            horizonContext = context.horizon()
//                    .format($wnd.d3.format(".2%"));
//        }
//    }-*/;

//    public native JavaScriptObject createMetric(String name, int metricIndex) /*-{
//        //function createMetric(name, metricIndex) {
//        var context = $wnd.cubism.context();
//        var metrics = [];
//        var metric = new Object();
//        metric = context.metric(function (start, stop, step, callback)
//            {
//                // query for metrics matching time
//                var jsonPoints = this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::getPoints(IDD)(metricIndex, start.getTime(), stop.getTime());
//
//                try
//                {
//                    var json = jsonPoints ? eval("json=" + jsonPoints) : null;
//                }
//                catch (e)
//                {
//                    console.log(e + jsonPoints);
//                }
//                callback(null, json);
//            }, name);
//            // push onto the stack of previously graphed metrics
//            metrics.push(metric);
//            return metric;
//        //}
//     }-*/;

//    public native void loadJson() /*-{
//        var context = $wnd.cubism.context();
//        var metrics = [];
//        var jsonMetrics = eval(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::getMetricsAsJson()());
//        for (var i = 0; i < jsonMetrics.length; i++)
//        {
//            var jsonMetric = jsonMetrics[i];
//            console.log('Label: '+jsonMetric.label + ':'+jsonMetric.metricIndex);
//            var metric = createMetric(jsonMetric.label, jsonMetric.metricIndex);
//            //var metric = this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::createMetric(Ljava/lang/String;I)(jsonMetric.label, jsonMetric.metricIndex);
//            var horizonContext;
//            var chartDiv = "#" + this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::chartId;
//            var selection = $wnd.d3.select(chartDiv);
//
//            // this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.GraphCanvas::determineAxisLabels(Ljava/lang/String;)(jsonMetric.metricUnit);
//            if (jsonMetric.metricUnit != 'PERCENTAGE')
//            {
//                horizonContext = context.horizon();
//            }
//            else
//            {
//                horizonContext = context.horizon()
//                        .format($wnd.d3.format(".2%"));
//            }
//            horizonContext.height(90);
//            horizonContext.mode("mirror");
//
//            selection.call(function (div)
//            {
//                div.datum(metric);
//                div.append("div")
//                        .attr("class", "horizon")
//                        .call(horizonContext);
//            });
//        }
//
//        function createMetric(name, metricIndex)
//        {
//            var metric = context.metric(function (start, stop, step, callback)
//            {
//                console.log('Creating metrics');
//                // query for metrics matching time
//                var jsonPoints = this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::getPointsAsJson(IDD)(metricIndex, start.getTime(), stop.getTime());
//                console.log('Creating metrics 1' + jsonPoints);
//
//                try
//                {
//                    var json = jsonPoints ? eval("json=" + jsonPoints) : null;
//                    console.log('Creating metrics 2');
//                }
//                catch (e)
//                {
//                    console.log(e + jsonPoints);
//                }
//                callback(null, json);
//            }, name);
//            // push onto the stack of previously graphed metrics
//            metrics.push(metric);
//            return metric;
//        }
//
//
//    }-*/;

    @Override
    public native void drawCharts() /*-{
        var context = $wnd.cubism.context()
                .serverDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::serverDelay)// allow seconds of collection lag
                .clientDelay(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::clientDelay)
                .step(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::step)
                .size(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::width)
                .stop();

        var chartDiv = "#" + this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::chartId;
        var jsonMetrics = eval(this.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::getMetricsAsJson()());


        var metrics = [];
        var self = this;

        function createMetric(name, metricIndex)
        {
            var metric = context.metric(function (start, stop, step, callback)
            {

                var jsonPoints = self.@org.rhq.enterprise.gui.coregui.client.components.graphing.d3.CubismGraphCanvas::getPointsAsJson(IDD)(metricIndex, start.getTime(), stop.getTime());

                try
                {
                    var json = jsonPoints ? eval("tmp=" + jsonPoints) : null;
                    console.log("JsonPoints :"+json);
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
            horizonContext.height(90);

            selection.call(function (div)
            {
                div.datum(graphMetric);
                div.append("div")
                        .attr("class", "horizon")
                        .call(horizonContext);
            });
        }
        // On mousemove, reposition the chart values to match the rule.
        context.on("focus", function (i)
        {
            $wnd.d3.selectAll(".value").style("left", i == null ? null : context.size() - i + "px");
        });
    }-*/;
}

