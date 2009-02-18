package org.rhq.enterprise.gui.inventory.resource;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.core.domain.operation.composite.ResourceOperationLastCompletedComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.alert.engine.internal.Tuple;
import org.rhq.enterprise.server.util.LookupUtil;

public class ResourceOverviewUIBean {

    private List<Alert> recentAlerts;
    private List<ResourceOperationLastCompletedComposite> recentOperations;
    private List<ResourceConfigurationUpdate> recentConfigChanges;
    private List<Tuple<EventSeverity, Integer>> recentEventCounts;

    private Subject subject;
    private int resourceId;

    public ResourceOverviewUIBean() {
        subject = EnterpriseFacesContextUtility.getSubject();
        resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
    }

    private List<Alert> getAlerts(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("a.ctime", PageOrdering.DESC);
        return LookupUtil.getAlertManager().findAlerts(resourceId, null, null, null, lastFive);
    }

    private List<ResourceOperationLastCompletedComposite> getOperations(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
        return LookupUtil.getOperationManager().getRecentlyCompletedResourceOperations(subject, resourceId, lastFive);
    }

    private List<ResourceConfigurationUpdate> getConfigUpdates(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("cu.createdTime", PageOrdering.DESC);
        return LookupUtil.getConfigurationManager().getResourceConfigurationUpdates(subject, resourceId, lastFive);
    }

    private List<Tuple<EventSeverity, Integer>> getEventCounts(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("ev.timestamp", PageOrdering.DESC);
        List<EventComposite> events = LookupUtil.getEventManager().getEventsForResource(subject, resourceId, 0,
            System.currentTimeMillis(), null, lastFive);

        int[] counts = new int[EventSeverity.values().length];
        for (EventComposite event : events) {
            counts[event.getSeverity().getOrdinal()]++;
        }

        List<Tuple<EventSeverity, Integer>> results = new ArrayList<Tuple<EventSeverity, Integer>>();

        for (EventSeverity severity : EventSeverity.values()) {
            results.add(new Tuple<EventSeverity, Integer>(severity, counts[severity.getOrdinal()]));
        }
        return results;
    }

    public List<Alert> getRecentAlerts() {
        if (recentAlerts == null) {
            recentAlerts = getAlerts(subject, resourceId, 5);
        }
        return recentAlerts;
    }

    public List<ResourceOperationLastCompletedComposite> getRecentOperations() {
        if (recentOperations == null) {
            recentOperations = getOperations(subject, resourceId, 5);
        }
        return recentOperations;
    }

    public List<ResourceConfigurationUpdate> getRecentConfigChanges() {
        if (recentConfigChanges == null) {
            recentConfigChanges = getConfigUpdates(subject, resourceId, 5);
        }
        return recentConfigChanges;
    }

    public List<Tuple<EventSeverity, Integer>> getRecentEventCounts() {
        if (recentEventCounts == null) {
            recentEventCounts = getEventCounts(subject, resourceId, 100);
        }
        return recentEventCounts;
    }
}
