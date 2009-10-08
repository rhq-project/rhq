/*-- START control_ControlActionProperties.js --*/
function toggleConfigFileDiv(optionValue){
  var configFileDiv = document.getElementById("configFile");
  
  var sel = document.getElementsByName("controlAction")[0];
  var ind = sel.selectedIndex;
  var selValue = sel[ind].value;
  
  if (selValue == optionValue)
    configFileDiv.style.display = "block";
  else
    configFileDiv.style.display = "none";
}
/*-- END control_ControlActionProperties.js --*/
