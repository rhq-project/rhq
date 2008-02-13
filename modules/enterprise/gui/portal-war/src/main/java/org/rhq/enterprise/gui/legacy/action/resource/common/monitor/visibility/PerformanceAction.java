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

import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.util.ActionUtils;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * A <code>BaseAction</code> that handles performance form submissions.
 */
public class PerformanceAction extends MetricsControlAction {
    protected static Log log = LogFactory.getLog(PerformanceAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Modify the metrics summary display as specified in the given <code>PerformanceForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        PerformanceForm perfForm = (PerformanceForm) form;
        Map forwardParams = new HashMap(2);

        Integer resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        if (resourceId != -1) {
            forwardParams.put(ParamConstants.RESOURCE_ID_PARAM, resourceId);
        }

        if (perfForm.isNextClicked()) {
            long newRb = perfForm.getRe();
            long diff = newRb - perfForm.getRb();
            long newRe = newRb + diff;

            MetricRange range = new MetricRange(newRb, newRe);
            range.shiftNow();
            request.setAttribute(ParamConstants.METRIC_RANGE, range);
        } else if (perfForm.isPrevClicked()) {
            long newRe = perfForm.getRb();
            long diff = perfForm.getRe() - newRe;
            long newRb = newRe - diff;

            MetricRange range = new MetricRange(newRb, newRe);
            range.shiftNow();
            request.setAttribute(ParamConstants.METRIC_RANGE, range);
        } else if (perfForm.isChartClicked()) {
            forwardParams.put(ParamConstants.URL_PARAM, perfForm.getUrl());
            return returnChart(request, mapping, forwardParams);
        }

        return super.execute(mapping, form, request, response);
    }

    // ---------------------------------------------------- Private Methods

    private ActionForward returnChart(HttpServletRequest request, ActionMapping mapping, Map params) throws Exception {
        // set return path
        String returnPath = ActionUtils.findReturnPath(mapping, params);
        SessionUtils.setReturnPath(request.getSession(), returnPath);

        return constructForward(request, mapping, Constants.CHART_URL, params, NO_RETURN_PATH);
    }
}