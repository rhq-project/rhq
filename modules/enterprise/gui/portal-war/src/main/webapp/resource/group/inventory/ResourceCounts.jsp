<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<!-- CONSTANT DEFINITIONS -->
<hq:constant
    classname="org.rhq.enterprise.server.legacy.appdef.shared.AppdefEntityConstants"
    symbol="APPDEF_TYPE_GROUP_ADHOC_PSS" var="CONST_ADHOC_PSS" />

<tiles:importAttribute name="resourceCount"/>
<tiles:importAttribute name="resourceTypeMap" ignore="true"/>


<!--  GENERAL PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.group.inventory.ResourceCountsTab"/>
</tiles:insert>
<!--  /  -->

<!--  RESOURCE COUNTS CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
	<tr>
		<td width="20%" class="BlockLabel"><fmt:message key="resource.group.inventory.TotalLabel"/></td>
		<td width="30%" class="BlockContent"><c:out value="${resourceCount}"/></td>
    <td width="20%" class="BlockLabel">&nbsp;</td>
    <td width="30%" class="BlockContent">&nbsp;</td>
  </tr>
  <tr valign="top">
		<td width="20%" class="BlockLabel">&nbsp;
              <c:if test="${Resource.groupType == CONST_ADHOC_PSS}"> 
                  <fmt:message key="resource.group.inventory.ResourcesByTypeLabel"/>
              </c:if>
        </td>
    <td width="30%" class="BlockContentNoPadding" colspan="3">
      <table width="66%" cellpadding="0" cellspacing="0" border="0" class="BlockContent">
        <tr valign="top">
<c:forEach var="entry" varStatus="status" items="${resourceTypeMap}">
          <td width="50%"><c:out value="${entry.key}"/> (<c:out value="${entry.value}"/>)</td>
  <c:choose>
    <c:when test="${status.count % 2 == 0}">
        </tr>
        <tr>
    </c:when>
    <c:otherwise>
      <c:if test="${status.last}">
        <c:forEach begin="${(status.count % 2) + 1}" end="2">
          <td width="50%">&nbsp;</td>
        </c:forEach>
      </c:if>
    </c:otherwise>
  </c:choose>
</c:forEach>
			    </tr>
			</table> 
        </td>
	</tr>
	<tr>
      <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->