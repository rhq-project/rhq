<%@ page language="java" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div class="effectsPortlet">
<!-- Content Block -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="dash.home.SummaryCounts"/>
  <tiles:put name="adminUrl" beanName="adminUrl" />
  <tiles:put name="portletName" beanName="portletName" />
  <tiles:put name="showRefresh" beanName="showRefresh" />
</tiles:insert>

<tiles:importAttribute name="summary"/>
<tiles:importAttribute name="platform"/>
<tiles:importAttribute name="server"/>
<tiles:importAttribute name="service"/>
<tiles:importAttribute name="groupCompat"/>
<tiles:importAttribute name="groupMixed"/>
<tiles:importAttribute name="software"/>

<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="BlockContent" align="right">
      <tiles:insert page="/resource/hub/ResourceHubLinks.jsp"/>
    </td>
  </tr>
  <tr>
    <td class="BlockContent">    
      <table width="100%" cellpadding="1" cellspacing="0" border="0">

        <c:if test="${platform}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?resourceCategory=PLATFORM"><fmt:message key="dash.home.DisplayCategory.PlatformTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?resourceCategory=PLATFORM"><c:out value="${summary.platformCount}"/></html:link></td>
          </tr>
        </c:if>
      
        <c:if test="${server}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?resourceCategory=SERVER"><fmt:message key="dash.home.DisplayCategory.ServerTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?resourceCategory=SERVER"><c:out value="${summary.serverCount}"/></html:link></td>
          </tr>
        </c:if>
      
        <c:if test="${service}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/ResourceHub.do?resourceCategory=SERVICE"><fmt:message key="dash.home.DisplayCategory.ServiceTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/ResourceHub.do?resourceCategory=SERVICE"><c:out value="${summary.serviceCount}"/></html:link></td>
          </tr>
        </c:if>

        <c:if test="${groupCompat}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/GroupHub.do?groupCategory=COMPATIBLE"><fmt:message key="dash.home.DisplayCategory.group.CompatGroupTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/GroupHub.do?groupCategory=COMPATIBLE"><c:out value="${summary.compatibleGroupCount}"/></html:link></td>
          </tr>
        </c:if>

        <c:if test="${groupMixed}">      
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/GroupHub.do?groupCategory=MIXED"><fmt:message key="dash.home.DisplayCategory.group.MixedGroupTotal"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/GroupHub.do?groupCategory=MIXED"><c:out value="${summary.mixedGroupCount}"/></html:link></td>
          </tr>
        </c:if>

<%-- Old software system, got removed
        <c:if test="${software}">
          <tr>
            <td colspan="2">&nbsp;</td>
          </tr>
          <tr>
            <td class="FormLabel"><html:link page="/resource/software/inventory/List.do?mode=viewSoftware"><fmt:message key="dash.home.DisplayCategory.Software"/></html:link></td>
            <td></td>
          </tr>
          <tr>
            <td class="FormLabel">&nbsp;-&nbsp;<html:link page="/resource/software/inventory/List.do?mode=viewSoftware"><fmt:message key="dash.home.DisplayCategory.Software.LatestPatches"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/resource/software/inventory/List.do?mode=viewSoftware"><c:out value="${summary.softwareUpdateCount}"/></html:link></td>
          </tr>
          <tr>
            <td class="FormLabel">&nbsp;-&nbsp;<html:link page="/resource/software/inventory/List.do?mode=viewSoftware"><fmt:message key="dash.home.DisplayCategory.Software.LatestProducts"/></html:link></td>
            <td class="FormLabelRight"><html:link page="/resource/software/inventory/List.do?mode=viewSoftware"><c:out value="${summary.softwareProductCount}"/></html:link></td>
          </tr>
        </c:if>
--%>        
    <tr>
      <td colspan="2">&nbsp;</td>
    </tr>
    <tr>
      <td class="FormLabel">Average Metrics per Minute</td>
      <td class="FormLabelRight"><c:out value="${summary.scheduledMeasurementsPerMinute}"/></td>
    </tr>
      
      </table>
    </td>
  </tr>
  <tr>
    <td class="BlockContent"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
  <tr>
    <td class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
</div>
