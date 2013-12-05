/**
 * RHQ Charting Javascript Functions.
 */

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
 * @param buttonBarDateTimeFormat
 * @param singleValueLabel
 * @param chartXaxisTimeFormatHours
 * @param chartXaxisTimeFormatHoursMinutes
 * @param hideLegend
 * @param chartAverage
 * @param chartMin
 * @param chartMax
 * @constructor
 */
var ChartContext = function (chartId, chartHeight, metricsData, xAxisLabel, chartTitle, yAxisUnits, minChartTitle, avgChartTitle, peakChartTitle, dateLabel, timeLabel, downLabel, unknownLabel, noDataLabel, hoverStartLabel, hoverEndLabel, hoverPeriodLabel, hoverBarLabel, chartHoverTimeFormat, chartHoverDateFormat, isPortalGraph, portalId, buttonBarDateTimeFormat, singleValueLabel, chartXaxisTimeFormatHours, chartXaxisTimeFormatHoursMinutes, hideLegend, chartAverage, chartMin, chartMax) {
            "use strict";
            if (!(this instanceof ChartContext)) {
                throw new Error("ChartContext function cannot be called as a function.");
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
            this.singleValueLabel = singleValueLabel;
            this.noDataLabel = noDataLabel;
            this.hoverStartLabel = hoverStartLabel;
            this.hoverEndLabel = hoverEndLabel;
            this.hoverPeriodLabel = hoverPeriodLabel;
            this.hoverBarLabel = hoverBarLabel;
            this.chartHoverTimeFormat = chartHoverTimeFormat;
            this.chartHoverDateFormat = chartHoverDateFormat;
            this.isPortalGraph = isPortalGraph;
            this.portalId = portalId;
            if (isPortalGraph) {
                this.chartHandle = "rChart-" + chartId + "-" + portalId;
            }
            else {
                this.chartHandle = "rChart-" + chartId;
            }
            this.chartSelection = this.chartHandle + " svg";
            this.buttonBarDateTimeFormat = buttonBarDateTimeFormat;
            this.chartXaxisTimeFormatHours = chartXaxisTimeFormatHours;
            this.chartXaxisTimeFormatHoursMinutes = chartXaxisTimeFormatHoursMinutes;
            this.hideLegend = hideLegend;
            this.chartAverage = chartAverage;
            this.chartMin = chartMin;
            this.chartMax = chartMax;

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
         * @param chartXaxisTimeFormatHours
         * @param chartXaxisTimeFormatHoursMinutes
         * @constructor
         */
                AvailChartContext = function (chartId, availData, dateLabel, timeLabel, hoverStartLabel, hoverBarLabel, availabilityLabel, chartHoverTimeFormat, chartHoverDateFormat, chartTitle, chartUpLabel, chartDownLabel, chartXaxisTimeFormatHours, chartXaxisTimeFormatHoursMinutes) {
            "use strict";
            if (!(this instanceof AvailChartContext)) {
                throw new Error("AvailChartContext function cannot be called as a function.");
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
            this.chartXaxisTimeFormatHours = chartXaxisTimeFormatHours;
            this.chartXaxisTimeFormatHoursMinutes = chartXaxisTimeFormatHoursMinutes;

        },

        /**
         * GraphDateContext object constructor.
         * @param startDate moment object representing startDate range
         * @param endDate moment object representing endDate range
         * @constructor
         */
                GraphDateContext = function (startDate, endDate) {
            "use strict";
            if (!(this instanceof GraphDateContext)) {
                throw new Error("GraphDateContext function cannot be called as a function.");
            }
            this.startDate = startDate;
            this.endDate = endDate;
        },
        rhqCommon = (function () {
            "use strict";


            var timeFormat = function (formats) {
                return function(date) {
                    var i = formats.length - 1, f = formats[i];
                    while (!f[1](date)) f = formats[--i];
                    return f[0](date);
                };
            };

            return {
                getD3CustomTimeFormat: function (xAxisTimeFormatHours, xAxisTimeFormatHoursMinutes) {
                    return  timeFormat([
                        [d3.time.format("%Y"), function () {
                            return true;
                        }],
                        [d3.time.format("%B"), function (d) {
                            return d.getMonth();
                        }],
                        [d3.time.format("%b %d"), function (d) {
                            return d.getDate() != 1;
                        }],
                        [d3.time.format("%a %d"), function (d) {
                            return d.getDay() && d.getDate() != 1;
                        }],
                        [d3.time.format(xAxisTimeFormatHours), function (d) {
                            return d.getHours();
                        }],
                        [d3.time.format(xAxisTimeFormatHoursMinutes), function (d) {
                            return d.getMinutes();
                        }],
                        [d3.time.format(":%S"), function (d) {
                            return d.getSeconds();
                        }],
                        [d3.time.format(".%L"), function (d) {
                            return d.getMilliseconds();
                        }]
                    ]);
                }

            };
        })();




