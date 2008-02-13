<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true"/>
   </f:subview>
   
   <p align="center">
      <h:outputText value="#{bundle.invalidDatabaseSettings}" />
   </p>
   
   <p align="center">
      <h:outputLink value="start.jsf">
         <h:outputText value="#{bundle.backToSettingsLink}" />
      </h:outputLink>
   </p>
   
   </body>
</html>

</f:view>