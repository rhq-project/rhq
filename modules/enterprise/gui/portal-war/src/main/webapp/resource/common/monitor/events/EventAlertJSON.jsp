<%@ page import="org.rhq.core.domain.alert.Alert" %>
<%@ page import="org.rhq.core.domain.alert.AlertConditionLog" %>
<%@ page import="org.rhq.core.domain.alert.AlertPriority" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.core.domain.util.PageOrdering" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.alert.AlertManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page contentType="text/javascript" language="java" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    AlertManagerLocal alertManager = LookupUtil.getAlertManager();

    PageControl pc = new PageControl(0, 100);
    pc.setPrimarySortOrder(PageOrdering.DESC);
    PageList<Alert> alerts = alertManager.findAlerts(resourceId, new Date(begin), pc);

    System.out.println(System.currentTimeMillis() - end);

%>

{ "events":

[
    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss z");

        boolean first = true;

        for (Alert alert : alerts) {
            if (!first)
                out.write(",\n");
            else
                first = false;


            String icon = null;
            switch (alert.getAlertDefinition().getPriority()) {
                case HIGH:
                    icon = "/images/icn_flag_red.png";
                    break;
                case MEDIUM:
                    icon = "/images/icn_flag_yellow.png";
                    break;
                case LOW:
                    icon = "/images/icn_flag_blue.png";
                    break;
            }
            StringBuilder buf = new StringBuilder();
            for (AlertConditionLog log : alert.getConditionLogs()) {
                buf.append("<b>Condition:</b>" + log.getCondition().getName() + "<br/>");
                buf.append("<b>Condition:</b>" + log.getCondition().getThreshold() + "<br/>");
                buf.append("<b>Value:</b>" + log.getValue() + "</b>");
            }

            String link = "/alerts/Alerts.do?mode=viewAlert&id=" + resourceId + "&a=" + alert.getId();
%>

{ "start" : new Date('<%=sdf.format(new Date(alert.getCtime()))%>'),
  "title" : "<%=alert.getAlertDefinition().getName()%>",
  "link" : "<%=link%>",
  "description" : "<%=buf.toString()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(alert.getAlertDefinition().getPriority() == AlertPriority.LOW ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>
]
}