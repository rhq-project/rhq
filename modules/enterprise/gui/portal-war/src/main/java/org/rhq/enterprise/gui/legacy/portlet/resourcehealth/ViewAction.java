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
package org.rhq.enterprise.gui.legacy.portlet.resourcehealth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUser user = SessionUtils.getWebUser(request.getSession());
        WebUserPreferences preferences = user.getPreferences();
        PageControl pc = WebUtility.getPageControl(request);
        String key = Constants.USERPREF_KEY_FAVORITE_RESOURCES;

        PageList<ResourceHealthComposite> list;
        try {
            list = getStuff(key, user, pc);
        } catch (Exception e) {
            DashboardUtils.verifyResources(key, user);
            list = getStuff(key, user, pc);
        }

        context.putAttribute("resourceHealth", list);

        Boolean availability = new Boolean(preferences.getPreference(".dashContent.resourcehealth.availability"));
        Boolean alerts = new Boolean(preferences.getPreference(".dashContent.resourcehealth.alerts"));

        context.putAttribute("availability", availability);
        context.putAttribute("alerts", alerts);

        return null;
    }

    private PageList<ResourceHealthComposite> getStuff(String key, WebUser user, PageControl pc) throws Exception {
        Integer[] resources = DashboardUtils.preferencesAsResourceIds(key, user);
        ResourceManagerLocal manager = LookupUtil.getResourceManager();
        PageList<ResourceHealthComposite> results = manager.getResourceHealth(user.getSubject(), resources, pc);
        return results;
    }
}