var overlay = {
    curTime : null,
    times: new Array(),

    _lastAnchor : null,
    _lastIndex : -1,
    _lastTime : null,
    
    findPosX: function (obj) {
        var curleft = 0;
        if (obj.offsetParent) {
            while (obj.offsetParent) {
                curleft += obj.offsetLeft
                obj = obj.offsetParent;
            }
        }
        else if (obj.x)
            curleft += obj.x;

        return curleft;
    },

    findPosY: function (obj) {
        var curtop = 0;
        if (obj.offsetParent) {
            while (obj.offsetParent) {
                curtop += obj.offsetTop
                obj = obj.offsetParent;
            }
        }
        else if (obj.y)
            curtop += obj.y;
    
            return curtop;
    },

    moveOverlay: function (anchor) {        
        var ovl = this.initOverlay($('overlay'));
        var anchorY = this.findPosY(anchor);
        
        var left = this.findPosX(anchor);
        var top = this.findPosY($('charttop'));

        var bottom = this.findPosY($('timetop'));
        if (detailsShowing) {
           bottom -= 230;
        }

        ovl.style.visibility='visible';
        ovl.style.left = left + 'px';
        ovl.style.top = top + 'px';
        ovl.style.height = (bottom - top) + 'px';
        
        this._lastAnchor = anchor;
    },

    showTimePopup: function (index,time) {
        if (this.curPopup != null) {
            this.curPopup.style.visibility='hidden';
        }
        var anchor = $('timePopup_' + index);

        var left = this.findPosX(anchor) - 50;
        var top = this.findPosY(anchor) + 10;
        
        this.curPopup = this.initOverlay($('timePopup'));
        this.curPopup.innerHTML = time;
        this.curPopup.style.left = left + 'px';
        this.curPopup.style.top = top + 'px';
        this.curPopup.style.height = '35px';
        this.curPopup.style.visibility ='visible';
        new Effect.Appear(this.curPopup);
        
        this._lastIndex = index;
        this._lastTime = time;
    },

    hideTimePopup: function () {
        if (this.curPopup != null) {
            this.curPopup.style.visibility='hidden';
        }
    },

    delayTimePopup: function (index,time) {
      this.curTime = time;
      setTimeout("overlay.showCurrentTimePopup('" + time + "'," + index + ")" ,
                 500);
    },

    showCurrentTimePopup: function (time, index) {
      if (this.curTime == time)
        this.showTimePopup(index,time);
    },

    /**
     * Check that the overlay is a direct child of document.body.
     * This is to ensure that IE6 computes the offsetParent
     * relatively to the document body in the same way as the rest
     * of the browsers.
     */
    initOverlay: function (overlay) {
        var parent = overlay.parentNode;

        if (parent && parent != document.body) {
            parent.removeChild(overlay);
            document.body.appendChild(overlay);                
        }
        return overlay;
    },

    refresh : function() {
        var ovl = $('overlay');
        if (this._lastAnchor && ovl.style.visibility != 'hidden') {
            this.moveOverlay(this._lastAnchor);    
        }
        
        if (this._lastTime != null && this.curPopup && this.curPopup.visibility != 'hidden') {
            this.showTimePopup(this._lastIndex, this._lastTime);    
        }
    }    
}

WindowResizeTracker.addListener(function() {
    overlay.refresh();
});
