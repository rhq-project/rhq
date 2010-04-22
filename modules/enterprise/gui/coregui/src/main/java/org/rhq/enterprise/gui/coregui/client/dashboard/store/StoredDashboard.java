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

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class StoredDashboard {

    public static final String STORE_KEY = ".dashboard.";
    private String id;

    private String name;

    private int columns;

    private String[] columnWidths;

    private ArrayList<ArrayList<StoredPortlet>> portlets = new ArrayList<ArrayList<StoredPortlet>>();


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColumns() {
        return columns;
    }


    public ArrayList<ArrayList<StoredPortlet>> getPortlets() {
        return portlets;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }


    public String[] getColumnWidths() {
        return columnWidths;
    }

    public void setColumnWidths(String... columnWidths) {
        this.columnWidths = columnWidths;
    }
}
