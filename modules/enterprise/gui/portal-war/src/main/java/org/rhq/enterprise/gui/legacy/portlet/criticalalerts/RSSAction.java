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

import java.text.SimpleDateFormat;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import org.rhq.core.clientapi.util.units.DateFormatter.DateSpecifics;
import org.rhq.core.clientapi.util.units.FormattedNumber;
import org.rhq.core.clientapi.util.units.ScaleConstants;
import org.rhq.core.clientapi.util.units.UnitNumber;
import org.rhq.core.clientapi.util.units.UnitsConstants;
import org.rhq.core.clientapi.util.units.UnitsFormat;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.portlet.BaseRSSAction;
import org.rhq.enterprise.gui.legacy.portlet.RSSFeed;
import org.rhq.enterprise.gui.legacy.util.DashboardUtils;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RSSAction extends BaseRSSAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        RSSFeed feed = getNewRSSFeed(request);

        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.CriticalAlerts"));

        // Get the alerts
        Subject subject = getSubject(request);
        WebUser webUser = new WebUser(subject);

        int count = Integer.parseInt(webUser.getPreference(".dashContent.criticalalerts.numberOfAlerts"));
        int priority = Integer.parseInt(webUser.getPreference(".dashContent.criticalalerts.priority"));
        long timeRange = Long.parseLong(webUser.getPreference(".dashContent.criticalalerts.past"));
        boolean all = "all".equals(webUser.getPreference(".dashContent.criticalalerts.selectedOrAll"));

        Integer[] resourceIds = null;
        if (all == false) {
            resourceIds = DashboardUtils.preferencesAsResourceIds(".dashContent.criticalalerts.resources", webUser);
        }

        PageControl pageControl = new PageControl(0, count);
        AlertManagerLocal alertManager = LookupUtil.getAlertManager();
        PageList<Alert> alerts = alertManager.findAlerts(webUser.getSubject(), resourceIds, AlertPriority
            .getByLegacyIndex(priority), timeRange, pageControl);

        if ((alerts != null) && (alerts.size() > 0)) {
            for (Alert alert : alerts) {
                String link = feed.getBaseUrl() + "/alerts/Alerts.do?mode=viewAlert&id="
                    + alert.getAlertDefinition().getResource().getId() + "&a=" + alert.getId();

                DateSpecifics specs = new DateSpecifics();
                specs.setDateFormat(new SimpleDateFormat(res.getMessage(Constants.UNIT_FORMAT_PREFIX_KEY
                    + "epoch-millis")));

                FormattedNumber fmtd = UnitsFormat.format(new UnitNumber(alert.getCtime(), UnitsConstants.UNIT_DATE,
                    ScaleConstants.SCALE_MILLI), request.getLocale(), specs);

                feed.addItem(alert.getAlertDefinition().getResource().getName() + " "
                    + alert.getAlertDefinition().getName(), link, fmtd.toString(), alert.getCtime());
            }
        }

        request.setAttribute("rssFeed", feed);

        return mapping.findForward(Constants.RSS_URL);
    }
}