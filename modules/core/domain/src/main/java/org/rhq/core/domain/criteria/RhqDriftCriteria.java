/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.domain.criteria;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.RhqDrift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RhqDriftCriteria extends Criteria implements DriftCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private List<DriftCategory> filterCategories = Collections.emptyList();
    private Integer filterChangeSetId; // needs override
    private String filterPath;
    private List<Integer> filterResourceIds = Collections.emptyList(); // requires overrides
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides    

    private boolean fetchChangeSet;

    private PageOrdering sortCtime;

    public RhqDriftCriteria() {
        filterOverrides.put("changeSetId", "changeSet.id = ?");
        filterOverrides.put("categories", "category IN ( ? )");
        filterOverrides.put("resourceIds", "changeSet.resource.id IN ( ? )");
        filterOverrides.put("startTime", "ctime >= ?");
        filterOverrides.put("endTime", "ctime <= ?");
    }

    @Override
    public Class<? extends Drift> getPersistentClass() {
        return RhqDrift.class;
    }

    public void addFilterId(String filterId) {
        if (filterId != null) {
            this.filterId = Integer.parseInt(filterId);
        }
    }

    @Override
    public String getFilterId() {
        return filterId == null ? null : filterId.toString();
    }

    public void addFilterCategories(DriftCategory... filterCategories) {
        this.filterCategories = CriteriaUtils.getListIgnoringNulls(filterCategories);
    }

    @Override
    public List<DriftCategory> getFilterCategories() {
        return filterCategories;
    }

    public void addFilterChangeSetId(String filterChangeSetId) {
        if (filterChangeSetId != null) {
            this.filterChangeSetId = Integer.parseInt(filterChangeSetId);
        }
    }

    @Override
    public String getFilterChangeSetId() {
        return filterChangeSetId == null ? null : filterChangeSetId.toString();
    }

    public void addFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    @Override
    public String getFilterPath() {
        return filterPath;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = CriteriaUtils.getListIgnoringNulls(filterResourceIds);
    }

    @Override
    public List<Integer> getFilterResourceIds() {
        return filterResourceIds;
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    @Override
    public Long getFilterStartTime() {
        return filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    @Override
    public Long getFilterEndTime() {
        return filterEndTime;
    }

    public void fetchChangeSet(boolean fetchChangeSet) {
        this.fetchChangeSet = fetchChangeSet;
    }

    @Override
    public boolean isFetchChangeSet() {
        return fetchChangeSet;
    }

    public void addSortCtime(PageOrdering sortCtime) {
        addSortField("ctime");
        this.sortCtime = sortCtime;
    }

    @Override
    public PageOrdering getSortCtime() {
        return sortCtime;
    }
}
