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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

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
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * A portlet for problem metrics
 */
public class ProblemMetricsDisplayAction extends TilesAction {
    private static Log log = LogFactory.getLog(ProblemMetricsDisplayAction.class.getName());

    // ---------------------------------------------------- Public
    // ---------------------------------------------------- Methods

    /**
     * Fetch the list of problem metrics for a resource
     */
    @Override
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        WebUser user = (WebUser) request.getSession().getAttribute(Constants.WEBUSER_SES_ATTR);
        MeasurementPreferences preferences = user.getMeasurementPreferences();

        Subject subject = RequestUtils.getSubject(request);

        MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();

        // Now fetch the display range
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        long beginTime = rangePreferences.begin;
        long endTime = rangePreferences.end;

        ProblemMetricsDisplayForm probForm = (ProblemMetricsDisplayForm) form;

        //      List<MeasurementProblem> problems;
        //      problems = problemManager.findOngoingProblemMeasurements(subject, 0, PageControl.getUnlimitedInstance());
        //      log.info("ProblemMetricsDisplay:  found " + problems.size() + " problem resources");
        //
        //      boolean allMetrics =
        //            probForm.getShowType() == ProblemMetricsDisplayForm.TYPE_ALL;
        //
        //        if (probForm.getCtype() != null && probForm.getCtype().length() > 0) {
        //            // Autogroup
        //            AppdefEntityTypeID childTypeID =
        //                new AppdefEntityTypeID(probForm.getCtype());
        //
        //            if (probForm.getHost().length > 0) {
        //                // Host selected, make ctype the children array
        //                AppdefEntityTypeID[] children =
        //                    new AppdefEntityTypeID[] { childTypeID };
        //
        //                problems = boss.findProblemMetrics(
        //                    sessionId, aeid, null, children, begin, end, allMetrics);
        //            }
        //            else {
        //                problems = boss.findProblemMetrics(
        //                    sessionId, aeid, childTypeID, begin, end, allMetrics);
        //            }
        //        }
        //        else {
        //            String[] resStrs;
        //
        //            AppdefEntityID[] hosts = null;
        //            if ((resStrs = probForm.getHost()).length > 0) {
        //                hosts = new AppdefEntityID[resStrs.length];
        //                for (int i = 0; i < resStrs.length; i++)
        //                    hosts[i] = new AppdefEntityID(resStrs[i]);
        //            }
        //
        //            AppdefEntityTypeID[] children= null;
        //            if ((resStrs = probForm.getChild()).length > 0) {
        //                children = new AppdefEntityTypeID[resStrs.length];
        //                for (int i = 0; i < resStrs.length; i++)
        //                    children[i] = new AppdefEntityTypeID(resStrs[i]);
        //            }
        //
        //            problems = boss.findProblemMetrics(
        //                sessionId, aeid, hosts, children, begin, end, allMetrics);
        //        }
        //
        //      context.putAttribute("problems", problems);
        return null;
    }
}