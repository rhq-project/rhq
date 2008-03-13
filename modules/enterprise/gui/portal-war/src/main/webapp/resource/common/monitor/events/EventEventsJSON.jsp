<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.event.composite.EventComposite" %>
<%@ page import="org.rhq.core.domain.util.OrderingField" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.event.EventManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%
    EventManagerLocal eventManager = LookupUtil.getEventManager();

    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    boolean tooManyEvents = false;

    PageList<EventComposite> list =
            eventManager.getEvents(subject, new int[] {resourceId} , begin, end, null, -1,
                null, null, new PageControl(0,100, new OrderingField()));

    /* TODO GH: Add alert to screen
        if (list.getTotalSize() != list.size()) {
        tooManyEvents = true;
    }*/

%>

{ "events": [


    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z");

        boolean first = true;
        for (EventComposite event : list) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String title = event.getEventDetail();
            title = title.replace('\n', ' ');

            if (title.length() > 30) {
                title = title.substring(0,29) + "...";
            }

            String icon = null;
            String color = null;
            switch (event.getSeverity()) {
                case DEBUG:
                    icon = "/images/icn_info_green.png";
                    color = "green";
                    break;
                case INFO:
                    icon = "/images/icn_info_blue.png";
                    color = "blue";
                    break;
                case WARN:
                    icon = "/images/icn_info_yellow.png";
                    color = "yellow";
                    break;
                case ERROR:
                    icon = "/images/icn_info_orange.png";
                    color = "orange";
                    break;
                case FATAL:
                    icon = "/images/icn_info_red.png";
                    color = "red";
            }

            String link = "/resource/common/Events.do?mode=events&id=" + resourceId + "&eventId=" + event.getEventId();

            String detail = event.getEventDetail().replaceAll("\"","\\\"");

            %>

{ "start" : new Date('<%=sdf.format(event.getTimestamp())%>'),
  "title" : "Event: <%= title%>",
  "link" : "<%=link%>",
  "description" : "<b>Source:</b> <%=event.getSourceLocation()%><br/><b>Detail:</b> <%=detail%>",
  "icon" : "<%=icon%>",
  "color" : "<%=color%>"
}

    <%
        }
    %>

]}