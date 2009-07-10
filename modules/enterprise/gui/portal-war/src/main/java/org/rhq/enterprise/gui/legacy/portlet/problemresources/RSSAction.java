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

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.gui.common.tag.FunctionTagLibrary;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.ProblemResourcesPortletPreferences;
import org.rhq.enterprise.gui.legacy.portlet.BaseRSSAction;
import org.rhq.enterprise.gui.legacy.portlet.RSSFeed;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
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
        WebUser user = getWebUser(request);
        if (user != null) {

            ProblemResourcesPortletPreferences preferences = user.getWebPreferences()
                .getProblemResourcesPortletPreferences();

            long begin = 0; // beginning of time, unless configured otherwise

            if (preferences.hours > 0) {
                List<Long> bounds = MeasurementUtils.calculateTimeFrame(preferences.hours, MeasurementUtils.UNIT_HOURS);
                begin = (Long) bounds.get(0);
            }

            MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
            List<ProblemResourceComposite> results;
            results = problemManager.findProblemResources(user.getSubject(), begin, new PageControl(0,
                preferences.range));

            if ((results != null) && (results.size() > 0)) {
                for (ProblemResourceComposite problem : results) {
                    String link = feed.getBaseUrl() + FunctionTagLibrary.getDefaultResourceTabURL() + "?id="
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
        } else {
            throw new LoginException("RSS access requires authentication");
        }

    }
}