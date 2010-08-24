/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.util.PageOrdering;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * RHQ {@link Criteria} object for filtered, sortable queries of {@link MeasurementDataTrait} data sets.
 *
 * @author Ian Springer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class MeasurementDataTraitCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    // sort fields
    public static final String SORT_FIELD_SCHEDULE_ID = "scheduleId";
    public static final String SORT_FIELD_TIMESTAMP = "timestamp";
    public static final String SORT_FIELD_NAME = "name";
    public static final String SORT_FIELD_VALUE = "value";

    // filter fields
    public static final String FILTER_FIELD_SCHEDULE_ID = "scheduleId";
    public static final String FILTER_FIELD_RESOURCE_ID = "resourceId";
    public static final String FILTER_FIELD_GROUP_ID = "groupId";
    public static final String FILTER_FIELD_MAX_TIMESTAMP = "maxTimestamp";

    private Integer filterScheduleId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterGroupId; // requires overrides
    private Boolean filterMaxTimestamp; // requires overrides

    private boolean fetchSchedule;

    private PageOrdering sortScheduleId; // requires overrides
    private PageOrdering sortTimestamp; // requires overrides
    private PageOrdering sortName; // requires overrides

    public MeasurementDataTraitCriteria() {
        filterOverrides.put(FILTER_FIELD_SCHEDULE_ID, "id.scheduleId = ?");
        filterOverrides.put(FILTER_FIELD_RESOURCE_ID, "schedule.resource.id = ?");
        filterOverrides.put(FILTER_FIELD_GROUP_ID, "schedule.resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups ig " //
            + "   WHERE ig.id = ? )");
        filterOverrides.put(FILTER_FIELD_MAX_TIMESTAMP, "id.timestamp = " //
            + "( SELECT MAX(mdt.id.timestamp) "
            + "    FROM MeasurementDataTrait mdt "
            + "   WHERE mdt.id.scheduleId = id.scheduleId )");

        sortOverrides.put(SORT_FIELD_SCHEDULE_ID, "id.scheduleId");
        sortOverrides.put(SORT_FIELD_TIMESTAMP, "id.timestamp");
        sortOverrides.put(SORT_FIELD_NAME, "schedule.definition.displayName");
    }

    @Override
    public Class getPersistentClass() {
        return MeasurementDataTrait.class;
    }

    public void addFilterScheduleId(Integer filterScheduleId) {
        this.filterScheduleId = filterScheduleId;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        if (this.filterGroupId != null) {
            throw new IllegalArgumentException("This criteria cannot be filtered by both a Resource id and a group id.");
        }
        this.filterResourceId = filterResourceId;
    }

    public void addFilterGroupId(Integer filterGroupId) {
        if (this.filterResourceId != null) {
            throw new IllegalArgumentException("This criteria cannot be filtered by both a Resource id and a group id.");
        }
        this.filterGroupId = filterGroupId;
    }

    public void addFilterMaxTimestamp(Boolean filterMaxTimestamp) {
        this.filterMaxTimestamp = filterMaxTimestamp;
    }

    public void fetchSchedule(boolean fetchSchedule) {
        this.fetchSchedule = fetchSchedule;
    }

    public void addSortScheduleId(PageOrdering sortScheduleId) {
        addSortField(SORT_FIELD_SCHEDULE_ID);
        this.sortScheduleId = sortScheduleId;
    }

    public void addSortTimestamp(PageOrdering sortTimestamp) {
        addSortField(SORT_FIELD_TIMESTAMP);
        this.sortTimestamp = sortTimestamp;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField(SORT_FIELD_NAME);
        this.sortName = sortName;
    }
}