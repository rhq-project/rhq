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
 * A simple bean that represents a subminitab in the monitor subsection.
 */
public class SubMiniTab {
    //-------------------------------------instance variables

    private String count;
    private String id;
    private String key;
    private String name;
    private String param;
    private Boolean selected;

    //-------------------------------------constructors

    public SubMiniTab() {
        super();
    }

    //-------------------------------------public methods

    public String getCount() {
        return count;
    }

    public void setCount(String s) {
        count = s;
    }

    public String getId() {
        return id;
    }

    public void setId(String s) {
        id = s;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String s) {
        key = s;
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
        StringBuffer b = new StringBuffer("{");
        b.append("name=").append(name);
        b.append(" id=").append(id);
        b.append(" count=").append(count);
        b.append(" key=").append(key);
        b.append(" param=").append(param);
        b.append(" selected=").append(selected);
        return b.append("}").toString();
    }
}