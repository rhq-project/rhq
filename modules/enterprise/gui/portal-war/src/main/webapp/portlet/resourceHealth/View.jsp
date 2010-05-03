 <%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<c:url var="rssUrl" value="/rss/ViewResourceHealth.rss">
  <c:param name="user" value="${webUser.username}"/>
</c:url>

<div class="effectsPortlet">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.ResourceHealth"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="rssUrl" beanName="rssUrl" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>

<tiles:importAttribute name="resourceHealth"/>
<tiles:importAttribute name="availability"/>
<tiles:importAttribute name="alerts"/>

<c:choose >
  <c:when test="${not empty resourceHealth}">   
  
    <display:table cellspacing="0" cellpadding="0" width="100%" action="/Dashboard.do"
                   var="item" items="${resourceHealth}" >
                
        <display:column width="25%" href="/rhq/resource/summary/overview.xhtml?id=${item.original.id}" property="original.name" sortAttr="res.name" title="dash.home.TableHeader.ResourceName"/>
        <display:column width="25%" property="original.typeName" title="dash.home.TableHeader.Type"/>
        <display:column width="25%" property="lineage" title="dash.home.TableHeader.Location"/>
        <c:if test="${alerts}">                  
          <display:column width="10%" property="original.alerts" title="dash.home.TableHeader.Alerts" align="center"/>          
        </c:if>
        <c:if test="${availability}">  
          <display:column width="15%" property="original.availabilityType" title="resource.common.monitor.visibility.AvailabilityTH" align="center"
                          sortAttr="avail.availabilityType" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox" valign="middle">
             <display:availabilitydecorator/>
          </display:column>
        </c:if>        
    </display:table>
     
    <tiles:insert definition=".dashContent.seeAll"/>
    
  </c:when>
  <c:otherwise>
    <table width="100%" cellpadding="0" cellspacing="0" border="0">
      <tr class="ListRow">
        <td class="ListCell"><fmt:message key="dash.home.no.resource.to.display"/></td>
      </tr>
    </table>
  </c:otherwise>
</c:choose>
</div>
