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

import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {
    private Integer numberOfAlerts;
    private String priority;
    private String past;
    private String selectedOrAll;
    private String key;
    private String[] ids;

    public PropertiesForm() {
        super();
    }

    public Integer getNumberOfAlerts() {
        return this.numberOfAlerts;
    }

    public void setNumberOfAlerts(Integer numberOfAlerts) {
        this.numberOfAlerts = numberOfAlerts;
    }

    public String getPriority() {
        return this.priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getPast() {
        return this.past;
    }

    public void setPast(String past) {
        this.past = past;
    }

    public String getSelectedOrAll() {
        return this.selectedOrAll;
    }

    public void setSelectedOrAll(String selectedOrAll) {
        this.selectedOrAll = selectedOrAll;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String[] getIds() {
        return this.ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }
}