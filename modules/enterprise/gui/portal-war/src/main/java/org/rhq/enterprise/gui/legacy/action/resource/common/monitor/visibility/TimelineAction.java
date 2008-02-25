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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

import org.rhq.core.clientapi.util.TimeUtil;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.enterprise.gui.legacy.AttrConstants;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.beans.TimelineBean;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Set an array for the timeline display
 */
public class TimelineAction extends TilesAction {

    Log log = LogFactory.getLog(TimelineAction.class);

    /* (non-Javadoc)
     * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping,
     * org.apache.struts.action.ActionForm, javax.servlet.http.HttpServletRequest,
     * javax.servlet.http.HttpServletResponse)
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        WebUser user = (WebUser) request.getSession().getAttribute(AttrConstants.WEBUSER_SES_ATTR);
        Map range = user.getMetricRangePreference();
        long begin = ((Long) range.get(MonitorUtils.BEGIN)).longValue();
        long end = ((Long) range.get(MonitorUtils.END)).longValue();
        long[] intervals = new long[DefaultConstants.DEFAULT_CHART_POINTS];

        // Get the events count
        ServletContext ctx = getServlet().getServletContext();

        int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, "groupId", -1);
        int parentId = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int typeId = WebUtility.getOptionalIntRequestParameter(request, "type", -1);
        int ctypeId = WebUtility.getOptionalIntRequestParameter(request, "ctype", -1);
        if (ctypeId > 0 && typeId == -1)
            typeId = ctypeId;

        EventManagerLocal eventManager = LookupUtil.getEventManager();
        EventSeverity[] eventsCounts;
        if (resourceId > 0) {
            eventsCounts = eventManager.getSeverityBuckets(user.getSubject(), resourceId, begin, end,
                DefaultConstants.DEFAULT_CHART_POINTS);
        } else if (groupId > 0) {
            eventsCounts = eventManager.getSeverityBucketsForCompGroup(user.getSubject(), groupId, begin, end,
                DefaultConstants.DEFAULT_CHART_POINTS);
        } else if (parentId > 0 && typeId > 0) {
            eventsCounts = eventManager.getSeverityBucketsForAutoGroup(user.getSubject(), parentId, typeId, begin, end,
                DefaultConstants.DEFAULT_CHART_POINTS);
        } else {
            log.warn("Invalid input parameter combination, not generating a timeline");
            return null;
        }

        // Create the time intervals beans
        TimelineBean[] beans = new TimelineBean[intervals.length];
        long interval = TimeUtil.getInterval(begin, end, DefaultConstants.DEFAULT_CHART_POINTS);
        for (int i = 0; i < intervals.length; i++) {
            beans[i] = new TimelineBean(begin + (interval * i), eventsCounts[i]);
        }

        request.setAttribute(AttrConstants.TIME_INTERVALS_ATTR, beans);

        return null;
    }
}