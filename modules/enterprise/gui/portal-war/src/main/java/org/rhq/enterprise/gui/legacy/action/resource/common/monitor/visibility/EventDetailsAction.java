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

import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.action.BaseAction;

/**
 * Set an array for the timeline display
 */
public class EventDetailsAction extends BaseAction {
    /* (non-Javadoc)
     * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping,
     * org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        //        WebUser user = (WebUser) request.getSession().getAttribute(
        //                AttrConstants.WEBUSER_SES_ATTR);
        //        Map range = user.getMetricRangePreference();
        //        long begin = (Long) range.get(MonitorUtils.BEGIN);
        //        long end = (Long) range.get(MonitorUtils.END);
        //        long interval = TimeUtil.getInterval(begin, end,
        //                DefaultConstants.DEFAULT_CHART_POINTS);
        //
        //        begin =
        //            Long.parseLong(RequestUtils.getStringParameter(request, "begin"));
        //
        //        AppdefEntityID aeid = RequestUtils.getEntityId(request);
        //
        //        ServletContext ctx = getServlet().getServletContext();
        //        EventsBoss boss = ContextUtils.getEventsBoss(ctx);
        //        int sessionId = user.getSessionId();
        //
        //        List<EventLog> eventLogs;
        //        try {
        //            String status = RequestUtils.getStringParameter(request, "status");
        //
        //            // Control logs are different, they store their return status
        //            // So we have to look it up by the type
        //            if (status.equals("CTL"))
        //                eventLogs = boss.getEvents(sessionId, ControlEvent.class.getName(),
        //                                        aeid, begin, begin + interval);
        //            else
        //                eventLogs = boss.getEvents(sessionId, aeid, status,
        //                                        begin, begin + interval);
        //        } catch (ParameterNotFoundException e) {
        //            String[] types = null;
        //            eventLogs = boss.getEvents(user.getSessionId(), aeid, types,
        //                                    begin, begin + interval);
        //        }
        //
        //        MessageResources res = getResources(request);
        //        String formatString = res.getMessage(
        //                StringConstants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis");
        //        DateFormatter.DateSpecifics dateSpecs;
        //
        //        dateSpecs = new DateFormatter.DateSpecifics();
        //        dateSpecs.setDateFormat(new SimpleDateFormat(formatString));
        //
        //        StringBuffer html;
        //        if (eventLogs.isEmpty()) {
        //            html = new StringBuffer(
        //                res.getMessage("resource.common.monitor.text.events.None"));
        //        }
        //        else {
        //            html = new StringBuffer("<ul class=\"boxy\">");
        //
        //           for (EventLog eventLog : eventLogs)
        //           {
        //              html.append("<li ");
        //
        //              String status = eventLog.getStatus();
        //              if (status.equals("EMR") ||
        //                status.equals("ALR") ||
        //                status.equals("CRT") ||
        //                status.equals("ERR"))
        //              {
        //                 html.append("class=\"red\"");
        //              }
        //              else if (status.equals("WRN"))
        //              {
        //                 html.append("class=\"yellow\"");
        //              }
        //              else if (status.equals("NTC") ||
        //                status.equals("INF") ||
        //                status.equals("DBG"))
        //              {
        //                 html.append("class=\"green\"");
        //              }
        //              else
        //              {
        //                 html.append("class=\"navy\"");
        //              }
        //
        //              html.append('>');
        //
        //              FormattedNumber fmtd =
        //                UnitsFormat.format(new UnitNumber(eventLog.getTimestamp(),
        //                  UnitsConstants.UNIT_DATE,
        //                  ScaleConstants.SCALE_MILLI),
        //                  request.getLocale(), dateSpecs);
        //
        //              html.append(res.getMessage(eventLog.getType(), fmtd.toString(),
        //                ridBadChars(eventLog.getDetail()), eventLog.getSubject(),
        //                eventLog.getStatus()));
        //
        //              html.append("</li>");
        //           }
        //
        //           html.append("</ul>");
        //        }
        //
        //        request.setAttribute(AttrConstants.AJAX_TYPE, StringConstants.AJAX_ELEMENT);
        //        request.setAttribute(AttrConstants.AJAX_ID, "eventsSummary");
        //        request.setAttribute(AttrConstants.AJAX_HTML, html);
        if (true) {
            throw new IllegalStateException("deprecated code");
        }

        return mapping.findForward(RetCodeConstants.SUCCESS_URL);
    }

    // In our Javascript, we are enclosing the whole string in single-quotes.
    // However, just escaping it does not seem to work because we are setting
    // the innerHTML.  So, just to be safe, we're getting rid of all single
    // quotes, double quotes, whitespace characters, and carat
    private String ridBadChars(String source) {
        int sourceLen = source.length();
        if (sourceLen == 0) {
            return source;
        }

        StringTokenizer st = new StringTokenizer(source);
        StringBuffer buffer = new StringBuffer();
        while (st.hasMoreElements()) {
            String tok = st.nextToken();
            tok = tok.replaceAll("['\"]", " ");

            if (tok.indexOf('<') > -1) {
                tok = StringUtil.replace(tok, "<", "&lt;");
            }

            buffer.append(tok).append(" ");
        }

        return buffer.toString();
    }
}