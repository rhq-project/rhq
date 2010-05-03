<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<tiles:importAttribute name="criticalAlerts"/>

<c:url var="rssUrl" value="/rss/ViewCriticalAlerts.rss">
  <c:param name="user" value="${webUser.username}"/>
</c:url>

<div class="effectsPortlet">
<!-- Content Block  -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.CriticalAlerts"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="rssUrl" beanName="rssUrl" />
  <tiles:put name="showRefresh" beanName="showRefresh" />  
</tiles:insert>

<c:choose >
  <c:when test="${not monitorEnabled}">   
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="common.marketing.FeatureDisabled"/></td>
      </tr>
    </table>
  </c:when>
  <c:when test="${not empty criticalAlerts}">  
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr>
        <td width="20%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.ResourceName"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
        <td width="20%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Type"/></td>
        <td width="20%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.Location"/></td>
        <td width="20%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.AlertName"/></td>
        <td width="20%" class="ListHeaderInactiveCenter"><fmt:message key="dash.home.TableHeader.DateTime"/></td>
      </tr>
      <c:forEach items="${criticalAlerts}" var="item">      
      <tr class="ListRow">
         <c:choose> 
          <c:when test="{item.original.alertDefinition.resource eq null}">
            <td class="ListCell" colspan="3">
              <fmt:message key="dash.home.removed.resource"/>
            </td>
          </c:when>
          <c:otherwise>
            <td class="ListCell">
              <html:link page="/rhq/resource/summary/overview.xhtml?id=${item.original.alertDefinition.resource.id}"><c:out value="${item.original.alertDefinition.resource.name}"/>&nbsp;</html:link>
            </td>
            <td class="ListCell"><c:out value="${item.original.alertDefinition.resource.resourceType.name}" /></td>
            <td class="ListCell"><c:out value="${item.lineage}" />&nbsp;</td>
          </c:otherwise>
        </c:choose>
        <td class="ListCell"><html:link page="/alerts/Alerts.do?mode=viewAlert&id=${item.original.alertDefinition.resource.id}&a=${item.original.id}"><c:out value="${item.original.alertDefinition.name}"/>&nbsp;</html:link></td>
        <td class="ListCell" align="center"><hq:dateFormatter value="${item.original.ctime}"/>&nbsp;</td>
      </tr>  
      </c:forEach>
    </table>
    <tiles:insert definition=".dashContent.seeAll"/>
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.alerts.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
