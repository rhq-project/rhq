package org.rhq.enterprise.gui.measurement.graphs;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.util.TimeUtil;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.enterprise.gui.legacy.DefaultConstants;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.beans.TimelineBean;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.event.EventManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

public class EventsTimelineUIBean {

    private final Log log = LogFactory.getLog(EventsTimelineUIBean.class);

    private EventManagerLocal eventManager = LookupUtil.getEventManager();

    private List<TimelineBean> data;
    private boolean showLogs = false;

    public List<TimelineBean> getData() {
        return data;
    }

    public boolean getShowLogs() {
        return showLogs;
    }

    public EventsTimelineUIBean() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

        long begin = rangePreferences.begin;
        long end = rangePreferences.end;
        int numberOfBuckets = DefaultConstants.DEFAULT_CHART_POINTS;

        EntityContext context = WebUtility.getEntityContext();

        EventSeverity[] eventsCounts;
        if (context.category == EntityContext.Category.Resource) {
            eventsCounts = eventManager.getSeverityBuckets(user.getSubject(), context.resourceId, begin, end,
                numberOfBuckets);
            showLogs = true;
        } else if (context.category == EntityContext.Category.ResourceGroup) {
            eventsCounts = eventManager.getSeverityBucketsForCompGroup(user.getSubject(), context.groupId, begin, end,
                numberOfBuckets);
        } else if (context.category == EntityContext.Category.AutoGroup) {
            eventsCounts = eventManager.getSeverityBucketsForAutoGroup(user.getSubject(), context.parentResourceId,
                context.resourceTypeId, begin, end, numberOfBuckets);
        } else {
            log.warn("Invalid input parameter combination, not generating a timeline");
            return;
        }

        data = new ArrayList<TimelineBean>();
        long interval = TimeUtil.getInterval(begin, end, numberOfBuckets);
        for (int i = 0; i < numberOfBuckets; i++) {
            TimelineBean bean = new TimelineBean(begin + (interval * i), eventsCounts[i]);
            data.add(bean);
        }

        return;
    }
}
