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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.RecentlyApprovedPortletPreferences;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class ProcessRAList extends BaseAction {

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getWebPreferences();
        RecentlyApprovedPortletPreferences recentlyApprovedPreferences = preferences
            .getRecentlyApprovedPortletPreferences();

        RAListForm listForm = (RAListForm) form;
        String platformId = new Integer(listForm.getPlatformId()).toString();

        List<String> expandedPlatforms = recentlyApprovedPreferences.expandedPlatforms;
        if (expandedPlatforms.contains(platformId)) {
            expandedPlatforms.remove(platformId);
        } else {
            expandedPlatforms.add(platformId);
        }
        preferences.setRecentlyApprovedPortletPreferences(recentlyApprovedPreferences);

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }
}