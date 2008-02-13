<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<script src="<html:rewrite page="/js/"/>functions.js" type="text/javascript"></script>
<link rel=stylesheet href="<html:rewrite page="/css/"/>win.css" type="text/css">

<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="MINUTES_LABEL" var="CONST_MINUTES" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="HOURS_LABEL" var="CONST_HOURS" />
<hq:constant
    classname="org.rhq.enterprise.gui.legacy.Constants"
    symbol="DAYS_LABEL" var="CONST_DAYS" />

  <logic:messagesPresent>
  <table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td class="ErrorField"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    <td colspan="3" align="left" class="ErrorField"><html:errors/></td>
  </tr>
  </table>
  </logic:messagesPresent>

<!--  BASE SERVER CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaseConfigTab"/>
</tiles:insert>
<!--  /  -->

<!--  BASE SERVER CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.BaseURL"/></td>
    <td width="30%" class="BlockContent"><html:text size="31" property="baseUrl" /></td>
    <td width="20%" class="BlockContent" colspan="2"></td>
  </tr>
  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG TITLE -->
  <tr>
    <td colspan="4" class="BlockHeader">
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.DataMangerConfigTab"/>
</tiles:insert>
    </td>
  </tr>
<!--  /  -->

<!--  DATA MANAGER CONFIG CONTENTS -->
  <tr>
    <td colspan="4" class="BlockContent"><fmt:message key="admin.settings.RestartNote"/></td>
  </tr>
  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.PurgeOlderThanLabel"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="deleteUnitsVal">
          <td class="ErrorField">
            <html:text size="2" property="deleteUnitsVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="deleteUnits">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="deleteUnitsVal">
          <td class="BlockContent">
            <html:text size="2" property="deleteUnitsVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="deleteUnits">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="deleteUnitsVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="deleteUnitsVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="deleteUnitsVal">
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
    <td class="BlockLabel"><fmt:message key="admin.settings.DataMaintInterval"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="maintIntervalVal">
          <td class="ErrorField">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="maintInterval">
              <html:option value="${CONST_HOURS}"><fmt:message key="admin.settings.Hours"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="maintIntervalVal">
          <td class="BlockContent">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="maintInterval">
              <html:option value="${CONST_HOURS}"><fmt:message key="admin.settings.Hours"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="maintIntervalVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="maintIntervalVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="maintIntervalVal">
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
    <td class="BlockLabel"><fmt:message key="admin.settings.RtDataPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="rtPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="maintIntervalVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="rtPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="rtPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="rtPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="rtPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="rtPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="rtPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="rtPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
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
    <td class="BlockLabel"><fmt:message key="admin.settings.AlertPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="alertPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="alertPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="alertPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="alertPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="alertPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent">- <html:errors property="alertPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="alertPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
    <td class="BlockLabel"></td>
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
    <td class="BlockLabel"><fmt:message key="admin.settings.Reindex"/></td>
    <td class="BlockLabel">
      <table cellpadding="0" cellspacing="4" border="0">
        <tr>
          <td align="left"><html:radio property="reindex" value="true"/><fmt:message key="yesno.true"/></td>
          <td align="left"><html:radio property="reindex" value="false"/><fmt:message key="yesno.false"/></td>
        </tr>
      </table>
    </td>
    <td class="BlockLabel"></td>
    <td class="BlockContent"></td>
  </tr>

  <tr>
    <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>

<!--  BASELINE CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.BaselineConfigTab"/>
</tiles:insert>
<!--  /  -->

<!--  Baseline configuration -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <!-- Baseline frequency -->
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.BaselineFrequencyLabel"/></td>
    <td width="30%" class="BlockContent">
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
            <span class="ErrorFieldContent">- <html:errors property="baselineFrequencyVal"/></span>
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
  <!-- Baseline dataset -->
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="admin.settings.BaselineDataSet"/></td>
    <td width="30%" class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="baselineDataSet">
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
            <span class="ErrorFieldContent">- <html:errors property="baselineDataSetVal"/></span>
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
</table>
<!--  /  -->
