<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic-el" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

    <logic:messagesPresent property="whenEnabled">
      <c:set var="whenEnabledErrs" value="true"/>
      <tr>
        <td colspan="2" class="ErrorField">
          <span class="ErrorFieldContent">
            <html:errors property="whenEnabled"/>
          </span>
        </td>
      </tr>
    </logic:messagesPresent>

    <logic:messagesPresent property="consecutiveCountValue">
      <c:set var="consecutiveCountValueErrs" value="true"/>
    </logic:messagesPresent>
    
    <c:choose>
      <c:when test="${consecutiveCountValueErrs}">
        <c:set var="consecutiveCountClass" value="ErrorField"/>
      </c:when>
      <c:otherwise>
        <c:set var="consecutiveCountClass" value="BlockContent"/>
      </c:otherwise>
    </c:choose>
    
    <logic:messagesPresent property="partialCountValue">
      <c:set var="partialCountValueErrs" value="true"/>
    </logic:messagesPresent>
    
    <logic:messagesPresent property="partialCountPeriod">
      <c:set var="partialCountPeriodErrs" value="true"/>
    </logic:messagesPresent>
    
    <c:choose>
      <c:when test="${partialCountValueErrs or partialCountPeriodErrs}">
        <c:set var="partialCountClass" value="ErrorField"/>
      </c:when>
      <c:otherwise>
        <c:set var="partialCountClass" value="BlockContent"/>
      </c:otherwise>
    </c:choose>

    <logic:messagesPresent property="inverseCountValue">
      <c:set var="inverseCountValueErrs" value="true"/>
    </logic:messagesPresent>
    
    <c:choose>
      <c:when test="${inverseCountValueErrs}">
        <c:set var="inverseCountClass" value="ErrorField"/>
      </c:when>
      <c:otherwise>
        <c:set var="inverseCountClass" value="BlockContent"/>
      </c:otherwise>
    </c:choose>

    <logic:messagesPresent property="durationCountValue">
      <c:set var="durationCountValueErrs" value="true"/>
    </logic:messagesPresent>
    
    <logic:messagesPresent property="durationCountPeriod">
      <c:set var="durationCountPeriodErrs" value="true"/>
    </logic:messagesPresent>
    
    <c:choose>
      <c:when test="${durationCountValueErrs or durationCountPeriodErrs}">
        <c:set var="durationCountClass" value="ErrorField"/>
      </c:when>
      <c:otherwise>
        <c:set var="durationCountClass" value="BlockContent"/>
      </c:otherwise>
    </c:choose>
    
    <tr>
      <td class="BlockLabel">
        <fmt:message key="alert.config.props.CB.Recovery"/>
      </td>
      <td class="BlockContent">
        <fmt:message key="alert.config.props.CB.RecoveryFor"/>
          <html:select property="recoverId" onchange="checkRecover();">
            <html:option value="" key="alert.dropdown.SelectOption"/>
            <html:optionsCollection property="alertnames" label="key" value="value"/>
          </html:select>
       </td>
    </tr>
    
    <!-- Start of Dampening Rules Section -->
    <tr>
      <!-- LHS Dampening Rule Label -->
      <td class="BlockLabel">
        <html:img page="/images/icon_required.gif" width="9" height="9" border="0"/>
        <b>
          <fmt:message key="alert.config.props.CB.DampeningRule"/>
        </b>
      </td>
      
      <!-- RHS "NONE" dampening -->
      <td class="BlockContent">
        <html:radio property="whenEnabled" value="${dampenNone}" onchange="selectDampeningRule();" />
        <fmt:message key="alert.config.props.CB.Content.DampenNone"/>
      </td>
    </tr>

    <!-- "CONSECUTIVE_COUNT" dampening -->
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${consecutiveCountClass}'/>">
        <html:radio property="whenEnabled" value="${dampenConsecutiveCount}" onchange="selectDampeningRule();" />
        
        <fmt:message key="alert.config.props.CB.Content.DampenConsecutiveCount.1"/>&nbsp;
        <html:text property="consecutiveCountValue" size="2" onkeypress="selectDampeningRuleConsecutiveCount();"/>&nbsp;
        
        <fmt:message key="alert.config.props.CB.Content.DampenConsecutiveCount.2"/>&nbsp;
        
        <c:if test="${consecutiveCountValueErrs}">
           <br>-- 
           <span class="ErrorFieldContent">
              <html:errors property="consecutiveCountValue"/>
           </span>
        </c:if>
      </td>
    </tr>
    
    <!-- "PARTIAL_COUNT" dampening -->
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${partialCountClass}'/>">
        <html:radio property="whenEnabled" value="${dampenPartialCount}" onchange="selectDampeningRule();" />
        
        <fmt:message key="alert.config.props.CB.Content.DampenPartialCount.1"/>&nbsp;
        <html:text property="partialCountValue" size="2" onkeypress="selectDampeningRulePartialCount();"/>&nbsp;
        
        <fmt:message key="alert.config.props.CB.Content.DampenPartialCount.2"/>&nbsp;
        <html:text property="partialCountPeriod" size="2" maxlength="3" onkeypress="selectDampeningRulePartialCount();" />&nbsp;
        
        <fmt:message key="alert.config.props.CB.Content.DampenPartialCount.3"/>
        
        <c:if test="${partialCountValueErrs or partialCountPeriodErrs}">
           <br>-- 
           <span class="ErrorFieldContent">
              <html:errors property="partialCountValue"/>
              <html:errors property="partialCountPeriod"/>
           </span>
        </c:if>
      </td>
    </tr>

    <!-- "INVERSE_COUNT" dampening -->
    <!-- 
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${inverseCountClass}'/>">
        <html:radio property="whenEnabled" value="${dampenInverseCount}" onchange="selectDampeningRule();" />
        
        <fmt:message key="alert.config.props.CB.Content.DampenInverseCount.1"/>&nbsp;
        <html:text property="inverseCountValue" size="2" onkeypress="selectDampeningRuleInverseCount();"/>&nbsp;
        
        <fmt:message key="alert.config.props.CB.Content.DampenInverseCount.2"/>&nbsp;
        
        <c:if test="${inverseCountValueErrs}">
           <br>-- 
           <span class="ErrorFieldContent">
              <html:errors property="inverseCountValue"/>
           </span>
        </c:if>
      </td>
    </tr>
      -->
    
    <!-- "DURATION_COUNT" dampening -->
    <tr>
      <td class="BlockLabel">&nbsp;</td>
      <td class="<c:out value='${durationCountClass}'/>">
        <html:radio property="whenEnabled" value="${dampenDurationCount}" onchange="selectDampeningRule();" />
        
        <fmt:message key="alert.config.props.CB.Content.DampenDurationCount.1"/>&nbsp;
        <html:text property="durationCountValue" size="2" maxlength="3" onkeypress="selectDampeningRuleDurationCount();" />&nbsp;
        
        <fmt:message key="alert.config.props.CB.Content.DampenDurationCount.2"/>&nbsp;
        <html:text property="durationCountPeriod" size="2" maxlength="3" onkeypress="selectDampeningRuleDurationCount();" />&nbsp;
        
        <tiles:insert definition=".events.config.conditions.enablement.timeunits">
          <tiles:put name="property" value="durationCountPeriodUnits"/>
          <tiles:put name="enableFunc" value="selectDampeningRuleDurationCount"/>
        </tiles:insert>
        
        <c:if test="${durationCountValueErrs or durationCountPeriodErrs}">
          <br>-- 
          <span class="ErrorFieldContent">
            <html:errors property="durationCountValue"/>
            <html:errors property="durationCountPeriod"/>
          </span>
        </c:if>
      </td>
    </tr>

    <tr>
      <td colspan="2" class="BlockLabel">&nbsp;</td>
    </tr>

    <tr>
      <td class="BlockLabel" valign="top">
        <b>
          <fmt:message key="alert.config.props.CB.Content.ActionFilters"/>
        </b>
      </td>
      <td class="BlockContent">
        <html:checkbox property="disableForRecovery" onchange="checkRecover();"/>
        <fmt:message key="alert.config.props.CB.Content.UntilRecovered"/>
        <script language="JavaScript" type="text/javascript">
          checkRecover();
        </script>
        <br>
        <%-- JBNADM-2183: disable filters that are not implemented yet
        <html:checkbox property="filteringControlActions"/>
        <fmt:message key="alert.config.props.CB.Content.Disregard"/>
        <br>
        <html:checkbox property="filteringNotificationActions"/>
        <fmt:message key="alert.config.props.CB.Content.Filter"/>
        --%>
      </td>
    </tr>
