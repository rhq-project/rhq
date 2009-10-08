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

/**
 * Forward to chart page for a designated metric.
 *
 * @deprecated the functionality in this class will be merged into ViewChartFormPrepareAction (ips, 04/04/07)
 */
@Deprecated
public class ViewDesignatedChartAction extends MetricDisplayRangeAction {
    protected final Log log = LogFactory.getLog(ViewDesignatedChartAction.class);

    /**
     * Modify the metric chart as specified in the given <code>@{link ViewActionForm}</code>.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        return null;
        //        Map forwardParams = new HashMap(4);
        //        Integer resourceId = RequestUtils.getResourceId(request);
        //        forwardParams.put(Constants.RESOURCE_ID_PARAM, resourceId);
        //
        //        // TODO (ips): Refactor the below code.
        //        MeasurementBoss boss =
        //            ContextUtils.getMeasurementBoss(getServlet().getServletContext());
        //        int sessionId  = RequestUtils.getSessionId(request).intValue();
        //        MeasurementTempl mtv;
        //        try {
        //            forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM,
        //                              WebUtility.getChildResourceTypeId(request));
        //            forwardParams.put(Constants.MODE_PARAM,
        //                              Constants.MODE_MON_CHART_SMMR);
        //
        //            // Now we have to look up the designated metric template ID
        //            mtv = boss.getAvailabilityMetricTemplate(sessionId, null, null);
        //        } catch (ParameterNotFoundException e) {
        //            forwardParams.put(Constants.MODE_PARAM,
        //                              Constants.MODE_MON_CHART_SMSR);
        //            // Now we have to look up the designated metric template ID
        //            mtv = boss.getAvailabilityMetricTemplate(sessionId, null);
        //        }
        //
        //        forwardParams.put(Constants.METRIC_ID_PARAM, mtv.getId());
        //
        //        return constructForward(request, mapping, Constants.REDRAW_URL,
        //                                forwardParams, false);
    }
}