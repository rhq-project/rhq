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
   
   <h:form>   
      <h:dataTable value="#{configurationBean.configuration}" var="prop"
                   headerClass="evenRow" rowClasses="oddRow,evenRow">
         <h:column>
            <f:facet name="header">
               <h:outputText value="#{bundle.propertyName}" />
            </f:facet>
            
            <h:outputLink value="javascript:popUp('#{bundle.helpDocRoot}#{bundle.helpDocRHQServerPropParentPage}#{prop.itemDefinition.help}', 'propertyHelp')">
               <h:outputText value="#{prop.itemDefinition.propertyLabel}" />
            </h:outputLink>
         </h:column>
         <h:column>
            <f:facet name="header">
               <h:outputText value="#{bundle.value}" />
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