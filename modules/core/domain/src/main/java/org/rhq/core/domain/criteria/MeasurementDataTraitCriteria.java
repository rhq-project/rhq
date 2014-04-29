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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.util.PageOrdering;

/**
 * RHQ {@link Criteria} object for filtered, sortable queries of {@link MeasurementDataTrait trait} data sets.
 *
 * @author Ian Springer
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class MeasurementDataTraitCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    // sort field names
    public static final String SORT_FIELD_SCHEDULE_ID = "scheduleId";
    public static final String SORT_FIELD_TIMESTAMP = "timestamp";
    public static final String SORT_FIELD_DISPLAY_NAME = "displayName";
    public static final String SORT_FIELD_VALUE = "value";
    public static final String SORT_FIELD_RESOURCE_NAME = "resourceName";

    // filter field names
    public static final String FILTER_FIELD_SCHEDULE_ID = "scheduleId";
    public static final String FILTER_FIELD_RESOURCE_ID = "resourceId";
    public static final String FILTER_FIELD_GROUP_ID = "groupId";
    public static final String FILTER_FIELD_DEFINITION_ID = "definitionId";
    public static final String FILTER_FIELD_MAX_TIMESTAMP = "maxTimestamp";
    public static final String FILTER_FIELD_ENABLED = "enabled";

    private Integer filterScheduleId; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterGroupId; // requires overrides
    private Integer filterDefinitionId; // requires overrides
    private Integer filterMaxTimestamp; // requires overrides
    private Boolean filterEnabled; // requires overrides

    private boolean fetchSchedule;

    private PageOrdering sortTimestamp; // requires overrides
    private PageOrdering sortDisplayName; // requires overrides
    private PageOrdering sortResourceName; // requires overrides

    public MeasurementDataTraitCriteria() {
    }

    @Override
    public Class<MeasurementDataTrait> getPersistentClass() {
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

    public void addFilterDefinitionId(Integer filterDefinitionId) {
        this.filterDefinitionId = filterDefinitionId;
    }

    public void addFilterMaxTimestamp() {
        this.filterMaxTimestamp = 1;
    }

    public void addFilterEnabled(boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void fetchSchedule(boolean fetchSchedule) {
        this.fetchSchedule = fetchSchedule;
    }

    public void addSortTimestamp(PageOrdering sortTimestamp) {
        this.sortTimestamp = sortTimestamp;
    }

    public void addSortName(PageOrdering sortName) {
        this.sortDisplayName = sortName;
    }

    public void addSortResourceName(PageOrdering sortResourceName) {
        this.sortResourceName = sortResourceName;
    }

    @Override
    public boolean isSupportsAddSortId() {
        return false;
    }

    @Override
    public boolean isSupportsAddFilterId() {
        return false;
    }

    @Override
    public boolean isSupportsAddFilterIds() {
        return false;
    }

    Integer getFilterScheduleId() {
        return filterScheduleId;
    }

    Integer getFilterResourceId() {
        return filterResourceId;
    }

    public Integer getFilterGroupId() {
        return filterGroupId;
    }

    Integer getFilterDefinitionId() {
        return filterDefinitionId;
    }

    public boolean isFilterMaxTimestamp() {
        return filterMaxTimestamp != null;
    }

    Boolean getFilterEnabled() {
        return filterEnabled;
    }

    public PageOrdering getSortTimestamp() {
        return sortTimestamp;
    }

    PageOrdering getSortDisplayName() {
        return sortDisplayName;
    }

    PageOrdering getSortResourceName() {
        return sortResourceName;
    }

}
