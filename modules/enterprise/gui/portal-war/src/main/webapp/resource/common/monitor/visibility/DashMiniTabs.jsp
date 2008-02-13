<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="selectedIndex" ignore="true"/>
<tiles:importAttribute name="appdefResourceType" ignore="true"/>
<tiles:importAttribute name="entityType" ignore="true"/>

<tiles:importAttribute name="tabListName" ignore="true"/>
<c:choose>
   <c:when test="${'perf' == tabListName}">
      <tiles:useAttribute id="tabList" name="perf" ignore="true"/>
   </c:when>
   <c:when test="${'nometrics' == tabListName}">
      <tiles:useAttribute id="tabList" name="nometrics" ignore="true"/>
   </c:when>
   <c:otherwise>
      <tiles:useAttribute id="tabList" name="standard" ignore="true"/>
   </c:otherwise>
</c:choose>

<c:if test="${not empty tabList}">
   <tiles:importAttribute name="resourceId" ignore="true"/>
   <tiles:importAttribute name="autogroupResourceType" ignore="true"/>
</c:if>
<tiles:importAttribute name="subTabList" ignore="true"/>
<c:if test="${not empty subTabList}">
   <tiles:importAttribute name="subTabUrl"/>
</c:if>

<c:if test="${empty selectedIndex}">
   <c:set var="selectedIndex" value="0"/>
</c:if>

<!-- DASH-MINI-TABS -->

<table border="0" cellspacing="0" cellpadding="0" class="DashTabs">
   <tr>
      <td class="MiniTabEmpty" width="20"><html:img page="/images/spacer.gif" width="20" height="1" alt=""
                                                    border="0"/></td>

      <c:forEach var="tab" items="${tabList}">

         <c:choose>
            <c:when test="${not empty tab.icon}">
               <fmt:message var="tabText" key="${tab.icon}"/>
            </c:when>
            <c:otherwise>
               <c:set var="tabText" value="${tab.name}"/>
            </c:otherwise>
         </c:choose>
         <%-- which base url do we go to? Needed below --%>
         <c:choose>
            <c:when test="${groupId > 0 }">
               <c:set var="baseUrl" value="/resource/group/monitor/Visibility.do"/>
            </c:when>
            <c:otherwise>
               <c:set var="baseUrl" value="/resource/common/monitor/Visibility.do"/>
            </c:otherwise>
         </c:choose>

         <c:choose>
            <c:when test="${tab.value == selectedIndex}">
               <!-- Table cell tags have to be right next to image, otherwise IE creates
               a space -->
               <td valign="bottom"><html:img page="/images/MiniTab_${tab.icon}_on.gif" border="0" width="104"
                                             height="15"/></td>
            </c:when>
            <c:otherwise>
               <c:url var="tabLink" value="${baseUrl}">
                  <c:choose>
                     <c:when test="${not empty tab.mode}">
                        <c:param name="mode" value="${tab.mode}"/>
                     </c:when>
                     <c:otherwise>
                        <c:param name="mode" value="currentHealth"/>
                     </c:otherwise>
                  </c:choose>
                  <c:choose>
                     <c:when test="${not empty groupId}">
                        <c:param name="groupId" value="${groupId }"/>
                     </c:when>
                     <c:when test="${not empty parent}">
                        <c:param name="parent" value="${parent}"/>
                        <c:param name="type" value="${autogroupResourceType}"/>
		               </c:when>
                     <c:otherwise>
                        <c:param name="id" value="${resourceId}"/>
                     </c:otherwise>
                  </c:choose>
                  <c:choose>
                     <c:when test="${not empty category}">
                        <c:param name="category" value="${category}"/>
                     </c:when>
                  </c:choose>
               </c:url>
               <td class="MiniTabEmpty" valign="bottom"><html:link href="${tabLink}"><html:img
                     page="/images/MiniTab_${tab.icon}_off.gif"
                     onmouseover="imageSwap(this, imagePath + 'MiniTab_${tab.icon}', '_over');"
                     onmouseout="imageSwap(this, imagePath + 'MiniTab_${tab.icon}', '_off');" width="104" height="14"
                     border="0"/></html:link></td>
            </c:otherwise>
         </c:choose>

      </c:forEach>

      <td class="MiniTabEmpty" width="100%"><html:img page="/images/spacer.gif" height="1" width="100%" alt=""
                                                      border="0"/></td>
   </tr>
</table>

<!-- / DASH-MINI-TABS -->
