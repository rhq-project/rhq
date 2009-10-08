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
package org.rhq.enterprise.gui.legacy.portlet.summaryCounts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.SummaryCountPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.resource.InventorySummary;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        SummaryCountPortletPreferences counts = new SummaryCountPortletPreferences(); // all defaults are false
        InventorySummary summary = new InventorySummary();
        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }
            WebUserPreferences preferences = user.getWebPreferences();

            ResourceBossLocal resourceBoss = LookupUtil.getResourceBoss();
            summary = resourceBoss.getInventorySummary(user.getSubject());
            counts = preferences.getSummaryCounts();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [SummaryCounts] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [SummaryCounts] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("summary", summary);
            context.putAttribute("platform", Boolean.valueOf(counts.showPlatforms));
            context.putAttribute("server", Boolean.valueOf(counts.showServers));
            context.putAttribute("service", Boolean.valueOf(counts.showServices));
            context.putAttribute("groupCompat", Boolean.valueOf(counts.showCompatibleGroups));
            context.putAttribute("groupMixed", Boolean.valueOf(counts.showMixedGroups));
            context.putAttribute("groupDefinition", Boolean.valueOf(counts.showGroupDefinitions));
        }

        return null;
    }
}