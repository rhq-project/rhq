<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/tld/hq.tld" prefix="hq" %>

<!--  DATA MANAGER CONFIG TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="admin.settings.DataMangerConfigTab"/>
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

<!--  DATA MANAGER CONFIG CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
  <tr>
    <td width="45%" class="BlockLabel"><fmt:message key="admin.settings.DataMaintInterval"/></td>
    <td width="55%" class="BlockContent">
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
            <span class="ErrorFieldContent"> <html:errors property="maintIntervalVal"/></span>
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
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.RtDataPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="rtPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="rtPurgeVal" />
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
            <span class="ErrorFieldContent"> <html:errors property="rtPurgeVal"/></span>
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
            <span class="ErrorFieldContent"> <html:errors property="alertPurgeVal"/></span>
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
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.EventPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="eventPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="eventPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="eventPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="eventPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="eventPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="eventPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="eventPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="eventPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="eventPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.TraitPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="traitPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="traitPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="traitPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="traitPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="traitPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="traitPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="traitPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="traitPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="traitPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
      </table>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel"><fmt:message key="admin.settings.AvailPurge"/></td>
    <td class="BlockContent">
      <table width="100%" cellpadding="0" cellspacing="0" border="0">
        <tr>
<logic:messagesPresent property="availPurgeVal">
          <td class="ErrorField">
            <html:text size="2" property="availPurgeVal" />
          </td>
          <td class="ErrorField" width="100%">
            <html:select property="availPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesPresent>
<logic:messagesNotPresent property="availPurgeVal">
          <td class="BlockContent">
            <html:text size="2" property="availPurgeVal" />
          </td>
          <td class="BlockContent" width="100%">
            <html:select property="availPurge">
              <html:option value="${CONST_DAYS}"><fmt:message key="admin.settings.Days"/></html:option>
            </html:select>
          </td>
</logic:messagesNotPresent>
        </tr>
<logic:messagesPresent property="availPurgeVal">
        <tr>
          <td class="ErrorField" colspan="2">
            <span class="ErrorFieldContent"> <html:errors property="availPurgeVal"/></span>
          </td>
        </tr>
</logic:messagesPresent>
<logic:messagesNotPresent property="availPurgeVal">
        <tr>
          <td class="BlockContent" colspan="2">
          </td>
        </tr>
</logic:messagesNotPresent>
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
  </tr>
  
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
