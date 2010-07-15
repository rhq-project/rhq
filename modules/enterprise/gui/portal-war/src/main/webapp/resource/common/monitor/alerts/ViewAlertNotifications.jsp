<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!-- Content Block Title: Notification -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.current.detail.notify.Tab"/>
</tiles:insert>

<!-- Notification Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
    <c:choose>
        <c:when test="${not empty aNotifLogs }">
            <tr>
                <td class="BlockLeftAlignLabel" width="15%">Sender</td>
                <td class="BlockLeftAlignLabel" width="15%">Result</td>
                <td class="BlockLeftAlignLabel" width="70%">Message</td>
            </tr>
            <tr>
                <td colspan="3" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
            </tr>
            <c:forEach var="notif" items="${aNotifLogs}">
                <tr valign="top">
                    <td class="BlockContent"><c:out value="${notif.sender}"/></td>
                    <td class="BlockContent"><c:out value="${notif.resultState}"/></td>
                    <td class="BlockContent"><c:out escapeXml="false" value="${fn:replace(fn:replace(notif.message, '&lt;', '<'), '&gt;', '>')}"/></td>
                </tr>
                <tr>
                    <td colspan="3" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
                </tr>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <tr>
               <td class="BlockContent">
                  <strong>No notifications were specified for this alert's definition</strong>
               </td>
               <tr>
                  <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
               </tr>
            </tr>
        </c:otherwise>
    </c:choose>
</table>
