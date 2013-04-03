/**
 * Charting Javascript Functions.
 */


/**
 * ChartContext Constructor Object
 * Contains all of the data required to render a chart.
 * A ChartContext can be passed to multiple chart renders to display different chart types
 * of that data.
 */
var ChartContext = function (chartId, chartHeight, metricsData, xAxisLabel, chartTitle, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, downLabel, unknownLabel, noDataLabel, hoverStartLabel,hoverEndLabel, hoverPeriodLabel, hoverBarLabel, chartHoverTimeFormat, chartHoverDateFormat, isPortalGraph, windowWidth )
{
    "use strict";
    if(!(this instanceof ChartContext)){
        throw new Error("ChartContext function cannot be called as a function.")
    }
    this.chartId = chartId;
    this.chartHandle = "#rChart-" + this.chartId;
    this.chartSelection = this.chartHandle + " svg";
    this.chartHeight = chartHeight;
    this.data = jQuery.parseJSON(metricsData); // make into json
    this.xAxisLabel = xAxisLabel;
    this.chartTitle = chartTitle;
    this.yAxisUnits = yAxisUnits;
    this.minChartTitle = minChartTitle;
    this.avgChartTitle = avgChartTitle;
    this.peakChartTitle = peakChartTitle;
    this.dateLabel = dateLabel;
    this.timeLabel = timeLabel;
    this.downLabel = downLabel;
    this.unknownLabel = unknownLabel;
    this.noDataLabel = noDataLabel;
    this.hoverStartLabel = hoverStartLabel;
    this.hoverEndLabel = hoverEndLabel;
    this.hoverPeriodLabel = hoverPeriodLabel;
    this.hoverBarLabel = hoverBarLabel;
    this.chartHoverTimeFormat = chartHoverTimeFormat;
    this.chartHoverDateFormat = chartHoverDateFormat;
    this.isPortalGraph = isPortalGraph;
    this.windowWidth = windowWidth;

},
AvailChartContext = function (chartId, availData, dateLabel, timeLabel, hoverStartLabel, hoverEndLabel, hoverBarLabel, availabilityLabel, chartHoverTimeFormat, chartHoverDateFormat) {
    "use strict";
    if (!(this instanceof AvailChartContext)) {
        throw new Error("AvailChartContext function cannot be called as a function.")
    }
    this.chartId = chartId;
    this.chartHandle = "#availChart-" + this.chartId;
    this.chartSelection = this.chartHandle + " svg";
    this.data = jQuery.parseJSON(availData); // make into json
    this.dateLabel = dateLabel;
    this.timeLabel = timeLabel;
    this.hoverStartLabel = hoverStartLabel;
    this.hoverEndLabel = hoverEndLabel;
    this.hoverBarLabel = hoverBarLabel;
    this.hoverBarAvailabilityLabel = availabilityLabel;
    this.chartHoverTimeFormat = chartHoverTimeFormat;
    this.chartHoverDateFormat = chartHoverDateFormat;

};
