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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourceHealthComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.FavoriteResourcePortletPreferences;
import org.rhq.enterprise.gui.legacy.util.DisambiguatedResourceListUtil;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    static final IntExtractor<ResourceHealthComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<ResourceHealthComposite>() {

        public int extract(ResourceHealthComposite object) {
            return object.getId();
        }
    };
    
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        PageList<DisambiguationReport<ResourceHealthComposite>> list = new PageList<DisambiguationReport<ResourceHealthComposite>>();
        boolean showAvailability = true;
        boolean showAlerts = true;
        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }

            WebUserPreferences preferences = user.getWebPreferences();
            PageControl pc = WebUtility.getPageControl(request);

            FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
                .getFavoriteResourcePortletPreferences();

            ResourceManagerLocal manager = LookupUtil.getResourceManager();
            PageList<ResourceHealthComposite> lst = manager.findResourceHealth(user.getSubject(), favoriteResourcePreferences.asArray(), pc);
            
            list = DisambiguatedResourceListUtil.disambiguate(manager, lst, RESOURCE_ID_EXTRACTOR);
            
            showAvailability = favoriteResourcePreferences.showAvailability;
            showAlerts = favoriteResourcePreferences.showAlerts;
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [ResourceHealth] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [ResourceHealth] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("resourceHealth", list);
            context.putAttribute("availability", showAvailability);
            context.putAttribute("alerts", showAlerts);
        }

        return null;
    }

}