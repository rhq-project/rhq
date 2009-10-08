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

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.util.ChartData;

/**
 * An <code>Action</code> that prepares pages containing the performance form.
 */
public class UrlPerformanceFormPrepareAction extends PerformanceFormPrepareAction {
    protected static Log log = LogFactory.getLog(UrlPerformanceFormPrepareAction.class.getName());

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve data needed to display the detail for a url. Respond to certain button clicks that alter the form
     * display.
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        //        HttpSession session = request.getSession();
        //
        //        PerformanceForm perfForm = (PerformanceForm) form;
        //
        //        AppdefResourceValue resource = RequestUtils.getHqResource(request);
        //        if (resource == null) {
        //            return null;
        //        }
        //        AppdefEntityID entityId = resource.getEntityId();
        //
        //        String url = RequestUtils.getUrl(request);
        //        request.setAttribute(Constants.URL_ATTR, url);
        //
        //        PageControl pc = RequestUtils.getPageControl(request);
        //        int sessionId = RequestUtils.getSessionId(request).intValue();
        //        ServletContext ctx = getServlet().getServletContext();
        //        RtBoss boss = ContextUtils.getRtBoss(ctx);
        //
        //        // decide what timeframe we're showing. it may have been
        //        // shifted on previous views of this page.
        //        MetricRange range = (MetricRange)
        //            request.getAttribute(Constants.METRIC_RANGE);
        //        if (range == null) {
        //            // this is the first time out. get the "metric range"
        //            // user pref.
        //            WebUser user = SessionUtils.getWebUser(session);
        //            Map pref = user.getMetricRangePreference();
        //
        //            range = new MetricRange();
        //            range.setBegin((Long) pref.get(MonitorUtils.BEGIN));
        //            range.setEnd((Long) pref.get(MonitorUtils.END));
        //        }
        //
        //        log.trace("finding performance for url [" + url + "] in resource" +
        //                  " [" + entityId + "] " + "in range " +
        //                  range.getFormattedRange());
        //        Map perfs =
        //            boss.getSegmentedUrlPerformance(sessionId, entityId, url,
        //                                            range.getBegin().longValue(),
        //                                            range.getEnd().longValue(),
        //                                            pc);
        //        request.setAttribute(Constants.PERF_SUMMARIES_ATTR, perfs);
        //
        //        // prepare form
        //        prepareForm(request, perfForm, range);
        //        perfForm.addUrl(url);
        //
        //        // save chart data
        //        ChartData data = getChartData(perfs, perfForm);
        //        session.setAttribute(Constants.CHART_DATA_SES_ATTR, data);
        //
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return null;
    }

    protected ChartData getChartData(Map all, PerformanceForm form) {
        ChartData data = new ChartData();
        data.setSegments(all);
        data.setShowLow(form.getLow());
        data.setShowAvg(form.getAvg());
        data.setShowPeak(form.getPeak());
        return data;
    }
}