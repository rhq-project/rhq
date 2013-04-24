/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.util;

/**
 * Utility class for dealing with browser quirks.
 * 
 * @author Joseph Marques
 */
public class BrowserUtility {
    private BrowserUtility() {
        // static access only
    }


    //This is a JSNI call out to the third party javascript lib to execute on the data inserted into the DOM.
    public static native void graphSparkLines()
    /*-{
     //find all elements where attribute class contains 'dynamicsparkline' and graph their contents
     $wnd.jQuery('.dynamicsparkline').sparkline();
    }-*/;

    /**
     * Things such as charting are not supported before IE9. So this is a test for browser compat.
     * @return true if the browser is IE and the version is before IE9.
     */
    public static native boolean isBrowserPreIE9()
        /*-{
            var myUserAgent = $wnd.navigator.userAgent;
            if (/MSIE (\d+\.\d+);/.test(myUserAgent)){ //test for MSIE x.x;
                var ieVersion = new Number(RegExp.$1); // capture x.x portion and store as a number
                if (ieVersion < 9){
                    return true;
                }
            }
            return false;
        }-*/;

}
