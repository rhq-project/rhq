<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>
<%@ page import="org.rhq.core.domain.resource.Resource" %>
<%@ page import="org.rhq.enterprise.server.drift.DriftManagerLocal"%>
<%@ page import="org.rhq.core.domain.criteria.GenericDriftChangeSetCriteria"%>
<%@ page import="org.rhq.core.domain.drift.DriftChangeSet"%>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    long end = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "end"));
    long begin = Long.parseLong(WebUtility.getRequiredRequestParameter(request, "begin"));

    DriftManagerLocal driftManager = LookupUtil.getDriftManager();
    GenericDriftChangeSetCriteria criteria = new GenericDriftChangeSetCriteria();
    criteria.addFilterResourceId(resourceId);
    criteria.addFilterCreatedAfter(begin);
    criteria.addFilterCreatedBefore(end);
    PageList<? extends DriftChangeSet<?>> results = driftManager.findDriftChangeSetsByCriteria(subject, criteria);
%>

{ "events": [

<%
    SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);
    String link = "/coregui/CoreGUI.html#Resource/" + resourceId + "/Drift/ChangeSets";
    String icon = "/images/icons/Drift_16.png";
    boolean first = true;
    for (DriftChangeSet entry : results) {
        if (!first) {
            out.write(",\n");
        }
        first = false;
%>

{ "start" : "<%=sdf.format(entry.getCtime())%>",
  "title" : "Drift Detected",
  "link" : "<%=link%>",
  "description" : "Drift was detected! A change set was created to document the changes.",
  "icon" : "<%=icon%>",
  "color" : "#3333FF"
}

<%
    }
%>

]}
