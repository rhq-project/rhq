/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.domain.criteria;

import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
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
