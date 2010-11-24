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
  <logic:messagesPresent property="condition[${i}].trigger"><td width="20%" class="ErrorField" style="text-align: right"></logic:messagesPresent>
  <logic:messagesNotPresent property="condition[${i}].trigger"><td width="20%" class="BlockLabel"></logic:messagesNotPresent>
    <b><fmt:message key="alert.config.props.CB.IfCondition"/></b>
    <logic:messagesPresent property="condition[${i}].trigger"><br /><span class="ErrorFieldContent"><html:errors property="condition[${i}].trigger"/></span></logic:messagesPresent>
  </td>

<c:if test="${showMetrics}" > <!-- begin conditional metric display logic -->

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
          <html:text property="condition[${i}].percentage" size="6" maxlength="6"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.Percent"/>&nbsp;
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

</c:if> <!-- end conditional metric display logic -->
<c:if test="${showCalltimeMetrics}" > <!-- begin conditional call-time metric display logic -->

  <logic:messagesPresent property="condition[${i}].callTimeMetricId"><td width="80%" class="ErrorField"></logic:messagesPresent>
  <logic:messagesNotPresent property="condition[${i}].callTimeMetricId"><td width="80%" class="BlockContent"></logic:messagesNotPresent>
    <html:radio property="condition[${i}].trigger" value="onCallTime"/>
    <fmt:message key="alert.config.props.CB.Content.Calltime"/>
    <c:set var="seldd"><fmt:message key="alert.dropdown.SelectOption"/></c:set>
    <html:select property="condition[${i}].callTimeMetricId">
      <html:option value="-1" key="alert.dropdown.SelectOption"/>
      <html:optionsCollection property="calltimeMetrics" label="displayName" value="id"/>
    </html:select>
    <logic:messagesPresent property="condition[${i}].callTimeMetricId">
    <span class="ErrorFieldContent">- <html:errors property="condition[${i}].callTimeMetricId"/></span>
    </logic:messagesPresent>
    <html:hidden property="condition[${i}].callTimeMetricName"/>
  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>
  <td class="BlockContent">

    <table width="100%" border="0" cellspacing="0" cellpadding="2">
      <tr>
        <td nowrap="true"><div style="width: 60px; position: relative;"/><html:img page="/images/schedule_return.gif" width="17" height="21" border="0" align="right"/></td>
        <logic:messagesPresent property="condition[${i}].ctAbsoluteValue"><c:set var="ctAbsValueErrs" value="true"/></logic:messagesPresent>
        <logic:messagesPresent property="condition[${i}].calltimeAbsPattern"><c:set var="callDestAbsPatternErrs" value="true"/></logic:messagesPresent>
        <c:choose>
        <c:when test="${ctAbsValueErrs || callDestAbsPatternErrs}"><td width="100%" class="ErrorField"></c:when>
        <c:otherwise><td width="100%"></c:otherwise>
        </c:choose>
        <logic:messagesPresent property="condition[${i}].calltimeOption"><c:set var="calltimeOptionErrs" value="true"/></logic:messagesPresent>
          <html:radio property="condition[${i}].thresholdType" value="absolute"/>

          <html:select property="condition[${i}].calltimeAbsOption">
            <hq:optionMessageList property="calltimeOptions" baseKey="alert.config.props.CB.Content.CalltimeOptions" filter="true"/>
          </html:select>
          <fmt:message key="alert.config.props.CB.Content.Is"/>
          <html:select property="condition[${i}].calltimeComparator">
            <hq:optionMessageList property="comparators" baseKey="alert.config.props.CB.Content.Comparator" filter="true"/>
          </html:select>
          <html:text property="condition[${i}].ctAbsoluteValue" size="8" maxlength="15"/>&nbsp;<fmt:message key="alert.config.props.CB.Content.AbsoluteValue"/>
          &nbsp;<fmt:message key="alert.config.props.CB.Content.CallDestMatches"/>&nbsp;
          <html:text property="condition[${i}].calltimeAbsPattern" size="15" maxlength="50"/>
          <c:if test="${ctAbsValueErrs || callDestAbsPatternErrs}">
          <span class="ErrorFieldContent">
          <c:if test="${ctAbsValueErrs}"><br>- <html:errors property="condition[${i}].ctAbsoluteValue"/></c:if>
          <c:if test="${callDestAbsPatternErrs}"><br>- <html:errors property="condition[${i}].calltimeAbsPattern"/></c:if>
          </span>
          </c:if>
        </td>
      </tr>
      <tr>
        <td>&nbsp;</td>
          <logic:messagesPresent property="condition[${i}].ctPercentage"><c:set var="ctPercErrs" value="true"/></logic:messagesPresent>
          <logic:messagesPresent property="condition[${i}].calltimeChgPattern"><c:set var="callDestChgPatternErrs" value="true"/></logic:messagesPresent>
          <c:choose>
          <c:when test="${ctPercErrs || callDestChgPatternErrs}"><td width="100%" class="ErrorField"></c:when>
          <c:otherwise><td width="100%"></c:otherwise>
          </c:choose>
          <logic:messagesPresent property="condition[${i}].calltimeOption"><c:set var="calltimeOptionErrs" value="true"/></logic:messagesPresent>
          <html:radio property="condition[${i}].thresholdType" value="changed"/>
          <html:select property="condition[${i}].calltimeChgOption">
            <hq:optionMessageList property="calltimeOptions" baseKey="alert.config.props.CB.Content.CalltimeOptions" filter="true"/>
          </html:select>&nbsp;
          <html:select property="condition[${i}].calltimeChgOp">
            <hq:optionMessageList property="calltimeOperators" baseKey="alert.config.props.CB.Content.CalltimeOperators" filter="true"/>
          </html:select>
          &nbsp;<fmt:message key="alert.config.props.CB.Content.AtLeast"/>
          <html:text property="condition[${i}].ctPercentage" size="6" maxlength="6"/>&nbsp;%&nbsp;
          <logic:messagesPresent property="condition[${i}].ctPercentage">
          <br><span class="ErrorFieldContent">- <html:errors property="condition[${i}].percentage"/></span>
          </logic:messagesPresent>
          &nbsp;<fmt:message key="alert.config.props.CB.Content.CallDestMatches"/>&nbsp;
          <html:text property="condition[${i}].calltimeChgPattern" size="15" maxlength="50"/>
          <c:if test="${ctPercErrs || callDestChgPatternErrs}">
          <span class="ErrorFieldContent">
          <c:if test="${ctPercErrs}"><br>- <html:errors property="condition[${i}].ctPercentage"/></c:if>
          <c:if test="${callDestChgPatternErrs}"><br>- <html:errors property="condition[${i}].calltimeChgPattern"/></c:if>
          </span>
          </c:if>
        </td>
      </tr>
    </table>

  </td>
