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
package org.rhq.enterprise.gui.legacy.action.resource.common.events;

import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;

import org.rhq.core.clientapi.util.StringUtil;
import org.rhq.core.clientapi.util.TimeUtil;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.RetCodeConstants;
import org.rhq.enterprise.gui.legacy.StringConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Set an array for the timeline display in EventLogs.jsp, showEventDetails().
 * The JavaScript function showEventDetails() is actually defined in Indicators.jsp
 * 
 * @author Heiko W. Rupp
 */
public class EventDetailsAction extends BaseAction {

    Log log = LogFactory.getLog(EventDetailsAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        WebUser user = (WebUser) request.getSession().getAttribute(AttrConstants.WEBUSER_SES_ATTR);
        Map range = user.getMetricRangePreference();
        long begin = (Long) range.get(MonitorUtils.BEGIN);
        long end = (Long) range.get(MonitorUtils.END);
        long interval = TimeUtil.getInterval(begin, end, DefaultConstants.DEFAULT_CHART_POINTS);

        begin = Long.parseLong(WebUtility.getOptionalRequestParameter(request, "begin", "0"));

        int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);

        EventManagerLocal eventManager = LookupUtil.getEventManager();
        List<EventComposite> events;

        Subject subject = user.getSubject();
        if (resourceId > -1) {
            events = eventManager.getEventsForResource(subject, resourceId, begin, begin + interval, null);
        } else if (groupId > -1) {
            events = eventManager.getEventsForCompGroup(subject, groupId, begin, begin + interval, null);
        } else if (parent > -1 && type > -1) {
            events = eventManager.getEventsForAutoGroup(subject, parent, type, begin, begin + interval, null);
        } else {
            log.error("Unknown input combination, can't compute events for input");
            return null;
        }

        MessageResources res = getResources(request);
        //        String formatString = res.getMessage(
        //                StringConstants.UNIT_FORMAT_PREFIX_KEY + "epoch-millis");
        //        DateFormatter.DateSpecifics dateSpecs;
        //
        //        dateSpecs = new DateFormatter.DateSpecifics();
        //        dateSpecs.setDateFormat(new SimpleDateFormat(formatString));
        //
        StringBuffer html;
        if (events.isEmpty()) {
            html = new StringBuffer(res.getMessage("resource.common.monitor.text.events.None"));
        } else {
            html = new StringBuffer("<ul class=\"boxy\">");

            for (EventComposite event : events) {
                html.append("<li ");

                EventSeverity severity = event.getSeverity();
                if (severity == EventSeverity.FATAL || severity == EventSeverity.ERROR) {
                    html.append("class=\"red\"");
                } else if (severity == EventSeverity.WARN) {
                    html.append("class=\"yellow\"");
                } else if (severity == EventSeverity.INFO) {
                    html.append("class=\"green\"");
                } else {
                    html.append("class=\"navy\"");
                }

                html.append('>');
                html.append("<a href=\"/resource/common/Events.do?mode=events&amp;eventId=");
                html.append(event.getEventId());
                if (resourceId > -1) {
                    html.append("&amp;id=").append(event.getResourceId());
                } else if (groupId > -1) {
                    html.append("&amp;groupId=").append(groupId);
                } else {
                    html.append("&amp;parent=").append(parent).append("&amp;type=").append(type);
                }
                html.append("\">");
                html.append(event.getEventId());
                html.append("</a>");
                html.append(" ");

                html.append(ridBadChars(event.getEventDetail()));
                html.append("</li>");
            }

            html.append("</ul>");
        }

        request.setAttribute(AttrConstants.AJAX_TYPE, StringConstants.AJAX_ELEMENT);
        request.setAttribute(AttrConstants.AJAX_ID, "eventsSummary");
        request.setAttribute(AttrConstants.AJAX_HTML, html);

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