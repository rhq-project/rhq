<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="viewRolesUrl"/>
<tiles:importAttribute name="viewUsersUrl"/>
<tiles:importAttribute name="viewOthersUrl"/>
<tiles:importAttribute name="viewSnmpUrl"/>

<!-- MINI-TABS -->
<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr> 
    <td class="MiniTabEmpty"><html:img page="/images/spacer.gif"
      width="20" height="1" alt="" border="0"/>
    </td>

    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewRoles'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.Roles"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link href="${viewRolesUrl}"><fmt:message key="monitoring.events.MiniTabs.Roles"/></html:link></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>
    
    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewUsers'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.CAMusers"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewUsersUrl}"><fmt:message
              key="monitoring.events.MiniTabs.CAMusers"/></html:link></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>
    
    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewOthers'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.OR"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewOthersUrl}"><fmt:message
              key="monitoring.events.MiniTabs.OR"/></html:link>
            </td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>

    <c:if test="${snmpEnabled}">
    <td nowrap>
      <table width="100%" border="0" cellspacing="0" cellpadding="0">
        <tr> 
          <c:choose>
          <c:when test="${param.mode == 'viewSnmp'}">
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_on.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOn" nowrap><fmt:message key="monitoring.events.MiniTabs.SNMP"/></td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_on.gif" width="11" height="19" alt="" border="0"/></td>
          </c:when>
          <c:otherwise>
            <td valign="top" width="15"><html:img page="/images/miniTabs_left_off.gif" width="11" height="19" alt="" border="0"/></td>
            <td class="MiniTabOff" nowrap><html:link
              href="${viewSnmpUrl}"><fmt:message
              key="monitoring.events.MiniTabs.SNMP"/></html:link>
            </td>
            <td valign="top" width="17"><html:img page="/images/miniTabs_right_off.gif" width="11" height="19" alt="" border="0"/></td>
          </c:otherwise>
          </c:choose>
        </tr>
      </table>
    </td>
    </c:if>

    <td width="100%" class="MiniTabEmpty"><html:img
      page="/images/spacer.gif" width="1" height="1" alt=""
      border="0"/>
    </td>
  </tr>
  
  <tr> 
    <td colspan="6" width="100%" class="SubTabCell"><html:img
      page="/images/spacer.gif" width="1" height="3" alt=""
      border="0"/>
    </td>
  </tr>
</table>
<!-- / MINI-TABS -->
