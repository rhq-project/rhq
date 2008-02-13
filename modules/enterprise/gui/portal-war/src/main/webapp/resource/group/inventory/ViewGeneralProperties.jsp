<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="CAN_MODIFY_GROUP" var="CAN_MODIFY" /> 
<c:set var="canModify" value="${requestScope[CAN_MODIFY]}"/>

<!--  TYPE AND NETWORK PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.props.GeneralPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td class="BlockLabel"><fmt:message key="common.label.Description"/>
    <td width="30%" class="BlockContent"><c:out value="${group.description}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateCreatedLabel"/></td>
    <td width="30%" class="BlockContent"><hq:dateFormatter value="${group.ctime}"/></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.security.ResourceTypeLabel"/></td>
    <td class="BlockContent"><fmt:message key="resource.type.Group"/></td>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateModLabel"/></td>
    <td class="BlockContent"><hq:dateFormatter value="${group.mtime}"/></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.RecursiveLabel"/></td>
    <td class="BlockContent"><c:out value="${group.recursive}"/></td>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.ModByLabel"/></td>
    <td class="BlockContent"><c:out value="${group.modifiedBy.name}" escapeXml="false"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<c:set var="editUrl" value="/resource/group/Inventory.do?mode=edit&category=${category}&groupId=${group.id}"/>

<c:if test="${canModify}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>
</c:if>

