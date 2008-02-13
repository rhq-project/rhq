<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<%--
  Tabs Layout tile component.
  This layout allows to render several tiles in a tabs fashion.

  @see org.apache.struts.tiles.beans.MenuItem A value object class that holds name and urls.
  
  @param tabList       A list of available tabs. We use MenuItem as a value object to
		       carry data (name, body, icon, ...)
  @param subTabList    A list of available sub tabs. We use MenuItem as a value object to 
                       carry data (name, body, icon, ...)
  @param selectedIndex Index of default selected tab
--%>

<tiles:useAttribute id="selectedIndexStr" name="selectedIndex" ignore="true" classname="java.lang.String"/>
<tiles:useAttribute name="tabList" classname="java.util.List"/>
<tiles:useAttribute name="subTabList" classname="java.util.List" ignore="true"/>
<tiles:useAttribute id="subSelectedIndexStr" name="subSelectedIndex" ignore="true"/>
<tiles:useAttribute name="subSectionName" ignore="true"/>
<tiles:useAttribute name="id" ignore="true"/>
<tiles:useAttribute name="type" ignore="true"/>
<tiles:useAttribute name="autogroupResourceId" ignore="true"/>
<tiles:useAttribute name="autogroupResourceType" ignore="true"/>
<tiles:importAttribute name="entityIds" ignore="true"/>

<%-- UNCOMMENT BELOW vvvvv TO ENABLE THE NAVIGATION MAP FEATURE --%>
<c:if test="${not empty autogroupResourceId}">
   <tiles:insert definition=".resource.common.navmap"/>
</c:if>
<%-- UNCOMMENT ABOVE ^^^^^ TO ENABLE THE NAVIGATION MAP FEATURE --%>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
   <tr>
      <td class="TabCell">
         <html:img page="/images/spacer.gif" width="20" height="1" alt="" border="0"/>
      </td>
      <c:forEach var="tab" varStatus="status" items="${tabList}">
         <hq:tabDisplayCheck tabName="${tab.value}" var="displayTab"/>
         <c:choose>
            <c:when test="${!displayTab}">
               <%-- The tab is not applicable to the current Resource - don't display anything. --%>
            </c:when>
            <c:when test="${status.index == selectedIndexStr}">
               <td>
                  <html:img page="/images/tab_${tab.value}_on.gif" width="${tab.width}" height="${tab.height}" alt=""
                            border="0"/>
               </td>
            </c:when>
            <c:when test="${not monitorEnabled && (tab.value eq 'Monitor' || tab.value eq 'Alert')}">
               <%--<td>--%>
                  <%--<html:img page="/images/tab_${tab.value}_off.gif" width="${tab.width}" height="${tab.height}" alt=""--%>
                            <%--border="0"/>--%>
               <%--</td>--%>
            </c:when>
            <c:otherwise>
               <c:url var="tabLink" value="${tab.link}">
                  <c:choose>
                     <c:when test="${not empty tab.mode}">
                        <c:param name="mode" value="${tab.mode}"/>
                     </c:when>
                     <c:when test="${tab.value eq 'Operations' and not empty groupId}"/>
                     <c:otherwise>
                        <c:param name="mode" value="view"/>
                     </c:otherwise>
                  </c:choose>
                  <c:choose>
                     <c:when test="${not empty groupId}">
                        <c:param name="groupId" value="${groupId }"/>
                        <c:param name="category" value="COMPATIBLE"/>
                     </c:when>
                     <c:otherwise>
                        <c:param name="id" value="${id}"/>
                     </c:otherwise>
                  </c:choose>
               </c:url>
               <td>
                  <html:link href="${tabLink}">
                     <html:img page="/images/tab_${tab.value}_off.gif"
                               onmouseover="imageSwap (this, imagePath +  'tab_${tab.value}', '_over')"
                               onmouseout="imageSwap (this, imagePath +  'tab_${tab.value}', '_off')"
                               width="${tab.width}" height="${tab.height}" alt="" border="0"/>
                  </html:link>
               </td>
            </c:otherwise>
         </c:choose>
      </c:forEach>
      <td width="100%" class="TabCell">
         <html:img page="/images/spacer.gif" width="1" height="1" alt="" border="0"/>
      </td>
   </tr>
   <tr>
      <td colspan="<%= ((java.util.List)pageContext.findAttribute("tabList")).size() + 2 %>">
         <table width="100%" border="0" cellspacing="0" cellpadding="0">
            <tr>
               <td class="SubTabCell">
                  <html:img page="/images/spacer.gif" width="5" height="25" alt="" border="0"/>
               </td>
               <c:forEach var="tab" varStatus="status" items="${subTabList}">
                  <c:choose>
                     <c:when test="${status.index == subSelectedIndexStr}">
                        <td>
                           <html:img page="/images/Sub${subSectionName}_${tab.value}_on.gif" width="${tab.width}"
                                     height="${tab.height}" border="0" alt=""/>
                        </td>
                     </c:when>
                     <c:otherwise>
                        <c:choose>
                           <c:when test="${tab.visible}">
                              <c:url var="tabLink" value="${tab.link}">
                                 <c:choose>
                                    <c:when test="${not empty tab.mode}">
                                       <c:param name="mode" value="${tab.mode}"/>
                                    </c:when>
                                    <c:otherwise>
                                       <c:param name="mode" value="view"/>
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
                                       <c:param name="id" value="${id}"/>
                                    </c:otherwise>
                                 </c:choose>
                                 <c:choose>
                                    <c:when test="${not empty category}">
                                       <c:param name="category" value="${category}"/>
                                    </c:when>                                    
                                 </c:choose>
                              </c:url>
                              <td>
                                 <html:link href="${tabLink}">
                                    <html:img page="/images/Sub${subSectionName}_${tab.value}_off.gif"
                                              onmouseover="imageSwap (this, imagePath +  'Sub${subSectionName}_${tab.value}', '_over')"
                                              onmouseout="imageSwap (this, imagePath +  'Sub${subSectionName}_${tab.value}', '_off')"
                                              width="${tab.width}" height="${tab.height}" alt="" border="0"/>
                                 </html:link>
                              </td>
                           </c:when>
                           <c:otherwise>
                              <td class="SubTabCell">
                                 <html:img page="/images/spacer.gif" width="${tab.width}" height="${tab.height}" alt=""
                                           border="0"/>
                              </td>
                           </c:otherwise>
                        </c:choose>
                     </c:otherwise>
                  </c:choose>
                  <c:if test="${status.count == 2}">
                     <td class="SubTabCell">
                        <html:img page="/images/spacer.gif" width="25" height="1" alt="" border="0"/>
                     </td>
                  </c:if>
               </c:forEach>
               <td width="100%" class="SubTabCell">
                  <html:img page="/images/spacer.gif" width="1" height="25" alt="" border="0"/>
               </td>
            </tr>
         </table>
      </td>
   </tr>
</table>
