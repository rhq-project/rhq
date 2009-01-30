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
package org.rhq.enterprise.gui.legacy.portlet.recentlyApproved;

// XXX: remove when ImageBeanButton works
import org.rhq.enterprise.gui.legacy.WebUserPreferences.RecentlyApprovedPortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {

    RecentlyApprovedPortletPreferences prefs = new RecentlyApprovedPortletPreferences();

    public int getHours() {
        return this.prefs.hours;
    }

    public void setHours(int hours) {
        this.prefs.hours = hours;
    }

    public int getRows() {
        return this.prefs.range;
    }

    public void setRows(int rows) {
        this.prefs.range = rows;
    }

    public RecentlyApprovedPortletPreferences getRecentlyApprovedPortletPreferences() {
        return prefs;
    }

    public void setRecentlyApprovedPortletPreferences(RecentlyApprovedPortletPreferences prefs) {
        this.prefs = prefs;
    }
}