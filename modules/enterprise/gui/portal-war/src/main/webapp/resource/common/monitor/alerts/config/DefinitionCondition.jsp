<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<tiles:importAttribute name="formName"/>

<html:hidden property="deletedCondition" value="${noneDeleted}"/>
<html:hidden property="numConditions"/>
<c:forEach var="i" begin="0" end="${numConditions - 1}">

<tr>
  <td width="20%" class="BlockLabel">
    <b><fmt:message key="alert.config.props.CB.IfCondition"/></b>
  </td>
  <logic:messagesPresent property="condition[${i}].metricId"><td width="80%" class="ErrorField"></logic:messagesPresent>
  <logic:messagesNotPresent property="condition[${i}].metricId"><td width="80%" class="BlockContent"></logic:messagesNotPresent>
    <html:radio property="condition[${i}].trigger" value="onMeasurement"/>
    <fmt:message key="alert.config.props.CB.Content.Metric"/>
    <c:set var="seldd"><fmt:message key="alert.dropdown.SelectOption"/></c:set>
    <html:select property="condition[${i}].metricId" 
                 onchange="javascript:selectMetric('condition[${i}].metricId', 'condition[${i}].metricName');changeDropDown('condition[${i}].metricId', 'condition[${i}].baselineOption', '${seldd}');">
      <html:option value="-1" key="alert.dropdown.SelectOption"/>
      <html:optionsCollection property="metrics" label="displayName" value="id"/>
    </html:select>
    <logic:messagesPresent property="condition[${i}].metricId">
    <span class="ErrorFieldContent">- <html:errors property="condition[${i}].metricId"/></span>
    </logic:messagesPresent>
    <html:hidden property="condition[${i}].metricName"/>
  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">
    
    <table width="100%" border="0" cellspacing="0" cellpadding="2">
      <tr> 
        <td nowrap="true"><div style="width: 60px; position: relative;"/><html:img page="/images/schedule_return.gif" width="17" height="21" border="0" align="right"/></td>
        <logic:messagesPresent property="condition[${i}].absoluteValue"><td width="100%" class="ErrorField"></logic:messagesPresent>
        <logic:messagesNotPresent property="condition[${i}].absoluteValue"><td width="100%"></logic:messagesNotPresent>
          <html:radio property="condition[${i}].thresholdType" value="absolute"/>
        
          <fmt:message key="alert.config.props.CB.Content.Is"/>
          <html:select property="condition[${i}].absoluteComparator">
            <hq:optionMessageList property="comparators" baseKey="alert.config.props.CB.Content.Comparator" filter="true"/>
          </html:select>
          <html:text property="condition[${i}].absoluteValue" size="8" maxlength="15"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.AbsoluteValue"/>
          <logic:messagesPresent property="condition[${i}].absoluteValue">
          <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].absoluteValue"/></span>
          </logic:messagesPresent>
        </td>
      </tr>
      <tr> 
        <td>&nbsp;</td>
        <logic:messagesPresent property="condition[${i}].percentage"><c:set var="percentageErrs" value="true"/></logic:messagesPresent>
        <logic:messagesPresent property="condition[${i}].baselineOption"><c:set var="baselineOptionErrs" value="true"/></logic:messagesPresent>
        <c:choose>
        <c:when test="${percentageErrs || baselineOptionErrs}"><td width="100%" class="ErrorField"></c:when>
        <c:otherwise><td width="100%"></c:otherwise>
        </c:choose>
          <html:radio property="condition[${i}].thresholdType" value="percentage"/>
          <fmt:message key="alert.config.props.CB.Content.Is"/>
          <html:select property="condition[${i}].percentageComparator">
            <hq:optionMessageList property="comparators" baseKey="alert.config.props.CB.Content.Comparator" filter="true"/>
          </html:select>
          <html:text property="condition[${i}].percentage" size="6" maxlength="4"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.Percent"/>&nbsp;
          <html:select property="condition[${i}].baselineOption" disabled="true">
          <html:option value="" key="alert.dropdown.SelectOption"/>
          </html:select>
          <c:if test="${! empty EditAlertDefinitionConditionsForm.conditions[i].metricId }">
          <script language="JavaScript" type="text/javascript">
          var baselineOption = '<c:out value="${EditAlertDefinitionConditionsForm.conditions[i].baselineOption}"/>';
          changeDropDown('condition[<c:out value="${i}"/>].metricId', 'condition[<c:out value="${i}"/>].baselineOption', '<c:out value="${seldd}"/>', baselineOption);
          </script>
          </c:if>
          <c:if test="${! empty NewAlertDefinitionForm.conditions[i].metricId}">
          <script language="JavaScript" type="text/javascript">
          var baselineOption = '<c:out value="${NewAlertDefinitionForm.conditions[i].baselineOption}"/>';
          changeDropDown('condition[<c:out value="${i}"/>].metricId', 'condition[<c:out value="${i}"/>].baselineOption', '<c:out value="${seldd}"/>', baselineOption);
          </script>
          </c:if> 
          <c:if test="${percentageErrs || baselineOptionErrs}">
          <span class="ErrorFieldContent">
          <c:if test="${percentageErrs}"><br>- <html:errors property="condition[${i}].percentage"/></c:if>
          <c:if test="${baselineOptionErrs}"><br>- <html:errors property="condition[${i}].baselineOption"/></c:if>
          </span>
          </c:if>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td width="100%">
          <html:radio property="condition[${i}].thresholdType" value="changed"/>
          <fmt:message key="alert.config.props.CB.Content.Changes"/>
        </td>
      </tr>
    </table>
    
  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  
  <logic:messagesPresent property="condition[${i}].traitStatus">
     <c:set var="traitStatusErrs" value="true"/>
  </logic:messagesPresent>
  
  <c:choose>
     <c:when test="${traitStatusErrs}">
        <td class="ErrorField">
     </c:when>
     <c:otherwise>
        <td class="BlockContent">
     </c:otherwise>
  </c:choose>
  
    <html:radio property="condition[${i}].trigger" value="onTrait"/>
    <fmt:message key="alert.config.props.CB.Content.Trait"/>&nbsp;
    <html:select property="condition[${i}].traitId"
                 onchange="javascript:selectMetric('condition[${i}].traitId', 'condition[${i}].traitName');">
       <html:option value="-1" key="alert.dropdown.SelectOption"/>
       <html:optionsCollection property="traits" label="displayName" value="id"/>
    </html:select>&nbsp;
    <fmt:message key="alert.config.props.CB.Content.Changes"/>
    
    <c:if test="${traitStatusErrs}">
      <br>
      <span class="ErrorFieldContent">- <html:errors property="condition[${i}].traitStatus"/></span>
    </c:if>
    
    <html:hidden property="condition[${i}].traitName"/>
    
  </td>
