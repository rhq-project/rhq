  var eventsTime = 0;

  function initEventDetails() {
    ajaxEngine.registerRequest( 'getEventDetails', '/resource/common/monitor/visibility/EventDetails.do');
    ajaxEngine.registerAjaxElement('eventsSummary',document.getElementById('eventsSummary'));
  }


  function showEventsCallback() {
    var detail = $('eventsSummary');
    if (detail.innerHTML == "") {
      setTimeout("showEventsCallback()", 500);
    }
    else {
      var div = $('eventDetailTable');
      if (div.style.display == 'none')
        new Effect.Appear(div);
    }
  }

  function hideEventDetail() {
    new Effect.Fade($('eventsSummary'));
  }

 var statusArr =
    new Array ("ALL", "ERR", "WRN", "INF", "DBG", "ALR", "CTL");

  function filterEventsDetails(severity) {
    for (i = 0; i < statusArr.length; i++) {
        $(statusArr[i] + "EventsTab").className = "eventsTab";
    }
    
    $(severity + "EventsTab").className = "eventsTabOn";

    if (severity != statusArr[0])
      showEventsDetails(eventsTime, severity);
    else
      showEventsDetails(eventsTime);
  }