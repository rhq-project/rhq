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

    /*
     * Adapted from http://code.google.com/p/google-web-toolkit/issues/detail?id=3608
     * 
     * Should 
     */
    public static native void forceIe6Hacks()
    /*-{
        if (typeof $doc.body.style.maxHeight == "undefined") {
            $wnd.XMLHttpRequestBackup = $wnd.XMLHttpRequest;
            $wnd.XMLHttpRequest = null;
        }
    }-*/;

    /*
     * Adapted from http://code.google.com/p/google-web-toolkit/issues/detail?id=3608
     */
    public static native void unforceIe6Hacks()
    /*-{
        if (typeof $doc.body.style.maxHeight == "undefined") {
            $wnd.XMLHttpRequest = $wnd.XMLHttpRequestBackup;
            $wnd.XMLHttpRequestBackup = null;
        }
    }-*/;

    //This is a JSNI call out to the third party javascript lib to execute on the data inserted into the DOM.
    public static native void graphSparkLines()
    /*-{
     //find all elements where attribute class contains 'dynamicsparkline' and graph their contents
     $wnd.jQuery('.dynamicsparkline').sparkline();
    }-*/;
}
