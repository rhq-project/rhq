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
package org.rhq.enterprise.gui.legacy.util.minitab;

/**
 * A simple bean that represents a minitab in the monitor subsection.
 */
public class MiniTab {
    //-------------------------------------instance variables

    private String key;
    private String mode;
    private String name;
    private String param;
    private Boolean selected;

    //-------------------------------------constructors

    public MiniTab() {
        super();
    }

    //-------------------------------------public methods

    public String getKey() {
        return key;
    }

    public void setKey(String s) {
        key = s;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String s) {
        mode = s;
    }

    public String getName() {
        return name;
    }

    public void setName(String s) {
        name = s;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String s) {
        param = s;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean b) {
        selected = b;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("key=").append(key);
        b.append(" mode=").append(mode);
        b.append(" name=").append(name);
        b.append(" param=").append(param);
        b.append(" selected=").append(selected);
        return b.toString();
    }
}