<%@ page language="java" %>
<%@ page errorPage="/common/Error.jsp" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-html" prefix="html" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-tiles" prefix="tiles" %>
<%@ taglib uri="http://jakarta.apache.org/struts/tags-logic" prefix="logic" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<tiles:importAttribute name="TheControlForm" ignore="true"/>
<tiles:importAttribute name="noRecurrence" ignore="true"/>
<c:if test="${empty TheControlForm}">
 <c:set var="TheControlForm" value="${requestScope[\"org.apache.struts.taglib.html.BEAN\"]}"/>
</c:if>
<script src="<html:rewrite page="/js/"/>schedule.js" type="text/javascript"></script>
<script language="JavaScript" type="text/javascript">
 var imagePath = "<html:rewrite page="/images/"/>";
 var jsPath = "<html:rewrite page="/js/"/>";
 var cssPath = "<html:rewrite page="/css/"/>";
 
 var isMonitorSchedule = false;
</script>

<!--  SCHEDULE TITLE -->
<tiles:insert definition=".header.tab">
  <tiles:put name="tabKey" value="resource.autodiscovery.ScheduleTab"/>
</tiles:insert>
<!--  /  -->

<!--  SCHEDULE CONTENTS -->
<table width="100%" cellpadding="0" cellspacing="0" border="0">
 <logic:messagesPresent property="startHour">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="startHour"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="startMin">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="startMin"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="numDays">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="numDays"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="recurrenceDay">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="recurrenceDay"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="numWeeks">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="numWeeks"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="numMonths">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="numMonths"/></span></td>
  </tr>
 </logic:messagesPresent>
 <logic:messagesPresent property="endDate">
  <tr valign="top">
   <td width="20%" class="BlockLabel">&nbsp;</td>
   <td width="80%" class="ErrorField"><span class="ErrorFieldContent">- <html:errors property="endDate"/></span></td>
  </tr>
 </logic:messagesPresent>
	<tr valign="top">
		<td width="20%" class="BlockLabel"><html:img page="/images/icon_required.gif" width="9" height="9" border="0"/><fmt:message key="resource.autodiscovery.schedule.Start"/></td>
		<td width="80%" class="BlockContent"><html:radio property="startTime" value="now" onclick="turnOnRecurrence(false)"/> <fmt:message key="resource.autodiscovery.schedule.Immediately"/></td>
	</tr>
