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
package org.rhq.enterprise.gui.common.framework;

import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;

/**
 * @author Greg Hinkle
 */
public class UserPreferencesUIBean {

    public static final String LEFT_RESOURCE_NAV_SHOWING = "ui.leftResourceNavShowing";
    public static final String SUMMARY_PANEL_DISPLAY_STATE = "ui.summaryPanelDisplayState";


    public String getLeftResourceNavState() {
        return EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getPreference(LEFT_RESOURCE_NAV_SHOWING, "tree");
    }

    public void setLeftResourceNavState(String state) {
        EnterpriseFacesContextUtility.getWebUser().getWebPreferences().setPreference(LEFT_RESOURCE_NAV_SHOWING, state);
    }

    public String getSummaryPanelDisplayState() {
        return EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getPreference(SUMMARY_PANEL_DISPLAY_STATE, "true");
    }

    public void setSummaryPanelDisplayState(String state) {
        EnterpriseFacesContextUtility.getWebUser().getWebPreferences().setPreference(SUMMARY_PANEL_DISPLAY_STATE, state);
    }
    
}
