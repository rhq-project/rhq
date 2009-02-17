<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="org.rhq.core.domain.auth.Subject" %>
<%@ page import="org.rhq.core.domain.configuration.ConfigurationUpdateStatus" %>
<%@ page import="org.rhq.core.domain.configuration.PluginConfigurationUpdate" %>
<%@ page import="org.rhq.core.domain.util.PageControl" %>
<%@ page import="org.rhq.core.domain.util.PageList" %>
<%@ page import="org.rhq.enterprise.gui.legacy.ParamConstants" %>
<%@ page import="org.rhq.enterprise.gui.legacy.WebUser" %>
<%@ page import="org.rhq.enterprise.gui.legacy.util.SessionUtils" %>
<%@ page import="org.rhq.enterprise.gui.util.WebUtility" %>
<%@ page import="org.rhq.enterprise.server.configuration.ConfigurationManagerLocal" %>
<%@ page import="org.rhq.enterprise.server.util.LookupUtil" %>

<%
    int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);

    WebUser user = SessionUtils.getWebUser(request.getSession());
    Subject subject = user.getSubject();

    ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    PageList<PluginConfigurationUpdate> configurationUpdates = configurationManager.getPluginConfigurationUpdates(subject, resourceId, new PageControl(0,100));
%>


{ "events": [

    <%
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss Z", Locale.US);

        boolean first = true;

        for (PluginConfigurationUpdate configUpdate : configurationUpdates) {

            if (!first)
                out.write(",\n");
            else
                first = false;

            String icon = null;
            switch (configUpdate.getStatus()) {
                case SUCCESS:
                    icon = "/images/icn_pconfig_success.png";
                    break;
                case FAILURE:
                    icon = "/images/icn_pconfig_error.png";
                    break;
                case INPROGRESS:
                    icon = "/images/icn_pconfig.png";
                    break;
            }

            String link = "/rhq/resource/inventory/history.xhtml?id=" + resourceId + "&configId=" + configUpdate.getId();

            %>

{ "start" : "<%=sdf.format(configUpdate.getCreatedTime())%>",
  "title" : "Plugin Configuration Change",
  "link" : "<%=link%>",
  "description" : "<b>User:</b> <%=configUpdate.getSubjectName()%><br/><b>Status:</b> <%=configUpdate.getStatus()%>",
  "icon" : "<%=icon%>",
  "color" : "<%=(configUpdate.getStatus() != ConfigurationUpdateStatus.FAILURE ? "#4EB84E" : "#DD5656")%>"
}

    <%
        }
    %>

]}