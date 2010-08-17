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

import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class MeasurementScheduleCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    // sort fields from the MeasurementSchedule itself    
    public static final String SORT_FIELD_ENABLED = "enabled";
    public static final String SORT_FIELD_INTERVAL = "interval";

    // sort fields from the MeasurementSchedule's MeasurementDefinition
    public static final String SORT_FIELD_DEFINITION_ID = "definitionId";
    public static final String SORT_FIELD_NAME = "name";
    public static final String SORT_FIELD_DISPLAY_NAME = "displayName";
    public static final String SORT_FIELD_DESCRIPTION = "description";
    public static final String SORT_FIELD_DATA_TYPE = "dataType";

    // filter fields
    public static final String FILTER_FIELD_RESOURCE_ID = "resourceId";
    public static final String FILTER_FIELD_RESOURCE_GROUP_ID = "resourceGroupId";
    public static final String FILTER_FIELD_RESOURCE_TYPE_ID = "resourceTypeId";

    private Integer filterId;
    private Boolean filterEnabled;
    private List<Integer> filterDefinitionIds; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterResourceGroupId; // requires overrides
    private Integer filterAutoGroupResourceTypeId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides
    private Integer filterResourceTypeId; // requires overrides

    private boolean fetchBaseline;
    private boolean fetchDefinition;
    private boolean fetchResource;

    private PageOrdering sortName; // requires overrides
    private PageOrdering sortDisplayName; // requires overrides
    private PageOrdering sortDataType; // requires overrides

    public MeasurementScheduleCriteria() {
        filterOverrides.put("definitionIds", "definition.id IN ( ? )");
        filterOverrides.put(FILTER_FIELD_RESOURCE_ID, "resource.id IN ( ? )");
        filterOverrides.put(FILTER_FIELD_RESOURCE_GROUP_ID, "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups ig " //
            + "   WHERE ig.id = ? )");
        filterOverrides.put("autoGroupResourceTypeId", "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.resourceType type " //
            + "   WHERE type.id = ? )");
        filterOverrides.put("autoGroupParentResourceId", "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.parentResource parent " //
            + "   WHERE parent.id = ? )");
        filterOverrides.put(FILTER_FIELD_RESOURCE_TYPE_ID, "resource.type.id = ?");

        sortOverrides.put(SORT_FIELD_DEFINITION_ID, "definition.id");
        sortOverrides.put(SORT_FIELD_NAME, "definition.name");
        sortOverrides.put(SORT_FIELD_DISPLAY_NAME, "definition.displayName");
        sortOverrides.put(SORT_FIELD_DESCRIPTION, "definition.description");
        sortOverrides.put(SORT_FIELD_DATA_TYPE, "definition.dataType");
    }

    @Override
    public Class getPersistentClass() {
        return MeasurementSchedule.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterEnabled(Boolean filterEnabled) {
        this.filterEnabled = filterEnabled;
    }

    public void addFilterDefinitionIds(Integer... filterDefinitionIds) {
        this.filterDefinitionIds = Arrays.asList(filterDefinitionIds);
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

    public void addFilterResourceTypeId(Integer filterResourceTypeId) {
        this.filterResourceTypeId = filterResourceTypeId;
    }

    public void fetchBaseline(boolean fetchBaseline) {
        this.fetchBaseline = fetchBaseline;
    }

    public void fetchDefinition(boolean fetchDefinition) {
        this.fetchDefinition = fetchDefinition;
    }

    public void fetchResource(boolean fetchResource) {
        this.fetchResource = fetchResource;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortDisplayName(PageOrdering sortDisplayName) {
        addSortField("displayName");
        this.sortDisplayName = sortDisplayName;
    }

    public void addSortDataType(PageOrdering sortDataType) {
        addSortField("dataType");
        this.sortDataType = sortDataType;
    }

}