function whisker(scheduleId,divId) {
// Width and height of the chart to print
    var w = 240,
            h = 125;

// div#one is the div with the id 'one' ( <div id="one"/>)
    var svg = d3.select("body").select("div#"+divId).append("svg:svg")
            .attr("width", w)
            .attr("height", h);
//  .append("svg:g");
//    .attr("transform", "translate(" + p[3] + "," + (h - p[2]) + ")");
console.log(scheduleId);
    d3.json(
            '/rest/1/metric/data/'+scheduleId+'.json?hideEmpty=true',
            function (jsondata) {

                var points = jsondata.metricAggregate.dataPoints;
                var minVal = jsondata.metricAggregate.min;
                var maxVal = jsondata.metricAggregate.max;
                var avgVal = jsondata.metricAggregate.avg;
                var minTs = jsondata.metricAggregate.minTimeStamp;
                var maxTs = jsondata.metricAggregate.maxTimeStamp;

                var minTsD = new Date(minTs);
                var maxTsD = new Date(maxTs);
                console.log(minTs, minTsD, maxTs, maxTsD);

                // X axis goes from lowest to highest timestamp
                var x = d3.time.scale().domain([minTsD,maxTsD]).range([0,w]);
                // Y axis goes from lowest to highest value
                var y = d3.scale.linear().domain([minVal, maxVal]).rangeRound([0,h]);

//      console.log(jsondata.metricAggregate);

                // Line for the average
                var avgLine = svg.append("svg:line")
                        .attr("x1", x(new Date(minTs)))
                        .attr("y1", h - y(avgVal))
                        .attr("x2", x(new Date(maxTs)))
                        .attr("y2", h - y(avgVal))
                        .attr("stroke", "lightgrey")
                        .attr("stroke-dasharray", "2,4");

                // TODO if present add lines for baselines

                // data() loops over all entries in 'points' above for the remaining statements
                var bars = svg.selectAll("bars").data(points);

                var currX = function(d) {
                    return x(new Date(d.timeStamp))
                };

                var line = bars.enter().append("svg:line")
                        .attr("x1", currX)
                        .attr("x2", currX)
                        .attr("y1", function(d) {
                            return h - y(d.low)
                        })
                        .attr("y2", function(d) {
                            return h - y(d.high)
                        })
                        .attr("stroke", "lightblue");

                var circleLow = bars.enter().append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.low)
                        })
                        .attr("r", 1)
                        .attr("stroke", "green")
                        .attr("fill", "lightgreen");

                var circleHigh = bars.enter().append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.high)
                        })
                        .attr("r", 1)
                        .attr("stroke", "green")
                        .attr("fill", "lightblue");

                var circleVal = bars.enter().append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.value)
                        })
                        .attr("r", 1.5)
                        .attr("stroke", "blue")
                        .attr("fill", "lightpink");

            });
}
