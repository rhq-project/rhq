<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- ViewUserProperties.jsp -->

<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.user.GeneralProperties"/>
</tiles:insert>

<!--  GENERAL PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Name"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.firstName} ${User.lastName}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Username"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.name}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Email"/></td>
    <td width="30%" class="BlockContent">
     <html:link href="mailto:${User.emailAddress}">
      <c:out value="${User.emailAddress}"/>
     </html:link>
    </td>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Phone"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.phoneNumber}"/></td>
  </tr>
  <tr>
<c:choose>
  <c:when test="${not User.hasPrincipal}">
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </c:when>
  <c:otherwise>
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Password"/></td>
    <td width="30%" class="BlockContent">
     <html:link page="/admin/user/UserAdmin.do?mode=editPass&u=${User.id}">
      <fmt:message key="admin.user.generalProperties.Change"/>
     </html:link>
    </td>
  </c:otherwise>
</c:choose>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.Department"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.department}"/></td>
  </tr>
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.EnableLogin"/></td>
    <td width="30%" class="BlockContent">
     <c:choose>
      <c:when test="${User.active}">
       <fmt:message key="admin.user.generalProperties.enableLogin.Yes"/>
      </c:when>
      <c:otherwise>
       <fmt:message key="admin.user.generalProperties.enableLogin.No"/>
      </c:otherwise>
     </c:choose>
    </td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
    <!-- 
    <td width="20%" class="BlockLabel"><fmt:message key="admin.user.generalProperties.smsAddress"/></td>
    <td width="30%" class="BlockContent"><c:out value="${User.smsaddress}"/></td>
     -->
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<!--  GENERAL PROPERTIES TOOLBAR -->
<c:if test="${useroperations['MANAGE_SECURITY'] or (User.name eq webUser.name)}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" value="/admin/user/UserAdmin.do?mode=edit"/>
  <tiles:put name="editParamName" value="u"/>
  <tiles:put name="editParamValue" beanName="User" beanProperty="id"/>
</tiles:insert>
</c:if>
<br>
