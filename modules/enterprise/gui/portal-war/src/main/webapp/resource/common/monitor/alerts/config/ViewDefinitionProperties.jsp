<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!-- Content Block Title: Properties -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="alert.config.props.PropertiesBox"/>
</tiles:insert>

<!-- Properties Content -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td colspan="4" class="BlockContent"><span style="height: 1px;"></span></td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Name"/></td>
    <td width="30%" class="BlockContent"><c:out value="${alertDef.name}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.Priority"/></td>
    <td width="30%" class="BlockContent">
      <c:out value="${alertDef.priority.displayName}" />
    </td>
  </tr>
  <tr valign="top">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.Description"/></td>
    <td width="30%" class="BlockContent"><c:out value="${alertDef.description}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.Active"/></td>
    <tiles:insert page="/resource/common/monitor/alerts/config/AlertDefinitionActive.jsp">
    <tiles:put name="alertDef" beanName="alertDef"/>
    </tiles:insert>
  </tr>
  <tr valign="top">
    <c:choose>
    <c:when test="${alertDef.parentId > 0}">
    <td width="20%" class="BlockLabel"><fmt:message key="common.label.ReadOnly"/></td>
    <td width="30%" class="BlockContent" rowspan="2"><c:out value="${alertDef.readOnly}"/></td>
    </c:when>
    <c:otherwise>
    <td width="50%" colspan="2" class="BlockLabel">&nbsp;</td>
    </c:otherwise>
    </c:choose>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.DateCreated"/></td>
    <td width="30%" class="BlockContent"><hq:dateFormatter time="false" value="${alertDef.ctime}"/></td>
  </tr>
  <tr valign="top">
    <td width="50%" colspan="2" class="BlockLabel">&nbsp;</td>
    <td width="20%" class="BlockLabel"><fmt:message key="alert.config.props.PB.DateMod"/></td>
    <td width="30%" class="BlockContent"><hq:dateFormatter time="false" value="${alertDef.mtime}"/></td>
  </tr>
  <c:if test="${alertDef.parentId > 0}">
  
  <tr>
    <td colspan="4" class="BlockContent"><span style="height: 3px;"></span></td>
  </tr>
  <c:if test="${!alertDef.deleted}">
  <tr>
    <td colspan="4" class="BlockContent">
      <span class="red" style="padding-left: 15px;">
        <fmt:message key="alerts.config.service.DefinitionList.isResourceAlert.false"/>
      </span> 
      <fmt:message key="alert.config.props.PB.IsTypeAlert"/>
      (<html:link page="/alerts/Config.do?mode=viewRoles&type=${Resource.resourceType.id}&ad=${alertDef.parentId}"><fmt:message key="alert.config.props.ViewTemplate"/></html:link>)
    </td>
  </tr>
  </c:if>
  
  </c:if>
  <tr>
    <td colspan="4" class="BlockContent"><span style="height: 1px;"></span></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><span style="height: 1px;"></span></td>
  </tr>
</table>

<c:if test="${not empty Resource}">
  <hq:authorization permission="MANAGE_ALERTS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/alerts/Config.do?mode=editProperties&ad=${alertDef.id}&id=${Resource.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
</c:if>

<c:if test="${not empty ResourceType}">
  <hq:authorization permission="MANAGE_SETTINGS">
    <tiles:insert definition=".toolbar.edit">
      <tiles:put name="editUrl"><c:out value="/alerts/Config.do?mode=editProperties&ad=${alertDef.id}&type=${ResourceType.id}"/></tiles:put>
    </tiles:insert>
  </hq:authorization>
</c:if>

<br>
