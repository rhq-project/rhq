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
import java.util.Iterator;
import java.util.Map;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
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

    /**
     * Called when a request came in and we need to determine the state of the checkboxes.
     */
    public void rebuildSelectedResources() {
        final String hiddenElementParameterNameStart = "platform_servers_";

        FacesContext facesContext = FacesContextUtility.getFacesContext();
        ExternalContext externalContext = facesContext.getExternalContext();
        Iterator<String> names = externalContext.getRequestParameterNames();

        // All selected checkboxes will have hidden element values associated with it.
        // A platform is selected if there exists a hidden element named "platform_servers_<platformId>".
        // A server is selected if its resource ID is found in the hidden element's comma-separated list value.

        while (names.hasNext()) {
            String name = names.next();
            if (name.startsWith(hiddenElementParameterNameStart)) {
                Integer platformId = Integer.valueOf(name.substring(hiddenElementParameterNameStart.length()));
                String[] servers = externalContext.getRequestParameterMap().get(name).split(",");

                if ((servers.length != 1) || !servers[0].trim().equals("-1")) {
                    selectedResources.put(platformId, Boolean.TRUE);
                    for (int i = 0; i < servers.length; i++) {
                        String serverString = servers[i].trim();
                        if (serverString.length() > 0) {
                            Integer serverId = Integer.valueOf(serverString);
                            selectedResources.put(serverId, Boolean.TRUE);
                        }
                    }
                }
            }
        }

        return;
    }
}