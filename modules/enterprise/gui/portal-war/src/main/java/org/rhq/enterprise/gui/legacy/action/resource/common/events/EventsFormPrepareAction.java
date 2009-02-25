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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.legacy.ParamConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility.MetricsControlFormPrepareAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Heiko W. Rupp
 * @author Jay Shaughnessy
 */
public class EventsFormPrepareAction extends MetricsControlFormPrepareAction {

    EventManagerLocal eventManager;

    Log log = LogFactory.getLog(EventsFormPrepareAction.class);

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        EventsForm eForm = (EventsForm) form;

        eventManager = LookupUtil.getEventManager();

        int eventId = WebUtility.getOptionalIntRequestParameter(request, "eventId", -1);
        int resourceId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.RESOURCE_ID_PARAM, -1);
        int groupId = WebUtility.getOptionalIntRequestParameter(request, ParamConstants.GROUP_ID_PARAM, -1);
        int parent = WebUtility.getOptionalIntRequestParameter(request, "parent", -1);
        int type = WebUtility.getOptionalIntRequestParameter(request, "type", -1);

        WebUser user = SessionUtils.getWebUser(request.getSession());
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        Subject subject = user.getSubject();

        // Get metric range defaults
        long begin = rangePreferences.begin;
        long end = rangePreferences.end;
        eForm.setRn(rangePreferences.lastN);
        request.getSession().setAttribute("rn", rangePreferences.lastN);
        eForm.setRu(rangePreferences.unit);
        request.getSession().setAttribute("ru", rangePreferences.unit);

        PageControl pc = WebUtility.getPageControl(request);

        // Get the filters set on the form. If set these settings take precedence
        String severityFilter = eForm.getSevFilter();
        String sourceFilter = eForm.getSourceFilter();
        String searchString = eForm.getSearchString();

        // If the form does not provide filter values then check for filters passed as parameters. 
        // Pagination bypasses the form settings so if navigating
        // from pagination we maintain the filter information only via request parameter.
        if (null == severityFilter) {
            severityFilter = WebUtility.getOptionalRequestParameter(request, "pSeverity", null);
            eForm.setSevFilter(severityFilter);
        }
        if (null == sourceFilter) {
            sourceFilter = WebUtility.getOptionalRequestParameter(request, "pSource", null);
            eForm.setSourceFilter(sourceFilter);
        }
        if (null == searchString) {
            searchString = WebUtility.getOptionalRequestParameter(request, "pSearch", null);
            eForm.setSearchString(searchString);
        }

        // Perform the query and get the (filtered) events
        EventSeverity eventSeverityFilter = getSeverityFromString(eForm.getSevFilter());
        EventSeverity[] filters = new EventSeverity[] { eventSeverityFilter };

        List<EventComposite> events;
        if (resourceId > 0) {
            events = eventManager.getEvents(subject, new int[] { resourceId }, begin, end, filters, sourceFilter,
                searchString, pc);
        } else if (groupId > 0) {
            events = eventManager.getEventsForCompGroup(subject, groupId, begin, end, filters, eventId, sourceFilter,
                searchString, pc);

        } else if (parent > 0 && type > 0) {
            events = eventManager.getEventsForAutoGroup(subject, parent, type, begin, end, filters, eventId,
                sourceFilter, searchString, pc);
        } else {
            log.warn("Invalid input combination - can not list events ");
            return null;
        }

        // highlight filter info
        for (EventComposite event : events) {
            event.setEventDetail(htmlFormat(event.getEventDetail(), eForm.getSearchString()));
            event.setSourceLocation(htmlFormat(event.getSourceLocation(), eForm.getSourceFilter()));
        }

        eForm.setEvents((PageList<EventComposite>) events);

        return null;
    }

    /**
     * Try to parse the passed String an return an appropriate severity value
     * @param sevFilter
     * @return
     */
    private EventSeverity getSeverityFromString(String sevFilter) {

        if (sevFilter == null || sevFilter.equals("") || (sevFilter.equals("ALL")))
            return null;
        try {
            EventSeverity sev = EventSeverity.valueOf(sevFilter);
            return sev;
        } catch (IllegalArgumentException iae) {
            log.warn("Illegal EventSeverity passed: " + sevFilter);
            return null;
        }
    }

    /**
     * Format the input so that CR becomes a html-break and
     * a searchResult will be highlighted
     * 
     * TODO extend and put in a Util class together with the version from {@link OneEventDetailAction}
     */
    private String htmlFormat(String input, String searchResult) {
        String output;
        output = input.replaceAll("\\n", "<br/>\n");
        if (searchResult != null && !searchResult.equals("")) {
            output = output.replaceAll("(" + searchResult + ")", "<b>$1</b>");
        }
        return output;
    }
}
