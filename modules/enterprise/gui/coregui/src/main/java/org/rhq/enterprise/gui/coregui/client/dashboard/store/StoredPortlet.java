/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard.store;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Greg Hinkle
 */
public class StoredPortlet {

    public static final String STORE_KEY = ".dashboardPortlet.";
    private String id;


    private String portletKey;
    private String name;

    private int column;
    private int index;

    private int height = 300;

    private Map<String,String> properties = new HashMap<String, String>();


    public StoredPortlet() {
    }

    public StoredPortlet(String name, String portletKey, int height) {
        this.name = name;
        this.portletKey = portletKey;
        this.height = height;
    }

    public String getId() {
        return id;
    }

    public String getPortletKey() {
        return portletKey;
    }

    public String getName() {
        return name;
    }

    public int getColumn() {
        return column;
    }

    public int getIndex() {
        return index;
    }

    public int getHeight() {
        return height;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPortletKey(String portletKey) {
        this.portletKey = portletKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setProperty(String name, String value) {
        properties.put(name, value);
    }

}
