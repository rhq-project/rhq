<%@ page import="java.util.List" %>
<%@ page import="org.rhq.core.domain.resource.ResourceType" %>
<%@ page import="java.util.Map" %>
<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<tiles:insert definition=".page.title">
  <tiles:put name="titleBgStyle" value="PageTitle"/>
  <tiles:put name="titleImg" value="spacer.gif"/>
  <tiles:put name="titleKey" value="admin.resource.templates.PageTitle"/>
</tiles:insert>


<table width="100%" cellpadding="0" cellspacing="0" border="0" id="listTable">
<!-- PLATFORM CONTENTS -->
	<tr class="ListHeaderDark">
      <td width="50%" class="ListHeaderInactiveSorted"><fmt:message key="resource.hub.PlatformTypeTH"/>s</td>
      <td width="25%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
      <td width="25%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
      </tr>
  	<c:set var="PlatformServices" value="${PlatformServices}"/>
    <c:forEach var="entry" varStatus="status" items="${platformTypes}">
    <tr class="ListRowSelected">
      <td class="ListCellPrimary"><c:out value="${entry.name}"/></td>
      <td class="ListCell" align="center" nowrap="nowrap">
         <c:if test="${monitorEnabled}">
         <html:link page="/admin/platform/monitor/Config.do?mode=configure&id=${entry.id}&type=${entry.id}" styleClass="buttonsmall">
             Edit Metric Template
         </html:link>
         </c:if>
     </td>
      <td class="ListCell" align="center" nowrap="nowrap">
        <c:if test="${monitorEnabled}">   
        <html:link page="/rhq/admin/listAlertTemplates.xhtml?type=${entry.id}" styleClass="buttonsmall">
            Edit Alert Templates
        </html:link>
        </c:if>
      </td>
    </tr>
    <tiles:insert definition=".resource.common.monitor.config.ShowOneResourceType">
       <tiles:put name="resourceType" beanName="entry"/> 
       <tiles:put name="servicesMap" beanName="PlatformServices"/>
    </tiles:insert>
    
  </c:forEach>
<!--  /  -->

	<tr>
	  <td><html:img page="/images/spacer.gif" width="1" height="15" border="0"/></td>
	  <td></td>
	  <td></td>
	</tr>

<!-- SERVER CONTENTS -->
	<tr class="ListHeaderDark">
      <td width="50%" class="ListHeaderInactiveSorted"><fmt:message key="resource.hub.ServerTypeTH"/>s</td>
      <td width="25%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
      <td width="25%" class="ListHeaderInactive" align="center" nowrap>&nbsp;</td>
  	</tr>
  	<c:set var="Services" value="${Services}"/> 
    <c:forEach var="entry" varStatus="status" items="${serverTypes}">
    <tr class="ListRowSelected">
      <td class="ListCellPrimary"><c:out value="${entry.name}"/></td>
      <td class="ListCell" align="center">
         <c:if test="${monitorEnabled}">
         <html:link page="/admin/platform/monitor/Config.do?mode=configure&type=${entry.id}" styleClass="buttonsmall">
             Edit Metric Template
         </html:link>
         </c:if>
      </td>
      <td class="ListCell" align="center">
        <c:if test="${monitorEnabled}">   
          <html:link page="/rhq/admin/listAlertTemplates.xhtml?type=${entry.id}" styleClass="buttonsmall">
              Edit Alert Templates
          </html:link>
        </c:if>
        </td>
    </tr>
    <tiles:insert definition=".resource.common.monitor.config.ShowOneResourceType">
       <tiles:put name="resourceType" beanName="entry"/> 
       <tiles:put name="servicesMap" beanName="Services"/>
    </tiles:insert>
  </c:forEach>
<!--  /  -->
</table>

<!-- FOOTER -->
<tiles:insert definition=".page.footer"/>
