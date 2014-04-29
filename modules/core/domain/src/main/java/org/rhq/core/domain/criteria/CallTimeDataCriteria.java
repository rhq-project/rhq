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

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class CallTimeDataCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterResourceId; // requires overrides
    private Integer filterResourceGroupId; // requires overrides
    private Integer filterAutoGroupResourceTypeId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides
    private Long filterBeginTime; // requires overrides
    private Long filterEndTime; // requires overrides
    private Double filterMinimum;
    private Double filterMaximum;
    private Double filterTotal;
    private Long filterCount;
    private String filterDestination; // requires overrides
    private DataType filterDataType = DataType.CALLTIME; // requires overrides, not user modifiable

    private PageOrdering sortMinimum; // requires overrides
    private PageOrdering sortMaximum; // requires overrides
    private PageOrdering sortAverage; // requires overrides
    private PageOrdering sortTotal; // requires overrides
    private PageOrdering sortCount; // requires overrides

    @Override
    public Class<CallTimeDataValue> getPersistentClass() {
        return CallTimeDataValue.class;
    }

    public CallTimeDataCriteria() {
        /*
        filterOverrides.put("resourceId", "key.schedule.resource.id = ?");
        filterOverrides.put("resourceGroupId", "key.schedule.resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups ig " //
            + "   WHERE ig.id = ? )");
        filterOverrides.put("autoGroupResourceTypeId", "key.schedule.resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.resourceType type " //
            + "   WHERE type.id = ? )");
        filterOverrides.put("autoGroupParentResourceId", "key.schedule.resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.parentResource parent " //
            + "   WHERE parent.id = ? )");
        */
        filterOverrides.put("resourceId", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData " //
            + "   WHERE callData.key.schedule.resource.id = ? )");

        filterOverrides.put("resourceGroupId", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData, Resource res " //
            + "    JOIN res.implicitGroups ig " //
            + "   WHERE callData.key.schedule.resource.id = res.id " //
            + "     AND ig.id = ? ) ");

        filterOverrides.put("autoGroupResourceTypeId", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData, Resource res " //
            + "   WHERE callData.key.schedule.resource.id = res.id " //
            + "     AND res.resourceType.id = ? )");

        filterOverrides.put("autoGroupParentResourceId", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData, Resource res " //
            + "   WHERE callData.key.schedule.resource.id = res.id " //
            + "     AND res.parentResource.id = ? )");

        filterOverrides.put("beginTime", "beginTime > ?");
        filterOverrides.put("endTime", "endTime < ?");

        filterOverrides.put("destination", "key.callDestination like ?");
        /*
        filterOverrides.put("destination", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData " //
            + "   WHERE callData.key.callDestination like ? )");
        */
        filterOverrides.put("dataType", "key.schedule.definition.dataType = ?");
        /*
        filterOverrides.put("dataType", "id IN " //
            + "( SELECT callData.id " //
            + "    FROM CallTimeDataValue callData " //
            + "   WHERE callData.key.schedule.definition.dataType = ? )");
        */

        sortOverrides.put("minimum", "MIN(" + getAlias() + ".minimum)");
        sortOverrides.put("maximum", "MAX(" + getAlias() + ".maximum)");
        sortOverrides.put("average", "(SUM(" + getAlias() + ".total) / SUM(" + getAlias() + ".count))");
        sortOverrides.put("total", "SUM(" + getAlias() + ".total)");
        sortOverrides.put("count", "SUM(" + getAlias() + ".count)");

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

    public void addFilterBeginTime(Long filterBeginTime) {
        this.filterBeginTime = filterBeginTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void addFilterMinimum(Double filterMinimum) {
        this.filterMinimum = filterMinimum;
    }

    public void addFilterMaximum(Double filterMaximum) {
        this.filterMaximum = filterMaximum;
    }

    public void addFilterTotal(Double filterTotal) {
        this.filterTotal = filterTotal;
    }

    // use basic type not object wrapper because it requires non-null value
    public void addFilterCount(long filterCount) {
        if (filterCount < 1) {
            throw new IllegalArgumentException("Filter 'count' must be greater than 0");
        }
        this.filterCount = filterCount;
    }

    public void addFilterDestination(String filterDestination) {
        this.filterDestination = filterDestination;
    }

    public void addFilterDataType(DataType filterDataType) {
        if (true) {
            throw new IllegalArgumentException(
                "Filter 'dataType' is not user modifiable, it must remain DataType.CALLTIME");
        }
        //this.filterDataType = filterDataType;
    }

    public void addSortMinimum(PageOrdering sortMinimum) {
        addSortField("minimum");
        this.sortMinimum = sortMinimum;
    }

    public void addSortMaximum(PageOrdering sortMaximum) {
        addSortField("maximum");
        this.sortMaximum = sortMaximum;
    }

    public void addSortAverage(PageOrdering sortAverage) {
        addSortField("average");
        this.sortAverage = sortAverage;
    }

    public void addSortTotal(PageOrdering sortTotal) {
        addSortField("total");
        this.sortTotal = sortTotal;
    }

    public void addSortCount(PageOrdering sortCount) {
        addSortField("count");
        this.sortCount = sortCount;
    }

    public boolean hasCustomizedSorting() {
        return true;
    }

    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public Integer getFilterResourceGroupId() {
        return filterResourceGroupId;
    }

    public Integer getFilterAutoGroupResourceTypeId() {
        return filterAutoGroupResourceTypeId;
    }

    public Integer getFilterAutoGroupParentResourceId() {
        return filterAutoGroupParentResourceId;
    }

    public DataType getFilterDataType() {
        return filterDataType;
    }

    public Long getFilterBeginTime() {
        return filterBeginTime;
    }

    public Long getFilterEndTimeDate() {
        return filterEndTime;
    }

    public String getFilterDestination() {
        return filterDestination;
    }

    public Double getFilterMinimum() {
        return filterMinimum;
    }

    public Double getFilterMaximum() {
        return filterMaximum;
    }

    public Double getFilterTotal() {
        return filterTotal;
    }

    public Long getFilterCount() {
        return filterCount;
    }

}
