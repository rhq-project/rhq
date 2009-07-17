package org.rhq.core.domain.criteria;

import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageOrdering;

public class EventCriteria extends Criteria {
    private Integer filterId;
    private String filterDetail;
    private String filterSourceName; // requires overrides
    private EventSeverity filterSeverity;
    private Long startTime; // requires overrides
    private Long endTime; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterResourceGroupId; // requires overrides
    private Integer filterAutoGroupResourceTypeId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides

    private boolean fetchSource;

    private PageOrdering sortTimestamp;
    private PageOrdering sortSeverity;

    public EventCriteria() {
        super();

        filterOverrides.put("sourceName", "source.definition.name like ?");
        filterOverrides.put("startTime", "timestamp >= ?");
        filterOverrides.put("endTime", "timestamp <= ?");
        filterOverrides.put("resourceId", "source.resourceId = ?");
        filterOverrides.put("resourceGroupId", "source.resourceId IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups group " //
            + "   WHERE group.id = ? )");
        filterOverrides.put("autoGroupResourceTypeId", "source.resourceId IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.resourceType type " //
            + "   WHERE type.id = ? )");
        filterOverrides.put("autoGroupParentResourceId", "source.resourceId IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.parentResource parent " //
            + "   WHERE parent.id = ? )");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterDetail(String filterDetail) {
        this.filterDetail = filterDetail;
    }

    public void addFilterSourceName(String filterSourceName) {
        this.filterSourceName = filterSourceName;
    }

    public void addFilterSeverity(EventSeverity filterSeverity) {
        this.filterSeverity = filterSeverity;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    public void addFilterResourceGroupId(Integer filterResourceGroupId) {
        this.filterResourceGroupId = filterResourceGroupId;
    }

    public void addFilterAutoGroupResourceTypeId(Integer filterAutoGroupResourceTypeId) {
        this.filterAutoGroupResourceTypeId = filterAutoGroupResourceTypeId;
    }

    public void addFilterAutoGroupParentResourceId(Integer filterAutoGroupParentResourceId) {
        this.filterAutoGroupParentResourceId = filterAutoGroupParentResourceId;
    }

    public void fetchSource(boolean fetchSource) {
        this.fetchSource = fetchSource;
    }

    public void addSortTimestamp(PageOrdering sortTimestamp) {
        addSortField("timestamp");
        this.sortTimestamp = sortTimestamp;
    }

    public void addSortSeverity(PageOrdering sortSeverity) {
        addSortField("severity");
        this.sortSeverity = sortSeverity;
    }
}
