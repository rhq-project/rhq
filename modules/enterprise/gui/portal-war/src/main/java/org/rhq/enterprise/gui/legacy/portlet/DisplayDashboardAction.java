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
package org.rhq.enterprise.gui.legacy.portlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.Portal;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.DashboardPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.uibeans.UIConstants;

public class DisplayDashboardAction extends TilesAction {

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);
        WebUserPreferences preferences = user.getPreferences();
        DashboardPreferences dashboardPreferences = preferences.getDashboardPreferences();

        int refreshPeriod = preferences.getPageRefreshPeriod();
        if (UIConstants.DONT_REFRESH_PAGE != refreshPeriod) {
            request.setAttribute("refreshPeriod", String.valueOf(refreshPeriod));
        }

        Portal portal = (Portal) session.getAttribute(Constants.USERS_SES_PORTAL);
        if (portal == null) {
            portal = new Portal();
            portal.setName("dashboard.template.title");
            portal.setColumns(2);

            //construct from user preferences.
            portal.addPortletsFromString(dashboardPreferences.leftColumnPortletNames, 1);
            portal.addPortletsFromString(".dashContent.addContent.narrow", 1);

            portal.addPortletsFromString(dashboardPreferences.rightColumnPortletNames, 2);
            portal.addPortletsFromString(".dashContent.addContent.wide", 2);

            session.setAttribute(Constants.USERS_SES_PORTAL, portal);
        }

        request.setAttribute(Constants.PORTAL_KEY, portal);
        return null;
    }
}