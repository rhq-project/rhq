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

import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Joseph Marques
 */
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RepoCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private String filterName;
    private String filterDescription;
    private List<Integer> filterResourceIds; // needs overrides
    private Boolean filterCandidate;
    private List<Integer> filterContentSourceIds; // needs overrides

    private boolean fetchResourceRepos;
    private boolean fetchRepoContentSources;
    private boolean fetchRepoPackageVersions;
    private boolean fetchRepoRepoGroups;
    private boolean fetchRepoRepoRelationships;

    private PageOrdering sortName;

    public RepoCriteria() {
        filterOverrides.put("resourceIds", "resourceRepos.resource.id IN ( ? )");
        filterOverrides.put("contentSourceIds", "" //
            + "id IN ( SELECT innerRepo " //
            + "          FROM Repo innerRepo " //
            + "          JOIN innerRepo.repoContentSources rcs " //
            + "         WHERE rcs.contentSource.id IN ( ? ))");
    }

    @Override
    public Class<Repo> getPersistentClass() {
        return Repo.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterName(String filterName) {
        this.filterName = filterName;
    }

    public void addFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = Arrays.asList(filterResourceIds);
    }

    public void addFilterCandidate(Boolean filterCandidate) {
        this.filterCandidate = filterCandidate;
    }

    public void addFilterContentSourceIds(Integer... filterContentSourceIds) {
        this.filterContentSourceIds = Arrays.asList(filterContentSourceIds);
    }

    public void fetchResourceRepos(boolean fetchResourceRepos) {
        this.fetchResourceRepos = fetchResourceRepos;
    }

    public void fetchRepoContentSources(boolean fetchRepoContentSources) {
        this.fetchRepoContentSources = fetchRepoContentSources;
    }

    public void fetchRepoPackageVersions(boolean fetchRepoPackageVersions) {
        this.fetchRepoPackageVersions = fetchRepoPackageVersions;
    }

    public void fetchRepoRepoGroups(boolean fetchRepoRepoGroups) {
        this.fetchRepoRepoGroups = fetchRepoRepoGroups;
    }

    public void fetchRepoRepoRelationships(boolean fetchRepoRepoRelationships) {
        this.fetchRepoRepoRelationships = fetchRepoRepoRelationships;
    }

    public void addSortName(PageOrdering sortName) {
        addSortField("name");
        this.sortName = sortName;
    }
}
