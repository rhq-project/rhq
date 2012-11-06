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
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.List;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.enterprise.gui.coregui.client.HasD3JsniChart;

/**
 * Contains the chart definition for a Candlestick chart.
 *
 * @author Mike Thompson
 */
public abstract class MetricCandlestickGraphView extends AbstractMetricD3GraphView implements HasD3JsniChart
{
   public MetricCandlestickGraphView(String locatorId){
            super(locatorId);
   }


    public MetricCandlestickGraphView(String locatorId, int entityId, MeasurementDefinition def,
                                      List<MeasurementDataNumericHighLowComposite> data) {
        super(locatorId,entityId, def,data);

    }

    /**
     * The magic JSNI to draw the charts with d3.
     */
    public native void drawJsniChart() /*-{
        console.log("Draw Candlestick jsni chart");
        var chartId =  this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getChartId()(),
            chartHandle = "#rChart-"+chartId,
            chartSelection = chartHandle + " svg",
            yAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisTitle()(),
            yAxisUnits = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getYAxisUnits()(),
            xAxisLabel = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getXAxisTitle()(),
            displayDayOfWeek = this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::shouldDisplayDayOfWeekInXAxisLabel()(),
            xAxisTimeFormat = (displayDayOfWeek) ? "%a %I %p" : "%I %p",
            json = eval(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        console.log("chart id: "+chartSelection );
        console.log(this.@org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMetricD3GraphView::getJsonMetrics()());

        var data = function() {
            return [
                {
                    values: json,
                    key: yAxisLabel
                }
            ];
        };

        var width = 400;
        var height = 300;

        var stockData = [{"date":"2012-10-26","Date":"2012-10-26","Open":"609.43","High":"614.00","Low":"591.00","Close":"604.00","Volume":"36361900","Adj_Close":"604.00"},{"date":"2012-10-25","Date":"2012-10-25","Open":"620.00","High":"622.00","Low":"605.55","Close":"609.54","Volume":"23440200","Adj_Close":"609.54"},{"date":"2012-10-24","Date":"2012-10-24","Open":"621.44","High":"626.55","Low":"610.64","Close":"616.83","Volume":"19947400","Adj_Close":"616.83"},{"date":"2012-10-23","Date":"2012-10-23","Open":"631.00","High":"633.90","Low":"611.70","Close":"613.36","Volume":"25255200","Adj_Close":"613.36"},{"date":"2012-10-22","Date":"2012-10-22","Open":"612.42","High":"635.38","Low":"610.76","Close":"634.03","Volume":"19526100","Adj_Close":"634.03"},{"date":"2012-10-19","Date":"2012-10-19","Open":"631.05","High":"631.77","Low":"609.62","Close":"609.84","Volume":"26574500","Adj_Close":"609.84"},{"date":"2012-10-18","Date":"2012-10-18","Open":"639.59","High":"642.06","Low":"630.00","Close":"632.64","Volume":"17022300","Adj_Close":"632.64"}];

        function min(a, b){ return a < b ? a : b ; }

        function max(a, b){ return a > b ? a : b; }

        function buildChart(data2){

            var margin = 50;

            var chart = $wnd.d3.select(chartSelection)
                    .append("svg:svg")
                    .attr("class", "chart")
                    .attr("width", width)
                    .attr("height", height);
            console.log(" one ");

            var y = $wnd.d3.scale.linear()
                    .domain([$wnd.d3.min(data2.map(function(x) {return x["Low"];})), $wnd.d3.max(data2.map(function(x){return x["High"];}))])
                    .range([height-margin, margin]);
            console.log(" one.five ");
            var x = $wnd.d3.scale.linear()
                    //.domain([$wnd.d3.min(data2.map(function(d){return d["date"];})), $wnd.d3.max(data2.map(function(d){ return d["date"];}))])
                    .domain([$wnd.d3.min(data2.map(function(d){return $wnd.d3.time.format("%Y-%m-%d").parse(d["date"]);})), $wnd.d3.max(data2.map(function(d){ return $wnd.d3.time.format("%Y-%m-%d").parse(d["date"]);}))])
                    .range([margin,width-margin]);
            console.log(" two ");

            chart.selectAll("line.x")
                    .data(x.ticks(10))
                    .enter().append("svg:line")
                    .attr("class", "x")
                    .attr("x1", x)
                    .attr("x2", x)
                    .attr("y1", margin)
                    .attr("y2", height - margin)
                    .attr("stroke", "#ccc");

            chart.selectAll("line.y")
                    .data(y.ticks(10))
                    .enter().append("svg:line")
                    .attr("class", "y")
                    .attr("x1", margin)
                    .attr("x2", width - margin)
                    .attr("y1", y)
                    .attr("y2", y)
                    .attr("stroke", "#ccc");

            console.log(" three ");
            chart.selectAll("text.xrule")
                    .data(x.ticks(10))
                    .enter().append("svg:text")
                    .attr("class", "xrule")
                    .attr("x", x)
                    .attr("y", height - margin)
                    .attr("dy", 20)
                    .attr("text-anchor", "middle")
                    .text(function(d){ var date = new Date(d * 1000);  return (date.getMonth() + 1)+"/"+date.getDate(); });

            chart.selectAll("text.yrule")
                    .data(y.ticks(10))
                    .enter().append("svg:text")
                    .attr("class", "yrule")
                    .attr("x", width - margin)
                    .attr("y", y)
                    .attr("dy", 0)
                    .attr("dx", 20)
                    .attr("text-anchor", "middle")
                    .text(String);

            console.log(" four ");
            chart.selectAll("rect")
                    .data(data2)
                    .enter().append("svg:rect")
                    .attr("x", function(d) { return x(d.timestamp); })
                    .attr("y", function(d) {return y(max(d.Open, d.Close));})
                    .attr("height", function(d) { return y(min(d.Open, d.Close))-y(max(d.Open, d.Close));})
                    .attr("width", function(d) { return 0.5 * (width - 2*margin)/data2.length; })
                    .attr("fill",function(d) { return d.Open > d.Close ? "red" : "green" ;});

            console.log(" five ");
            chart.selectAll("line.stem")
                    .data(data2)
                    .enter().append("svg:line")
                    .attr("class", "stem")
                    .attr("x1", function(d) { return x(d.timestamp) + 0.25 * (width - 2 * margin)/ data2.length;})
                    .attr("x2", function(d) { return x(d.timestamp) + 0.25 * (width - 2 * margin)/ data2.length;})
                    .attr("y1", function(d) { return y(d.High);})
                    .attr("y2", function(d) { return y(d.Low); })
                    .attr("stroke", function(d){ return d.Open > d.Close ? "red" : "green"; });
            console.log(" six ");

        }


        console.log("Getting ready to build chart");
        buildChart(eval(stockData));
        console.log("Built chart");

    }-*/;

}
