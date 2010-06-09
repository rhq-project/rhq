<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.RecentlyApproved"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>

<tiles:importAttribute name="recentlyApproved"/>

<c:choose >
  <c:when test="${not empty recentlyApproved}">
    <html:form action="/dashboard/ProcessRAList">
    <html:hidden property="platformId"/>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td width="3%" class="ListHeaderInactive">&nbsp;</td>
        <td width="37%" class="ListHeaderInactive" align="left"><fmt:message key="dash.home.TableHeader.Resource"/></td>
        <td width="30%" class="ListHeaderInactive" align="left"><fmt:message key="dash.home.TableHeader.DateTime"/></td>
      </tr>
      <c:forEach items="${recentlyApproved}" var="platform">
      <tr class="ListRow">
        <td class="ListCell">
           <c:choose>
             <c:when test="${platform.original.showChildren}">
               <a href="." onclick="RAListForm.platformId.value=<c:out
                  value="${platform.original.id}"/>; 
                  RAListForm.submit(); return false;">
                  <html:img page="/images/minus.gif" border="0"/>
               </a>
             </c:when>
             <c:otherwise>
               <a href="." onclick="RAListForm.platformId.value=<c:out
                  value="${platform.original.id}"/>; 
                  RAListForm.submit(); return false;">
                  <html:img page="/images/plus.gif" border="0"/>
               </a>
             </c:otherwise>
           </c:choose>
        </td>
        <td class="ListCell" align="left"><display:disambiguatedResourceName disambiguationReport="${platform}" resourceName="${platform.original.name}" resourceId="${platform.original.id}"/></td>
        <td class="ListCell" align="left"><hq:dateFormatter value="${platform.original.ctime}"/>&nbsp;</td>
      </tr>
      <c:if test="${platform.original.showChildren}">
      <!-- Show the platform's servers -->
      <c:forEach items="${platform.children}" var="server">
      <tr class="ListRow">
        <td class="ListCell"></td>
        <td class="ListCell" align="left"><display:disambiguatedResourceName disambiguationReport="${server}" resourceName="${server.original.name}" resourceId="${server.original.id}"/></td>
        <td class="ListCell" align="left"><hq:dateFormatter value="${server.original.ctime}"/>&nbsp;</td>
      </tr>
      </c:forEach> <!-- For each server -->
      </c:if>

      </c:forEach> <!-- For each platform -->
    </table>
    </html:form>

  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
