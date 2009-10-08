<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
 
<hq:constant var="PLATFORM"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="PLATFORM"/>
<hq:constant var="SERVER"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVER"/>
<hq:constant var="SERVICE"
             classname="org.rhq.core.domain.resource.ResourceCategory"
             symbol="SERVICE"/>

<!--  TYPE AND NETWORK PROPERTIES TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.inventory.props.GeneralPropertiesTab"/>
</tiles:insert>
<!--  /  -->

<!--  TYPE AND HOST PROPERTIES CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0" class="BlockBg">
  <tr>
    <td class="BlockLabel"><fmt:message key="common.label.Description"/>
    <td width="30%" class="BlockContent"><c:out value="${Resource.description}"/></td>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateCreatedLabel"/></td>
    <td width="30%" class="BlockContent"><hq:dateFormatter value="${Resource.ctime}"/></td>
  </tr>
  <tr>
    <c:choose>
      <c:when test="${Resource.resourceType.category.name == PLATFORM}">
         <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.LocationLabel"/>
         <td class="BlockContent"><c:out value="${Resource.location}"/></td>
      </c:when>
      <c:otherwise>
        <td colspan="2" class="BlockLabel">&nbsp;</td>
      </c:otherwise>
    </c:choose>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.DateModLabel"/></td>
    <td class="BlockContent"><hq:dateFormatter value="${Resource.mtime}"/></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.security.ResourceTypeLabel"/></td>
    <td class="BlockContent">
        <c:out value="${Resource.resourceType.name}"/>
    </td>
    <td class="BlockLabel"><fmt:message key="resource.common.inventory.props.ModByLabel"/></td>
    <td class="BlockContent"><c:out value="${Resource.modifiedBy.name}" escapeXml="false"/></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
<!--  /  -->

<c:set var="editUrl" value="/resource/Inventory.do?mode=edit&id=${Resource.id}"/>

<c:if test="${canModify}">
<tiles:insert definition=".toolbar.edit">
  <tiles:put name="editUrl" beanName="editUrl"/>
</tiles:insert>
</c:if>

