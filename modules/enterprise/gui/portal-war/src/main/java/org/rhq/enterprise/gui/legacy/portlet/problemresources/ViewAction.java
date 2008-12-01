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
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ViewAction extends TilesAction implements PortletConstants {
    private static final Log LOG = LogFactory.getLog(ViewAction.class.getName());

    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<ProblemResourceComposite> list = null;

        try {
            WebUser user = SessionUtils.getWebUser(request.getSession());
            Subject subject = user.getSubject();
            WebUserPreferences preferences = user.getPreferences();

            int hours = 8;
            int rows = 10;
            try {
                rows = Integer.parseInt(preferences.getPreference(ROWS));
                hours = Integer.parseInt(preferences.getPreference(HOURS));
            } catch (NumberFormatException e) {
                preferences.setPreference(ROWS, String.valueOf(rows));
                preferences.setPreference(HOURS, String.valueOf(hours));
                preferences.persistPreferences();
            }

            String timeRange;
            long begin = 0;

            if (hours > 0) {
                List bounds = MonitorUtils.calculateTimeFrame(hours, MonitorUtils.UNIT_HOURS);
                begin = (Long) bounds.get(0);
                long end = (Long) bounds.get(1);

                SimpleDateFormat formatter = new SimpleDateFormat("MMM d, hh:mm a");

                timeRange = getResources(request).getMessage("dash.home.ProblemResources.timeRange",
                    new String[] { formatter.format(new Date(begin)), formatter.format(new Date(end)) });
            } else {
                timeRange = getResources(request).getMessage("dash.home.ProblemResources.timeRangeUnlimited");
            }

            request.setAttribute("timeRange", timeRange);

            try {
                MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
                long start = System.currentTimeMillis();
                list = problemManager.findProblemResources(subject, begin, rows);
                long end = System.currentTimeMillis();
                LOG.debug("Performance: Took [" + (end - start) + "]ms to find " + rows + " problem resources");
            } catch (Exception e) {
                throw new ServletException("Error finding problem resources", e);
            }
        } catch (Exception e) {
            LOG.warn("Cannot prepare the problem resources portlet", e);
            throw new ServletException("Cannot prepare the problem resources portlet", e);
        } finally {
            if (list == null) {
                list = new ArrayList<ProblemResourceComposite>();
            }

            context.putAttribute("problemResources", list);
        }

        return null;
    }
}