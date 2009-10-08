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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;

/**
 * Action class to edit the baseline for a metric associated with an Alert Definition
 */
public class EditMetricBaselineAction extends BaseAction {
    private Log log = LogFactory.getLog(EditMetricBaselineAction.class);

    // ---------------------------------------------------- Public Methods

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        EditMetricBaselineForm metricBaselineForm = (EditMetricBaselineForm) form;

        Integer metricId = metricBaselineForm.getM();
        Integer alertDefId = metricBaselineForm.getAd();
        String oldMode = metricBaselineForm.getOldMode();
        String mname = metricBaselineForm.getMetricName();
        Integer rid = metricBaselineForm.getRid();
        //        Integer type = metricBaselineForm.getType();

        log.debug("in edit metric baseline actions ...");
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.RESOURCE_PARAM, rid);
        //        params.put(Constants.RESOURCE_TYPE_ID_PARAM,type);
        params.put(Constants.ALERT_DEFINITION_PARAM, alertDefId);
        params.put(Constants.METRIC_ID_PARAM, metricId);
        params.put("oldMode", oldMode);
        params.put(Constants.METRIC_NAME_PARAM, mname);

        ActionForward forward = checkSubmit(request, mapping, form, params);
        if (forward != null) {
            BaseValidatorForm spiderForm = metricBaselineForm;
            if (spiderForm.isCancelClicked()) {
                params.put("mode", oldMode);
                params.remove("oldMode");
                forward = checkSubmit(request, mapping, form, params);
            }

            return forward;
        }

        if (RequestUtils.getStringParameter(request, "recalc").equals("y")) {
            // ok clicked
            ServletContext ctx = getServlet().getServletContext();
            //            BaselineBoss boss= ContextUtils.getBaselineBoss(ctx);
            Integer sessionId = RequestUtils.getSessionId(request);

            //XXX call the recalculate baseline from the BaselineBoss
            //based on the radio button value
            log.debug("START = " + metricBaselineForm.getStartDate().getTime() + "END = "
                + metricBaselineForm.getEndDate().getTime() + "Metric id is " + metricId);

            //            MeasurementBaseline value = boss.recalculateBaseline(sessionId,metricId,
            //                  metricBaselineForm.getStartDate().getTime(),
            //                  metricBaselineForm.getEndDate().getTime() );
            //            request.setAttribute("BaselineValue",value);
            return returnReset(request, mapping, params);
        }

        log.debug("returning success");
        return returnSuccess(request, mapping, params);
    }
}

// EOF
