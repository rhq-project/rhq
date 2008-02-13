/*-- START monitorSchedule.js --*/

/* start and end parameters are passed in the same fashion as writing a date, i.e. 8/11/74
 * end* parameters may be null or empty
 * recurInterval will be one of "recurNever" "recurDaily" "recurWeekly" "recurMonthly"
 * Month fields are 0 indexed - i.e. Aug == 7
 * Day fields are 1 indexed - i.e 1st == 1
 * Year is literal - "2004" is "2004"
 */

function monitorInit(jspStartMonth, jspStartDay, jspStartYear, jspEndMonth, jspEndDay, jspEndYear) {
  setSelect("startDay", START_DATE-1);
  setSelect("startMonth", START_MONTH );
  setSelect("startYear", START_YEAR - SEL_STARTYEAR);

  setSelect("endDay", endDate-1);
  setSelect("endMonth", endMonth);
  setSelect("endYear", endYear - SEL_STARTYEAR);
  
  changeMonitorDropDown ("startMonth", "startDay", "startYear");
  changeMonitorDropDown ("endMonth", "endDay", "endYear");
  
  if (jspStartMonth) {
    setFullDate(schedDate, jspStartMonth, jspStartDay, jspStartYear);
    resetMonitorDropdowns('startMonth', 'startDay', 'startYear');
  }
  
  if (jspEndYear) {
    setFullDate(schedEndDate, jspEndMonth, jspEndDay, jspEndYear);
    resetMonitorDropdowns('endMonth', 'endDay', 'endYear');
  }
}

/*----------- end "service" DATE FUNCTIONS -----------*/

function changeMonitorDropDown(monthId, dateId, yearId) {
	var monthDropDown = document.getElementById(monthId);
	var dateDropDown = document.getElementById(dateId);
	var yearDropDown = document.getElementById(yearId);
	
	var selectedMonthValue = getSelectValue(monthDropDown);
	var selectedDateValue = getSelectValue(dateDropDown);
	var selectedYearValue = getSelectValue(yearDropDown);

	var startMonthIndex = 0;
	var startDateIndex = 0;
	
	changeDateDropdown (dateDropDown, selectedMonthValue, selectedDateValue, selectedYearValue, startDateIndex);
	
	if (monthId == "startMonth") {
		var endMonthDropDown = document.getElementById("endMonth");
		var endDateDropDown = document.getElementById("endDay");
		var endYearDropDown = document.getElementById("endYear");
		
		endMonthDropDown.selectedIndex = selectedMonthValue;
		changeDateDropdown (endDateDropDown, selectedMonthValue, selectedDateValue, selectedYearValue, startDateIndex);
		endYearDropDown.selectedIndex = selectedYearValue - yearArr[0];
	}
}

function calMonitor(monthId, dateId, yearId) {
	var monthDropDown = document.getElementById(monthId);
	var dateSel = document.getElementById(dateId);
	var yearSel = document.getElementById(yearId);
	
	var originalMonth = monthDropDown.selectedIndex;
	var originalDate = dateSel[dateSel.selectedIndex].value;
	var originalYear = yearSel[yearSel.selectedIndex].value;
	
	writeCal(originalMonth, originalYear, monthId, dateId, yearId);
}

function resetMonitorDropdowns(monthId, dateId, yearId) {
	var parentWin = window.opener;
  var d = new Date();
	// called by /web/resource/server/control/Edit.jsp
  if (parentWin == null) {
    parentWin = window;
    var isSetByJsp = true;
    if (monthId == "startMonth")
      d = schedDate;
    else
      d = schedEndDate;
  }
  else
    d = calDate;
  
  var monthDropDown = parentWin.document.getElementById(monthId);
	var dateDropDown = parentWin.document.getElementById(dateId);
	var yearDropDown = parentWin.document.getElementById(yearId);
  
  var oldMonthValue = monthDropDown[monthDropDown.selectedIndex].value;
  var oldYearValue = yearArr[yearDropDown.selectedIndex];
	
	var newMonthValue = d.getMonth(); 
	var newDateValue = d.getDate();
  var newYearValue = d.getFullYear();
  
	yearDropDown.selectedIndex = newYearValue - yearArr[0];

  monthDropDown.selectedIndex = newMonthValue;
	changeDateDropdown (dateDropDown, newMonthValue, newDateValue, newYearValue, 0);
	
	if (monthId == "startMonth")
		resetMonitorDropdowns("endMonth", "endDay", "endYear");
	if (!isSetByJsp) // called by /web/resource/server/control/Edit.jsp
    self.close();
}

/*-- END monitorSchedule.js --*/