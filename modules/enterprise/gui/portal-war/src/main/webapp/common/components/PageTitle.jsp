<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script language="JavaScript" type="text/javascript">
   var help = "<hq:help/>";

   function loadResourceSummary(url, divName)
   {
      var div = document.getElementById(divName);
      var xmlhttp = getXMLHttpRequest();
      xmlhttp.open('GET', url, true);
      xmlhttp.onreadystatechange = function()
      {
         if (xmlhttp.readyState == 4) // i.e. response fully received
         {
            if (xmlhttp.status == 200) // i.e. success
            {
               div.innerHTML = xmlhttp.responseText;
            }
            else
            {
               window.location.reload(true); // refresh the page for re-login
            }
         }
      }
      xmlhttp.send(null);
   }
</script>

<tiles:importAttribute name="titleKey" ignore="true"/>
<tiles:importAttribute name="titleName" ignore="true"/>
<tiles:importAttribute name="titleBgStyle" ignore="true"/>
<tiles:importAttribute name="titleImg" ignore="true"/>
<tiles:importAttribute name="subTitleName" ignore="true"/>
<tiles:importAttribute name="resource" ignore="true"/>
<tiles:importAttribute name="resourceOwner" ignore="true"/>
<tiles:importAttribute name="linkUrl" ignore="true"/>
<tiles:importAttribute name="showSearch" ignore="true"/>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
<tr>
<td colspan="4">

<table width="100%" border="0" cellspacing="0" cellpadding="0">

<tr>
   <td valign="top" align="left" rowspan="99">
      <html:img page="/images/spacer.gif" width="8" height="1" alt="" border="0"/>
   </td>
   <td colspan="4">
      <html:img page="/images/spacer.gif" width="1" height="10" alt="" border="0"/>
   </td>
</tr>

<c:if test="${not empty resource || not empty linkUrl || not empty showSearch}">
<tr valign="top">
<c:choose>
   <c:when test="${not empty resource}">
      <td colspan="3">
         <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
               <td colspan=3>
                  <div id="resourceSummary"/>
                  <script language="JavaScript" type="text/javascript">
                     loadResourceSummary('<c:url value="/rhq/resource/layout/summary.xhtml?id=${param.id}"/>', "resourceSummary");
                  </script>
               </td>
            </tr>

            <c:if test="${empty ResourceType}">
               <tr>
                  <td colspan="3">
                        <%--
                        <tiles:insert definition=".resource.common.navmap">
                           <tiles:put name="resource" beanName="resource"/>
                        </tiles:insert>
                        --%>
                  </td>
               </tr>
            </c:if>
         </table>
      </td>
   </c:when>

   <c:when test="${not empty groupId}">
      <td colspan="3">
         <div id="groupSummary"/>
         <script language="JavaScript" type="text/javascript">
             loadResourceSummary('<c:url value="/rhq/group/layout/summary.xhtml?groupId=${param.groupId}"/>', "groupSummary");
         </script>
      </td>
   </c:when>

   <c:when test="${showSearch}">
      <td>
         <!--  SEARCH TOOLBAR CONTENTS -->
         <table width="100%" cellpadding="0" cellspacing="0" border="0">
            <tr>
               <td nowrap class="SearchBold">
                  <fmt:message key="resource.hub.search.label.Search"/>
               </td>
               <td class="SearchRegular">
                  <html:text property="keywords" onfocus="ClearText(this)" size="30" maxlength="40"/>
               </td>
               <td class="SearchRegular" width="100%">
                  <html:image page="/images/dash-button_go-arrow.gif" border="0" property="ok"/>
               </td>
            </tr>
         </table>
         <!--  /  -->
      </td>
   </c:when>

   <c:otherwise>
      <td class="PageTitleSmallText">&nbsp;</td>
      <td>&nbsp;</td>
   </c:otherwise>

</c:choose>

<c:choose>
   <c:when test="${not empty linkUrl}">
      <td colspan="2">
         <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
               <td>
                  <html:img page="/images/spacer.gif" width="5" height="1" alt="" border="0"/>
               </td>
               <td width="100%" class="PageTitleSmallText">
                  <tiles:insert attribute="linkUrl">
                     <c:if test="${not empty resource}">
                        <tiles:put name="resource" beanName="resource"/>
                     </c:if>
                  </tiles:insert>
               </td>
            </tr>
         </table>
      </td>
   </c:when>
   <c:otherwise>
      <td colspan="2"/>
   </c:otherwise>
</c:choose>

</tr>
</c:if>
<tr>
   <td colspan="4">
      <html:img page="/images/spacer.gif" width="1" height="13" alt="" border="0"/>
   </td>
</tr>
</table>

</td>
</tr>

   <tr>
      <td width="100%" colspan="5">
         <html:img page="/images/spacer.gif" width="20" height="1" alt="" border="0"/>
      </td>
   </tr>

</table>
