<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.measurement.Availability" %>
<%@ page import="org.rhq.core.domain.measurement.AvailabilityType" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.measurement.AvailabilityManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementPreferences" %>
<%@ page import="org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%--
  Author: Greg Hinkle
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <head>
      <base target="_top"/>

      <script type="text/javascript">
          SimileAjax_urlPrefix = "/js/timeline/ajax/api/";
      </script> 
      <script src="/js/timeline/ajax/api/simile-ajax-api.js" type="text/javascript"></script>
      <script src="/js/timeline/api/timeline-api.js" type="text/javascript"></script>
      <script src="/js/timeline/examples/examples.js" type="text/javascript"></script>
      <title>Simple jsp page</title>

      <style type="text/css">
          .timeline-band-layer-inner { font-size: smaller; }
          .table-start-label { font-size: smaller; }
      </style>
  </head>
  <%
    int eventId = WebUtility.getOptionalIntRequestParameter(request, "eventId", -1);
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    MeasurementPreferences preferences = user.getMeasurementPreferences();
    MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
    long begin = rangePreferences.begin;
    long end = rangePreferences.end;

      // System.out.println("Displaying from " + new Date(begin) + " to " + new Date(end));
//    long end = System.currentTimeMillis();
//    long begin = end - (1000L * 60 * 60 * 24);



    AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();

    List<Availability> availabilities = availabilityManager.findAvailabilityWithinInterval(resourceId, new Date(begin), new Date(end));


%>


<body onload="onLoad();" onresize="onResize();">

<div id="loading" style="position: absolute; left:40%; top: 40%;z-index: 10"><img src="/images/LoadingEvents.png" alt="Loading events"/></div>

<form action="#" onsubmit="onLoad()" name="show">
<input type="checkbox" name="alerts" checked="checked" onchange="onLoad()" id="alertsField"/>
    <label for="alertsField"><img src="/images/icons/Alert_grey_16.png"/> Alerts</label>
&nbsp;
<input type="checkbox" name="events" checked="checked" onchange="onLoad()" id="eventsField"/>
    <label for="eventsField"><img src="/images/icons/Events_grey_16.png"/> Events</label>
&nbsp;
<input type="checkbox" name="configurations" checked="checked" onchange="onLoad()" id="configurationsField"/>
    <label for="configurationsField"><img src="/images/icons/Configure_grey_16.png"/> Configuration Changes</label>
&nbsp;
<input type="checkbox" name="operations" checked="checked" onchange="onLoad()" id="operationsField"/>
    <label for="operationsField"><img src="/images/icons/Operation_grey_16.png"/> Operations</label>
&nbsp;
<input type="checkbox" name="content" checked="checked" onchange="onLoad()" id="contentField"/>
    <label for="contentField"><img src="/images/icons/Content_grey_16.png"/> Content Changes</label>
</form>
<script type="text/javascript">

var tl;

function onLoad() {
    

    var begin = <%= begin%>;
    var end = <%= end%>;

    var date = "<%= new SimpleDateFormat("MMM dd yyyy HH:mm:ss z",Locale.US).format(new Date()) %>";
    var dateAndOne = "<%= new SimpleDateFormat("MMM dd yyyy HH:mm:ss z").format(new Date(System.currentTimeMillis() + (1000L * 60))) %>";
    var timeZoneOffset = <%= (-new Date().getTimezoneOffset()) / 60 %>;

  var resourceId = <%= WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1) %>;

  var eventSource = new Timeline.DefaultEventSource();

   var theme = Timeline.ClassicTheme.create();
    theme.event.bubble.width = 650;
    theme.event.bubble.height = 320;
    theme.event.label.width = 200;
    theme.event.label.lineColor = "black";
    theme.event.highlightColors = ["red", "orange"];

  var bandInfos = [
    Timeline.createBandInfo({
        eventSource:    eventSource,
        date:      "<%= new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z",Locale.US).format(new Date()) %>",
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
        overview: true,
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
        overview: true,
        timeZone: timeZoneOffset,
        theme: theme
    })
  ];
  bandInfos[1].syncWith = 0;
  bandInfos[1].highlight = true;
  bandInfos[2].syncWith = 1;
  bandInfos[2].highlight = true;

    for (var i = 0; i < bandInfos.length; i++) {
        bandInfos[i].decorators = [
            new Timeline.PointHighlightDecorator({
                date:  "<%= new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z",Locale.US).format(new Date()) %>",
                color:      "#0000CC",
                opacity:    30,
                startLabel: "Now",
                endLabel: ""
            })

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z",Locale.US);

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


  tl = Timeline.create(document.getElementById("t1"), bandInfos);

    var toLoad = 7;
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

      if (document.forms['show'].configurations.checked) {
          var link = "/resource/common/monitor/events/EventConfigJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

      if (document.forms['show'].configurations.checked) {
          link = "/resource/common/monitor/events/EventPluginConfigJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

      if (document.forms['show'].configurations.checked) {
          link = "/resource/common/monitor/events/EventCreateDeleteChildJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

      if (document.forms['show'].alerts.checked) {
          link = "/resource/common/monitor/events/EventAlertJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }


      if (document.forms['show'].operations.checked) {
          link = "/resource/common/monitor/events/EventOperationsJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

      if (document.forms['show']['events'].checked) {
          link = "/resource/common/monitor/events/EventEventsJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

    if (document.forms['show']['content'].checked) {
          link = "/resource/common/monitor/events/EventContentJSON.jsp?id=" + resourceId + "&begin=" + begin + "&end=" + end;
          Timeline.loadJSON(link, function(json, url) {
              eventSource.loadJSON(json, url);
              document.getElementById("event-count").innerHTML = eventSource.getCount();
              done();
          });
      }

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

    <!-- not accurate with grouping <div>Events: -->
    <span id="event-count" style="display:none;"></span>
    <div class="controls" id="controls"></div>


  </body>
</html>