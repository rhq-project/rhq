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

import java.util.List;

import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
public class MeasurementScheduleCriteria extends Criteria {

    private Integer filterId;
    private List<Integer> filterDefinitionIds; // requires overrides
    private Integer filterResourceId; // requires overrides
    private Integer filterResourceGroupId; // requires overrides
    private Integer filterAutoGroupResourceTypeId; // requires overrides
    private Integer filterAutoGroupParentResourceId; // requires overrides

    private boolean fetchBaseline;
    private boolean fetchDefinition;
    private boolean fetchResource;

    private PageOrdering sortName; // requires overrides

    public MeasurementScheduleCriteria() {
        super();

        filterOverrides.put("definitionIds", "definition.id IN ( ? )");
        filterOverrides.put("resourceId", "resource.id IN ( ? )");
        filterOverrides.put("resourceGroupId", "resource.id IN " //
            + "( SELECT res.id " //
            + "    FROM Resource res " //
            + "    JOIN res.implicitGroups group " //
            + "   WHERE group.id = ? )");
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

        sortOverrides.put("name", "definition.name");
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterDefinitionIds(List<Integer> filterDefinitionIds) {
        this.filterDefinitionIds = filterDefinitionIds;
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

}
