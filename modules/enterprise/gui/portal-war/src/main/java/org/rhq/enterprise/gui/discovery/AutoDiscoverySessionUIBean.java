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
package org.rhq.enterprise.gui.discovery;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.gui.util.FacesContextUtility;

public class AutoDiscoverySessionUIBean {
    public static final String MANAGED_BEAN_NAME = "AutoDiscoverySessionUIBean";

    private Map<Integer, Boolean> expandedPlatforms = new HashMap<Integer, Boolean>();
    private Map<Integer, Boolean> selectedResources = new HashMap<Integer, Boolean>();
    private String showNewIgnore;

    public AutoDiscoverySessionUIBean() {
    }

    public String expand() {
        Integer id = FacesContextUtility.getRequiredRequestParameter("platformId", Integer.class);
        expandedPlatforms.put(id, Boolean.TRUE);
        return "success";
    }

    public String collapse() {
        Integer id = FacesContextUtility.getRequiredRequestParameter("platformId", Integer.class);
        expandedPlatforms.put(id, Boolean.FALSE);
        return "success";
    }

    public String expandAll() {
        for (Integer id : expandedPlatforms.keySet()) {
            expandedPlatforms.put(id, Boolean.TRUE);
        }
        return "success";
    }

    public String collapseAll() {
        for (Integer id : expandedPlatforms.keySet()) {
            expandedPlatforms.put(id, Boolean.FALSE);
        }
        return "success";
    }

    /**
     * Returns a map keyed on platform ID. Values are true if the platform is expanded, false if collapsed.
     *
     * @return map to indicate which platforms are expanded and which are not.
     */
    public Map<Integer, Boolean> getExpandedMap() {
        return expandedPlatforms;
    }

    public String getShowNewIgnore() {
        return showNewIgnore;
    }

    public void setShowNewIgnore(String val) {
        showNewIgnore = val;
    }

    /**
     * Returns a map keyed on resource ID. Values are true if the resource is selected (checkbox was checked).
     *
     * @return map to indicate which resources were selected
     */
    public Map<Integer, Boolean> getSelectedResources() {
        return selectedResources;
    }
}