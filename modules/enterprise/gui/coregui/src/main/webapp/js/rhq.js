/**
 * Charting Javascript Functions.
 */


/**
 * ChartContext Constructor Object
 * Contains all of the data required to render a chart.
 * A ChartContext can be passed to multiple chart renders to display different chart types
 * of that data.
 */
var ChartContext = function (chartId, metricsData, xAxisLabel, yAxisLabel, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, downLabel, unknownLabel, hoverStartLabel,hoverEndLabel, hoverPeriodLabel)
{
    this.chartId = chartId;
    this.chartHandle = "#rChart-" + this.chartId;
    this.chartSelection = this.chartHandle + " svg";
    this.data = eval(metricsData); // make into json
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.yAxisUnits = yAxisUnits;
    this.minChartTitle = minChartTitle;
    this.avgChartTitle = avgChartTitle;
    this.peakChartTitle = peakChartTitle;
    this.dateLabel = dateLabel;
    this.timeLabel = timeLabel;
    this.downLabel = downLabel;
    this.unknownLabel = unknownLabel;
    this.hoverStartLabel = hoverStartLabel;
    this.hoverEndLabel = hoverEndLabel;
    this.hoverPeriodLabel = hoverPeriodLabel;
    /**
     * Can be called to validate the charting arguments.
     * @return {boolean}
     */
    this.validate = function ()
    {
        return this.chartId != undefined && this.data != undefined;
    };
};
