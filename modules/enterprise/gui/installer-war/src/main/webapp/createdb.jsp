<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>
   <head>
      <link rel="stylesheet" type="text/css" href="style.css" />
      <title><h:outputText value="#{bundle.createDatabaseUserTitle}"/></title>

      <meta http-equiv="Pragma"        content="no-cache" />
      <meta http-equiv="Expires"       content="-1" />
      <meta http-equiv="Cache-control" content="no-cache" />
   </head>

   <body>
      <h1 align="center"><h:outputText value="#{bundle.createDatabaseUserTitle}" /></h1>
      <h:form>
         <h:panelGrid columns="1" style="margin-left: auto; margin-right: auto; text-align: center;">
            <h:outputText value="#{bundle.createDatabaseUserHelp}"/>

            <h:panelGrid columns="2" rowClasses="evenRow,oddRow" style="margin-left: auto; margin-right: auto; text-align: center;">
               <h:outputText value="#{bundle.adminConnectionUrl}" />
               <h:inputText  value="#{configurationBean.adminConnectionUrl}" size="40"/>
               
               <h:outputText value="#{bundle.adminUsername}" />
               <h:inputText value="#{configurationBean.adminUsername}" size="40"/>
         
               <h:outputText value="#{bundle.adminPassword}" />
               <h:inputSecret value="#{configurationBean.adminPassword}" size="40" />
            </h:panelGrid>
      
            <h:commandButton id="createDatabaseButton" action="#{configurationBean.createDatabase}" value="#{bundle.createDatabaseButton}" />
         </h:panelGrid>
      </h:form>
   </body>
   
</html>

</f:view>