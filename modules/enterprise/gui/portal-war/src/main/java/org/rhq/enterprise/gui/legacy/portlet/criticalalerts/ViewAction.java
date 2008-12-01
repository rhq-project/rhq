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
package org.rhq.enterprise.gui.legacy.portlet.criticalalerts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {
    @Override
    @SuppressWarnings( { "unchecked", "deprecation" })
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        // get the logged in user
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();

        // get the user's preferences for the alerts portlet
        int count = new Integer(preferences.getPreference(".dashContent.criticalalerts.numberOfAlerts")).intValue();
        int priority = new Integer(preferences.getPreference(".dashContent.criticalalerts.priority")).intValue();
        long timeRange = new Long(preferences.getPreference(".dashContent.criticalalerts.past")).longValue();
        boolean all = "all".equals(preferences.getPreference(".dashContent.criticalalerts.selectedOrAll"));

        Integer[] resourceIds = null;
        if (all == false) {
            resourceIds = DashboardUtils.preferencesAsResourceIds(".dashContent.criticalalerts.resources", user);
        }

        PageControl pageControl = new PageControl(0, count);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        PageList<Alert> alerts = alertManager.findAlerts(user.getSubject(), resourceIds, AlertPriority
            .getByLegacyIndex(priority), timeRange, pageControl);

        context.putAttribute("criticalAlerts", alerts);

        return null;
    }
}