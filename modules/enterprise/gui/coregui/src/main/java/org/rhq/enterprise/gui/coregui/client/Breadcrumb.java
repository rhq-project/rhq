/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client;

import com.smartgwt.client.widgets.Canvas;

/**
 * Information about a breadcrumb (i.e. history token component) that is used to render that breadcrumb in the
 * breadcrumb trail at the top of the page.
 *
 * @author Ian Springer
 */
public class Breadcrumb {
    private String name;
    private String displayName;
    private String icon;
    private boolean hyperlink;

    public Breadcrumb(String name) {
        this(name, null);
    }

    public Breadcrumb(String name, String displayName) {
        this(name, displayName, null, true);
    }

    public Breadcrumb(String name, boolean hyperlink) {
        this(name, name, null, hyperlink);
    }

    public Breadcrumb(String name, String displayName, String icon, boolean hyperlink) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null.");
        }
        this.name = name;
        this.icon = icon;
        setDisplayName(displayName);
        this.hyperlink = hyperlink;
    }

    /**
     * Returns this breadcrumb's name (e.g. "10001").
     *
     * @return this breadcrumb's name (e.g. "10001")
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the display name to use for this breadcrumb in the breadcrumb trail, or null if the display name should
     * be derived from the name.
     *
     * @return the display name to use for this breadcrumb in the breadcrumb trail, or null if the display name should
     *         be derived from the name
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Sets the display name to use for this breadcrumb in the breadcrumb trail, or null if the display name should
     * be derived from the name.
     */
    public void setDisplayName(String displayName) {
        this.displayName = (displayName != null) ? displayName : getDefaultDisplayName();
    }

    /**
     * Return true if this breadcrumb should be a hyperlink in the breadcrumb trail, or false otherwise. NOTE:
     * Regardless of what this method returns, if this breadcrumb is the last item in the breadcrumb trail, it
     * will not be made a hyperlink, since it already corresponds to the current view.
     *
     * @return true if this breadcrumb should be a hyperlink in the breadcrumb trail, or false otherwise
     */
    public boolean isHyperlink() {
        return this.hyperlink;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getDisplayHTML() {
        String display = "";
        if (icon != null) {
            display += Canvas.imgHTML(icon, 16, 16);
        }
        display += this.displayName;

        return display;
    }


    @Override
    public String toString() {
        return this.name;
    }

    private String getDefaultDisplayName() {
        StringBuilder displayName = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < this.name.length(); i++) {
            char currentChar = this.name.charAt(i);
            if (!first) {
                if (Character.isUpperCase(currentChar)) {
                    if ((i + 1) == this.name.length() || Character.isLowerCase(this.name.charAt(i + 1))) {
                        displayName.append(" ");
                    }
                }
            } else {
                first = false;
            }
            displayName.append(currentChar);
        }
        return displayName.toString();
    }
}