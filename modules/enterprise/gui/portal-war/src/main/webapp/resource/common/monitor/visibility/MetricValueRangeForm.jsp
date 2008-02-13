<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html-el" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!-- Content Block Title -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.common.monitor.visibility.MetricValueRangeTab"/>
</tiles:insert>

<table width="100%" border="0" cellspacing="0" cellpadding="0">
  <tr>
    <td width="20%" class="BlockLabel"><fmt:message key="resource.common.monitor.visibility.DefineRangeLabel"/></td>
    <td width="80%" class="BlockContent"><html:radio property="a" value="1"/>
      <fmt:message key="monitoring.baseline.BlockContent.Last"/>&nbsp;&nbsp;
      <html:text property="rn" size="2" maxlength="3"/> 
      <html:select property="ru">
        <html:option value="1" key="resource.common.monitor.visibility.metricsToolbar.CollectionPoints"/>
        <html:option value="2" key="resource.common.monitor.visibility.metricsToolbar.Minutes"/>
        <html:option value="3" key="resource.common.monitor.visibility.metricsToolbar.Hours"/>
        <html:option value="4" key="resource.common.monitor.visibility.metricsToolbar.Days"/>
      </html:select>
    </td>
  </tr>

  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <td width="80%" class="BlockContent"><html:radio property="a" value="2"/>
      <fmt:message key="monitoring.baseline.BlockContent.WithinRange"/><br>

      <table width="100%" border="0" cellspacing="0" cellpadding="2">
        <tr> 
          <td><html:img page="/images/spacer.gif" width="20" height="20" border="0"/></td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.From"/></td>
          <td width="100%">
            <html:select property="startMonth"> 
              <html:option value="1">01 (Jan)</html:option>
              <html:option value="2">02 (Feb)</html:option>
              <html:option value="3">03 (Mar)</html:option>
              <html:option value="4">04 (Apr)</html:option>
              <html:option value="5">05 (May)</html:option>
              <html:option value="6">06 (Jun)</html:option>
              <html:option value="7">07 (Jul)</html:option>
              <html:option value="8">08 (Aug)</html:option>
              <html:option value="9">09 (Sep)</html:option>
              <html:option value="10">10 (Oct)</html:option>
              <html:option value="11">11 (Nov)</html:option>
              <html:option value="12">12 (Dec)</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="startDay"> 
              <html:option value="1">01</html:option>
              <html:option value="2">02</html:option>
              <html:option value="3">03</html:option>
              <html:option value="4">04</html:option>
              <html:option value="5">05</html:option>
              <html:option value="6">06</html:option>
              <html:option value="7">07</html:option>
              <html:option value="8">08</html:option>
              <html:option value="9">09</html:option>
              <html:option value="10">10</html:option>
              <html:option value="11">11</html:option>
              <html:option value="12">12</html:option>
              <html:option value="13">13</html:option>
              <html:option value="14">14</html:option>
              <html:option value="15">15</html:option>
              <html:option value="16">16</html:option>
              <html:option value="17">17</html:option>
              <html:option value="18">18</html:option>
              <html:option value="19">19</html:option>
              <html:option value="20">20</html:option>
              <html:option value="21">21</html:option>
              <html:option value="22">22</html:option>
              <html:option value="23">23</html:option>
              <html:option value="24">24</html:option>
              <html:option value="25">25</html:option>
              <html:option value="26">26</html:option>
              <html:option value="27">27</html:option>
              <html:option value="28">28</html:option>
              <html:option value="29">29</html:option>
              <html:option value="30">30</html:option>
              <html:option value="31">31</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="startYear"> 
              <html:option value="1">02</html:option>
              <html:option value="2">03</html:option>
              <html:option value="3">04</html:option>
              <html:option value="4">05</html:option>
              <html:option value="5">06</html:option>
            </html:select>&nbsp;<html:link href=""><html:img page="/images/schedule_iconCal.gif" width="19" height="17" alt="" border="0"/></html:link>
            &nbsp;@&nbsp;
            <html:text property="startHour" size="2" maxlength="2"/>&nbsp;:&nbsp;<html:text property="startMinute" size="2" maxlength="2"/>&nbsp;
            <html:select property="startAmPm"> 
              <html:option value="1">AM</html:option>
              <html:option value="2">PM</html:option>
            </html:select>&nbsp;
            <html:select property="startTimezone"> 
              <html:option value="1">PST</html:option>
              <html:option value="2">GMT</html:option>
            </html:select>&nbsp;</td>
        </tr>
        <tr> 
          <td>&nbsp;</td>
          <td>
            <fmt:message key="monitoring.baseline.BlockContent.To"/>&nbsp;</td>
          <td width="100%">
            <html:select property="endMonth"> 
              <html:option value="1">01 (Jan)</html:option>
              <html:option value="2">02 (Feb)</html:option>
              <html:option value="3">03 (Mar)</html:option>
              <html:option value="4">04 (Apr)</html:option>
              <html:option value="5">05 (May)</html:option>
              <html:option value="6">06 (Jun)</html:option>
              <html:option value="7">07 (Jul)</html:option>
              <html:option value="8">08 (Aug)</html:option>
              <html:option value="9">09 (Sep)</html:option>
              <html:option value="10">10 (Oct)</html:option>
              <html:option value="11">11 (Nov)</html:option>
              <html:option value="12">12 (Dec)</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="endDay"> 
              <html:option value="1">01</html:option>
              <html:option value="2">02</html:option>
              <html:option value="3">03</html:option>
              <html:option value="4">04</html:option>
              <html:option value="5">05</html:option>
              <html:option value="6">06</html:option>
              <html:option value="7">07</html:option>
              <html:option value="8">08</html:option>
              <html:option value="9">09</html:option>
              <html:option value="10">10</html:option>
              <html:option value="11">11</html:option>
              <html:option value="12">12</html:option>
              <html:option value="13">13</html:option>
              <html:option value="14">14</html:option>
              <html:option value="15">15</html:option>
              <html:option value="16">16</html:option>
              <html:option value="17">17</html:option>
              <html:option value="18">18</html:option>
              <html:option value="19">19</html:option>
              <html:option value="20">20</html:option>
              <html:option value="21">21</html:option>
              <html:option value="22">22</html:option>
              <html:option value="23">23</html:option>
              <html:option value="24">24</html:option>
              <html:option value="25">25</html:option>
              <html:option value="26">26</html:option>
              <html:option value="27">27</html:option>
              <html:option value="28">28</html:option>
              <html:option value="29">29</html:option>
              <html:option value="30">30</html:option>
              <html:option value="31">31</html:option>
            </html:select>&nbsp;/&nbsp;
            <html:select property="endYear">
              <html:option value="2">03</html:option>
              <html:option value="3">04</html:option>
              <html:option value="4">05</html:option>
              <html:option value="5">06</html:option>
            </html:select>&nbsp;<html:link href=""><html:img page="/images/schedule_iconCal.gif" width="19" height="17" alt="" border="0"/></html:link>
            &nbsp;@&nbsp;
            <html:text property="endHour" size="2" maxlength="2"/>&nbsp;:&nbsp;<html:text property="endMinute" size="2" maxlength="2"/>&nbsp;
            <html:select property="endAmPm">
              <html:option value="1">AM</html:option>
              <html:option value="2">PM</html:option>
            </html:select>&nbsp;
            <html:select property="endTimezone"> 
              <html:option value="1">PST</html:option>
              <html:option value="2">GMT</html:option>
            </html:select>&nbsp;</td>
        </tr>
      </table>

    </td>
  </tr>
  <tr>
    <td class="BlockLabel">&nbsp;</td>
    <td class="BlockContent"><span class="CaptionText"><fmt:message key="resource.common.monitor.visibility.TheseSettings"/></span></td>
  <tr>
    <td colspan="2" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
</table>
