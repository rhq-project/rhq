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

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementCategory;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class MeasurementDefinitionCriteria extends Criteria {

    private Integer filterId;
    private String filterName;
    private String filterDisplayName;
    private String filterDescription;
    private String filterResourceTypeName;
    private MeasurementCategory filterCategory;
    private MeasurementUnits filterUnits;
    private NumericType filterNumericType;
    private DataType filterDataType;
    private DisplayType filterDisplayType;
    private Boolean filterDefaultOn;
    private Long filterDefaultInterval;

    private boolean fetchSchedules;
    private boolean fetchAlertConditions;

    private PageOrdering sortName;
    private PageOrdering sortDisplayName;
    private PageOrdering sortResourceTypeName;
    private PageOrdering sortCategory;
    private PageOrdering sortUnits;
    private PageOrdering sortNumericType;
    private PageOrdering sortDataType;
    private PageOrdering sortDisplayType;
    private PageOrdering sortDefaultOn;
    private PageOrdering sortDefaultInterval;

    public MeasurementDefinitionCriteria() {
        super();

        filterOverrides.put("resourceTypeName", "alertDefinition.resourceType.name like ?");
        filterOverrides.put("resourceIds", "alertDefinition.resource.id IN ( ? )");

        sortOverrides.put("resourceTypeName", "resourceType.name");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDisplayName(String filterDisplayName) {
        this.filterDisplayName = filterDisplayName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterResourceTypeName(String filterResourceTypeName) {
        this.filterResourceTypeName = filterResourceTypeName;
    }

    public void addFilterCategory(MeasurementCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    public void addFilterUnits(MeasurementUnits filterUnits) {
        this.filterUnits = filterUnits;
    }

    public void addFilterNumericType(NumericType filterNumericType) {
        this.filterNumericType = filterNumericType;
    }

    public void addFilterDataType(DataType filterDataType) {
        this.filterDataType = filterDataType;
    }

    public void addFilterDisplayType(DisplayType filterDisplayType) {
        this.filterDisplayType = filterDisplayType;
    }

    public void addFilterDefaultOn(Boolean filterDefaultOn) {
        this.filterDefaultOn = filterDefaultOn;
    }

    public void addFilterDefaultInterval(Long filterDefaultInterval) {
        this.filterDefaultInterval = filterDefaultInterval;
    }

    public void fetchSchedules(boolean fetchSchedules) {
        this.fetchSchedules = fetchSchedules;
    }

    public void fetchAlertConditions(boolean fetchAlertConditions) {
        this.fetchAlertConditions = fetchAlertConditions;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortDisplayName(PageOrdering sortDisplayName) {
        addSortField("displayName");
        this.sortDisplayName = sortDisplayName;
    }

    public void addSortResourceTypeName(PageOrdering sortResourceTypeName) {
        addSortField("resourceTypeName");
        this.sortResourceTypeName = sortResourceTypeName;
    }

    public void addSortCategory(PageOrdering sortCategory) {
        addSortField("category");
        this.sortCategory = sortCategory;
    }

    public void addSortUnits(PageOrdering sortUnits) {
        addSortField("units");
        this.sortUnits = sortUnits;
    }

    public void addSortNumericType(PageOrdering sortNumericType) {
        addSortField("numericType");
        this.sortNumericType = sortNumericType;
    }

    public void addSortDataType(PageOrdering sortDataType) {
        addSortField("dataType");
        this.sortDataType = sortDataType;
    }

    public void addSortDisplayType(PageOrdering sortDisplayType) {
        addSortField("displayType");
        this.sortDisplayType = sortDisplayType;
    }

    public void addSortDefaultOn(PageOrdering sortDefaultOn) {
        addSortField("defaultOn");
        this.sortDefaultOn = sortDefaultOn;
    }

    public void addSortDefaultInterval(PageOrdering sortDefaultInterval) {
        addSortField("defaultInterval");
        this.sortDefaultInterval = sortDefaultInterval;
    }

}
