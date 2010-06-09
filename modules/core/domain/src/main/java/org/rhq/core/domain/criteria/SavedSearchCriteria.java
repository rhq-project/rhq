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

import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class SavedSearchCriteria extends Criteria {
    public static final long serialVersionUID = 1L;

    private Integer filterId;
    private SearchSubsystem filterSearchSubsystem;
    private String filterName;
    private String filterDescription;
    private String filterPattern;
    private Long filterLastComputeTimeMin;
    private Long filterLastComputeTimeMax;
    private Long filterResultCountMin;
    private Long filterResultCountMax;
    private Integer filterSubjectId;
    private Boolean filterGlobal;

    private boolean fetchSubject;

    private PageOrdering sortContext;
    private PageOrdering sortName;
    private PageOrdering sortLastComputeTime;
    private PageOrdering sortResultCount;
    private PageOrdering sortGlobal;

    public SavedSearchCriteria() {
        filterOverrides.put("lastComputeTimeMin", "lastComputeTime >= ?");
        filterOverrides.put("lastComputeTimeMax", "lastComputeTime <= ?");
        filterOverrides.put("resultCountMin", "resultCount >= ?");
        filterOverrides.put("resultCountMax", "resultCount <= ?");
    }

    @Override
    public Class getPersistentClass() {
        return SavedSearch.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterSearchSubsystem(SearchSubsystem filterSearchSubsystem) {
        this.filterSearchSubsystem = filterSearchSubsystem;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterPattern(String filterPattern) {
        this.filterPattern = filterPattern;
    }

    public void addFilterResultCountMin(Long filterResultCountMin) {
        this.filterResultCountMin = filterResultCountMin;
    }

    public void addFilterResultCountMax(Long filterResultCountMax) {
        this.filterResultCountMax = filterResultCountMax;
    }

    public void addFilterLastComputeTimeMin(Long filterLastComputeTimeMin) {
        this.filterLastComputeTimeMin = filterLastComputeTimeMin;
    }

    public void addFilterLastComputeTimeMax(Long filterLastComputeTimeMax) {
        this.filterLastComputeTimeMax = filterLastComputeTimeMax;
    }

    public void addFilterSubjectId(Integer filterSubjectId) {
        this.filterSubjectId = filterSubjectId;
    }

    public void addFilterGlobal(Boolean filterGlobal) {
        this.filterGlobal = filterGlobal;
    }

    public void setFetchSubject(boolean fetchSubject) {
        this.fetchSubject = fetchSubject;
    }

    public void addSortContext(PageOrdering sortContext) {
        addSortField("context");
        this.sortContext = sortContext;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }

    public void addSortLastComputeTime(PageOrdering sortLastComputeTime) {
        addSortField("lastComputeTime");
        this.sortLastComputeTime = sortLastComputeTime;
    }

    public void addSortResultCount(PageOrdering sortResultCount) {
        addSortField("resultCount");
        this.sortResultCount = sortResultCount;
    }

    public void addSortGlobal(PageOrdering sortGlobal) {
        addSortField("global");
        this.sortGlobal = sortGlobal;
    }

}
