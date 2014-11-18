function Tabzilla()
{
    if (typeof jQuery != 'undefined' && jQuery) {
        jQuery(document).ready(Tabzilla.init);
    } else {
        Tabzilla.run();
    }
}

Tabzilla.READY_POLL_INTERVAL = 40;
Tabzilla.readyInterval = null;
Tabzilla.jQueryCDNSrc =
    '//ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js';

Tabzilla.hasCSSTransitions = (function() {
    var div = document.createElement('div');
    div.innerHTML = '<div style="'
        + '-webkit-transition: color 1s linear;'
        + '-moz-transition: color 1s linear;'
        + '-ms-transition: color 1s linear;'
        + '-o-transition: color 1s linear;'
        + '"></div>';

    var hasTransitions = (
           (div.firstChild.style.webkitTransition !== undefined)
        || (div.firstChild.style.MozTransition !== undefined)
        || (div.firstChild.style.msTransition !== undefined)
        || (div.firstChild.style.OTransition !== undefined)
    );

    delete div;

    return hasTransitions;
})();
Tabzilla.run = function()
{
    var webkit = 0, isIE = false, ua = navigator.userAgent;
    var m = ua.match(/AppleWebKit\/([^\s]*)/);

    if (m && m[1]) {
        webkit = parseInt(m[1], 10);
    } else {
        m = ua.match(/Opera[\s\/]([^\s]*)/);
        if (!m || !m[1]) {
            m = ua.match(/MSIE\s([^;]*)/);
            if (m && m[1]) {
                isIE = true;
            }
        }
    }

     if (isIE) {
        if (self !== self.top) {
            document.onreadystatechange = function() {
                if (document.readyState == 'complete') {
                    document.onreadystatechange = null;
                    Tabzilla.ready();
                }
            };
        } else {
            var n = document.createElement('p');
            Tabzilla.readyInterval = setInterval(function() {
                try {
                    n.doScroll('left');
                    clearInterval(Tabzilla.readyInterval);
                    Tabzilla.readyInterval = null;
                    Tabzilla.ready();
                    n = null;
                } catch (ex) {
                }
            }, Tabzilla.READY_POLL_INTERVAL);
        }

    } else if (webkit && webkit < 525) {
        Tabzilla.readyInterval = setInterval(function() {
            var rs = document.readyState;
            if ('loaded' == rs || 'complete' == rs) {
                clearInterval(Tabzilla.readyInterval);
                Tabzilla.readyInterval = null;
                Tabzilla.ready();
            }
        }, Tabzilla.READY_POLL_INTERVAL);

    } else {
        Tabzilla.addEventListener(document, 'DOMContentLoaded', Tabzilla.ready);
    }
};

Tabzilla.ready = function()
{
    if (!Tabzilla.DOMReady) {
        Tabzilla.DOMReady = true;

        var onLoad = function() {
            Tabzilla.init();
            Tabzilla.removeEventListener(
                document,
                'DOMContentLoaded',
                Tabzilla.ready
            );
        };

        if (typeof jQuery == 'undefined') {
            var script = document.createElement('script');
            script.type = 'text/javascript';
            script.src = Tabzilla.jQueryCDNSrc;
            document.getElementsByTagName('body')[0].appendChild(script);

            if (script.readyState) {
                // IE
                script.onreadystatechange = function() {
                    if (   script.readyState == 'loaded'
                        || script.readyState == 'complete'
                    ) {
                        onLoad();
                    }
                };
            } else {
                // Others
                script.onload = onLoad;
            }
        } else {
            onLoad();
        }
    }
};

Tabzilla.init = function()
{
    if (!Tabzilla.hasCSSTransitions) {
        // add easing functions
        jQuery.extend(jQuery.easing, {
            'easeInOut':  function (x, t, b, c, d) {
                if (( t /= d / 2) < 1) {
                    return c / 2 * t * t + b;
                }
                return -c / 2 * ((--t) * (t - 2) - 1) + b;
            }
        });
    }

    Tabzilla.link  = document.getElementById('tab');
    Tabzilla.panel = Tabzilla.buildPanel();

    var body = document.getElementsByTagName('body')[0];
    body.insertBefore(Tabzilla.panel, body.firstChild);

    Tabzilla.addEventListener(Tabzilla.link, 'click', function(e) {
        Tabzilla.preventDefault(e);
        Tabzilla.toggle();
    });

    Tabzilla.$panel = jQuery(Tabzilla.panel);
    Tabzilla.$link  = jQuery(Tabzilla.link);

    Tabzilla.$panel.addClass('tabnav-closed');
    Tabzilla.$link.addClass('tabnav-closed');
    Tabzilla.$panel.removeClass('tabnav-opened');
    Tabzilla.$link.removeClass('tabnav-opened');

    Tabzilla.opened = false;

    initializeSearchBar();
};

Tabzilla.buildPanel = function()
{
    var panel = document.createElement('div');
    panel.id = 'tabnav-panel';
    panel.innerHTML = Tabzilla.content;
    return panel;
};

