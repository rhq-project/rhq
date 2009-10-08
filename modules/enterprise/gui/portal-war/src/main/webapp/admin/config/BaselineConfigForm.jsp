<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!--  BASELINE CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaselineConfigTab"/>
</tiles:insert>
<!--  /  -->

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="DAYS_LABEL" var="CONST_DAYS" />

<!--  Baseline configuration -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <!-- Baseline frequency -->
  <tr>
    <td width="45%" class="BlockLabel"><fmt:message key="admin.settings.BaselineFrequencyLabel"/></td>
    <td width="55%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="baselineFrequencyVal">
          <td class="ErrorField">
            <html:text size="2" property="baselineFrequencyVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="baselineFrequency">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineFrequencyVal">
          <td class="BlockContent">
            <html:text size="2" property="baselineFrequencyVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="baselineFrequency">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="baselineFrequencyVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="baselineFrequencyVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineFrequencyVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td width="20%" class="BlockLabel"/>
    <td width="30%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <!-- Baseline dataset admin.settings.BaselineDataSet-->
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.BaselineDataSet"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="baselineDataSetVal">
          <td class="ErrorField">
            <html:text size="2" property="baselineDataSetVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="baselineDataSet">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineDataSetVal">
          <td class="BlockContent">
            <html:text size="2" property="baselineDataSetVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="baselineDataSet">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="baselineDataSetVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="baselineDataSetVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="baselineDataSetVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"/>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
      </table>
    </td>
  </tr>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>