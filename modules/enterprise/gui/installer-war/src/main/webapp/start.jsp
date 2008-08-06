<%@ page contentType="text/html" %>

<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h" %>
<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<f:view>
<f:loadBundle var="bundle" basename="InstallerMessages" />

<html>

   <body>

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
                  <h:commandButton id="createDatabaseButton" action="#{configurationBean.showCreateDatabasePage}" value="#{bundle.createDatabaseButton}" />
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

      <h:panelGrid columns="2" columnClasses="warningColor" rendered="#{configurationBean.databaseSchemaExist == true}">
         <h:outputText value="#{bundle.existingSchemaQuestion}" style="font-size: 80%"/>
         <h:selectOneMenu onchange="submit()" label="#{bundle.existingSchemaQuestion}" value="#{configurationBean.existingSchemaOption}">
                                       <f:selectItems value="#{configurationBean.existingSchemaOptions}" />
         </h:selectOneMenu>
      </h:panelGrid>
 
      
      <h3 align="center">
         <h:outputText value="#{bundle.installSettingsNote}" />
      </h3>
      
      <h:panelGrid columns="1">
         <h:panelGrid columns="1" rendered="#{configurationBean.existingServers == true}" >
         
            <h:panelGrid columns="2" >

               <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}-ExistingServers', 'propertyHelp')" >
                  <h:outputText value="#{bundle.existingServersLabel}" />
               </h:outputLink>
               <h:selectOneMenu value="#{configurationBean.existingServerName}"
                                onchange="{ document.getElementById('propForm:haservername').value = this.options[this.selectedIndex].value;
                                            document.getElementById('propForm:haendpointaddress').value = '';
                                            document.getElementById('propForm:haendpointport').value = '';
                                            document.getElementById('propForm:haendpointsecureport').value = '';
                                            if (document.getElementById('propForm:haaffinitygroup') != null) {
                                               document.getElementById('propForm:haaffinitygroup').value = ''; }                                            
                                            submit(); }" >
                  <f:selectItems value="#{configurationBean.existingServerNames}" />
               </h:selectOneMenu>       
            
            </h:panelGrid>

            <h:outputText value="#{bundle.newServerNote}" />

         </h:panelGrid>
         
         <h:panelGrid columns="2" rowClasses="evenRow,oddRow">

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{configurationBean.serverConfiguration.itemDefinition.help}', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityName}" />
            </h:outputLink>
            <h:inputText id="haservername" size="#{configurationBean.serverConfiguration.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServerName}" >
            </h:inputText>
            
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}-ServerEndpointAddress', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityEndpointAddress}" />
            </h:outputLink>
            <h:inputText id="haendpointaddress" size="#{configurationBean.serverConfiguration.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.endpointAddress}" >
            </h:inputText>
            
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}-ServerEndpointPort', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityEndpointPort}" />
            </h:outputLink>            
            <h:inputText id="haendpointport" size="#{configurationBean.serverConfiguration.itemDefinition.fieldSize}"
                      value="#{configurationBean.haServer.endpointPortString}" >
            </h:inputText>
            
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}-ServerEndpointSecurePort', 'propertyHelp')" >
               <h:outputText value="#{bundle.propertyHighAvailabilityEndpointSecurePort}" />
            </h:outputLink>
            <h:inputText id="haendpointsecureport" size="#{configurationBean.serverConfiguration.itemDefinition.fieldSize}"
                      value="#{configurationBean.haServer.endpointSecurePortString}" >
            </h:inputText>

            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}-ServerAffinityGroup', 'propertyHelp')" 
                          rendered="#{configurationBean.showAdvancedSettings == true}" >
               <h:outputText value="#{bundle.propertyHighAvailabilityAffinityGroup}" />
            </h:outputLink>
            <h:inputText id="haaffinitygroup" size="#{configurationBean.serverConfiguration.itemDefinition.fieldSize}"
                         value="#{configurationBean.haServer.affinityGroup}"
                         rendered="#{configurationBean.showAdvancedSettings == true}" >                         
            </h:inputText>            

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