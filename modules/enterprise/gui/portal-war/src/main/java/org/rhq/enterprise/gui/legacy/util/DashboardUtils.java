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

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Utilities class that provides general convenience methods.
 */
public class DashboardUtils {
    public static final String DASHBOARD_DELIMITER = "|";

    public static PageList<Resource> listAsResources(List<String> resourceIdStrings, WebUser user, PageControl pc)
        throws Exception {
        Integer[] resourceIds = new Integer[resourceIdStrings.size()];
        for (int i = 0; i < resourceIds.length; i++) {
            if (resourceIdStrings.get(i).trim().length() > 0) {
                resourceIds[i] = Integer.valueOf(resourceIdStrings.get(i));
            }
        }

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        return resourceManager.getResourceByIds(user.getSubject(), resourceIds, false, pc);
    }

    public static Integer[] preferencesAsResourceIds(String key, WebUser user) {
        try {
            List<String> resourceIdStrings = user.getPreferences().getPreferenceAsList(key, DASHBOARD_DELIMITER);

            Integer[] resourceIds = new Integer[resourceIdStrings.size()];
            for (int i = 0; i < resourceIds.length; i++) {
                if (resourceIdStrings.get(i).trim().length() > 0) {
                    resourceIds[i] = Integer.valueOf(resourceIdStrings.get(i));
                }
            }

            return resourceIds;
        } catch (Exception e) {
            return new Integer[0];
        }
    }

    public static PageList<Resource> preferencesAsResources(String key, WebUser user, PageControl pc) throws Exception {
        try {
            List<String> resourceList = user.getPreferences().getPreferenceAsList(key, DASHBOARD_DELIMITER);
            return listAsResources(resourceList, user, pc);
        } catch (Exception e) {
            return new PageList<Resource>(pc);
        }
    }

    public static List<String> getUserPreferences(String key, WebUser user) throws Exception {
        try {
            List<String> resourceList = user.getPreferences().getPreferenceAsList(key, DASHBOARD_DELIMITER);
            return resourceList;
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    public static void removePortlet(WebUser user, String PortletName) throws Exception {
        WebUserPreferences preferences = user.getPreferences();
        String first = preferences.getPreference(Constants.USER_PORTLETS_FIRST);
        String second = preferences.getPreference(Constants.USER_PORTLETS_SECOND);

        first = StringUtil.remove(first, PortletName);
        second = StringUtil.remove(second, PortletName);

        first = StringUtil.replace(first, DASHBOARD_DELIMITER + DASHBOARD_DELIMITER, DASHBOARD_DELIMITER);
        second = StringUtil.replace(second, DASHBOARD_DELIMITER + DASHBOARD_DELIMITER, DASHBOARD_DELIMITER);

        preferences.setPreference(Constants.USER_PORTLETS_FIRST, first);
        preferences.setPreference(Constants.USER_PORTLETS_SECOND, second);
    }

    /**
     * A helper method for #verifyResources(String, ServletContext, WebUser) that will look up the <b>user</b>'s
     * persisted preference specified by the <b>key</b> and remove the following list of ids that no long represent
     * valid resourceIds. This method is also called directly from certain Action classes when the user wants to
     * explicitly deletes items from a resource filter list for some portlet.
     *
     * @param  resourceIds
     * @param  key
     * @param  user
     *
     * @throws Exception
     */
    public static void removeResources(String[] resourceIds, String key, WebUser user) throws Exception {
        WebUserPreferences preferences = user.getPreferences();
        String resources = preferences.getPreference(key);

        for (String resourceId : resourceIds) {
            resources = StringUtil.remove(resources, resourceId);
            resources = StringUtil.replace(resources, DASHBOARD_DELIMITER + DASHBOARD_DELIMITER, DASHBOARD_DELIMITER);
        }

        preferences.setPreference(key, resources);
    }

    /**
     * This method checks the resourceId that are persisted as view preferences for various portlets that can be
     * filtered by a list of resources, and removes any ids that are no longer valid - invalid ids might occur from a
     * bug on our part, malicious mucking with the request, or an inventory update / delete.
     *
     * @param  key
     * @param  user
     *
     * @throws Exception
     */
    public static void verifyResources(String key, WebUser user) throws Exception {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        List<String> resourceList = getUserPreferences(key, user);
        boolean didUpdatePrefs = false;

        for (String resourceId : resourceList) {
            try {
                resourceManager.getResourceById(user.getSubject(), Integer.valueOf(resourceId));
                // if no exception, we're golden
            } catch (Exception e) {
                removeResources(new String[] { resourceId }, key, user);
                didUpdatePrefs = true;
            }
        }

        if (didUpdatePrefs) {
            user.getPreferences().persistPreferences();
        }
    }
}