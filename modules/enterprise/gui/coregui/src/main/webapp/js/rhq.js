/**
 * Charting Javascript Functions.
 *
 */


// ChartContext Constructor Object
var ChartContext = function(chartId,  metricsData, xAxisLabel, yAxisLabel, yAxisUnits){
    this.chartId = chartId;
    this.chartHandle = "#rChart-"+this.chartId;
    this.chartSelection = this.chartHandle + " svg";
    this.data = eval(metricsData); // make into json
    this.xAxisLabel = xAxisLabel;
    this.yAxisLabel = yAxisLabel;
    this.yAxisUnits = yAxisUnits;
    this.validate = function() {
        return this.chartId != undefined && this.chartHandle != undefined && this.data != undefined;
    };
};

