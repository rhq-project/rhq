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
package org.rhq.enterprise.gui.legacy.portlet.criticalalerts;

import java.util.Arrays;

import org.rhq.enterprise.gui.legacy.WebUserPreferences.AlertsPortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {

    private AlertsPortletPreferences prefs = new AlertsPortletPreferences();
    private String key;

    public PropertiesForm() {
        super();
    }

    public Integer getNumberOfAlerts() {
        return this.prefs.count;
    }

    public void setNumberOfAlerts(Integer numberOfAlerts) {
        this.prefs.count = numberOfAlerts;
    }

    public Integer getPriority() {
        return this.prefs.priority;
    }

    public void setPriority(Integer priority) {
        this.prefs.priority = priority;
    }

    public Long getPast() {
        return this.prefs.timeRange;
    }

    public void setPast(Long past) {
        this.prefs.timeRange = past;
    }

    public String getSelectedOrAll() {
        return this.prefs.displayAll;
    }

    public void setSelectedOrAll(String selectedOrAll) {
        this.prefs.displayAll = selectedOrAll;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer[] getIds() {
        return this.prefs.asArray();
    }

    public void setIds(Integer[] ids) {
        this.prefs.setResource(Arrays.asList(ids));
    }

    public AlertsPortletPreferences getAlertsPortletPreferences() {
        return prefs;
    }

    public void setAlertsPortletPreferences(AlertsPortletPreferences prefs) {
        this.prefs = prefs;
    }
}