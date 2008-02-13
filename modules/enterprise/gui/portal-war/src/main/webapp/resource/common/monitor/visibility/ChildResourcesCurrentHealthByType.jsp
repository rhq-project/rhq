<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>
<%@ taglib uri="/WEB-INF/tld/display.tld" prefix="display" %>

<c:set var="chbtWidget" value="currentHealthByType"/>
<script type="text/javascript">
initializeWidgetProperties('<c:out value="${chbtWidget}"/>');
chbtWidgetProps = getWidgetProperties('<c:out value="${chbtWidget}"/>');
</script>

<tiles:importAttribute name="summaries" ignore="true"/>
<tiles:importAttribute name="selfAction"/>

<c:url var="psAction" value="${selfAction}">
  <c:if test="${not empty param.pn}">
    <c:param name="pn" value="${param.pn}"/>
  </c:if>
</c:url>

<c:url var="pnAction" value="${selfAction}">
  <c:if test="${not empty param.ps}">
    <c:param name="ps" value="${param.ps}"/>
  </c:if>
  <c:if test="${not empty param.so}">
    <c:param name="so" value="${param.so}"/>
  </c:if>
  <c:if test="${not empty param.sc}">
    <c:param name="sc" value="${param.sc}"/>
  </c:if>
</c:url>

<c:set var="tmpImg"><html:img page="/images/icon_alert.gif" width="11" height="11" alt="" border="0"/></c:set>

<c:choose>
  <c:when test="${not empty summaries}">
    <div id="chbtListDiv">
      <display:table items="${summaries}" var="summary" action="${psAction}" width="100%" cellspacing="0" cellpadding="0">
        <display:column width="1%" property="id" title="<input type=\"checkbox\" onclick=\"ToggleAllCompare(this, chbtWidgetProps)\" name=\"listToggleAll\">" isLocalizedTitle="false" styleClass="ListCellCheckbox" headerStyleClass="ListHeaderCheckbox">
          <display:checkboxdecorator name="r" onclick="ToggleSelectionCompare(this, chbtWidgetProps);" styleClass="availableListMember"/>
        </display:column>
        <display:column width="50%" property="name" title="${typeName}" isLocalizedTitle="false" sort="true" sortAttr="5" defaultSort="true" styleClass="ListCell"
                        href="/resource/common/monitor/Visibility.do?mode=currentHealth&category=COMPATIBLE" paramId="id" paramProperty="id" nowrap="true"/>
        <display:column width="1%" value="${tmpImg}" title="&nbsp;"
                        isLocalizedTitle="false" styleClass="ListCellCheckboxLeftLine"
                        href="/rhq/resource/alert/listAlertDefinitions.xhtml?mode=list" paramId="id" paramProperty="id" nowrap="true"/>
        <display:column width="1%" property="alerts" title="resource.common.monitor.visibility.AlertsTH" styleClass="ListCell" align="center"/>
        <display:column property="availabilityType" width="8%" title="resource.common.monitor.visibility.AVAILTH" styleClass="ListCellCheckboxLeftLine" align="center">
          <display:availabilitydecorator/>
        </display:column>
      </display:table>

      <!--TODO the pagination needs tweaking to get to work-->
      <tiles:insert definition=".resource.common.monitor.visibility.metricsToolbar">
        <tiles:put name="widgetInstanceName" beanName="chbtWidget"/>
        <tiles:put name="useCompareButton" value="true"/>
        <tiles:put name="usePager" value="true"/>
        <tiles:put name="listItems" beanName="summaries"/>
        <tiles:put name="listSize" beanName="summaries" beanProperty="totalSize"/>
        <tiles:put name="pageSizeAction" beanName="psAction"/>
        <tiles:put name="pageNumAction" beanName="pnAction"/>
      </tiles:insert>

    </div>

  </c:when>
  <c:otherwise>
    <tiles:insert definition=".resource.common.monitor.visibility.noHealths"/>
  </c:otherwise>
</c:choose>

<input type="Hidden" id="privateChildResource">
<script type="text/javascript">
  testCheckboxes("ToggleButtonsCompare", '<c:out value="${chbtWidget}"/>', "privateChildResource", "availableListMember");
</script>
