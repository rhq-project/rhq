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
package org.rhq.enterprise.gui.event;

import java.util.Map;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.util.MonitorUtils;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class EventHistoryUIBean extends PagedDataTableUIBean {
    private static final Log log = LogFactory.getLog(EventHistoryUIBean.class);

    public static final String MANAGED_BEAN_NAME = "EventHistoryUIBean";

    private int id;
    private EventSeverity sevFilter;
    private String sourceFilter;
    private String searchString;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public EventSeverity getSevFilter() {
        return sevFilter;
    }

    public void setSevFilter(EventSeverity sevFilter) {
        this.sevFilter = sevFilter;
    }

    public String getSourceFilter() {
        return sourceFilter;
    }

    public void setSourceFilter(String sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public String getSearchString() {
        return searchString;
    }

    public void setSearchString(String searchString) {
        this.searchString = searchString;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListEventsHistoryDataModel(PageControlView.EventsHistoryList, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    private class ListEventsHistoryDataModel extends PagedListDataModel<EventComposite> {
        public ListEventsHistoryDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<EventComposite> fetchPage(PageControl pc) {
            Resource requestResource = EnterpriseFacesContextUtility.getResourceIfExists();
            EventManagerLocal manager = LookupUtil.getEventManager();
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            Map<String, ?> pref = user.getPreferences().getMetricRangePreference();
            long begin = (Long) pref.get(MonitorUtils.BEGIN);
            long end = (Long) pref.get(MonitorUtils.END);

            int eventId = -1;
            try {
                eventId = FacesContextUtility.getRequiredRequestParameter("eventId", Integer.class);
            } catch (IllegalStateException isex) {
                eventId = -1;
            }
            PageList<EventComposite> results = null;

            results = manager.getEvents(subject, new int[] { requestResource.getId() }, begin, end, sevFilter, eventId,
                getSourceFilter(), searchString, pc);
            return results;
        }
    }
}
