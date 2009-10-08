/*
Class: SplitPane
    Separates two divs with a draggable divider that you can use to resize the divs, kind of like
    a frame but without using frames! The two divs should be siblings, that is they should both
    have the same parent. You can have an arbitrary number of such siblings separated using different
    instances of this class, i.e. you can have any number of columns separated by a draggable divider
    that alows you to resize them at will.

    You can ask to be notified when the following events occur

    - A drag starts.
    - A drag occurs.
    - A drag ends.

    This allows you to perform any housekeepng not already performed for you.

    You can disable the resizing.

    You can ask an instance to serialize itself as an HTTP POST argument list, this is useful in
    combination with onEnd hooking to save the current div sizes on the server.

NOTE:
    In IE all parent divs must have a height other than 'auto'.
    div1 and div2 should probably have overflow=hidden set.
*/
var SplitPane = Class.create();

/*
property: SplitPane.cache
    Holds all instances of SplitPane. Used to delay intialization until Window.onLoad().
*/
SplitPane.cache = new Array();
SplitPane.cacheIndex = 0;
SplitPane.handleWidth = 6; // Width of the handle

SplitPane.prototype = {
    /*
    Constructor: intialize

    parameters:
        div1_id - a div, or the ID of a div notionally on the 'left' of the divider.
        div1_width - the initial width of div1 as a percentage of its parent's width
        div2_id - a div, or the ID of a div notionally on the 'right' of the divider.
        div2_left - the coordinate of the left edge of div2 relative to the parent div as a percentage.
        div2_width - the initial width of div2 as a percentage of its parent's width.
        options - an associative array of optional arguments which include

    options:
        onStart - a function to be called when a drag of the divider starts.
        onDrag - a function to be called when a drag occurs.
        onEnd - a function to call when a drag ends.
        active - if true then resizing can occur. If false then the two divs are set to
            the specified widths and that is that. Defaults to false.
    */
    initialize: function(div1_id, div1_width, div2_id, options) {
        this.options = {
            onStart:    Prototype.emptyFunction,
            onDrag:     Prototype.emptyFunction,
            onEnd:      Prototype.emptyFunction,
            active:     false
        }

        div1_width = parseFloat(div1_width);
        if (div1_width < 0 || div1_width > 80) {
            div1_width = 30;
        }

        Object.extend(this.options, options || {});

        this.div1 = $(div1_id);
        this.div2 = $(div2_id);
        this.container = this.div1.parentNode;  // This had better be the same for both divs
        this.div1_width = div1_width;   // as a percentage

        this.div2_width = 100.0 - this.div1_width - (SplitPane.handleWidth / this.getWidth(this.container));
        this.div2_left = div1_width;     // as a percentage

        SplitPane.cache[SplitPane.cacheIndex] = this;
        SplitPane.cacheIndex = SplitPane.cacheIndex+1;
    },

    /*
        function: set
            create a divider. If its marked as 'active' then wire it up for events.
    */
    set: function() {
        Element.makePositioned(this.container); // Fix IE

        // Change widths to percents so that window resizing works
        this.div1.style.width = this.div1_width + "%";
        this.div2.style.width = ((this.div2_width) - ((SplitPane.handleWidth*2) / this.getWidth(this.container))) + "%";
        this.div2.style.left  = this.div2_left + "%";

        // Create a divider and make it a child of container
        this.divider = document.createElement("DIV");
        this.container.appendChild(this.divider);
        this.divider.className="splitpane-divider";
        this.divider.style.position="absolute";
        this.divider.style.width=SplitPane.handleWidth + "px";
        this.divider.style.top="0px";
        this.divider.style.zIndex=0;

        this.containerWidth = this.getWidth(this.container);

        this.setDividerX();
        this.setDividerHeight();

        if (this.options.active) {
            this.eventMouseDown = this.startDrag.bindAsEventListener(this);
            this.eventMouseUp   = this.endDrag.bindAsEventListener(this);
            this.eventChangeCursor = this.cursor.bindAsEventListener(this);
            this.eventMouseMove = this.update.bindAsEventListener(this);

            Event.observe(this.divider, "mousedown", this.eventMouseDown);
            Event.observe(document, "mouseup", this.eventMouseUp);
            Event.observe(this.divider, "mousemove", this.eventChangeCursor);
            Event.observe(document, "mousemove", this.eventMouseMove);
        }
    },

    /*
        function: serialize
            serialize the splitpane in a form suitable to be used in an HTTP request.

        serialized values:
            div1 - the id of div1
            div1_left - the left edge of div1 expressed as a percentage of the parent width
            div1_width - the width of div1 expressed as a percentage of the parent width
            div2 - the id of div2
            div1_left - the left edge of div2 expressed as a percentage of the parent width
            div1_width - the width of div2 expressed as a percentage of the parent width
    */
    serialize: function() {
        return "div1=" + this.div1.id
        + "&div1_left=" + this.getXPercent(this.div1)
        + "&div1_width=" + this.getWidthPercent(this.div1)
        + "&div2=" + this.div2.id
        + "&div2_left=" + this.getXPercent(this.div2)
        + "&div2_width=" + this.getWidthPercent(this.div2);
    },

    /*
        function: dispose
            unhook from events
    */
    dispose: function() {
        Event.stopObserving(this.divider, "mousedown", this.eventMouseDown);
        Event.stopObserving(document, "mouseup", this.eventMouseUp);
        Event.stopObserving(this.divider, "mousemove", this.eventChangeCursor);
        Event.stopObserving(document, "mousemove", this.eventMouseMove);
    },

    cursor: function(event) {
        this.divider.style.cursor = "e-resize";
    },

    startDrag: function(event) {
        if(Event.isLeftClick(event)) {
            this.active = true;
            var offsets = Position.cumulativeOffset(this.divider);

            this.start_pointer  = [Event.pointerX(event), Event.pointerY(event)];
            this.inset = this.start_pointer[0] - offsets[0];
            this.containerWidth = this.getWidth(this.container);
            this.start_div1_width = this.getWidth(this.div1);
            this.start_div2_left = this.getX(this.div2);
            this.start_div2_width = this.getWidth(this.div2);
            this.start_divider_x = this.getX(this.divider);

            Event.stop(event);

            this.options.onStart(this, event);
        }
    },

    endDrag: function(event) {
        if (this.active) {
            this.active = false;
            Event.stop(event);
            this.setDividerX();
            this.setDividerHeight();
            this.options.onEnd(this, event);
        }
    },

    update: function(event) {
        if (this.active) {
            var pointer  = [Event.pointerX(event), Event.pointerY(event)];
            var delta = pointer[0] - this.start_pointer[0];

            this._move(delta);
            
            Event.stop(event);

            this.options.onDrag(this, event);
        }
    },

    _move: function(delta) {
        var delta_percent = delta * 100.0 / this.containerWidth;

        // Calculate new div1 width
        var new_div1_width = this.start_div1_width + delta;

        // Limit width of div1
        if (new_div1_width < 0.0) {
            new_div1_width = 0.0;
            delta = -this.start_div1_width;
        }

        // Calculate new div2 width (in %)
        var new_div2_width = this.start_div2_width - delta;

        // Limit width of div2
        if (new_div2_width < 0.0) {
            new_div2_width = 0.0;
            delta = this.start_div2_width;
            new_div1_width = this.start_div1_width + delta;
        }

        // resize/position the divs
        this.div1.style.width = ((new_div1_width) * 100.0 / this.containerWidth) + "%";
        this.div2.style.left  = ((this.start_div2_left + delta) * 100.0 / this.containerWidth) + "%";
        this.div2.style.width = ((new_div2_width) * 100.0 / this.containerWidth) + "%";

        // Set absolute position of divider - fix it up to be a % in endDrag().
        this.divider.style.left = (this.start_divider_x + delta) + "px";
    },

    move: function(delta) {
        this.containerWidth = this.getWidth(this.container);
        this.start_div1_width = this.getWidth(this.div1);
        this.start_div2_left = this.getX(this.div2);
        this.start_div2_width = this.getWidth(this.div2);
        this.start_divider_x = this.getX(this.divider);

        this._move(delta);

        this.setDividerX();
        this.setDividerHeight();
    },
    
    setDividerX: function() {
        // Place the center of 'divider' half way between div1 and div2
        var div1_right = this.getX(this.div1) + this.getWidth(this.div1);
//        var l = (((this.getX(this.div2)- div1_right - SplitPane.handleWidth)/2 + div1_right) * 100.0 / this.containerWidth) + "%";
        var l = ((this.getX(this.div2)) * 100.0 / this.containerWidth) + "%";
        this.divider.style.left = l;
    },

    setDividerHeight: function() {
        // Set the divider height to the greater of the heights of the two divs
        var h = Math.max(this.getHeight(this.div1), this.getHeight(this.div2));
        this.divider.style.height = h + "px";
    },

    getX: function(el) {
        return el.x ? el.x : el.offsetLeft;
    },

    getXPercent: function(el) {
        var x = "0";
        x = Element.getStyle(el,"left");
        if (x) {
            x = x.replace("%","");  //moz
        }

        return x ? parseFloat(x) : 0.0;
    },

    getWidthPercent: function(el) {
        var w = "0";
        w = Element.getStyle(el,"width");
        if (w) {
            w = w.replace("%","");  //moz
        }

        return w ? parseFloat(w) : 0.0;
    },

    getWidth: function(el) {
        return el.offsetWidth;
    },

    getHeight: function(el) {
        if (el.currentStyle){
            return el.offsetHeight;                                 //ie
        } else {
            return Element.getStyle(el,"height").replace("px","");  //moz
        }
    }
}

SplitPane.setAll = function () {
    for(i=0; i<SplitPane.cache.length; i++){
        SplitPane.cache[i].set();
    }
}

Event.observe(window, "load", SplitPane.setAll);