<c:if test="${empty noRecurrence}">
	<tr>
		<td class="BlockLabel">&nbsp;</td>
		<td class="BlockContent">
			<table width="100%" cellpadding="0" cellspacing="0" border="0">
				<tr>
					<td><html:radio property="startTime" value="onDate" onclick="turnOnRecurrence(true)"/>&nbsp;</td>
					<td nowrap>
						<html:select property="startMonth" styleId="startMonth" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1); changeDropDown('startMonth', 'startDay', 'startYear');">
							<html:option value="0">01 (Jan)</html:option>
							<html:option value="1">02 (Feb)</html:option>
							<html:option value="2">03 (Mar)</html:option>
							<html:option value="3">04 (Apr)</html:option>
							<html:option value="4">05 (May)</html:option>
							<html:option value="5">06 (Jun)</html:option>
							<html:option value="6">07 (Jul)</html:option>
							<html:option value="7">08 (Aug)</html:option>
							<html:option value="8">09 (Sep)</html:option>
							<html:option value="9">10 (Oct)</html:option>
							<html:option value="10">11 (Nov)</html:option>
							<html:option value="11">12 (Dec)</html:option>
						</html:select>
						/
						<html:select property="startDay" styleId="startDay" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1); changeDropDown('startMonth', 'startDay', 'startYear');">
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
						</html:select>
						/
						<html:select property="startYear" styleId="startYear" onchange="turnOnRecurrence(true); toggleRadio('startTime', 1); changeDropDown('startMonth', 'startDay', 'startYear');">
							<script language="JavaScript" type="text/javascript">
								for (i=0; i<SEL_NUMYEARS; i++) {
									document.writeln("<option value=\"" + yearArr[i] + "\">" + yearArr[i] + "</option>");
								}
							</script>
						</html:select>
					</td>
					<td><html:link href="#" onclick="cal('startMonth', 'startDay', 'startYear'); return false;"><html:img page="/images/schedule_iconCal.gif" width="19" height="17" hspace="5" border="0"/></html:link></td>
					<td nowrap>&nbsp;&nbsp;<b>@</b>&nbsp;&nbsp;</td>
					<td width="100%">
						<html:text property="startHour" styleId="startHour" size="2" maxlength="2"/> : <html:text property="startMin" styleId="startMin" size="2" maxlength="2" />
						<html:select property="startAmPm">
							<html:option value="am">AM</html:option>
							<html:option value="pm">PM</html:option>
						</html:select>
					</td>
				</tr>
				<tr>
					<td>&nbsp;</td>
					<td colspan="4" class="BlockContent"><span class="CaptionText"><fmt:message key="resource.autodiscovery.schedule.recur.Specify"/></span></td>
				</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td class="BlockLabel" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
  </tr>
	<tr>
		<td class="BlockContent" colspan="2">
			<div id="recur">
			<table width="100%" border="0" cellspacing="0" cellpadding="0">
				<tr>
					<td class="BlockLabel" width="20%"><b><fmt:message key="resource.autodiscovery.schedule.Recur"/></b></td>
					<td class="BlockContent" width="80%">
						<html:select property="recurInterval" styleId="recurInterval" onchange="getRecurrence();">
							<html:option value="recurNever"><fmt:message key="resource.autodiscovery.schedule.recur.Never"/></html:option>
							<html:option value="recurDaily"><fmt:message key="resource.autodiscovery.schedule.recur.Daily"/></html:option>
							<html:option value="recurWeekly"><fmt:message key="resource.autodiscovery.schedule.recur.Weekly"/></html:option>
							<html:option value="recurMonthly"><fmt:message key="resource.autodiscovery.schedule.recur.Monthly"/></html:option>
						</html:select>
					</td>
				</tr>
			</table>
			</div>
		</td>
	</tr>
	<tr>
      <td class="BlockContent">&nbsp;</td>
			<td class="BlockContent">
		<div id="recurNever">&nbsp;</div>
		<div id="recurDaily">
		<table width="100%" border="0" cellspacing="0" cellpadding="0">
          <tr> 
            <td class="BlockContent"><html:img page="/images/schedule_return.gif" width="17" height="21" border="0"/></td>
            <td class="BlockContent"><html:radio property="recurrenceFrequencyDaily" value="everyDay"/></td>
			<td class="BlockContent"><fmt:message key="resource.autodiscovery.schedule.recur.Every"/></td>
			<td class="BlockContent"><html:text property="numDays" styleId="numDays" size="2" maxlength="2" onchange="toggleRadio('recurrenceFrequencyDaily', 0);"/></td>
			<td class="BlockContent" width="100%"><fmt:message key="resource.autodiscovery.schedule.daily.Days"/></td>
          </tr>
          <tr> 
            <td class="BlockContent">&nbsp;</td>
            <td class="BlockContent"><html:radio property="recurrenceFrequencyDaily" value="everyWeekday"/></td>
			<td class="BlockContent" colspan="3"><fmt:message key="resource.autodiscovery.schedule.daily.EveryWeekday"/></td>
          </tr>
        </table>
		</div>
		<div id="recurWeekly">
			<table width="100%" border="0" cellspacing="0" cellpadding="0">
	          <tr valign="top"> 
	            <td class="BlockContent"><html:img page="/images/schedule_return.gif" width="17" height="21" border="0"/></td>
	            <td class="BlockContent" nowrap><fmt:message key="resource.autodiscovery.schedule.recur.Every"/> 
	              <html:text property="numWeeks" styleId="numWeeks" size="2" maxlength="2"/>
	              <fmt:message key="resource.autodiscovery.schedule.weekly.WeeksOn"/></td>
	            <td class="BlockContent" width="100%">
		     <table border="0" cellspacing="0" cellpadding="2">
	               <tr> 
	                  <td><html:multibox property="recurrenceDay" value="1" titleKey="admin.role.alert.Sunday"/>
	                    <fmt:message key="admin.role.alert.Sunday"/></td>
	                  <td><html:multibox property="recurrenceDay" value="2" titleKey="admin.role.alert.Monday"/>
	                    <fmt:message key="admin.role.alert.Monday"/></td>
	                  <td><html:multibox property="recurrenceDay" value="3" titleKey="admin.role.alert.Tuesday"/>
	                    <fmt:message key="admin.role.alert.Tuesday"/></td>
	                  <td><html:multibox property="recurrenceDay" value="4" titleKey="admin.role.alert.Wednesday"/>
	                    <fmt:message key="admin.role.alert.Wednesday"/></td>
	                </tr>
	                <tr> 
	                  <td><html:multibox property="recurrenceDay" value="5" titleKey="admin.role.alert.Thursday"/>
	                    <fmt:message key="admin.role.alert.Thursday"/></td>
	                  <td><html:multibox property="recurrenceDay" value="6" titleKey="admin.role.alert.Friday"/>
	                    <fmt:message key="admin.role.alert.Friday"/></td>
	                  <td><html:multibox property="recurrenceDay" value="7" titleKey="admin.role.alert.Saturday"/><fmt:message key="admin.role.alert.Saturday"/></td>
	                  <td>&nbsp;</td>
	                </tr>
	              </table>
		    </td>
	          </tr>
	        </table>
		</div>
		<div id="recurMonthly">
			<table width="100%" border="0" cellspacing="0" cellpadding="2">
          <tr> 
            <td><html:img page="/images/schedule_return.gif" width="17" height="21" border="0"/></td>
            <td nowrap><fmt:message key="resource.autodiscovery.schedule.recur.Every"/> 
              <html:text property="numMonths" styleId="numMonths" size="2" maxlength="2" />
              <fmt:message key="resource.autodiscovery.schedule.monthly.MonthsOn"/></td>
            <td><html:radio property="recurrenceFrequencyMonthly" value="onDay" /> 
            </td>
            <td nowrap><fmt:message key="resource.autodiscovery.schedule.monthly.OnThe"/></td>
            <td><html:select property="recurrenceWeek" styleId="recurrenceWeek" onchange="toggleRadio('recurrenceFrequencyMonthly', 0);">
                <html:option value="1"><fmt:message key="resource.autodiscovery.schedule.monthly.first"/></html:option>
                <html:option value="2"><fmt:message key="resource.autodiscovery.schedule.monthly.second"/></html:option>
                <html:option value="3"><fmt:message key="resource.autodiscovery.schedule.monthly.third"/></html:option>
                <html:option value="4"><fmt:message key="resource.autodiscovery.schedule.monthly.fourth"/></html:option>
              </html:select> <html:select property="monthlyRecurrenceDay" styleId="recurrenceDay" onchange="toggleRadio('recurrenceFrequencyMonthly', 0);">
                <html:option value="1"><fmt:message key="admin.role.alert.Sunday"/></html:option>
		<html:option value="2"><fmt:message key="admin.role.alert.Monday"/></html:option>
                <html:option value="3"><fmt:message key="admin.role.alert.Tuesday"/></html:option>
                <html:option value="4"><fmt:message key="admin.role.alert.Wednesday"/></html:option>
                <html:option value="5"><fmt:message key="admin.role.alert.Thursday"/></html:option>
                <html:option value="6"><fmt:message key="admin.role.alert.Friday"/></html:option>
                <html:option value="7"><fmt:message key="admin.role.alert.Saturday"/></html:option>
              </html:select></td>
          </tr>
          <tr> 
            <td>&nbsp;</td>
            <td>&nbsp;</td>
            <td> <html:radio property="recurrenceFrequencyMonthly" value="onEach"/>
            </td>
            <td><fmt:message key="resource.autodiscovery.schedule.monthly.Each"/></td>
            <td width="100%">
							<html:select property="eachDay" onchange="toggleRadio('recurrenceFrequencyMonthly', 1);">
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
							</html:select> <fmt:message key="resource.autodiscovery.schedule.monthly.OfTheMonth"/>
						</td>
          </tr>
        </table>
	</div>
      </td>
    </tr>
	<tr>
      <td class="BlockLabel" colspan="2"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
	<tr>
		<td colspan="2" class="BlockContent">
			<div id="recurrenceEnd">
			<table width="100%" border="0" cellspacing="0" cellpadding="0">
				<tr>
			      <td width="20%" class="BlockLabel" valign="top"><b><fmt:message key="resource.autodiscovery.schedule.RecurrenceEnd"/></b></td>
                  <td width="80%" class="BlockContent"><html:radio property="endTime" value="none" /> <fmt:message key="resource.autodiscovery.schedule.NoEnd"/></td>
			    </tr>
			    <tr>
			      <td class="BlockLabel">&nbsp;</td>
				  <td class="BlockContent">
						<table width="100%" cellpadding="0" cellspacing="0" border="0">
							<tr>
								<td nowrap>
									<html:radio property="endTime" value="onDate"/>
									<html:select property="endMonth" styleId="endMonth" onchange="toggleRadio('endTime', 1); changeDropDown('endMonth', 'endDay', 'endYear');">
										<html:option value="0">01 (Jan)</html:option>
										<html:option value="1">02 (Feb)</html:option>
										<html:option value="2">03 (Mar)</html:option>
										<html:option value="3">04 (Apr)</html:option>
										<html:option value="4">05 (May)</html:option>
										<html:option value="5">06 (Jun)</html:option>
										<html:option value="6">07 (Jul)</html:option>
										<html:option value="7">08 (Aug)</html:option>
										<html:option value="8">09 (Sep)</html:option>
										<html:option value="9">10 (Oct)</html:option>
										<html:option value="10">11 (Nov)</html:option>
										<html:option value="11">12 (Dec)</html:option>
									</html:select>
									/
									<html:select property="endDay" styleId="endDay" onchange="toggleRadio('endTime', 1);">
										<html:option value="0">01</html:option>
										<html:option value="1">02</html:option>
										<html:option value="2">03</html:option>
										<html:option value="3">04</html:option>
										<html:option value="4">05</html:option>
										<html:option value="5">06</html:option>
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
									</html:select>
									/
									<html:select property="endYear" styleId="endYear" onchange="toggleRadio('endTime', 1); changeDropDown('endMonth', 'endDay', 'endYear');">
									<script language="JavaScript" type="text/javascript">
										for (i=0; i<SEL_NUMYEARS; i++) {
											document.writeln("<option value=\"" + yearArr[i] + "\">" + yearArr[i] + "</option>");
										}
									</script>
									</html:select>
								</td>
								<td><html:link href="#" onclick="cal('endMonth', 'endDay', 'endYear'); return false;"><html:img page="/images/schedule_iconCal.gif" width="19" height="17"  hspace="5" border="0"/></html:link></td>
								<td width="100%">&nbsp;</td>
							</tr>
							</table>
			      </td>
			    </tr>
				<tr>
					<td class="BlockLabel">&nbsp;</td>
					<td class="BlockContent">&nbsp;</td>		
			    </tr>
			</table>
      </div>
     </td>
    </tr>
</c:if>
    <tr>
     <td colspan="4" class="BlockBottomLine"><html:img page="/images/spacer.gif" width="1" height="1" border="0"/></td>
    </tr>
</table>
<!--  /  -->

<script language="JavaScript" type="text/javascript">
<c:choose>
 <c:when test="${param.mode eq 'edit'}">
 init(<c:out escapeXml="false" value="\"${TheControlForm.startMonth}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.startDay}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.startYear}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endMonth}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endDay}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endYear}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.recurInterval}\""/>);
 </c:when>
 <c:otherwise>
 <%-- an error occurred, for a 'new' mode. still init the select boxes. --%>
 <logic:messagesPresent> 
  init(<c:out escapeXml="false" value="\"${TheControlForm.startMonth}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.startDay}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.startYear}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endMonth}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endDay}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.endYear}\""/>, <c:out escapeXml="false" value="\"${TheControlForm.recurInterval}\""/>);
 </logic:messagesPresent>
 <logic:messagesNotPresent> 
  init();
 </logic:messagesNotPresent>
 </c:otherwise>
</c:choose>
  toggleRecurrence("startTime");
</script>

