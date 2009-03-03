package org.rhq.enterprise.gui.inventory.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
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
    private List<InstalledPackageHistory> recentPackageHistory;

    private Subject subject;
    private int resourceId;
    private List<MeasurementOOBComposite> recentOObs;

    public ResourceOverviewUIBean() {
        subject = EnterpriseFacesContextUtility.getSubject();
        resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
    }

    private List<Alert> getAlerts(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("a.ctime", PageOrdering.DESC);
        return LookupUtil.getAlertManager().findAlerts(resourceId, null, null, null, null, lastFive);
    }

    private List<ResourceOperationLastCompletedComposite> getOperations(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("ro.createdTime", PageOrdering.DESC);
        return LookupUtil.getOperationManager().getRecentlyCompletedResourceOperations(subject, resourceId, lastFive);
    }

    private List<ResourceConfigurationUpdate> getConfigUpdates(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("cu.createdTime", PageOrdering.DESC);
        return LookupUtil.getConfigurationManager().getResourceConfigurationUpdates(subject, resourceId, null, null,
            lastFive);
    }

    private List<Tuple<EventSeverity, Integer>> getEventCounts(Subject subject, int resourceId) {
        PageControl unlimited = PageControl.getUnlimitedInstance();
        unlimited.initDefaultOrderingField("ev.timestamp", PageOrdering.DESC);

        long now = System.currentTimeMillis();
        long nowMinus24Hours = now - (24 * 60 * 60 * 1000);
        Map<EventSeverity, Integer> eventCounts = LookupUtil.getEventManager().getEventCountsBySeverity(subject,
            resourceId, nowMinus24Hours, now);

        List<Tuple<EventSeverity, Integer>> results = new ArrayList<Tuple<EventSeverity, Integer>>();
        for (EventSeverity severity : eventCounts.keySet()) {
            int count = eventCounts.get(severity);
            if (count > 0) {
                results.add(new Tuple<EventSeverity, Integer>(severity, count));
            }
        }
        return results;
    }

    private List<InstalledPackageHistory> getPackageHistory(Subject subject, int resourceId, int count) {
        PageControl lastFive = new PageControl(0, count);
        lastFive.initDefaultOrderingField("iph.timestamp", PageOrdering.DESC);
        return LookupUtil.getContentUIManager().getInstalledPackageHistoryForResource(resourceId, lastFive);
    }

    private List<MeasurementOOBComposite> getRecentOObs(Subject subject, int resourceId, int n) {
        return LookupUtil.getOOBManager()
            .getHighestNOOBsForResource(subject, resourceId, n);
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
            recentEventCounts = getEventCounts(subject, resourceId);
        }
        return recentEventCounts;
    }

    public List<InstalledPackageHistory> getRecentPackageHistory() {
        if (recentPackageHistory == null) {
            recentPackageHistory = getPackageHistory(subject, resourceId, 5);
        }
        return recentPackageHistory;
    }

    public List<MeasurementOOBComposite> getRecentOOBs() {
        if (recentOObs == null)
            recentOObs = getRecentOObs(subject, resourceId, 5);

        return recentOObs;
    }

}