</tr>

<tr>
  <td class="BlockLabel">&nbsp;</td>

</c:if> <!-- end conditional metric display logic -->

<c:if test="${showTraits}" > <!-- begin conditional traits display logic -->

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


<tr>
  <td class="BlockLabel">&nbsp;</td>

</c:if> <!-- end conditional traits display logic -->
<c:if test="${showResourceConfiguration}" > <!-- begin conditional resource config display logic -->

  <logic:messagesPresent property="condition[${i}].resourceConfigurationStatus">
     <c:set var="resourceConfigurationStatusErrs" value="true"/>
  </logic:messagesPresent>

  <c:choose>
     <c:when test="${resourceConfigurationStatusErrs}">
        <td class="ErrorField">
     </c:when>
     <c:otherwise>
        <td class="BlockContent">
     </c:otherwise>
  </c:choose>

    <html:radio property="condition[${i}].trigger" value="onResourceConfiguration"/>
    <fmt:message key="alert.config.props.CB.Content.ResourceConfiguration"/>&nbsp;
    <fmt:message key="alert.config.props.CB.Content.Changes"/>
  </td>
</tr>


<tr>
  <td class="BlockLabel">&nbsp;</td>

</c:if> <!-- end conditional resource config display logic -->
<c:if test="${showAvailability}"> <!-- begin conditional availabilityOptions display logic -->

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

</c:if> <!-- end conditional availabilityOptions display logic -->
<c:if test="${showOperations}"> <!-- begin conditional controlActions display logic -->

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

</c:if> <!-- end conditional controlActions display logic -->
<c:if test="${showEvents}"> <!-- begin conditional events display logic -->

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

</c:if> <!-- end conditional events display logic -->


<c:if test="${numConditions != 1}">


<c:if test="${showEvents}"> <!-- begin conditional events display logic -->

<tr>
  <td class="BlockLabel">&nbsp;</td>

</c:if> <!-- end conditional events display logic -->

  <td class="BlockContent">

    <html:link href="javascript:document.${formName}.submit()"
               onclick="clickRemove('${formName}', '${i}');"
               titleKey="alert.config.props.CB.Delete">
      <fmt:message key="alert.config.props.CB.Delete"/>
    </html:link>
  </td>
</tr>


</c:if>
</c:forEach>

<c:if test="${numConditions == 1 && showEvents}"> <!-- begin conditional events display logic -->

<tr>
  <td class="BlockLabel">&nbsp;</td>

</c:if> <!-- end conditional events display logic -->

<c:if test="${numConditions != 1}">
<tr>
  <td class="BlockLabel">&nbsp;</td>
</c:if>
  <td class="BlockContent">
    <html:hidden property="addingCondition" value="false"/>
    <html:link href="javascript:document.${formName}.submit()"
               onclick="clickAdd('${formName}');"
               titleKey="alert.config.props.CB.Another">
      <fmt:message key="alert.config.props.CB.Another"/>
    </html:link>
  </td>
</tr>

