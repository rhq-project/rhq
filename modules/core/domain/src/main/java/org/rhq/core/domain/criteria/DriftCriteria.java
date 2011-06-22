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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.Drift;
import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.util.CriteriaUtils;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class DriftCriteria extends Criteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private List<DriftCategory> filterCategories;
    private Integer filterChangeSetId; // needs override
    private String filterPath;
    private List<Integer> filterResourceIds; // requires overrides    
    private Long filterStartTime; // requires overrides
    private Long filterEndTime; // requires overrides    

    private boolean fetchChangeSet;

    private PageOrdering sortCtime;

    public DriftCriteria() {
        filterOverrides.put("changeSetId", "changeSet.id = ?");
        filterOverrides.put("categories", "category IN ( ? )");
        filterOverrides.put("resourceIds", "changeSet.resource.id IN ( ? )");
        filterOverrides.put("startTime", "ctime >= ?");
        filterOverrides.put("endTime", "ctime <= ?");
    }

    @Override
    public Class<Drift> getPersistentClass() {
        return Drift.class;
    }

    public void addFilterId(Integer filterId) {
        this.filterId = filterId;
    }

    public void addFilterCategories(DriftCategory... filterCategories) {
        this.filterCategories = CriteriaUtils.getListIgnoringNulls(filterCategories);
    }

    public void addFilterChangeSetId(Integer filterChangeSetId) {
        this.filterChangeSetId = filterChangeSetId;
    }

    public void addFilterPath(String filterPath) {
        this.filterPath = filterPath;
    }

    public void addFilterResourceIds(Integer... filterResourceIds) {
        this.filterResourceIds = CriteriaUtils.getListIgnoringNulls(filterResourceIds);
    }

    public void addFilterStartTime(Long filterStartTime) {
        this.filterStartTime = filterStartTime;
    }

    public void addFilterEndTime(Long filterEndTime) {
        this.filterEndTime = filterEndTime;
    }

    public void fetchChangeSet(boolean fetchChangeSet) {
        this.fetchChangeSet = fetchChangeSet;
    }

    public void addSortCtime(PageOrdering sortCtime) {
        addSortField("ctime");
        this.sortCtime = sortCtime;
    }

}
