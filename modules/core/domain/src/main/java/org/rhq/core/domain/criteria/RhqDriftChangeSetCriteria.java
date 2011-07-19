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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.RhqDriftChangeSet;
import org.rhq.core.domain.util.PageOrdering;

/**
 * @author Jay Shaughnessy
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@SuppressWarnings("unused")
public class RhqDriftChangeSetCriteria extends Criteria implements DriftChangeSetCriteria {
    private static final long serialVersionUID = 1L;

    private Integer filterId;
    private Integer filterInitial; // needs override
    private Integer filterResourceId; // needs override
    private String filterVersion;
    private DriftChangeSetCategory filterCategory;
    private boolean fetchDrifts;

    private PageOrdering sortVersion;

    public RhqDriftChangeSetCriteria() {
        filterOverrides.put("initial", "version = 0");
        filterOverrides.put("resourceId", "resource.id = ?");
    }

    @Override
    public Class<RhqDriftChangeSet> getPersistentClass() {
        return RhqDriftChangeSet.class;
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

    public void addFilterVersion(String filterVersion) {
        this.filterVersion = filterVersion;
    }

    @Override
    public String getFilterVersion() {
        return filterVersion;
    }

    public void addFilterResourceId(Integer filterResourceId) {
        this.filterResourceId = filterResourceId;
    }

    @Override
    public Integer getFilterResourceId() {
        return filterResourceId;
    }

    public void addFilterCategory(DriftChangeSetCategory filterCategory) {
        this.filterCategory = filterCategory;
    }

    @Override
    public DriftChangeSetCategory getFilterCategory() {
        return filterCategory;
    }

    public void fetchDrifts(boolean fetchDrifts) {
        this.fetchDrifts = fetchDrifts;
    }

    @Override
    public boolean isFetchDrifts() {
        return fetchDrifts;
    }

    public void addSortVersion(PageOrdering sortVersion) {
        addSortField("version");
        this.sortVersion = sortVersion;
    }

    @Override
    public PageOrdering getSortVersion() {
        return sortVersion;
    }
}
