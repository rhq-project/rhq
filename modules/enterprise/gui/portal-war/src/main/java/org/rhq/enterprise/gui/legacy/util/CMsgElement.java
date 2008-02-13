/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.legacy.util;

public class CMsgElement {
    /**
     * text is a key into ApplicationResources.properties
     */
    public String text = null;

    /**
     * url is plaintext
     */
    public String url = null;

    /**
     * mouseover is plaintext
     */
    public String mouseover = null;

    /**
     * params are plaintext, and will subtituted as {0} style parameters into the 'text' message
     */
    public String[] params = null;

    public CMsgElement(String text) {
        this(text, new String[0]);
    }

    public CMsgElement(String text, String url) {
        this(text, url, new String[0]);
    }

    public CMsgElement(String text, String[] params) {
        this.text = text;
        this.params = params;
    }

    public CMsgElement(String text, String url, String[] params) {
        this.text = text;
        this.url = url;
        this.params = params;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public String[] getParams() {
        return params;
    }

    public String getMouseover() {
        return mouseover;
    }
}