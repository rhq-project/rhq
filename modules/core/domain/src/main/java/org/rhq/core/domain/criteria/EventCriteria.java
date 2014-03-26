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

import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.event.Event;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class EventCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    private String filterDetail;
    private String filterSourceName; // requires overrides
    private List<EventSeverity> filterSeverities;
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides
    private Integer filterResourceId; // requires overrides
    private String filterResourceName; // requires overrides
    private Integer filterResourceGroupId; // requires overrides
    private Integer filterAutoGroupResourceTypeId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides
    private Integer filterSourceId; // requires overrides

    private boolean fetchSource;

    private PageOrdering sortTimestamp;
    private PageOrdering sortSeverity;

    public EventCriteria() {
        filterOverrides.put("sourceName", "source.location like ?");
        filterOverrides.put("sourceId","source.id = ?");
        filterOverrides.put("startTime", "timestamp >= ?");
        filterOverrides.put("endTime", "timestamp <= ?");
        filterOverrides.put("resourceId", "source.resourceId = ?");
        filterOverrides.put("resourceName", "source.resource.name like ?");
        filterOverrides.put("resourceGroupId", "source.resourceId IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups ig " //
            + "   WHERE ig.id = ? )");
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
        filterOverrides.put("severities", "severity IN ( ? )");
    }

    @Override
    public Class<Event> getPersistentClass() {
        return Event.class;
    }

    public void addFilterDetail(String filterDetail) {
        this.filterDetail = filterDetail;
    }

    public void addFilterSourceName(String filterSourceName) {
        this.filterSourceName = filterSourceName;
    }

    public void addFilterSourceId(Integer sourceId) {
        this.filterSourceId = sourceId;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void addFilterSeverities(EventSeverity... filterSeverities) {
        if (filterSeverities != null && filterSeverities.length > 0) {
            this.filterSeverities = Arrays.asList(filterSeverities);
        }
    }

    public void addFilterEntityContext(EntityContext filterEntityContext) {
        if (filterEntityContext.getType() == EntityContext.Type.Resource) {
            addFilterResourceId(filterEntityContext.getResourceId());
        } else if (filterEntityContext.getType() == EntityContext.Type.ResourceGroup) {
            addFilterResourceGroupId(filterEntityContext.getGroupId());
        } else if (filterEntityContext.getType() == EntityContext.Type.AutoGroup) {
            addFilterAutoGroupParentResourceId(filterEntityContext.getParentResourceId());
            addFilterAutoGroupResourceTypeId(filterEntityContext.getResourceTypeId());
        }
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }
    
    public void addFilterResourceName(String filterResourceName) {
        this.filterResourceName = filterResourceName;
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
