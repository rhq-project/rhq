<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<td>
<link rel="stylesheet" type="text/css" media="screen"  href="<html:rewrite page="/css/start.css"/>" />


<%
  String divStart;
  String divEnd;
  String narrowWidth;
    
  String agent = request.getHeader("USER-AGENT");
  
  if (null != agent && -1 !=agent.indexOf("MSIE")) {
    divStart = "";
    divEnd = "";
    narrowWidth = "width='25%'";
  }
  else {
    divStart = "<div name=\"containerDiv\" style=\"margin:0px; width: 25%\">";
    divEnd = "</div>";
    narrowWidth = "width='100%'";
  }
%>


<div>
	<ul class="icon">
	    <c:if test="${useroperations['MANAGE_SETTINGS']}">
			<li class="category" id="admin"><fmt:message key="start.category.AdminTools.title"/></li>
			<li class="items">
					<ul class="pipe">
						<li class="first"><html:link action="admin/config/Config.do?mode=edit"><fmt:message key="start.category.AdminTools.item.EditConfig"/></html:link></li>
						<li><html:link action="admin/config/EditDefaults.do?mode=monitor&viewMode=all"><fmt:message key="start.category.AdminTools.item.ViewMonitoring"/></html:link></li>
						<li><html:link action="admin/license/LicenseAdmin.do?mode=view"><fmt:message key="start.category.AdminTools.item.ManageLicense"/></html:link></li>
					</ul>
			</li>
		</c:if>
		<li class="category" id="user"><fmt:message key="start.category.Users.title"/></li>
		<li class="items">
				<ul class="pipe">
					<li class="first"><html:link action="admin/user/UserAdmin.do?mode=editPass&u=${userId}"><fmt:message key="start.category.Users.item.changePassword"/></html:link></li>
					<li><html:link action="admin/user/UserAdmin.do?mode=edit&u=${userId}"><fmt:message key="start.category.Users.item.edit"/></html:link></li>
					<c:if test="${useroperations['MANAGE_SECURITY']}">
						<li><html:link action="admin/user/UserAdmin.do?mode=list"><fmt:message key="start.category.Users.item.remove"/></html:link></li>
						<li><html:link action="admin/role/RoleAdmin.do?mode=list"><fmt:message key="start.category.Users.item.manageRoles"/></html:link></li>
					</c:if>
				</ul>
		</li>
		<li class="category" id="solution"><fmt:message key="start.category.Solutions.title"/></li>
		<li class="items">
				<ul class="pipe">
					<li class="first"><html:link target="_blank" href="${helpBaseURL}"><fmt:message key="start.category.Solutions.item.helpContents"/></html:link></li>
					<%--<li><html:link target="_blank" href="${helpBaseURL}Console+Reference"><fmt:message key="start.category.Solutions.item.consoleReference"/></html:link></li>--%>
					<li><html:link target="_blank" href="https://support.redhat.com/jbossnetwork/restricted/addCase.html"><fmt:message key="start.category.Solutions.item.openCase"/></html:link></li>
				</ul>
		</li>

	</ul>
</div>

<!-- /Content Block --> 
</td>
