<%@ page contentType="text/html" %>

<%@page import="org.rhq.enterprise.installer.ServerInformation"%>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true"/>
   </f:subview>

   <c:if test="<%= new ServerInformation().isFullyDeployed() %>">
      <p align="left"><h:outputText value="#{bundle.alreadyInstalled}" /></p>
      <table align="left">
         <tr>
            <td align="left">
               <h:graphicImage id="pleasewait-image" url="/images/pleasewait.gif" alt="..."/><br/>
            </td>
         </tr>
         <tr>
            <td align="left" id="progressBarMessage">
               <h:outputText styleClass="small" value="#{bundle.starting}" />
            </td>
         </tr>
      </table>
      <br/>
      <br/>      
      <br/>      
      <br/>      
   </c:if>
   
   <c:if test="<%= !new ServerInformation().isFullyDeployed() %>">
      <p align="left"><h:outputText value="#{bundle.welcomeMessage}" /></p>
      <p align="left">
         <li><h:outputLink value="start.jsf"><h:outputText value="#{bundle.startInstallingLink}" /></h:outputLink></li>
      </p>
      <br/>      
   </c:if>

   <hr align="left" style="width: 30%">
   <br/>      
      
   <p align="left">
      <h:outputText value="#{bundle.introduceHelpDocs}"/>
   </p>
   
   <table align="left"><tr><td>

      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocRHQServerInstallGuide}"><h:outputText value="#{bundle.helpDocRHQServerInstallGuideLabel}"/></h:outputLink></li>
      <%--
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocRHQGuiConsoleUsersGuide}"><h:outputText value="#{bundle.helpDocRHQGuiConsoleUsersGuideLabel}"/></h:outputLink></li>
      --%>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocRHQServerUsersGuide}"><h:outputText value="#{bundle.helpDocRHQServerUsersGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocRHQAgentUsersGuide}"><h:outputText value="#{bundle.helpDocRHQAgentUsersGuideLabel}"/></h:outputLink></li>
      <li><h:outputLink target="_blank" value="#{bundle.helpDocRoot}#{bundle.helpDocFaq}"><h:outputText value="#{bundle.helpDocFaqLabel}"/></h:outputLink></li>

   </td></tr></table>

   </body>
   
</html>

</f:view>