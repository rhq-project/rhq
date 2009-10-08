<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true" />
   </f:subview>
   
   <p align="center">
      <h:outputText value="#{bundle.saveError}" />
   </p>
   
   <table align="center" border="1">
      <tr><th><h:outputText value="#{bundle.errorLabel}" /></th></tr>
      <tr><td><h:outputText value="#{configurationBean.lastError}" /></td></tr>
   </table>
   
   <p align="center">
      <h:outputLink value="start.jsf"><h:outputText value="#{bundle.backToSettingsLink}"/></h:outputLink>
   </p>
   
   </body>

</html>

</f:view>