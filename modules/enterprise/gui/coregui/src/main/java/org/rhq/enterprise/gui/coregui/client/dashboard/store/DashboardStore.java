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

import java.util.HashSet;
import java.util.Set;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.preferences.UserPreferences;

/**
 * @author Greg Hinkle
 */
public class DashboardStore {

    public static final String STORE_KEY = ".dashboards";

    private UserPreferences preferences;


    private HashSet<StoredDashboard> storedDashboards = new HashSet<StoredDashboard>();


    public DashboardStore() {

        preferences = CoreGUI.getUserPreferences();
        load();

    }


    public void load() {
//        Set<Integer> dashboardIds = preferences.getPreferenceAsIntegerSet(STORE_KEY);


    }


    public Set<StoredDashboard> getStoredDashboards() {
        return storedDashboards;
    }
}
