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
  if (masterSelValue == '') {
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

function selectDampeningRule()
{
  if ( document.forms[0].whenEnabled[0].checked == true )
  {
	selectDampeningRuleNone();
  } 
  else if ( document.forms[0].whenEnabled[1].checked == true ) 
  {
	selectDampeningRuleConsecutiveCount();
  } 
  else if ( document.forms[0].whenEnabled[2].checked == true ) 
  {
	selectDampeningRulePartialCount();
  }
/*
  else if ( document.forms[0].whenEnabled[3].checked == true )
  {
	selectDampeningRuleInverseCount();
  }
*/
  else if ( document.forms[0].whenEnabled[3].checked == true )
  {
	selectDampeningRuleDurationCount();
  }
}

function selectDampeningRuleNone()
{
   document.forms[0].whenEnabled[0].click();
   
   document.forms[0].consecutiveCountValue.value = "";
    
   document.forms[0].partialCountValue.value = "";
   document.forms[0].partialCountPeriod.value = "";
    
   //document.forms[0].inverseCountValue.value = "";
   
   document.forms[0].durationCountValue.value = "";
   document.forms[0].durationCountPeriod.value = "";
}

function selectDampeningRuleConsecutiveCount()
{  
   document.forms[0].whenEnabled[1].click();
 
   document.forms[0].partialCountValue.value = "";
   document.forms[0].partialCountPeriod.value = "";
    
   //document.forms[0].inverseCountValue.value = "";
   
   document.forms[0].durationCountValue.value = "";
   document.forms[0].durationCountPeriod.value = "";
}

function selectDampeningRulePartialCount()
{
   document.forms[0].whenEnabled[2].click();

   document.forms[0].consecutiveCountValue.value = "";
    
   //document.forms[0].inverseCountValue.value = "";
   
   document.forms[0].durationCountValue.value = "";
   document.forms[0].durationCountPeriod.value = "";
}

/*
function selectDampeningRuleInverseCount()
{
   document.forms[0].whenEnabled[3].click();

   document.forms[0].consecutiveCountValue.value = "";
    
   document.forms[0].partialCountValue.value = "";
   document.forms[0].partialCountPeriod.value = "";
   
   document.forms[0].durationCountValue.value = "";
   document.forms[0].durationCountPeriod.value = "";
}
*/

function selectDampeningRuleDurationCount()
{
   document.forms[0].whenEnabled[3].click();

   document.forms[0].consecutiveCountValue.value = "";
    
   document.forms[0].partialCountValue.value = "";
   document.forms[0].partialCountPeriod.value = "";
    
   //document.forms[0].inverseCountValue.value = "";
}

function checkRecover() {
  if (document.forms[0].disableForRecovery.checked == true) {
    document.forms[0].recoverId.disabled = true;
  }
  else {
    document.forms[0].recoverId.disabled = false;
  }

  if (document.forms[0].recoverId.value == '') {
    document.forms[0].disableForRecovery.disabled = false;
  }
  else {
    document.forms[0].disableForRecovery.disabled = true;
  }
}

function syslogFormEnabledToggle() {
  if (document.forms[0].shouldBeRemoved.checked) {
    document.forms[0].metaProject.disabled = true;
    document.forms[0].project.disabled = true;
    document.forms[0].version.disabled = true;
  } else {
    document.forms[0].metaProject.disabled = false;
    document.forms[0].project.disabled = false;
    document.forms[0].version.disabled = false;
  }
}
/*-- END alertConfigFunctions.js --*/