Tabzilla.addEventListener = function(el, ev, handler)
{
    if (typeof el.attachEvent != 'undefined') {
        el.attachEvent('on' + ev, handler);
    } else {
        el.addEventListener(ev, handler, false);
    }
};

Tabzilla.removeEventListener = function(el, ev, handler)
{
    if (typeof el.detachEvent != 'undefined') {
        el.detachEvent('on' + ev, handler);
    } else {
        el.removeEventListener(ev, handler, false);
    }
};

Tabzilla.toggle = function()
{
    if (Tabzilla.opened) {
        Tabzilla.close();
    } else {
        Tabzilla.open();
    }
};

Tabzilla.open = function()
{
    if (Tabzilla.opened) {
        return;
    }

    if (Tabzilla.hasCSSTransitions) {
        Tabzilla.$panel.addClass('tabnav-opened');
        Tabzilla.$link.addClass('tabnav-opened');
        Tabzilla.$panel.removeClass('tabnav-closed');
        Tabzilla.$link.removeClass('tabnav-closed');
    } else {
        jQuery(Tabzilla.panel).animate({ height: 225 }, 225, 'easeInOut');
    }

    Tabzilla.opened = true;
};

Tabzilla.close = function()
{
    if (!Tabzilla.opened) {
        return;
    }

    if (Tabzilla.hasCSSTransitions) {
        Tabzilla.$panel.removeClass('tabnav-opened');
        Tabzilla.$link.removeClass('tabnav-opened');
        Tabzilla.$panel.addClass('tabnav-closed');
        Tabzilla.$link.addClass('tabnav-closed');
    } else {
        jQuery(Tabzilla.panel).animate({ height: 0 }, 225, 'easeInOut');
    }

    Tabzilla.opened = false;
};

Tabzilla.preventDefault = function(ev)
{
    if (ev.preventDefault) {
        ev.preventDefault();
    } else {
        ev.returnValue = false;
    }
};

Tabzilla.content =
'<div class="tabnavclearfix" id="tabnav">'
+'<div class="tabcontent">'
+'  <p class="overview"> Like the project? It’s part of the community of Red Hat projects. Learn more about Red Hat and our open source communities:</p>'
+'  <div class="row-fluid">'
+'    <span class="span4 middlewarelogo">'
+'      <img src="img/RHJB_Middleware_Logotype.png" alt="Red Hat JBoss MIDDLEWARE" />'
+'    </span>'
+'    <span class="span4">'
+'      <ul class="level1">'
+'        <li class="leaf"><a href="#">Red Hat JBoss Middleware Overview</a></li>'
+'        <li class="leaf"><a href="#">Red Hat JBoss Middleware Products</a></li>'
+'        <li class="leaf"><a href="#">Red Hat JBoss Projects & Standards</a></li>'
+'      </ul>'
+'    </span>'
+'    <span class="span4">'
+'      <ul class="level1">'
+'        <li class="leaf"><a href="#">redhat.com</a></li>'
+'        <li class="leaf"><a href="#">Red Hat Customer Portal</a></li>'
+'        <li class="leaf"><a href="#">OpenShift</a></li>'
+'      </ul>'
+'    </span>'
+'  </div>'
+'</div>'
+'</div>';

var _srch = window.search = {};

_srch.context = [
    { 
        description: "Search the Community",
        url: "http://community.jboss.org/search.jspa?"
    },
    {
        description: "Search Project Pages",
        url: "http://www.google.com/search?as_sitesearch=jboss.org"
    }
];

_srch.disappearDelay = 250;
_srch.hideMenuOnClick = true;
_srch.selectedContext = 0;
_srch.initialized = false;