</tr>

<c:if test="${custPropsAvail}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
  <logic:messagesPresent property="condition[${i}].customProperty">
  <c:set var="customPropertyErrs" value="true"/>
  </logic:messagesPresent>
  <c:choose>
  <c:when test="${customPropertyErrs}">
  <td class="ErrorField" nowrap>
  </c:when>
  <c:otherwise>
  <td class="BlockContent" nowrap>
  </c:otherwise>
  </c:choose>
    <html:radio property="condition[${i}].trigger" value="onCustProp"/>
    <fmt:message key="alert.config.props.CB.Content.CustomProperty"/>
    <html:select property="condition[${i}].customProperty">
      <html:option value="" key="alert.dropdown.SelectOption"/>
      <html:optionsCollection property="customProperties"/>
    </html:select>
    <fmt:message key="alert.config.props.CB.Content.Changes"/>
  </td>
</tr>
</c:if>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  
  <logic:messagesPresent property="condition[${i}].availabilityStatus">
     <c:set var="availabilityStatusErrs" value="true"/>
  </logic:messagesPresent>
  
  <c:choose>
     <c:when test="${availabilityStatusErrs}">
        <td class="ErrorField">
     </c:when>
     <c:otherwise>
        <td class="BlockContent">
     </c:otherwise>
  </c:choose>
  
    <html:radio property="condition[${i}].trigger" value="onAvailability"/>
    <fmt:message key="alert.config.props.CB.Content.Availability"/>&nbsp;
    <html:select property="condition[${i}].availability">
       <html:option value="" key="alert.dropdown.SelectOption"/>
       <html:optionsCollection property="availabilityOptions"/>
    </html:select>
    
    <c:if test="${availabilityStatusErrs}">
      <br>
      <span class="ErrorFieldContent">- <html:errors property="condition[${i}].availabilityStatus"/></span>
    </c:if>
  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  <logic:messagesPresent property="condition[${i}].controlAction">
  	<c:set var="controlActionErrs" value="true"/>
  </logic:messagesPresent>
  <logic:messagesPresent property="condition[${i}].controlActionStatus">
  	<c:set var="controlActionStatusErrs" value="true"/>
  </logic:messagesPresent>
  <c:choose>
  <c:when test="${controlActionErrs or controlActionStatusErrs}">
  <td class="ErrorField">
  </c:when>
  <c:otherwise>
  <td class="BlockContent">
  </c:otherwise>
  </c:choose>
    <html:radio property="condition[${i}].trigger" value="onOperation"/>
    <fmt:message key="alert.config.props.CB.Content.ControlAction"/>&nbsp;
    <html:select property="condition[${i}].controlAction">
       <html:option value="" key="alert.dropdown.SelectOption"/>
       <html:optionsCollection property="controlActions"/>
    </html:select>
    &nbsp;<fmt:message key="alert.config.props.CB.Content.Comparator.="/>&nbsp;
    <html:select property="condition[${i}].controlActionStatus">
       <html:option value="" key="alert.dropdown.SelectOption"/>
       <html:options property="controlActionStatuses"/>
    </html:select>
    <c:if test="${controlActionErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].controlAction"/></span>
    </c:if>
    <c:if test="${controlActionStatusErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].controlActionStatus"/></span>
    </c:if>
  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  <logic:messagesPresent property="condition[${i}].eventSeverity">
  	<c:set var="eventSeverityErrs" value="true"/>
  </logic:messagesPresent>
  <logic:messagesPresent property="condition[${i}].eventDetails">
  	<c:set var="eventDetailsErrs" value="true"/>
  </logic:messagesPresent>
  <c:choose>
  <c:when test="${eventSeverityErrs or eventDetailsErrs}">
  <td class="ErrorField">
  </c:when>
  <c:otherwise>
  <td class="BlockContent">
  </c:otherwise>
  </c:choose>
    <html:radio property="condition[${i}].trigger" value="onEvent"/>
    <fmt:message key="alert.config.props.CB.Content.EventSeverity"/>
    <html:select property="condition[${i}].eventSeverity">
       <html:option value="" key="alert.dropdown.SelectOption"/>
       <html:options property="eventSeverities"/>
    </html:select>
    <fmt:message key="alert.config.props.CB.Content.Match"/>
    <html:text property="condition[${i}].eventDetails" size="10" maxlength="30"/>
    </td>
    <c:if test="${eventSeverityErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].eventSeverity"/></span>
    </c:if>
    <c:if test="${eventDetailsErrs}">
    <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].eventDetails"/></span>
    </c:if>
  </td>
</tr>

<c:if test="${numConditions != 1}">
<tiles:insert definition=".events.config.conditions.condition.deletelink">
  <tiles:put name="formName"><c:out value="${formName}"/></tiles:put>
  <tiles:put name="i"><c:out value="${i}"/></tiles:put>
</tiles:insert>
</c:if>
</c:forEach>
