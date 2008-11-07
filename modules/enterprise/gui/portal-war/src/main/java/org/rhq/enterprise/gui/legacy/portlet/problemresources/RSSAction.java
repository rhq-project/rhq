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
package org.rhq.enterprise.gui.legacy.portlet.problemresources;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.portlet.BaseRSSAction;
import org.rhq.enterprise.gui.legacy.portlet.RSSFeed;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class RSSAction extends BaseRSSAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        RSSFeed feed = getNewRSSFeed(request);

        // Set title
        MessageResources res = getResources(request);
        feed.setTitle(res.getMessage("dash.home.ProblemResources"));

        // Get the problem resources
        Subject subject = getSubject(request);
        WebUser webUser = new WebUser(subject);

        int rows = Integer.parseInt(webUser.getPreference(PortletConstants.ROWS));
        int hours = Integer.parseInt(webUser.getPreference(PortletConstants.HOURS));
        long begin = 0; // beginning of time, unless configured otherwise

        if (hours > 0) {
            List bounds = MonitorUtils.calculateTimeFrame(hours, MonitorUtils.UNIT_HOURS);
            begin = (Long) bounds.get(0);
        }

        MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
        List<ProblemResourceComposite> results;
        results = problemManager.findProblemResources(subject, begin, rows);

        if ((results != null) && (results.size() > 0)) {
            for (ProblemResourceComposite problem : results) {
                String link = feed.getBaseUrl() + "/resource/common/monitor/Visibility.do?mode=currentHealth&id="
                    + problem.getResourceId();

                String availText = "";
                if (problem.getAvailabilityType() != null) {
                    if (problem.getAvailabilityType() == AvailabilityType.DOWN) {
                        availText = res.getMessage("dash.home.ProblemResources.rss.item.downAvail");
                    } else if (problem.getAvailabilityType() == AvailabilityType.UP) {
                        availText = res.getMessage("dash.home.ProblemResources.rss.item.upAvail");
                    } else {
                        throw new IllegalStateException("invalid availability type - please report this bug");
                    }
                }

                feed.addItem(problem.getResourceName(), link, res.getMessage(
                    "dash.home.ProblemResources.rss.item.description", availText, problem.getNumAlerts()), System
                    .currentTimeMillis());
            }
        }

        request.setAttribute("rssFeed", feed);

        return mapping.findForward(Constants.RSS_URL);
    }
}