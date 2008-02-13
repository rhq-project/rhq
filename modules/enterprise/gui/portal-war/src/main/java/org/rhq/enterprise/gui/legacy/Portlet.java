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
package org.rhq.enterprise.gui.legacy;

/**
 * A class representing an individual component of an overall page layout (<code>Portal</code>).
 */
public class Portlet {
    private String name;
    private String description;
    private String url;
    private String label;
    private boolean isFirst = false;
    private boolean isLast = false;

    public Portlet() {
    }

    public Portlet(String url) {
        super();
        this.url = url;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setIsFirst() {
        isFirst = true;
    }

    public boolean getIsFirst() {
        return isFirst;
    }

    public void setIsLast() {
        isLast = true;
    }

    public boolean getIsLast() {
        return isLast;
    }

    public String toString() {
        return "[ name:" + getName() + " url: " + getUrl() + " ]";
    }
}