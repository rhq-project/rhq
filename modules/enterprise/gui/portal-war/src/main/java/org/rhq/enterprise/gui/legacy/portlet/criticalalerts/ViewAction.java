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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.AlertsPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.DisambiguatedResourceListUtil;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    static final IntExtractor<Alert> RESOURCE_ID_EXTRACTOR = new IntExtractor<Alert>() {
        public int extract(Alert object) {
            return object.getAlertDefinition().getResource().getId();
        }
    };

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        PageList<DisambiguationReport<Alert>> disambiguatedAlerts = new PageList<DisambiguationReport<Alert>>();
        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }

            WebUserPreferences preferences = user.getWebPreferences();

            // get the user's preferences for the alerts portlet
            AlertsPortletPreferences alertPrefs = preferences.getAlertsPortletPreferences();

            PageControl pageControl = new PageControl(0, alertPrefs.count);
            AlertManagerLocal alertManager = LookupUtil.getAlertManager();
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();

            AlertCriteria criteria = new AlertCriteria();
            criteria.addFilterPriorities(AlertPriority.getByLegacyIndex(alertPrefs.priority));
            criteria.addFilterStartTime(alertPrefs.timeRange);
            criteria.addFilterResourceIds(("all".equals(alertPrefs.displayAll) ? null : ArrayUtils
                .wrapInArray(alertPrefs.asArray())));
            criteria.setPageControl(pageControl);

            PageList<Alert> alerts = alertManager.findAlertsByCriteria(user.getSubject(), criteria);

            disambiguatedAlerts = DisambiguatedResourceListUtil.disambiguate(resourceManager, alerts,
                RESOURCE_ID_EXTRACTOR);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [CriticalAlerts] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [CriticalAlerts] experienced an error: " + e.getMessage());
            }
        } finally {
            context.putAttribute("criticalAlerts", disambiguatedAlerts);
        }

        return null;
    }
}