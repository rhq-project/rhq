var rhq = {

dashboard : function() {
    // get all platforms
    $.get('/rest/resource/platforms', function(data) {

    var table = $("#table");
    for (var i = 0; i < data.length ; i++ ) {
      var resource = data[i];

      var rowId = "tr" + i ;
      table.append('<tr id="'+ rowId + '">');

      // Now get the schedules for this resource
      var resId = resource.resourceId;
      console.log(resId);

         $.getJSON("/rest/resource/"+resId+"/schedules.json?type=metric&enabledOnly=true", function (json2) {

         var tr = $("#" + rowId);
         for ( var n = 0 ; n < json2.length ; n ++ ) {
            var tdid = "td_" + i + "_" + n;
            var schedule = json2[n];
            var td = tr.append('<td id="'+tdid+'">');
            $("#"+tdid).html(schedule.displayName + " (" + schedule.scheduleId + ')<br/><div id="d' + tdid +'"/>');
          rhq.whisker(schedule.scheduleId,tdid,180,90);
         }
        })
     }
    })
    },
whisker: function(scheduleId,divId,w,h) {

    d3.json(
            '/rest/metric/data/'+scheduleId+'.json?hideEmpty=true',
            function (jsondata) {
//   console.log(scheduleId +", "+ divId);
    var svg = d3.select("body").select("#"+divId);
       svg=svg.append("svg:svg")
            .attr("width", w)
            .attr("height", h);

                var points = jsondata.dataPoints;
                var minVal = jsondata.min;
                var maxVal = jsondata.max;
                var avgVal = jsondata.avg;
                var minTs = jsondata.minTimeStamp;
                var maxTs = jsondata.maxTimeStamp;

                var minTsD = new Date(minTs);
                var maxTsD = new Date(maxTs);

                // X axis goes from lowest to highest timestamp
                var x = d3.time.scale().domain([minTsD,maxTsD]).range([0,w]);
                // Y axis goes from lowest to highest value
                var y = d3.scale.linear().domain([minVal, maxVal]).rangeRound([0,h]);


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

                var group = bars.enter().append("svg:g");

                var line = group.append("svg:line")
                        .attr("x1", currX)
                        .attr("x2", currX)
                        .attr("y1", function(d) {
                            return h - y(d.low)
                        })
                        .attr("y2", function(d) {
                            return h - y(d.high)
                        })
                        .attr("stroke", "lightblue");

                var circleLow = group.append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.low)
                        })
                        .attr("r", 1)
                        .attr("stroke", "green")
                        .attr("fill", "lightgreen");

                var circleHigh = group.append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.high)
                        })
                        .attr("r", 1)
                        .attr("stroke", "green")
                        .attr("fill", "lightblue");

                var circleVal = group.append("svg:circle")
                        .attr("cx", currX)
                        .attr("cy", function(d) {
                            return h - y(d.value)
                        })
                        .attr("r", 1.5)
                        .attr("stroke", "blue")
                        .attr("fill", "lightpink");

            });
} // whisker

};//var rhq
