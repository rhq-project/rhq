<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.enterprise.server.measurement.AvailabilityManagerLocal" %>
<%@ page import="org.rhq.core.domain.measurement.Availability" %>
<%@ page import="java.util.List" %>
<%@ page import="org.rhq.core.domain.measurement.AvailabilityType" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.MonitorUtils" %>
<%--
  Author: Greg Hinkle
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
      <base target="_top"/>

      <script src="http://simile.mit.edu/timeline/api/timeline-api.js" type="text/javascript"></script>
      <script src="http://simile.mit.edu/timeline/examples/examples.js" type="text/javascript"></script>
      <title>Simple jsp page</title></head>

      <style type="text/css">
          .timeline-band-layer-inner { font-size: smaller; }
          .table-start-label { font-size: smaller; }
      </style>

  <%
    int eventId = WebUtility.getOptionalIntRequestParameter(request, "eventId", -1);
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();


    Map pref = user.getMetricRangePreference(true);
    long begin = (Long) pref.get(MonitorUtils.BEGIN);
    long end = (Long) pref.get(MonitorUtils.END);

      // System.out.println("Displaying from " + new Date(begin) + " to " + new Date(end));
//    long end = System.currentTimeMillis();
//    long begin = end - (1000L * 60 * 60 * 24);



    AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

    List<Availability> availabilities = availabilityManager.findAvailabilityWithinInterval(resourceId, new Date(begin), new Date(end));


%>


<body onload="onLoad();" onresize="onResize();">

<script type="text/javascript">


function onLoad() {

    var begin = <%= begin%>;
    var end = <%= end%>;

    var date = "<%= new SimpleDateFormat("MMM dd yyyy HH:mm:ss z").format(new Date()) %>";
    var dateAndOne = "<%= new SimpleDateFormat("MMM dd yyyy HH:mm:ss z").format(new Date(System.currentTimeMillis() + (1000L * 60))) %>";
    var timeZoneOffset = <%= (-new Date().getTimezoneOffset()) / 60 %>;

  var resourceId = <%= WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1) %>;

  var eventSource = new Timeline.DefaultEventSource();

   var theme = Timeline.ClassicTheme.create();
    theme.event.bubble.width = 320;
    theme.event.bubble.height = 320

  var bandInfos = [
    Timeline.createBandInfo({
        eventSource:    eventSource,
        date:      date,
        width:          "80%",
        intervalUnit:   Timeline.DateTime.MINUTE,
        multiple: 5,
        magnify: 10,
        intervalPixels: 20,
        trackGap:       0.4,
        timeZone: timeZoneOffset,
        theme: theme
    }),
    Timeline.createBandInfo({
        eventSource:    eventSource,
        showEventText:  false,
        width:          "10%",
        trackHeight:    0.5,
        trackGap:       0.2,
        intervalUnit:   Timeline.DateTime.HOUR,
        intervalPixels: 100,
        magnify: 5,
        timeZone: timeZoneOffset,
        theme: theme
    }),
    Timeline.createBandInfo({
        eventSource:    eventSource,
        showEventText:  false,
        width:          "10%",
        trackHeight:    0.5,
        trackGap:       0.2,
        intervalUnit:   Timeline.DateTime.DAY,
        intervalPixels: 300,
        timeZone: timeZoneOffset,
        theme: theme
    })
  ];
  bandInfos[1].syncWith = 0;
  bandInfos[1].highlight = true;
  bandInfos[2].syncWith = 0;
  bandInfos[2].highlight = true;

    for (var i = 0; i < bandInfos.length; i++) {
        bandInfos[i].decorators = [
            new Timeline.PointHighlightDecorator({
                date:  date,
                color:      "#0000CC",
                opacity:    30,
                startLabel: "Now",
                endLabel: ""
            })

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z");

for (Availability avail : availabilities) {

        out.write(", new Timeline.SpanHighlightDecorator({\n" +
"                startDate:  \"" + sdf.format(avail.getStartTime()) + "\",\n" +
"                endDate:  \"" + sdf.format(avail.getEndTime() == null ? new Date() : avail.getEndTime()) + "\",\n" +
"                color:      \"" + (avail.getAvailabilityType() == AvailabilityType.UP ? "#B0FFC5":"#F7A6A8") +  "\",\n" +
"                opacity:    20,\n" +
"                startLabel: \"\",\n" +
"                endLabel: \"\"\n" +
"            })");
}

    %>


        ];
    }

    var foo = "hello, world";

  tl = Timeline.create(document.getElementById("t1"), bandInfos);

    var toLoad = 4;
     function done() {
         toLoad--;
         if (toLoad == 0) {
             if (eventSource.getCount() == 0) {
                 document.getElementById("loading").innerHTML = '<img src="/images/NoEvents.png" alt="Loading events"/>';
             } else {
                 document.getElementById("loading").style.display = "none";
             }
         }
     }

      var link = "/resource/common/monitor/events/EventConfigJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
      Timeline.loadJSON(link, function(json, url) {
          eventSource.loadJSON(json, url);
          document.getElementById("event-count").innerHTML = eventSource.getCount();
          done();
      });

      var link = "/resource/common/monitor/events/EventAlertJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
      Timeline.loadJSON(link, function(json, url) {
          eventSource.loadJSON(json, url);
          document.getElementById("event-count").innerHTML = eventSource.getCount();
          done();
      });

      var link = "/resource/common/monitor/events/EventOperationsJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
      Timeline.loadJSON(link, function(json, url) {
          eventSource.loadJSON(json, url);
          document.getElementById("event-count").innerHTML = eventSource.getCount();
          done();
      });
      var link = "/resource/common/monitor/events/EventEventsJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
      Timeline.loadJSON(link, function(json, url) {
          eventSource.loadJSON(json, url);
          document.getElementById("event-count").innerHTML = eventSource.getCount();
          done();
      });


    setupFilterHighlightControls(document.getElementById("controls"), tl, [0,1], theme);
}
    var resizeTimerID = null;
        function onResize() {
            if (resizeTimerID == null) {
                resizeTimerID = window.setTimeout(function() {
                    resizeTimerID = null;
                    tl.layout();
                }, 500);
            }
        }
</script>

  <div id="t1" style="height: 450px; border: 1px solid #aaa"></div>

    <div>Events: <span id="event-count"/></div>
    <div class="controls" id="controls"/>

    <div id="loading" style="position: absolute; left:40%; top: 40%;z-index: 10"><img src="/images/LoadingEvents.png" alt="Loading events"/></div>

  </body>
</html>