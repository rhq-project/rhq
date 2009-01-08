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
        <td width="60%" class="ListHeaderInactiveSorted"><fmt:message key="dash.home.TableHeader.ResourceName"/><html:img page="/images/tb_sortup_inactive.gif" width="9" height="9" border="0"/></td>
        <td width="20%" class="ListHeaderInactive"><fmt:message key="dash.home.TableHeader.AlertName"/></td>
        <td width="20%" class="ListHeaderInactiveCenter"><fmt:message key="dash.home.TableHeader.DateTime"/></td>
      </tr>
      <c:forEach items="${criticalAlerts}" var="alert">      
      <tr class="ListRow">
        <td class="ListCell">
         <c:choose> 
          <c:when test="{alert.resource eq null}">
            <fmt:message key="dash.home.removed.resource"/>
          </c:when>
          <c:otherwise>
            <html:link page="/rhq/resource/monitor/graphs.xhtml?id=${alert.alertDefinition.resource.id}"><c:out value="${alert.alertDefinition.resource.name}"/>&nbsp;</html:link>
          </c:otherwise>
        </c:choose>
        </td>
        <td class="ListCell"><html:link page="/alerts/Alerts.do?mode=viewAlert&id=${alert.alertDefinition.resource.id}&a=${alert.id}"><c:out value="${alert.alertDefinition.name}"/>&nbsp;</html:link></td>
        <td class="ListCell" align="center"><hq:dateFormatter value="${alert.ctime}"/>&nbsp;</td>
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
