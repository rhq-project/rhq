<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="section"/>


<!-- because we want to do our own dynamic processing of file-uploads in this form's multipart/form-data, we can't use html:form -->
<form name="MapForm"
      enctype="multipart/form-data"

      action="<c:out value="${pageContext.request.contextPath}"/><c:out value="/resource/${section}/manage/${actionHandler}.do"/>"
      method="POST">
<html:hidden property="appDefId" value="${appDefId}"/>
<html:hidden property="actionName" value="${actionName}"/>

<tiles:insert definition=".page.title.resource.platform">
   <tiles:put name="titleKey" value="resource.platform.inventory.NewPlatformPageTitle"/>  
</tiles:insert>

<%--<tiles:insert definition=".resource.platform.inventory.generalProperties"/>--%>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="showLocation" ignore="true"/>
<tiles:importAttribute name="locationRequired" ignore="true"/>
<tiles:importAttribute name="configText" ignore="true"/>


<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
   <tiles:put name="tabKey" value="resource.common.control.props.GeneralPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<tiles:insert definition=".portlet.confirm"/>
<tiles:insert definition=".portlet.error"/>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">

<c:forEach items="${attribs}" var="attrib">
<tr valign="top">
   <c:set var="hidden" value="${attrib.definition.renderingInfo.hidden}" scope="page" />

   <td width="20%" class="BlockLabel">
      <c:if test="${!hidden}">
         <c:choose>
            <c:when test="${empty attrib.definition.renderingInfo.description}">
               <span><c:out value="${attrib.definition.renderingInfo.label}"/></span>
            </c:when>
            <c:otherwise>
               <span onmouseover="return escape('<c:out value="${attrib.definition.renderingInfo.description}"/>')"><c:out value="${attrib.definition.renderingInfo.label}"/></span>
            </c:otherwise>
         </c:choose>
      </c:if>
   </td>
   
      <logic:present name="validationErrors" property="${attrib.name}">
         <td width="30%" class="ErrorField">
      </logic:present>

      <logic:notPresent name="validationErrors" property="${attrib.name}">
         <td width="30%" class="BlockContent">
      </logic:notPresent>

      <c:choose>
         <c:when test="${attrib.definition.renderingInfo.readOnly}">
            <c:set var="disabled" value="true" scope="page" />
         </c:when>
         <c:otherwise>
            <c:set var="disabled" value="false" scope="page" />
         </c:otherwise>
      </c:choose>

      <c:choose>
         <c:when test="${attrib.definition.renderingInfo.hidden}">
            <html:hidden property="${attrib.name}" value="${attrib.value}" />
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.obfuscated}">
            <html:password property="${attrib.name}" value="${attrib.value}" disabled="${disabled}" /><br>
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.class.name == 'com.jboss.jbossnetwork.command.param.OptionListRenderingInformation'}">
            <html:select property="${attrib.name}" value="${attrib.value}" disabled="${disabled}">
               <html:options
                name="attrib"
                property="definition.allowedValues"
                labelName="attrib"
                labelProperty="definition.renderingInfo.optionLabels"/>
            </html:select><br>
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.class.name == 'com.jboss.jbossnetwork.command.param.TextFieldRenderingInformation'
                      and attrib.definition.renderingInfo.fieldHeight > 1}">
            <html:textarea 
             rows="${attrib.definition.renderingInfo.fieldHeight}" 
             cols="${attrib.definition.renderingInfo.fieldLength}" 
             property="${attrib.name}"
             value="${attrib.value}"
             readonly="${disabled}" /><br>
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.class.name == 'com.jboss.jbossnetwork.command.param.UnorderedListRenderingInformation'}">
            <ul>
               <c:forEach var="item" items="${attrib.value}">
                  <li><c:out value="${item}"/></li>
               </c:forEach>                    
            </ul><br>
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.class.name == 'com.jboss.jbossnetwork.command.param.TextFieldRenderingInformation'}">
            <html:text
             size="${attrib.definition.renderingInfo.fieldLength}" 
             property="${attrib.name}"
             value="${attrib.value}" 
             readonly="${disabled}" /><br>
         </c:when>
         <c:when test="${attrib.definition.renderingInfo.class.name == 'com.jboss.jbossnetwork.command.param.FileUploadRenderingInformation'}">
            <html:file property="${attrib.name}" value="" /><br>
         </c:when>
         <c:when test="${attrib.definition.type == 'java.lang.Boolean'}">
            <input type="checkbox" name="<c:out value='${attrib.name}'/>" value="true"
               <c:if test="${attrib.value}">CHECKED</c:if>
               <c:if test="${disabled}">DISABLED</c:if>
            /><br>
         </c:when>
         <c:otherwise>
            <%-- this field uses readonly rather than disabled to make sure that
                 the value gets submitted when used in a part readonly/part writable form --%>
            <html:text 
             property="${attrib.name}"
             value="${attrib.value}"
             readonly="${disabled}"/><br>
         </c:otherwise>
      </c:choose>
      <logic:present name="validationErrors" property="${attrib.name}">
         <span class="ErrorFieldContent">- <c:out value="${validationErrors[attrib.name]}"/></span>
      </logic:present>
   </td>
</tr>
</c:forEach>

<tr>
   <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
</tr>
</table>
<!--  /  -->

&nbsp;<br>

<tiles:insert definition=".form.buttons" />
<tiles:insert definition=".page.footer"/>

</form>
<script language="JavaScript" src="<html:rewrite page="/js/wz_tooltip.js"/>" type="text/javascript"></script>