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

import java.util.Arrays;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;
import javax.faces.model.SelectItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.converter.SelectItemUtils;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.metric.MetricComponent;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.event.EventException;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

public class EventHistoryUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(EventHistoryUIBean.class);

    private EventManagerLocal eventManager = LookupUtil.getEventManager();

    public static final String MANAGED_BEAN_NAME = "EventHistoryUIBean";

    private EntityContext context;

    private String severityFilter;
    private String sourceFilter;
    private String searchFilter;
    private SelectItem[] severityFilterSelectItems;

    private EventComposite selectedEvent;

    private MetricComponent metric;

    public EventHistoryUIBean() {
        context = WebUtility.getEntityContext();
    }

    public EntityContext getContext() {
        return this.context;
    }

    public String getSeverityFilter() {
        if (severityFilter == null) {
            severityFilter = SelectItemUtils.getSelectItemFilter("eventHistoryForm:severityFilter");
            severityFilter = SelectItemUtils.cleanse(severityFilter);
        }
        return severityFilter;
    }

    public void setSeverityFilter(String severityFilter) {
        this.severityFilter = severityFilter;
    }

    public SelectItem[] getSeverityFilterSelectItems() {
        if (severityFilterSelectItems == null) {
            severityFilterSelectItems = SelectItemUtils.convertFromEnum(EventSeverity.class, true);
        }

        return severityFilterSelectItems;
    }

    public void setSeverityFilterSelectItems(SelectItem[] sevFilterSelectItems) {
        this.severityFilterSelectItems = sevFilterSelectItems;
    }

    public String getSourceFilter() {
        if (sourceFilter == null) {
            sourceFilter = FacesContextUtility.getOptionalRequestParameter("eventHistoryForm:sourceFilter");
        }
        return sourceFilter;
    }

    public void setSourceFilter(String sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    public String getSearchFilter() {
        if (searchFilter == null) {
            searchFilter = FacesContextUtility.getOptionalRequestParameter("eventHistoryForm:searchFilter");
        }
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }

    public EventComposite getSelectedEvent() {
        if (selectedEvent == null) {
            int eventId = FacesContextUtility.getOptionalRequestParameter("eventId", Integer.class);
            try {
                selectedEvent = eventManager.getEventDetailForEventId(getSubject(), eventId);
            } catch (EventException ee) {
                selectedEvent = null; // keep it null, handle at the UI layer
            }
        }
        return selectedEvent;
    }

    public String deleteSelectedEvents() {
        String[] selectedEvents = getSelectedEvents();
        Integer[] eventIds = StringUtility.getIntegerArray(selectedEvents);

        try {
            int numDeleted = eventManager.deleteEvents(getSubject(), Arrays.asList(eventIds));
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + numDeleted + " events.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete selected events.", e);
        }

        return "success";
    }

    public String purgeAllEvents() {
        try {
            int numDeleted = 0;

            if (context.category == EntityContext.Category.Resource) {
                numDeleted = eventManager.deleteAllEventsForResource(getSubject(), context.resourceId);
            } else if (context.category == EntityContext.Category.ResourceGroup) {
                numDeleted = eventManager.deleteAllEventsForCompatibleGroup(getSubject(), context.groupId);
            } else {
                log.error(context.getUnknownContextMessage());
            }

            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Deleted " + numDeleted + " events");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to delete events", e);
            log.error("Failed to delete events for " + context, e);
        }

        return "success";
    }

    public MetricComponent getMetric() {
        return metric;
    }

    public void setMetric(MetricComponent metric) {
        this.metric = metric;
    }

    @Override
    public DataModel getDataModel() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

        if (metric.getUnit() != null) {
            MetricComponent metric = getMetric();
            metric.setValue(Integer.parseInt(FacesContextUtility.getOptionalRequestParameter("metricComponentValue")));
            metric.setUnit(FacesContextUtility.getOptionalRequestParameter("metricComponentUnit"));
            Long millis = metric.getMillis();
            rangePreferences.begin -= millis;
        }

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
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            MeasurementPreferences preferences = user.getMeasurementPreferences();
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
            PageList<EventComposite> results = new PageList<EventComposite>();

            EventSeverity severity = getEventSeverity();
            String search = getSearchFilter();
            String source = getSourceFilter();
            MetricComponent metric = getMetric();

            if (metric.getUnit() != null) {
                rangePreferences.end -= metric.getMillis();
            }

            preferences.persistPreferences();
            if (context.category == EntityContext.Category.Resource) {
                results = eventManager.getEvents(getSubject(), new int[] { context.resourceId },
                    rangePreferences.begin, rangePreferences.end, severity, -1, source, search, pc);
            } else if (context.category == EntityContext.Category.ResourceGroup) {
                results = eventManager.getEventsForCompGroup(getSubject(), context.groupId, rangePreferences.begin,
                    rangePreferences.end, severity, -1, source, search, pc);
            } else if (context.category == EntityContext.Category.AutoGroup) {
                results = eventManager.getEventsForAutoGroup(getSubject(), context.parentResourceId,
                    context.resourceTypeId, rangePreferences.begin, rangePreferences.end, severity, pc);
            } else {
                log.error(context.getUnknownContextMessage());
            }
            return results;
        }
    }

    private String[] getSelectedEvents() {
        return FacesContextUtility.getRequest().getParameterValues("selectedEvents");
    }

    private EventSeverity getEventSeverity() {
        String severityName = getSeverityFilter();
        if (severityName != null) {
            return Enum.valueOf(EventSeverity.class, severityName);
        }
        return null;
    }
}
