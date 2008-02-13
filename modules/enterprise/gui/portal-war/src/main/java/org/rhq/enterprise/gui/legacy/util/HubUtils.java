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
package org.rhq.enterprise.gui.legacy.util;

import java.util.Set;
import org.apache.struts.util.LabelValueBean;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.legacy.HubConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.resource.hub.HubForm;
import org.rhq.enterprise.gui.legacy.action.resource.hub.HubView;
import org.rhq.enterprise.gui.legacy.taglib.display.StringUtil;

public class HubUtils {
    public static final String BLANK = "";

    public static void initView(HubForm hubForm, WebUser user) throws Exception {
        HubView prefView;
        try {
            prefView = HubView.valueOf(user.getPreference(HubConstants.VIEW_ATTRIB).toUpperCase());
        } catch (IllegalArgumentException iae) {
            prefView = HubView.LIST;
        }

        String viewStr = hubForm.getView();
        if (viewStr == null) {
            hubForm.setView(prefView.name());
        }

        HubView view = HubView.valueOf(hubForm.getView().toUpperCase());
        if (!view.equals(prefView)) {
            user.setPreference(HubConstants.VIEW_ATTRIB, view); // Save new preference.

            // AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
            // authzBoss.setUserPrefs(user.getSessionId(), user.getId(),
            // user.getPreferences());
        }
    }

    public static String buildNavHierarchy(String categoryString, ResourceType resourceType) {
        String navHierarchy; // Start the navHierarchy with the group category.
        navHierarchy = StringUtil.toUpperCaseAt(categoryString, 0) + "s" + HubConstants.HIERARCHY_SEPARATOR;
        if (resourceType != null) {
            navHierarchy += getResourceTypeDisplayName(resourceType) + " "
                + StringUtil.toUpperCaseAt(categoryString, 0) + "s";
        } else {
            navHierarchy += "All " + StringUtil.toUpperCaseAt(categoryString, 0) + "s";
        }

        return navHierarchy;
    }

    private static String getResourceTypeDisplayName(ResourceType resourceType) {
        // TODO: Type display name should probably also include ancestor server/service type names.
        //       (e.g. "JBoss Datasource" rather than simply "Datasource" to distinguish a JBoss datasource from a
        //       WebLogic datasource)
        return resourceType.getName();
    }

    public static void addResourceTypeMenuItems(HubForm form, Set<ResourceType> types, String headerLabel,
        String headerValue) {
        if (!types.isEmpty()) {
            form.addType(new LabelValueBean(BLANK, BLANK));
            form.addType(new LabelValueBean(headerLabel, headerValue));
            for (ResourceType resourceType : types) {
                String typeDisplayName = getResourceTypeDisplayName(resourceType);
                form.addType(new LabelValueBean(typeDisplayName, Integer.toString(resourceType.getId())));
            }
        }
    }
}