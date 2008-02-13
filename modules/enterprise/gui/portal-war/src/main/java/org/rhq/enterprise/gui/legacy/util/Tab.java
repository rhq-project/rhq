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

import org.apache.struts.tiles.beans.SimpleMenuItem;

/**
 * A simple bean that extends <code>MenuItem</code> to supply image dimensions for a tab button.
 */
public class Tab extends SimpleMenuItem {
    //-------------------------------------instance variables

    private Integer height;
    private String mode;
    private Integer width;
    private Boolean visible;

    //-------------------------------------constructors

    public Tab() {
        super();
    }

    //-------------------------------------public methods

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer i) {
        height = i;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String i) {
        mode = i;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer i) {
        width = i;
    }

    public Boolean getVisible() {
        // default to true
        if (visible == null) {
            visible = Boolean.TRUE;
        }

        return visible;
    }

    public void setVisible(Boolean visibleFlag) {
        visible = visibleFlag;
    }
}