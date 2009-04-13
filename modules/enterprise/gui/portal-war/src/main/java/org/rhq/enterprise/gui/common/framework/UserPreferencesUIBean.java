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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class UserPreferencesUIBean {

    private final Log log = LogFactory.getLog(UserPreferencesUIBean.class);

    public static final String LEFT_RESOURCE_NAV_SHOWING = "ui.leftResourceNavShowing";
    public static final String SUMMARY_PANEL_DISPLAY_STATE = "ui.summaryPanelDisplayState";

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal groupManager = LookupUtil.getResourceGroupManager();
    private List<Resource> resourceFavorites;
    private List<ResourceGroup> groupFavorites;

    public Subject getSubject() {
        return EnterpriseFacesContextUtility.getSubject();
    }

    public WebUser getWebUser() {
        return EnterpriseFacesContextUtility.getWebUser();
    }

    public String getLeftResourceNavState() {
        updateRecentVisits();
        return EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getPreference(LEFT_RESOURCE_NAV_SHOWING,
            "30");
    }

    public void setLeftResourceNavState(String state) {
        WebUser webUser = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = webUser.getWebPreferences();
        preferences.setPreference(LEFT_RESOURCE_NAV_SHOWING, state);
    }

    public void updateSummaryPanelDisplayState(javax.faces.event.ActionEvent event) {
        String summaryPanelState = getSummaryPanelDisplayState();
        setSummaryPanelDisplayState(summaryPanelState.equals("true") ? "false" : "true");
    }

    public String getSummaryPanelDisplayState() {
        return EnterpriseFacesContextUtility.getWebUser().getWebPreferences().getPreference(
            SUMMARY_PANEL_DISPLAY_STATE, "true");
    }

    public void setSummaryPanelDisplayState(String state) {
        WebUser webUser = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = webUser.getWebPreferences();
        preferences.setPreference(SUMMARY_PANEL_DISPLAY_STATE, state);
    }

    public List<Resource> getResourceFavorites() {
        if (resourceFavorites == null) {
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            WebUserPreferences.FavoriteResourcePortletPreferences favoriteResources = user.getWebPreferences()
                .getFavoriteResourcePortletPreferences();

            resourceFavorites = resourceManager.getResourceByIds(getSubject(), favoriteResources.asArray(), false,
                PageControl.getUnlimitedInstance());
        }
        return resourceFavorites;
    }

    public List<ResourceGroup> getGroupFavorites() {
        if (groupFavorites == null) {
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            WebUserPreferences.FavoriteGroupPortletPreferences favoriteGroups = user.getWebPreferences()
                .getFavoriteGroupPortletPreferences();

            groupFavorites = groupManager.getResourceGroupByIds(getSubject(), favoriteGroups.asArray(), PageControl
                .getUnlimitedInstance());
        }
        return groupFavorites;
    }

    public List<WebUserPreferences.ResourceVisit> getRecentVisits() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        return user.getWebPreferences().getRecentResourceVisits();
    }

    public void updateRecentVisits() {
        String resourceId = FacesContextUtility.getOptionalRequestParameter(ParamConstants.RESOURCE_ID_PARAM);
        String groupId = FacesContextUtility.getOptionalRequestParameter(ParamConstants.GROUP_ID_PARAM);

        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences prefs = user.getWebPreferences();
        WebUserPreferences.ResourceVisit visit = null;

        if (resourceId != null) {
            Resource res = resourceManager.getResourceById(getSubject(), Integer.parseInt(resourceId));
            visit = new WebUserPreferences.ResourceVisit(Integer.parseInt(resourceId), res.getName(),
                WebUserPreferences.ResourceVisit.Kind.valueOf(res.getResourceType().getCategory().name()));
        } else if (groupId != null) {
            ResourceGroup group = groupManager.getResourceGroupById(getSubject(), Integer.parseInt(groupId), null);
            visit = new WebUserPreferences.ResourceVisit(Integer.parseInt(groupId), group.getName(), (group
                .getResourceType() != null ? WebUserPreferences.ResourceVisit.Kind.COMPATIBLE_GROUP
                : WebUserPreferences.ResourceVisit.Kind.MIXED_GROUP));
        }

        if (visit != null) {
            prefs.addRecentResource(visit);
        }
    }
}
