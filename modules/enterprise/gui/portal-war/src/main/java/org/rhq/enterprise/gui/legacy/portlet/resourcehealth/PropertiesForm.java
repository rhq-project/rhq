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
package org.rhq.enterprise.gui.legacy.portlet.resourcehealth;

import java.util.Arrays;

import org.rhq.enterprise.gui.legacy.WebUserPreferences.FavoriteResourcePortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.DashboardBaseForm;

public class PropertiesForm extends DashboardBaseForm {

    FavoriteResourcePortletPreferences prefs = new FavoriteResourcePortletPreferences();

    public boolean isAvailability() {
        return this.prefs.showAvailability;
    }

    public void setAvailability(boolean availability) {
        this.prefs.showAvailability = availability;
    }

    public boolean isAlerts() {
        return this.prefs.showAlerts;
    }

    public void setAlerts(boolean flag) {
        this.prefs.showAlerts = flag;
    }

    public Integer[] getIds() {
        return this.prefs.asArray();
    }

    public void setIds(Integer[] ids) {
        this.prefs.resourceIds = Arrays.asList(ids);
    }

    public FavoriteResourcePortletPreferences getFavoriteResourcePortletPreferences() {
        return prefs;
    }

    public void setFavoriteResourcePortletPreferences(FavoriteResourcePortletPreferences prefs) {
        this.prefs = prefs;
    }
}