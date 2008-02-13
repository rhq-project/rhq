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
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.resource.InventorySummary;
import org.rhq.enterprise.server.resource.ResourceBossLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        ResourceBossLocal resourceBoss = LookupUtil.getResourceBoss();
        InventorySummary summary = resourceBoss.getInventorySummary(user.getSubject());
        context.putAttribute("summary", summary);

        //get all the displayed subtypes
        context.putAttribute("platform", Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.platform")));
        context.putAttribute("server", Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.server")));
        context.putAttribute("service", Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.service")));
        context.putAttribute("software", Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.software")));
        context.putAttribute("groupCompat", Boolean.valueOf(user
            .getPreference(".dashContent.summaryCounts.group.compat")));
        context.putAttribute("groupMixed", Boolean
            .valueOf(user.getPreference(".dashContent.summaryCounts.group.mixed")));
        return null;
    }
}