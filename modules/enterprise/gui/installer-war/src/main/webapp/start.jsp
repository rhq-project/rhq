<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <f:subview id="header">
      <jsp:include page="/header.jsp" flush="true" />
   </f:subview>
   
   <p align="center">
      <h:outputText value="#{bundle.setPropertiesInstructions}" />
   </p>
   
   <h:form>
      <p align="center">
         <h:selectBooleanCheckbox onclick="submit()"
                                  id="showAdvancedSettings"
                                  value="#{configurationBean.showAdvancedSettings}" />
         <h:outputLabel for="showAdvancedSettings"
                        value="#{bundle.showAdvancedSettings}" />
      </p>
   </h:form>
   
   <h:form id="propForm">
      <h3 align="center">
         <h:outputText value="#{bundle.databasePropertiesNote}" />
      </h3>

      <h:panelGrid columns="2">
         <h:panelGrid columns="2"
                      rowClasses="evenRow,oddRow">
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[0].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[0].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:selectOneMenu id="databasetype"
                             value="#{configurationBean.databaseConfiguration[0].value}"
                             onchange="if (this.options[this.selectedIndex].value == 'PostgreSQL'){
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:postgresql://127.0.0.1:5432/rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'org.postgresql.Driver';
                                       } else if (this.options[this.selectedIndex].value == 'Oracle10g') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:oracle:thin:@127.0.0.1:1521:rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'oracle.jdbc.driver.OracleDriver';
                                       } else if (this.options[this.selectedIndex].value == 'MySQL') {
                                          document.getElementById('propForm:databaseconnectionurl').value = 'jdbc:mysql://127.0.0.1/rhq';
                                          document.getElementById('propForm:databasedriverclass').value = 'com.mysql.jdbc.Driver';
                                       }
                                      ">
               <f:selectItems value="#{configurationBean.databaseConfiguration[0].itemDefinition.options}" />
            </h:selectOneMenu>

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[1].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[1].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databaseconnectionurl"
                         size="#{configurationBean.databaseConfiguration[1].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[1].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[2].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[2].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databasedriverclass"
                         size="#{configurationBean.databaseConfiguration[2].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[2].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[3].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[3].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databaseusername"
                         size="#{configurationBean.databaseConfiguration[3].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[3].value}" />

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.databaseConfiguration[4].itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{configurationBean.databaseConfiguration[4].itemDefinition.propertyLabel}" />
            </h:outputLink>
            <h:inputText id="databasepassword"
                         size="#{configurationBean.databaseConfiguration[4].itemDefinition.fieldSize}"
                         value="#{configurationBean.databaseConfiguration[4].value}" />
         </h:panelGrid>

         <h:panelGrid columns="1" border="1" width="250">
            <h:panelGrid columns="1">
               <h:outputText value="#{bundle.testDatabaseNote}" style="font-size: 66%"/>
               <h:panelGrid columns="2">
                  <h:commandButton id="testDatabaseButton"
                                   action="#{configurationBean.testConnection}"
                                   value="#{bundle.testDatabaseButton}"/>
                  <h:panelGroup rendered="#{configurationBean.lastTest != null && configurationBean.lastTest != 'OK'}">
                     <h:graphicImage value="/images/warning.gif" alt="Error" onclick="alert('#{configurationBean.lastTest}')" onmouseover="this.style.cursor='pointer'" onmouseout="this.style.cursor='default'"/>
                  </h:panelGroup>
                  <h:panelGroup rendered="#{configurationBean.lastTest != null && configurationBean.lastTest == 'OK'}">
                     <h:graphicImage value="/images/ok.gif" alt="OK"/>
                  </h:panelGroup>
               </h:panelGrid>
            </h:panelGrid>

            <h:panelGrid columns="1">
               <h:outputText value="#{bundle.createDatabaseNote}" style="font-size: 66%"/>
               <h:panelGrid columns="2">
                  <h:commandButton id="createDatabaseButton" action="#{configurationBean.createDatabase}" value="#{bundle.createDatabaseButton}" />
                  <h:panelGroup rendered="#{configurationBean.lastCreate != null && configurationBean.lastCreate != 'OK'}">
                     <h:graphicImage value="/images/warning.gif" alt="Error" onclick="alert('#{configurationBean.lastCreate}')" onmouseover="this.style.cursor='pointer'" onmouseout="this.style.cursor='default'"/>
                  </h:panelGroup>
                  <h:panelGroup rendered="#{configurationBean.lastCreate != null && configurationBean.lastCreate == 'OK'}">
                     <h:graphicImage value="/images/ok.gif" alt="OK"/>
                  </h:panelGroup>
               </h:panelGrid>
            </h:panelGrid>
         </h:panelGrid>
      </h:panelGrid>

      <h3 align="center">
         <h:outputText value="#{bundle.serverPropertiesNote}" />
      </h3>

      <h:dataTable value="#{configurationBean.nonDatabaseConfiguration}" var="prop"
                   headerClass="evenRow" rowClasses="oddRow,evenRow">
         <h:column>
            <f:facet name="header">
               <h:outputText value="" />
            </f:facet>
            
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{prop.itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{prop.itemDefinition.propertyLabel}" />
            </h:outputLink>
         </h:column>
         <h:column>
            <f:facet name="header">
               <h:outputText value="" />
            </f:facet>
            <h:inputText size="#{prop.itemDefinition.fieldSize}"
                         value="#{prop.value}"
                         rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.Integer' || prop.itemDefinition.propertyType.name eq 'java.net.InetAddress')}" />
            <h:inputText size="#{prop.itemDefinition.fieldSize}"
                         value="#{prop.value}"
                         rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.String' && !prop.itemDefinition.secret)}" />
            <h:inputSecret size="#{prop.itemDefinition.fieldSize}"
                           value="#{prop.value}"
                           rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.String' && prop.itemDefinition.secret)}" />
            <h:selectOneRadio value="#{prop.value}"
                              rendered="#{prop.itemDefinition.options == null && (prop.itemDefinition.propertyType.name eq 'java.lang.Boolean')}">
               <f:selectItem itemLabel="#{bundle.yesString}" itemValue="true"/>
               <f:selectItem itemLabel="#{bundle.noString}" itemValue="false"/>
            </h:selectOneRadio>
            <h:selectOneMenu value="#{prop.value}"
                             rendered="#{prop.itemDefinition.options != null}">
               <f:selectItems value="#{prop.itemDefinition.options}" />
            </h:selectOneMenu>
         </h:column>
         <h:column>
            <f:facet name="header">
               <h:outputText value="#{bundle.requiresRestart}" />
            </f:facet>
            <h:outputText value="#{bundle.yesString}" rendered="#{prop.itemDefinition.requiresRestart}" />
         </h:column>
      </h:dataTable>

      <p align="center">
         <h:commandButton id="save" action="#{configurationBean.save}" value="#{bundle.save}" />
      </p>
   </h:form>
   
   </body>

</html>

</f:view>