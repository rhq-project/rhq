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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.WebUserPreferences.ProblemResourcesPortletPreferences;
import org.rhq.enterprise.gui.legacy.util.DisambiguatedResourceListUtil;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.util.MeasurementUtils;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction {

    private static final Log log = LogFactory.getLog(ViewAction.class);

    private static final IntExtractor<ProblemResourceComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<ProblemResourceComposite>() {
        public int extract(ProblemResourceComposite object) {
            return object.getResourceId();
        }
    };
    
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {

        List<DisambiguationReport<ProblemResourceComposite>> disambiguatedList = new ArrayList<DisambiguationReport<ProblemResourceComposite>>();

        String timeRange = getResources(request).getMessage("dash.home.ProblemResources.timeRangeUnlimited");
        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            if (user == null) {
                // session timed out, return prematurely
                return null;
            }

            Subject subject = user.getSubject();
            WebUserPreferences preferences = user.getWebPreferences();

            ProblemResourcesPortletPreferences problemResourcePreferences = preferences
                .getProblemResourcesPortletPreferences();

            long begin = 0;

            if (problemResourcePreferences.hours > 0) {
                List<Long> bounds = MeasurementUtils.calculateTimeFrame(problemResourcePreferences.hours,
                    MeasurementUtils.UNIT_HOURS);
                begin = bounds.get(0);
                long end = bounds.get(1);

                SimpleDateFormat formatter = new SimpleDateFormat("MMM d, hh:mm a");

                timeRange = getResources(request).getMessage("dash.home.ProblemResources.timeRange",
                    new String[] { formatter.format(new Date(begin)), formatter.format(new Date(end)) });
            } else {
                timeRange = getResources(request).getMessage("dash.home.ProblemResources.timeRangeUnlimited");
            }

            try {
                MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
                ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
                long start = System.currentTimeMillis();
                PageList<ProblemResourceComposite> list = problemManager.findProblemResources(subject, begin, new PageControl(0,
                    problemResourcePreferences.range));
                
                disambiguatedList = DisambiguatedResourceListUtil.disambiguate(
                    resourceManager, list, RESOURCE_ID_EXTRACTOR);
                
                long end = System.currentTimeMillis();
                log.debug("Performance: Took [" + (end - start) + "]ms to find " + problemResourcePreferences.range
                    + " problem resources");
            } catch (Exception e) {
                throw new ServletException("Error finding problem resources", e);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Dashboard Portlet [ProblemResources] experienced an error: " + e.getMessage(), e);
            } else {
                log.error("Dashboard Portlet [ProblemResources] experienced an error: " + e.getMessage());
            }
        } finally {
            request.setAttribute("timeRange", timeRange);
            context.putAttribute("problemResources", disambiguatedList);
        }

        return null;
    }
}