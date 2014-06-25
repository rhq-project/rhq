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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.rhq.core.domain.resource.group.GroupDefinition;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Greg Hinkle
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class ResourceGroupDefinitionCriteria extends Criteria {
    private static final long serialVersionUID = 2L;

    private String filterName;
    private String filterDescription;
    private Boolean filterRecursive;
    private String filterExpression;
    private NonBindingOverrideFilter filterUserDefinedOnly; // requires overrides - finds only user defined group definitions (excludes expressions provided by plugins)

    private boolean fetchManagedResourceGroups;

    private PageOrdering sortName;
    private PageOrdering sortDescription;
    private PageOrdering sortRecursive;
    private PageOrdering sortExpression;

    public ResourceGroupDefinitionCriteria() {
        filterOverrides.put("userDefinedOnly", "cannedExpression IS NULL");
    }    

    @Override
    public Class<GroupDefinition> getPersistentClass() {
        return GroupDefinition.class;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterRecursive(Boolean filterRecursive) {
        this.filterRecursive = filterRecursive;
    }

    public void addFilterExpression(String filterExpression) {
        this.filterExpression = filterExpression;
    }

    public void addFilterUserDefinedOnly(boolean filterUserDefinedOnly) {
        this.filterUserDefinedOnly = (filterUserDefinedOnly ? NonBindingOverrideFilter.ON
            : NonBindingOverrideFilter.OFF);
    }

    public void fetchManagedResourceGroups(boolean fetchManagedResourceGroups) {
        this.fetchManagedResourceGroups = fetchManagedResourceGroups;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortDescription(PageOrdering sortDescription) {
        addSortField("description");
        this.sortDescription = sortDescription;
    }

    public void addSortRecursive(PageOrdering sortRecursive) {
        addSortField("recursive");
        this.sortRecursive = sortRecursive;
    }

    public void addSortExpression(PageOrdering sortExpression) {
        addSortField("expression");
        this.sortExpression = sortExpression;
    }
}