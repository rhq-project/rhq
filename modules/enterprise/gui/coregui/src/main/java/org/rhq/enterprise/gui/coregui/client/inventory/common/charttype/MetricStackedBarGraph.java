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

/**
 * Contains the javascript chart definition for a d3 Stacked Bar graph chart.
 *
 * @author Mike Thompson
 */
public final class MetricStackedBarGraph extends AbstractGraph {

    /**
     * General constructor for stacked bar graph when you have all the data needed to produce the graph. (This is true
     * for all cases but the dashboard portlet).
     */
    public MetricStackedBarGraph(MetricGraphData metricGraphData) {
        setMetricGraphData(metricGraphData);
    }


    /**
     * The magic JSNI to draw the charts with $wnd.d3.js
     */
    @Override
    public native void drawJsniChart() /*-{

        console.log("Draw Stacked Bar jsni chart");
        var global = this;

        // create a chartContext object (from rhq.js) with the data required to render to a chart
        // this same data could be passed to different chart types
        // This way, we are decoupled from the dependency on globals and JSNI and kept all the java interaction right here.
        var chartContext = new $wnd.ChartContext(global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartId()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHeight()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getJsonMetrics()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getXAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getYAxisTitle()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getYAxisUnits()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitleMinLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitleAvgLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTitlePeakLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartDateLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartTimeLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartDownLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartUnknownLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartNoDataLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverStartLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverEndLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverPeriodLabel()(),
                global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::getChartHoverBarLabel()()
        );


        // Define the Stacked Bar Graph function using the module pattern
        var metricStackedBarGraph = function () {
            "use strict";
            // privates

            console.log("Chart Height: "+chartContext.height);
            var margin = {top: 10, right: 5, bottom: 5, left: 40},
                    width = 750 - margin.left - margin.right,
                    adjustedChartHeight = chartContext.chartHeight - 50,
                    height = adjustedChartHeight - margin.top - margin.bottom,
                    titleHeight = 30, titleSpace = 10,
                    barOffset = 2,
                    interpolation = "basis";

            var avg = $wnd.d3.mean(chartContext.data.map(function (d) {
                        return d.y;
                    })),
                    peak = $wnd.d3.max(chartContext.data.map(function (d) {
                        return d.high;
                    })),
                    min = $wnd.d3.min(chartContext.data.map(function (d) {
                        return d.low;
                    })),
                    timeScale = $wnd.d3.time.scale()
                            .range([0, width])
                            .domain($wnd.d3.extent(chartContext.data, function (d) {
                                return d.x;
                            })),

            // adjust the min scale so blue low line is not in axis
                    determineLowBound = function (min, peak) {
                        var newLow = min - ((peak - min) * 0.1);
                        if (newLow < 0) {
                            return 0;
                        }
                        else {
                            return newLow;
                        }
                    },
                    lowBound = determineLowBound(min, peak),
                    highBound = peak + ((peak - min) * 0.1),

                    yScale = $wnd.d3.scale.linear()
                            .clamp(true)
                            .rangeRound([height, 0])
                            .domain([lowBound, highBound]),

                    xAxis = $wnd.d3.svg.axis()
                            .scale(timeScale)
                            .ticks(12)
                            .tickSubdivide(5)
                            .tickSize(4, 4, 0)
                            .orient("bottom"),

                    yAxis = $wnd.d3.svg.axis()
                            .scale(yScale)
                            .tickSubdivide(2)
                            .ticks(5)
                            .tickSize(4, 4, 0)
                            .orient("left"),


                    // create the actual chart group
                    chart = $wnd.d3.select(chartContext.chartSelection),

                    svg = chart.append("g")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top - titleHeight - titleSpace + margin.bottom)
                            .attr("transform", "translate(" + margin.left + "," + (+titleHeight + titleSpace + margin.top) + ")");

            console.log(" *** Chart js obj: "+chart);


            function createHeader(titleName, minLabel, minValue, avgLabel, avgValue, highLabel, highValue, uom) {
                var fontSize = 12,
                        yTitle = 27,
                        fgColor = "#003168",
                        baseX = 420,
                        xInc = 55;


                // title/header
                var title = chart.append("g").append("rect")
                        .attr("class", "title")
                        .attr("x", 10)
                        .attr("y", margin.top)
                        .attr("height", titleHeight)
                        .attr("width", width + 30 + margin.left)
                        .attr("stroke", "#878B94")
                        .attr("stroke-width", "0.25" )
                        .attr("fill", "url(#headerGrad)");
                        //.attr("fill", "#E6E6E6");

                chart.append("text")
                        .attr("class", "titleName")
                        .attr("x", 30)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(titleName)
                        .attr("fill", fgColor);


                chart.append("text")
                        .attr("class", "minLabel")
                        .attr("x", baseX)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(minLabel + " - ")
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "minText")
                        .attr("x", baseX + xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(minValue.toPrecision(3)+ " "+uom)
                        .attr("fill", fgColor);

                //avg
                chart.append("text")
                        .attr("class", "avgLabel")
                        .attr("x", baseX + 2 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(avgLabel + " - ")
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "avgText")
                        .attr("x", baseX + 3 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(avgValue.toPrecision(3) + " "+uom)
                        .attr("fill", fgColor);

                // high
                chart.append("text")
                        .attr("class", "highLabel")
                        .attr("x", 10 + baseX + 4 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("font-weight", "bold")
                        .attr("text-anchor", "left")
                        .text(highLabel + " - ")
                        .attr("fill", fgColor);

                chart.append("text")
                        .attr("class", "highText")
                        .attr("x", 10 + baseX + 5 * xInc)
                        .attr("y", yTitle)
                        .attr("font-size", fontSize)
                        .attr("text-anchor", "left")
                        .text(highValue.toPrecision(3) + " "+ uom)
                        .attr("fill", fgColor);
                return title;

            }

            function createStackedBars() {

                var pixelsOffHeight = 0;

                // The gray bars at the bottom leading up
                svg.selectAll("rect.leaderBar")
                        .data(chartContext.data)
                        .enter().append("rect")
                        .attr("class", "leaderBar")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return yScale(highBound);
                            }
                            else {
                                return yScale(d.low);
                            }
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(highBound) - pixelsOffHeight;
                            }
                            else {
                                return height - yScale(d.low) - pixelsOffHeight;
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset  );
                        })

                        .attr("opacity", ".9")
                        .attr("fill", function (d) {
                            if (d.down) {
                                return  "url(#downStripes)";
                            } else if (d.unknown) {
                                return  "url(#unknownStripes)";
                            }else if (d.nodata) {
                                return  "url(#noDataStripes)";
                            }
                            else {
                                return  "#d3d3d6";
                            }
                        });


                // upper portion representing avg to high
                svg.selectAll("rect.high")
                        .data(chartContext.data)
                        .enter().append("rect")
                        .attr("class", "high")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.high) ? yScale(lowBound) : yScale(d.high);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.y) - yScale(d.high);
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset  );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", "#1794bc");


                // lower portion representing avg to low
                svg.selectAll("rect.low")
                        .data(chartContext.data)
                        .enter().append("rect")
                        .attr("class", "low")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y);
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                return  yScale(d.low) - yScale(d.y);
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", "#70c4e2");

                // if high == low put a "cap" on the bar to show non-aggregated bar
                svg.selectAll("rect.singleValue")
                        .data(chartContext.data)
                        .enter().append("rect")
                        .attr("class", "singleValue")
                        .attr("x", function (d) {
                            return timeScale(d.x);
                        })
                        .attr("y", function (d) {
                            return isNaN(d.y) ? height : yScale(d.y)-2;
                        })
                        .attr("height", function (d) {
                            if (d.down || d.unknown || d.nodata) {
                                return height - yScale(lowBound);
                            }
                            else {
                                if(d.low === d.high  ){
                                    return  yScale(d.low) - yScale(d.y) +2;
                                }else {
                                    return  yScale(d.low) - yScale(d.y);
                                }
                            }
                        })
                        .attr("width", function (d) {
                            return  (width / chartContext.data.length - barOffset );
                        })
                        .attr("opacity", 0.9)
                        .attr("fill", function (d) {
                                if(d.low === d.high  ){
                                    return  "#50505a";
                                }else {
                                    return  "#70c4e2";
                                }
                        });
            }

            function createYAxisGridLines() {
                // create the y axis grid lines
                svg.append("g").classed("grid y_grid", true)
                        .call($wnd.d3.svg.axis()
                                .scale(yScale)
                                .orient("left")
                                .ticks(10)
                                .tickSize(-width, 0, 0)
                                .tickFormat("")
                        );
            }

            function createXandYAxes() {

                // create x-axis
                svg.append("g")
                        .attr("class", "x axis")
                        .attr("transform", "translate(0," + height + ")")
                        .attr("font-size", "10px")
                        .attr("font-family", "Arial, Verdana, sans-serif")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .call(xAxis);


                // create y-axis
                svg.append("g")
                        .attr("class", "y axis")
                        .call(yAxis)
                        .append("text")
                        .attr("transform", "rotate(-90),translate( -60,0)")
                        .attr("y", -30)
                        .attr("font-size", "10px")
                        .attr("font-family", "Arial, Verdana, sans-serif")
                        .attr("letter-spacing", "3")
                        .style("text-anchor", "end")
                        .text(chartContext.yAxisUnits === "NONE" ? "" : chartContext.yAxisUnits);

            }

            function createMinAvgPeakLines() {

                var avgLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale((avg));
                        }),
                peakLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale((peak));
                        }),
                minLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale(min);
                        }),
                minBaselineLine = $wnd.d3.svg.line()
                        .interpolate(interpolation)
                        .x(function (d) {
                            return timeScale(d.x);
                        })
                        .y(function (d) {
                            return yScale(d.baselineMin);
                        }),
                maxBaselineLine = $wnd.d3.svg.line()
                                .interpolate(interpolation)
                                .x(function (d) {
                                    return timeScale(d.x);
                                })
                                .y(function (d) {
                                    return yScale(d.baselineMax);
                                }),
                barAvgLine = $wnd.d3.svg.line()
                                .interpolate("linear")
                                .x(function (d) {
                                    return timeScale(d.x)+ ((width / chartContext.data.length - barOffset)/ 2);
                                })
                                .y(function (d,i) {

                                    var showBarAvgTrendline =
                                            global.@org.rhq.enterprise.gui.coregui.client.inventory.common.charttype.AbstractGraph::showBarAvgTrendLine()();

                                    if(showBarAvgTrendline){
                                    // on a bar avg line if the value is undefined then use the last defined value
                                        if(d.y == undefined){
                                            if(i >= 1){
                                                // count backward until there is a defined value
                                                for(var j=i; j>=1;j--){
                                                   if(this.__data__[j].y != undefined){
                                                       return yScale(this.__data__[j].y);
                                                   }
                                                }
                                                return yScale(0);
                                            }else {
                                                return yScale(0);
                                            }

                                        }else {
                                            return yScale(+d.y);
                                        }
                                    }else {
                                        return yScale(0);
                                    }
                                });

                // peak Line (must be before line.high to look right
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "peakLine")
                        .attr("fill", "none")
                        .attr("stroke", "#ff8a9a")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "3,3")
                        .attr("stroke-opacity", ".7")
                        .attr("d", peakLine);

                // min Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "minLine")
                        .attr("fill", "none")
                        .attr("stroke", "#8ad6ff")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-dasharray", "3,3")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minLine);

                // avg line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "avgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#b0d9b0")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-dasharray", "3,3")
                        //.attr("stroke-opacity", ".9")
                        .attr("d", avgLine);

                // min baseline Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "minBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "purple")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".9")
                        .attr("d", minBaselineLine);

                // max baseline Line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "maxBaselineLine")
                        .attr("fill", "none")
                        .attr("stroke", "orange")
                        .attr("stroke-width", "1")
                        .attr("stroke-dasharray", "20,10,5,5,5,10")
                        .attr("stroke-opacity", ".7")
                        .attr("d", maxBaselineLine);

                // Bar avg line
                svg.append("path")
                        .datum(chartContext.data)
                        .attr("class", "barAvgLine")
                        .attr("fill", "none")
                        .attr("stroke", "#2e376a")
                        .attr("stroke-width", "1.5")
                        .attr("stroke-opacity", ".7")
                        .attr("d", barAvgLine);
            }

            function formatHovers(chartContext, d) {
                var hoverString,
                        xValue = (d.x == undefined) ? 0 : +d.x,
                        date = new Date(+xValue),
                        availStartDate = new Date(+d.availStart),
                        availEndDate = new Date(+d.availEnd),
                        availDuration = d.availDuration,
                        barDuration = d.barDuration,
                        unknownStartDate = new Date(+d.unknownStart),
                        unknownEndDate = new Date(+d.unknownEnd),
                        unknownDuration = d.unknownDuration,
                        timeFormatter = $wnd.d3.time.format("%I:%M:%S %p"),
                        dateFormatter = $wnd.d3.time.format("%m/%d/%y"),
                        highValue = (d.high == undefined) ? 0 : d.high.toFixed(2),
                        lowValue = (d.low == undefined) ? 0 : d.low.toFixed(2),
                        avgValue = (d.y == undefined) ? 0 : d.y.toFixed(2);

                if (d.down) {
                    // down availability
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + chartContext.timeLabel + ': </span>' + timeFormatter(date) + '</div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr class="chartHoverDivider"></hr>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.hoverStartLabel+": "+ timeFormatter(availStartDate)+ '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.hoverEndLabel+": "+ timeFormatter(availEndDate) + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.hoverPeriodLabel+": "+ availDuration + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverDownValue">'+chartContext.downLabel +'</span></div>' +
                                    '</div>';
                } else  if (d.unknown) {
                    // unknown availability
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + chartContext.timeLabel + ': </span>' + timeFormatter(date) + '</div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr class="chartHoverDivider" ></hr>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan" >'+chartContext.hoverStartLabel+": "+ timeFormatter(unknownStartDate)+ '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan" >'+chartContext.hoverEndLabel+": "+ timeFormatter(unknownEndDate) + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.hoverPeriodLabel+": "+ unknownDuration + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.unknownLabel+'</span></div>' +
                                    '</div>';
                }

                else if (d.y == undefined) {
                    // no data
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel" >' + chartContext.timeLabel + ': </span>' + timeFormatter(date) + '</div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ': </span>' + dateFormatter(date) + '</div>' +
                                    '<hr class="chartHoverDivider" ></hr>' +
                                    '<div class="chartHoverAlignRight"><span class="chartHoverLabelSpan">'+chartContext.noDataLabel+'</span></div>' +
                                    '</div>';


                }
                else {
                    // regular bar hover
                    hoverString =
                            '<div class="chartHoverEnclosingDiv"><span class="chartHoverTimeLabel">' + chartContext.timeLabel + ':  </span><span style="width:50px;">' + timeFormatter(date) + '</span></div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverDateLabel">' + chartContext.dateLabel + ':  </span><span style="width:50px;">' + dateFormatter(date) + '</span></div>' +
                                    '<div class="chartHoverAlignLeft"><span class="chartHoverLabelSpan">'+chartContext.hoverBarLabel+": "+ barDuration + '</span></div>' +
                                    '<hr  class="chartHoverDivider"></hr>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverPeakValue" >' + chartContext.peakChartTitle + ': </span><span style="width:50px;">' + highValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverAvgValue" >' + chartContext.avgChartTitle + ':  </span><span style="width:50px;">' + avgValue + '</span></div>' +
                                    '<div class="chartHoverAlignRight"><span id="chartHoverLowValue" >' + chartContext.minChartTitle + ': </span><span style="width:50px;">' + lowValue + '</span></div>' +
                                    '</div>';
                }
                return hoverString;

            }

            function createHovers(chartContext) {
                //console.log("Create Hovers");
                $wnd.jQuery('svg rect.leaderBar, svg rect.high, svg rect.low').tipsy({
                    gravity: 'w',
                    html: true,
                    trigger: 'hover',
                    title: function () {
                        var d = this.__data__;
                        return formatHovers(chartContext, d);
                    },
                    show: function (e, el)
                    {
                        el.css({ 'z-index':'990000'})
                    }
                });
            }

            return {
                // Public API
                draw: function (chartContext) {
                    "use strict";
                    console.log("chart id: " + chartContext.chartSelection);
                    //console.log("Json Data:\n"+chartContext.data);

                    createHeader(chartContext.yAxisLabel, chartContext.minChartTitle, min, chartContext.avgChartTitle, avg, chartContext.peakChartTitle, peak, chartContext.yAxisUnits, chartContext.hoverBarLabel);
                    createYAxisGridLines();
                    createStackedBars();
                    createXandYAxes();
                    createMinAvgPeakLines();
                    createHovers(chartContext);
                    console.log("finished drawing paths");
                }
            }; // end public closure
        }();

        metricStackedBarGraph.draw(chartContext);

    }-*/;

}
