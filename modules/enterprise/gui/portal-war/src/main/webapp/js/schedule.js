/*----------- start DECLARATIONS -----------*/
var today = new Date();
var date = today.getDate(); // from 1 to 31
var dayOfWeek = today.getDay(); // from 0 to 6 (Sun - Sat)
var month = today.getMonth(); // from 0 to 11
var year = today.getFullYear();

var SEL_STARTYEAR = today.getFullYear();
var SEL_NUMYEARS = 5;

var START_DATE = date;
var START_MONTH = month;
var START_YEAR = year;

var endDate = START_DATE;
var endMonth = START_MONTH;
var endYear = START_YEAR;

/* //in case we want endDate to be different from START_DATE
if (endDate > getAllDaysInMonth(START_MONTH, START_YEAR)) {
  endDate = endDate - getAllDaysInMonth(START_MONTH);
  endMonth = START_MONTH + 1;
  if (endMonth > 11) {
    endMonth = 0;
    endYear = START_YEAR + 1;
  }
}
*/

var schedDate = new Date();
var schedEndDate = new Date();

var monthArr = new Array(
  "01 (Jan)",
  "02 (Feb)",
  "03 (Mar)",
  "04 (Apr)",
  "05 (May)",
  "06 (Jun)",
  "07 (Jul)",
  "08 (Aug)",
  "09 (Sep)",
  "10 (Oct)",
  "11 (Nov)",
  "12 (Dec)");

