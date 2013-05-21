/**
 * Charting Javascript Functions.
 */

// Handle browsers not supporting console object
if (!window.console) window.console = {};
if (!window.console.log) window.console.log = function () { };

/**
 * ChartContext Constructor Object
 * Contains all of the data required to render a chart.
 * A ChartContext can be passed to multiple chart renders to display different chart types
 * of that data.
 * @param chartId
 * @param chartHeight
 * @param metricsData
 * @param xAxisLabel
 * @param chartTitle
 * @param yAxisUnits
 * @param minChartTitle
 * @param avgChartTitle
 * @param peakChartTitle
 * @param dateLabel
 * @param timeLabel
 * @param downLabel
 * @param unknownLabel
 * @param noDataLabel
 * @param hoverStartLabel
 * @param hoverEndLabel
 * @param hoverPeriodLabel
 * @param hoverBarLabel
 * @param chartHoverTimeFormat
 * @param chartHoverDateFormat
 * @param isPortalGraph
 * @param portalId
 * @constructor
 */
var ChartContext = function (chartId, chartHeight, metricsData, xAxisLabel, chartTitle, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, downLabel, unknownLabel, noDataLabel, hoverStartLabel,hoverEndLabel, hoverPeriodLabel, hoverBarLabel, chartHoverTimeFormat, chartHoverDateFormat, isPortalGraph, portalId )
{
    "use strict";
    if(!(this instanceof ChartContext)){
        throw new Error("ChartContext function cannot be called as a function.")
    }
    this.chartId = chartId;
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
    this.portalId = portalId;
    if(isPortalGraph){
        this.chartHandle =  "rChart-"+chartId+"-"+portalId;
    }else {
        this.chartHandle =  "rChart-"+chartId;
    }
    this.chartSelection = this.chartHandle + " svg";

},
/**
 * Availability Context object constructor
 * @param chartId
 * @param availData
 * @param dateLabel
 * @param timeLabel
 * @param hoverStartLabel
 * @param hoverBarLabel
 * @param availabilityLabel
 * @param chartHoverTimeFormat
 * @param chartHoverDateFormat
 * @param chartTitle
 * @param chartUpLabel
 * @param chartDownLabel
 * @constructor
 */
AvailChartContext = function (chartId, availData, dateLabel, timeLabel, hoverStartLabel, hoverBarLabel, availabilityLabel, chartHoverTimeFormat, chartHoverDateFormat, chartTitle, chartUpLabel, chartDownLabel) {
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
    this.hoverBarLabel = hoverBarLabel;
    this.hoverBarAvailabilityLabel = availabilityLabel;
    this.chartHoverTimeFormat = chartHoverTimeFormat;
    this.chartHoverDateFormat = chartHoverDateFormat;
    this.chartTitle = chartTitle;
    this.chartDownLabel = chartDownLabel;
    this.chartUpLabel = chartUpLabel;

},

/**
 * GraphDateContext object constructor.
 * @param startDate
 * @param endDate
 * @constructor
 */
GraphDateContext = function (startDate, endDate){
    "use strict";
    if (!(this instanceof GraphDateContext)) {
        throw new Error("GraphDateContext function cannot be called as a function.")
    }
    this.startDate = startDate;
    this.endDate = endDate;
};