_srch.init = function () {

    var searchBar = jQuery("#searchbar");
    if (searchBar.length == 0) {
        return;
    }
    if (_srch.initialized) {
    return;
    }

    var innerMenuContent =
        "<div><a href='#' context='0'>"+_srch.context[0].description+"</a></div>" +
        "<div><a href='#' context='1'>"+_srch.context[1].description+"</a></div>";

    var searchButton = jQuery("#searchGo");
    var dropDownMenu = jQuery("#dropmenudiv");
    if (dropDownMenu.length == 0) {
        var htmlContent = "<div id='dropmenudiv' style='display: none; position: absolute; left: 0px; top: 0px;' />";
        jQuery('body').prepend(htmlContent);
        dropDownMenu = jQuery("#dropmenudiv");
    }

    var leaveSearchBarHandler = function (searchBar, dropDownMenu) {
        var text = searchBar.val();
        if (text == undefined || text == "" || equalsAnyDescription(text)) {
            text = dropDownMenu.find('a.selected').text();
            searchBar.val(text);
        }
    };

    var enterSearchBarHandler = function(searchBar, dropDownMenu) {
        var text = searchBar.val();
        if (equalsAnyDescription(text)) {
            searchBar.val("");
        }
    };

    var equalsAnyDescription = function (text) {
        if (text != undefined) {
            for (var i = 0; i < _srch.context.length; i++) {
                if (text == _srch.context[i].description) {
                    return true;
                }
            }
        }
        return false;
    };

    var showDropDownMenu = function (menu, attributes) {
        if (menu) {
            if (attributes) {
                menu.css('top', attributes.top + attributes.height);
                menu.css('left', attributes.left);
                menu.css('width', attributes.width - menu.css('padding-left').replace(/\D+/,"") - menu.css('padding-right').replace(/\D+/,""));
            }
            menu.stop(true, true).show();
        }
    };

    var hideDropDownMenu = function (menu, delay) {
        if (menu) {
            menu.delay(delay != undefined ? delay : _srch.disappearDelay).fadeOut(100);
        }
    };

    var getPositionAttributes = function (element) {
        var attr = { top: 0, left: 0, height: 10, width: 150 };
        if (element) {
            attr.height = element.outerHeight();
            var offset = element.offset();
            attr.top = offset.top;
            attr.left = offset.left;
            attr.width = element.outerWidth();
        }
        return attr;
    };

    var executeSearch = function (query) {
        window.location.href = _srch.context[_srch.selectedContext].url + "&q=" + query;
    };

    var catchEnter = function (event){
        if (event.keyCode == '13') {
            event.preventDefault();
            executeSearch(searchBar.val());
        }
    };

    dropDownMenu.html(innerMenuContent);

    dropDownMenu.find('a').click(
        function(e) {
            dropDownMenu.find('a').removeClass('selected');
            var target = jQuery(e.target || e.srcElement);
            _srch.selectedContext = parseInt(target.attr('context'),10);
            target.addClass('selected');
            hideDropDownMenu(dropDownMenu, 0);
            leaveSearchBarHandler(searchBar, dropDownMenu);
            return false;
        }
    );

    leaveSearchBarHandler(searchBar, dropDownMenu);

    dropDownMenu.css('width', searchBar.outerWidth());

    searchBar.unbind();

    searchBar.keydown(function(e) { catchEnter(e) });
    searchBar.blur(function() { leaveSearchBarHandler(searchBar, dropDownMenu) });
    searchBar.focus(function() { enterSearchBarHandler(searchBar, dropDownMenu) });

    searchBar.hover(
        function() { showDropDownMenu(dropDownMenu, getPositionAttributes(searchBar)) },
        function() { hideDropDownMenu(dropDownMenu) });

    dropDownMenu.hover(
        function() { showDropDownMenu(dropDownMenu, getPositionAttributes(searchBar)) },
        function() { hideDropDownMenu(dropDownMenu) }
    );

    searchButton.click(function() { executeSearch(searchBar.val()) });

    if (_srch.hideMenuOnClick == true) { document.onclick = function() { hideDropDownMenu(dropDownMenu); } }

    _srch.initialized = true;
};

jQuery(document).ready( _srch.init );

var initializeSearchBar = function() {
    _srch.init();
};
Tabzilla();

function renderTabzilla( projectName , projectId, fullWidth ) {

  if ( ( typeof projectName=='undefined' ) || ( typeof projectId=='undefined' ) ) {
    console.error("Variables 'project' and 'project_name' have to be provided in your site.yml configuration file.");
    return;
  }

  if ( fullWidth ) {
    $("#tabnav-panel").addClass( "fullwidth" );
  }

  var valueFromCache = null;
  if (window.localStorage && window.localStorage.getItem(projectId+"TabzillaCache") ) {

    var temp = JSON.parse(window.localStorage.getItem(projectId+"TabzillaCache"));

    if ( new Date() - new Date(Date.parse(temp.cachedDate)) < (1000*60*60*24*7) ) {
      valueFromCache = temp;
    }

  }

  if (valueFromCache==null) {

    var wrapper = $.ajax({url:"//static.jboss.org/partials/tabcontent.html",
      dataType:'html'
    });

    $.when( wrapper ).then( function(wrapperResult ) {
      var data = undefined;
      var content = wrapperResult;
      
      var htmlContent;

      if (typeof data!='undefined' && data.total>0) {

        htmlContent = $(content).find('#supported');
        htmlContent.find("#project_name").html(projectName);
        var firstUl = htmlContent.find("#products-first-column");
        var secondUl = htmlContent.find("#products-second-column");

        var firstColumnSize = Math.ceil(data.total/2);
        $.each(data.hits , function(index, value) {
          if (index<firstColumnSize) {
            firstUl.html(firstUl.html()+'<li class="leaf"><a href="'+value.url+'">'+value.name+'</a></li>');
          } else {
            secondUl.html(secondUl.html()+'<li class="leaf"><a href="'+value.url+'">'+value.name+'</a></li>');
          }
        });

        $(".tabcontent").html(htmlContent);

      } else {

        htmlContent = $(content).find("#nonsupported");
        htmlContent.find("#project_name").html(projectName);
        $(".tabcontent").html(htmlContent);

      }

      if (window.localStorage && htmlContent) {
        var entry = new Object();
        entry.cachedDate = new Date();
        entry.htmlContent = htmlContent.html();
        window.localStorage.setItem(projectId+"TabzillaCache",JSON.stringify(entry));
      }

    });

  } else {

    $(".tabcontent").html(valueFromCache.htmlContent);

  }
}
