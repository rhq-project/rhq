/*-- START alertConfigFunctions.js --*/
function selectMetric(selName, hidName) {
  var sel = document.getElementsByName(selName)[0];
  var selValue = sel[sel.selectedIndex].value;
  var selText = sel[sel.selectedIndex].text;

  document.getElementsByName(hidName)[0].value = selText;
}

function changeDropDown (masterSelName, selName, selDD, baselineOption) {
  var masterSel = document.getElementsByName(masterSelName)[0];
  var sel = document.getElementsByName(selName)[0];

  var masterSelValue = masterSel[masterSel.selectedIndex].value;
/*
  alert("masterSel: " + masterSel +
        "\nselName: " + selName +
        "\nsel: " + sel +
        "\nbaselineOption: " + baselineOption +
        "\nmasterSelValue: " + masterSelValue);
*/
  if (masterSelValue == '' || masterSelValue == "-1") {
    sel.disabled = true;
  } else {
    sel.options.length = 0;
    sel.options.length = baselines[masterSelValue].length;

    if (isIE) {
      sel.options[0].text = selDD;
      sel.options[0].value = '';
    } else {
      sel.options[0] = new Option(selDD, '');
    }
    sel.options[0].selected = true;
    for (i=0; i<baselines[masterSelValue].length-1; i++) {
      if (isIE) {
        sel.options[i+1].text = baselines[masterSelValue][i].label;
        sel.options[i+1].value = baselines[masterSelValue][i].value;
      } else {
        sel.options[i+1] = new Option(baselines[masterSelValue][i].label, baselines[masterSelValue][i].value);
      }
      if (baselineOption != null) {
        if (sel.options[i+1].value == baselineOption) {
          sel.options[0].selected = false;
          sel.options[i+1].selected = true;
        }
      }
    }
    sel.disabled = false;
  }
}

function selectDampeningRule(alertForm)
{
  if ( alertForm.whenEnabled[0].checked == true )
  {
	selectDampeningRuleNone(alertForm);
  } 
  else if ( alertForm.whenEnabled[1].checked == true ) 
  {
	selectDampeningRuleConsecutiveCount(alertForm);
  } 
  else if ( alertForm.whenEnabled[2].checked == true ) 
  {
	selectDampeningRulePartialCount(alertForm);
  }
/*
  else if ( alertForm.whenEnabled[3].checked == true )
  {
	selectDampeningRuleInverseCount(alertForm);
  }
*/
  else if ( alertForm.whenEnabled[3].checked == true )
  {
	selectDampeningRuleDurationCount(alertForm);
  }
}

function selectDampeningRuleNone(alertForm)
{
	alertForm.whenEnabled[0].click();
   
	alertForm.consecutiveCountValue.value = "";
    
	alertForm.partialCountValue.value = "";
	alertForm.partialCountPeriod.value = "";
    
   //alertForm.inverseCountValue.value = "";
   
	alertForm.durationCountValue.value = "";
	alertForm.durationCountPeriod.value = "";
}

function selectDampeningRuleConsecutiveCount(alertForm)
{  
	alertForm.whenEnabled[1].click();
 
	alertForm.partialCountValue.value = "";
	alertForm.partialCountPeriod.value = "";
    
   //alertForm.inverseCountValue.value = "";
   
	alertForm.durationCountValue.value = "";
	alertForm.durationCountPeriod.value = "";
}

function selectDampeningRulePartialCount(alertForm)
{
	alertForm.whenEnabled[2].click();

	alertForm.consecutiveCountValue.value = "";
    
   //alertForm.inverseCountValue.value = "";
   
	alertForm.durationCountValue.value = "";
	alertForm.durationCountPeriod.value = "";
}

/*
function selectDampeningRuleInverseCount(alertForm)
{
	alertForm.whenEnabled[3].click();

	alertForm.consecutiveCountValue.value = "";
    
	alertForm.partialCountValue.value = "";
	alertForm.partialCountPeriod.value = "";
   
	alertForm.durationCountValue.value = "";
	alertForm.durationCountPeriod.value = "";
}
*/

function selectDampeningRuleDurationCount(alertForm)
{
	alertForm.whenEnabled[3].click();

	alertForm.consecutiveCountValue.value = "";
    
	alertForm.partialCountValue.value = "";
	alertForm.partialCountPeriod.value = "";
    
   //alertForm.inverseCountValue.value = "";
}

function checkRecover(alertForm) {
  if (alertForm.disableForRecovery.checked == true) {
	  alertForm.recoverId.disabled = true;
  }
  else {
	  alertForm.recoverId.disabled = false;
  }

  if (alertForm.recoverId.value == '') {
	  alertForm.disableForRecovery.disabled = false;
  }
  else {
	  alertForm.disableForRecovery.disabled = true;
  }
}

function syslogFormEnabledToggle(alertForm) {
  if (alertForm.shouldBeRemoved.checked) {
	  alertForm.metaProject.disabled = true;
	  alertForm.project.disabled = true;
	  alertForm.version.disabled = true;
  } else {
	  alertForm.metaProject.disabled = false;
	  alertForm.project.disabled = false;
	  alertForm.version.disabled = false;
  }
}
/*-- END alertConfigFunctions.js --*/