var weekArr = new Array("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

var yearArr = new Array();
for (i=-3 ; i< 0 ; i++) {
  yearArr[i+3] = i + SEL_STARTYEAR;
}
for (i=0; i<SEL_NUMYEARS; i++) {
  yearArr[i+3] = i + SEL_STARTYEAR;
}
/*----------- end DECLARATIONS -----------*/

/* start and end parameters are passed in the same fashion as writing a date, i.e. 8/11/74
 * end* parameters may be null or empty
 * recurInterval will be one of "recurNever" "recurDaily" "recurWeekly" "recurMonthly"
 * Month fields are 0 indexed - i.e. Aug == 7
 * Day fields are 1 indexed - i.e 1st == 1
 * Year is literal - "2004" is "2004"
 */
function init(jspStartMonth, jspStartDay, jspStartYear, jspEndMonth, jspEndDay, jspEndYear, recurInterval) {
  if (document.getElementById("recur")!=null) {
    hideDiv("recur");
    hideDiv("recurNever");
    hideDiv("recurDaily");
    hideDiv("recurWeekly");
    hideDiv("recurMonthly");
    hideDiv("recurrenceEnd");
  }
  
  if (recurInterval && (recurInterval != "recurNever")) {
    showDiv("recur");
    setRecurDropdown(recurInterval);
    showDiv(recurInterval);
    showDiv("recurrenceEnd");
  }

  setSelect("startDay", START_DATE-1);
  setSelect("startMonth", START_MONTH );
  setSelect("startYear", START_YEAR - SEL_STARTYEAR);

  setSelect("endDay", endDate-1);
  setSelect("endMonth", endMonth);
  setSelect("endYear", endYear - SEL_STARTYEAR);
  
  changeDropDown ("startMonth", "startDay", "startYear");
  changeDropDown ("endMonth", "endDay", "endYear");
  
  if (jspStartMonth) {
    setFullDate(schedDate, jspStartMonth, jspStartDay, jspStartYear);
    resetDropdowns('startMonth', 'startDay', 'startYear');
  }
  
  if (jspEndYear) {
    setFullDate(schedEndDate, jspEndMonth, jspEndDay, jspEndYear);
    resetDropdowns('endMonth', 'endDay', 'endYear');
  }
  
}

/*----------- start SHOW/HIDE DIV -----------*/
function hideAll() {
  hideDiv("recurNever");
  hideDiv("recurDaily");
  hideDiv("recurWeekly");
  hideDiv("recurMonthly");
}
/*----------- end SHOW/HIDE DIV -----------*/
function toggleRadio(eName, index) {
  var list = document.getElementsByName(eName)
  
  for (i=0; i<list.length; i++) {
    list[i].checked=false;
  }
  list[index].checked=true;
}

function turnOnRecurrence(state) {
  if (state == true) {
    showDiv("recur");
    showDiv("recurNever");
  }
  else {
    hideDiv("recur");
    hideAll();
    hideDiv("recurrenceEnd");
  }
}

function getRecurrence() {
  var sel = document.getElementById("recurInterval");
  
  var index = sel.selectedIndex;
  var recurName = sel.options[index].value;
  hideAll();
  showDiv(recurName);
  if (recurName!="recurNever")
    showDiv("recurrenceEnd");
  else
    hideDiv("recurrenceEnd");
}

/*----------- start "service" DATE FUNCTIONS -----------*/
function isThisLeapYear (Year) {
  if (((Year % 4)==0) && ((Year % 100)!=0) || ((Year % 400)==0)) { return (true); }
    else { return (false); }
} 

function getAllDaysInMonth(monthNum, yearNum)  {
  var days;
  if (monthNum==0 || monthNum==2 || monthNum==4 || monthNum==6 || monthNum==7 || monthNum==9 || monthNum==11)  { days=31; }
    else if (monthNum==3 || monthNum==5 || monthNum==8 || monthNum==10) { days=30; }
    else if (monthNum==1)  {
        if (isThisLeapYear(yearNum)) { days=29; }
        else { days=28; }
    }
    return (days);
} 

function setSelect(selId, index) {
  var sel = document.getElementById(selId);
  sel.selectedIndex = index;
}

function setRecurDropdown(recurInterval) {
  var sel = document.getElementById("recurInterval");
  var ind;
  
  switch (recurInterval) {
    case "recurNever":
      ind = 0;
      break;
    case "recurDaily":
      ind = 1;
      break;
    case "recurWeekly":
      ind = 2;
      break;
    case "recurMonthly":
      ind = 3;
      break;
    default:
      ind = 0;
  }
  
  sel.selectedIndex = ind;
}

function getSelectIndex(selId) {
  var sel = document.getElementById(selId);
  return sel.selectedIndex;
}

function getSelectValue(sel) {
  var indexSel = sel.selectedIndex;
  return sel[indexSel].value;
}

function changeMonthDropdown (monthDropDown, selectedMonthValue, startMonthIndex){
  var newMonthIndex = selectedMonthValue - startMonthIndex;

  monthDropDown.options.length = 0;
  monthDropDown.options.length = 12-startMonthIndex;
  
  for(i=startMonthIndex; i<12; i++) {
    if (isIE) {
      monthDropDown.options[i-startMonthIndex].text = monthArr[i];
      monthDropDown.options[i-startMonthIndex].value = i;
    }
    else
      monthDropDown.options[i-startMonthIndex] = new Option (monthArr[i], i);
  }

  if (newMonthIndex < 0)
    newMonthIndex = 0;
  
  monthDropDown.selectedIndex = newMonthIndex;
}

function changeDateDropdown (dateDropDown, selectedMonthValue, selectedDateValue, selectedYearValue, startDateIndex) {
  var daysInMonth = getAllDaysInMonth(selectedMonthValue, selectedYearValue);
  var newDateIndex = selectedDateValue - startDateIndex - 1;
  
  dateDropDown.options.length = 0;
  dateDropDown.options.length = daysInMonth - startDateIndex;
  
  for(i=startDateIndex; i<daysInMonth; i++) {
    if (isIE) {
      dateDropDown.options[i-startDateIndex].text = i+1;
      dateDropDown.options[i-startDateIndex].value = i+1;
    }
    else
      dateDropDown.options[i-startDateIndex] = new Option (i+1, i+1);
  }
  
  if (newDateIndex < 0)
    newDateIndex = 0;
  
  
  if (newDateIndex <= dateDropDown.length-1)
    dateDropDown.selectedIndex = newDateIndex;
  else
    dateDropDown.selectedIndex = dateDropDown.length-1;
    
}
/*----------- end "service" DATE FUNCTIONS -----------*/

function changeDropDown(monthId, dateId, yearId) {
  var monthDropDown = document.getElementById(monthId);
  var dateDropDown = document.getElementById(dateId);
  var yearDropDown = document.getElementById(yearId);
  
  var selectedMonthValue = getSelectValue(monthDropDown);
  var selectedDateValue = getSelectValue(dateDropDown);
  var selectedYearValue = getSelectValue(yearDropDown);

  var startMonthIndex = 0;
  var startDateIndex = 0;
  
  if (selectedYearValue == START_YEAR) {
    startMonthIndex = START_MONTH;
    if (selectedMonthValue < START_MONTH)
      selectedMonthValue = START_MONTH;
    
    if (selectedMonthValue == START_MONTH)
      startDateIndex = START_DATE - 1;
  }
  
  changeMonthDropdown (monthDropDown, selectedMonthValue, startMonthIndex);
  changeDateDropdown (dateDropDown, selectedMonthValue, selectedDateValue, selectedYearValue, startDateIndex);
  
  if (monthId == "startMonth") {
    var endMonthDropDown = document.getElementById("endMonth");
    var endDateDropDown = document.getElementById("endDay");
    var endYearDropDown = document.getElementById("endYear");
    
    changeMonthDropdown (endMonthDropDown, selectedMonthValue, startMonthIndex);
    changeDateDropdown (endDateDropDown, selectedMonthValue, selectedDateValue, selectedYearValue, startDateIndex);
    endYearDropDown.selectedIndex = selectedYearValue - yearArr[0];
  }
}

function toggleRecurrence(eName) {
  var startRadio = document.getElementsByName(eName)[1];
  if (startRadio.checked==true) {
    turnOnRecurrence(true);
    getRecurrence();
  }
}
/*----------- start "service" CALENDAR FUNCTIONS -----------*/
function setCalMonth(selId, nav, monthId, dateId, yearId) {
  var modifier = 0;
  if (nav == "left")
    modifier = -1;
  else if (nav == "right")
    modifier = 1;

  var sel = document.getElementById(selId);
  var newMonth = parseInt(getSelectValue(sel)) + modifier;
  
  calDate.setMonth(newMonth);
  
  var calHtml = writeCalBody(calDate.getMonth(), calDate.getFullYear(), monthId, dateId, yearId);
  var bodyHtml = document.getElementById("bodyHtml");
  bodyHtml.innerHTML = calHtml;
}

function setCalYear(selId, monthId, dateId, yearId) {
  var yearSel = document.getElementById(selId);
  var newYear = getSelectValue(yearSel);
  var newMonth = calDate.getMonth(); 
  if (!isMonitorSchedule) {
    if (newYear == START_YEAR && newMonth < START_MONTH)
      newMonth = START_MONTH;
  }

  calDate.setYear(newYear);
  calDate.setMonth(newMonth);
  
  var calHtml = writeCalBody(calDate.getMonth(), calDate.getFullYear(), monthId, dateId, yearId);
  var bodyHtml = document.getElementById("bodyHtml");
  bodyHtml.innerHTML = calHtml;
}

function setFullDate(d, month, date, year) {
  d.setFullYear(year);
  d.setDate(date);
  d.setMonth(month);
}

function getFullDate(d) {
  alert(monthArr[d.getMonth()] + "/" + d.getDate() + "/" + d.getFullYear());
}
/*----------- end "service" CALENDAR FUNCTIONS -----------*/

function cal(monthId, dateId, yearId) {
  var monthDropDown = document.getElementById(monthId);
  var dateSel = document.getElementById(dateId);
  var yearSel = document.getElementById(yearId);
  
  var originalMonth = monthDropDown.selectedIndex;
  var originalDate = dateSel[dateSel.selectedIndex].value;
  var originalYearIndex = yearSel.selectedIndex;
  var originalYear = yearSel[originalYearIndex].value;
  
  if (originalYear == START_YEAR)
    originalMonth = originalMonth + START_MONTH;
  
  writeCal(originalMonth, originalYear, monthId, dateId, yearId);
}

function resetDropdowns(monthId, dateId, yearId) {
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
  
  var startMonthIndex = 0;
  var startDateIndex = 0;
  
  var newMonthValue = d.getMonth(); 
  var newMonthIndex = newMonthValue;
  var newDateValue = d.getDate();
  var newYearValue = d.getFullYear();
  
  yearDropDown.selectedIndex = newYearValue - yearArr[0];

  if (newYearValue == START_YEAR) {
    newMonthIndex = newMonthValue - START_MONTH;
    
    startMonthIndex = START_MONTH;
    if (newMonthIndex < 0)
      newMonthIndex = START_MONTH;
    
    if (newMonthIndex == START_MONTH)
      startDateIndex = START_DATE - 1;
  }
  
  if (oldYearValue != START_YEAR && oldYearValue == newYearValue && oldMonthValue == newMonthValue) {
    monthDropDown.selectedIndex = newMonthValue - startMonthIndex;
    dateDropDown.selectedIndex = newDateValue - 1;
  }
  else {
    changeMonthDropdown (monthDropDown, newMonthValue, startMonthIndex);
    changeDateDropdown (dateDropDown, newMonthIndex, newDateValue, newYearValue, startDateIndex);
  }
  
  if (monthId == "startMonth")
    resetDropdowns("endMonth", "endDay", "endYear");
  if (!isSetByJsp) // called by /web/resource/server/control/Edit.jsp
    self.close();
}

function parentOpen(calDate, currDay, url) {
  var parentWin = window.opener;
  parentWin.document.location =
    url + '&year=' + calDate.getFullYear() + '&month=' + calDate.getMonth() +
          '&day=' + currDay; 
  self.close();
}

function writeCal(month, year, monthId, dateId, yearId) {
  var calPopup = window.open("","calPopup","width=230,height=195,resizable=yes,scrollbars=no,left=600,top=50");
  calPopup.document.open();
  var calHtml = getCalHTMLHead(month, year);
  calHtml += writeCalBody(month, year, monthId, dateId, yearId);
  calHtml += getCalFooter();
  calPopup.document.write(calHtml);
  calPopup.document.close();
}

function writeCalBody(month, year, monthId, dateId, yearId) {
  var bodyHtml = getCalHeader(month, year, monthId, dateId, yearId);
  bodyHtml += getCalBody(month, year, monthId, dateId, yearId);
  
  return bodyHtml;
}

/*----------- start CALENDAR COMPONENTS (header, body, footer) -----------*/
function getCalHTMLHead(month, year) {
  var calHtml = "";
  calHtml += "<html>\n" + 
    "<head>\n" + 
    "<title>Calendar</title>\n" + 
    "<script src=\"" + jsPath + "functions.js\" type=\"text/javascript\"></script>\n" +
    "<script src=\"" + jsPath + "schedule.js\" type=\"text/javascript\"></script>\n";
  
  if (isMonitorSchedule == true)
    calHtml += "<script src=\"" + jsPath + "monitorSchedule.js\" type=\"text/javascript\"></script>\n";
  
  calHtml += "<link rel=stylesheet href=\"" + cssPath + "win.css\" type=\"text/css\">\n" +
    "<script type=\"text/javascript\">\n" +
    "  var imagePath = \"" + imagePath + "\";\n" + 
    "  var jsPath = \"" + jsPath + "\";\n" + 
    "  var cssPath = \"" + cssPath + "\";\n" + 
    "  var calDate = new Date(" + year + ", " + month + ", " + START_DATE + ");\n" + 
    "  var isMonitorSchedule = " + isMonitorSchedule + ";\n" + 
    "</script>\n" + 
    "<style type=\"text/css\">\n" +
    "body {background: #DBE3F5 url() no-repeat; }\n" +
    "</style>\n" +
    "</head>\n" + 
    "<body class=\"CalBody\">\n" + 
    "<div id=\"bodyHtml\">\n" +     
    "<form>\n";

  return calHtml;
}

function getCalHeader(month, year, monthId, dateId, yearId) {
  var calHtml = "";
  var startIndex = 0;
  var leftNav = "<input type=\"image\" src=\"" + imagePath + "schedule_left.gif\" onClick=\"setCalMonth('calMonth', 'left', '" + monthId + "', '" + dateId + "', '" + yearId+ "')\">";
  
  if (!isMonitorSchedule) {
    if (year == START_YEAR) {
      if (month == START_MONTH)
        leftNav = "<img src=\"" + imagePath + "spacer.gif\" height=\"19\" width=\"20\" border=\"0\">";
      startIndex = START_MONTH;
    }
  }
  
  calHtml += "<table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n" + 
"      <tr> \n" + 
"        <td class=\"CalHeader\" width=\"50%\" align=\"right\">\n" + leftNav + "</td>\n" + 
"        <td class=\"CalHeader\" align=\"center\" nowrap>\n" + 
"    <select name=\"calMonth\" id=\"calMonth\" onChange=\"setCalMonth('calMonth', 'none', '" + monthId + "', '" + dateId + "', '" + yearId+ "');\">";
  
  for(i=startIndex; i<12; i++) {
    var strSelected = "";
    if (i==month)
      strSelected = " selected";
    calHtml += "<option value=\"" + i + "\"" + strSelected +">" + monthArr[i] + "</option>\n";
  }

  calHtml += "</select>&nbsp;/&nbsp;\n" + "<select name=\"calYear\" id=\"calYear\" onChange=\"setCalYear('calYear', '" + monthId + "', '" + dateId + "', '" + yearId+ "');\">\n";
  for(i=0; i<yearArr.length; i++) {
    var strSelected = "";
    if (yearArr[i]==year) 
      strSelected = " selected";
    calHtml += "<option value=\"" + yearArr[i] + "\"" + strSelected +">" + yearArr[i] + "</option>\n";
  }
  
  calHtml +=
"    </select></td>\n" + 
"        <td class=\"CalHeader\" width=\"50%\"><input type=\"image\" src=\"" + imagePath + "schedule_right.gif\" onClick=\"setCalMonth('calMonth', 'right', '" + monthId + "', '" + dateId + "', '" + yearId+ "')\"></td>\n" + 
"      </tr>\n" + 
"      <tr> \n" + 
"        <td class=\"BlockContent\">&nbsp;</td>\n" + 
"        <td class=\"BlockContent\"><table width=\"100%\" border=\"0\" cellspacing=\"0\" cellpadding=\"2\">\n" + 
"            <tr> \n" + 
"              <td class=\"CalDays\">Su</td>\n" + 
"              <td class=\"CalDays\">Mo</td>\n" + 
"              <td class=\"CalDays\">Tu</td>\n" + 
"              <td class=\"CalDays\">We</td>\n" + 
"              <td class=\"CalDays\">Th</td>\n" + 
"              <td class=\"CalDays\">Fr</td>\n" + 
"              <td class=\"CalDays\">Sa</td>\n" + 
"            </tr>";
  
  return calHtml;
}

function getCalBody (month, year, monthId, dateId, yearId) {
  var calHtml = "";
  var i = 0;
  var coloredDays = 35;
  
  var days = getAllDaysInMonth(month,year);
  var firstOfMonth = new Date (year, month, 1);
  var startingPos = firstOfMonth.getDay();
  days += startingPos;
  
  if (startingPos!=0)
    calHtml += "<tr>\n";
  for (i = 0; i < startingPos; i++) {
    calHtml += "<td class=\"BlockContent\">&nbsp;</td>\n";
  }

  for (i = startingPos; i < days; i++) {
    var dayStr = "";
    
    if (i%7==0 && i!==0)
      calHtml += "</tr>\n" + "<tr>\n";
    else if (i==0)
      calHtml += "<tr>\n";
    
    
    var currDay = i-startingPos+1;
    if (currDay < 10)
      dayStr += "0";
    dayStr += currDay;
    
    if (isMonitorSchedule == true) {
      if (dateId == null || dateId == 'undefined') {
        calHtml += "<td class=\"BlockContent\"><a href=\"javascript:parentOpen(calDate, " + currDay + ", '" + monthId + "');\">" + dayStr + "</a></td>\n";
      }
      else {
        calHtml += "<td class=\"BlockContent\"><a href=\"javascript:setFullDate(calDate, " + month + ", " + currDay + ", " + year + "); resetMonitorDropdowns('" + monthId + "', '" + dateId + "', '" + yearId+ "');\">" + dayStr + "</a></td>\n";
      }
    }
    else {
      if (year == START_YEAR && month == START_MONTH && currDay < START_DATE)
        calHtml += "<td class=\"BlockContent\"><span class=\"CalInactiveDay\">" + dayStr + "</span></td>\n";
      else
        calHtml += "<td class=\"BlockContent\"><a href=\"javascript:setFullDate(calDate, " + month + ", " + currDay + ", " + year + "); resetDropdowns('" + monthId + "', '" + dateId + "', '" + yearId+ "');\">" + dayStr + "</a></td>\n";
    }
    
  }
  
  if (days<=28)
    coloredDays = 28;
  else if (days>35)
    coloredDays = 42;
    
  for (i=days; i<coloredDays; i++)  {
    if ( i%7 == 0 )
      calHtml += "</tr>\n" + "<tr bgcolor=\"#cccccc\">\n";
    calHtml += "<td class=\"BlockContent\">&nbsp;</td>\n";
  }
  
  calHtml += "      </tr>\n" + 
  "    </table></td>\n" +
  "        <td class=\"BlockContent\">&nbsp;</td>\n" + 
  "      </tr>\n" + 
  "      <tr> \n" + 
  "        <td class=\"BlockBottomLine\" colspan=\"3\"><img src=\"" + imagePath + "spacer.gif\" height=\"1\" width=\"1\" border=\"0\"></td>\n" + 
  "      </tr>\n" + 
  "    </table>\n";

  return calHtml;
}

function getCalFooter() {
  return "</form>\n" +
"</div></body>\n" +
"</html>\n";
}
/*----------- end CALENDAR COMPONENTS (header, body, footer) -----------*/
