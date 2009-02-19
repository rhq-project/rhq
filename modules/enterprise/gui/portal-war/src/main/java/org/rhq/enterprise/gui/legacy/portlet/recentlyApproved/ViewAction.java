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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.RecentlyApprovedPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        List<RecentlyAddedResourceComposite> platformList = new ArrayList<RecentlyAddedResourceComposite>();

        try {
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }

            WebUserPreferences preferences = user.getWebPreferences();
            RecentlyApprovedPortletPreferences recentlyApprovedPreferences = preferences
                .getRecentlyApprovedPortletPreferences();
            Subject subject = user.getSubject();

            // Based on the user preference, generate a timestamp of the oldest resource to display.
            long range = recentlyApprovedPreferences.hours;
            long ts = 0; // defaults to ANY time in the past
            if (range != -1) {
                // range encoded as hours (UI shows days)
                ts = System.currentTimeMillis() - (range * 60 * 60 * 1000);
            }

            platformList = resourceManager.getRecentlyAddedPlatforms(subject, ts, recentlyApprovedPreferences.range);

            Map<Integer, RecentlyAddedResourceComposite> platformMap;
            platformMap = new HashMap<Integer, RecentlyAddedResourceComposite>();

            for (RecentlyAddedResourceComposite platform : platformList) {
                platformMap.put(platform.getId(), platform);
            }

            // Set the show servers flag on all expanded platforms.
            // Find the list of expanded platforms for this user and make it available to the jsp.
            List<String> removeExpandedPlatforms = new ArrayList<String>();

            for (String expandedPlatform : recentlyApprovedPreferences.expandedPlatforms) {
                Integer platformId = null;
                try {
                    platformId = Integer.valueOf(expandedPlatform);
                } catch (NumberFormatException nfe) {
                    removeExpandedPlatforms.add(expandedPlatform);
                    continue;
                }
                RecentlyAddedResourceComposite miniPlatform = platformMap.get(platformId);
                if (miniPlatform != null) {
                    miniPlatform.setShowChildren(true);
                    miniPlatform.setChildren(resourceManager
                        .getRecentlyAddedServers(subject, ts, platformId.intValue()));
                } else {
                    removeExpandedPlatforms.add(expandedPlatform);
                }
            }

            // we do this just to clean up the preferences for platforms in the filter list which no longer exist
            if (removeExpandedPlatforms.size() > 0) {
                recentlyApprovedPreferences.expandedPlatforms.removeAll(removeExpandedPlatforms);
                preferences.setRecentlyApprovedPortletPreferences(recentlyApprovedPreferences);
            }

            // Make the list available to the jsp.

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [RecentlyApproved] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [RecentlyApproved] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("recentlyApproved", platformList);
        }

        return null;
    }
}