/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * To support pluggable drift server implementations this Interface provides the contract required for
 * Criteria based searching on DriftChangeSet.  
 * 
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public interface DriftChangeSetCriteria extends BaseCriteria {

    void addFilterId(String filterId);

    String getFilterId();

    void addFilterVersion(String filterVersion);

    String getFilterVersion();

    void addFilterStartVersion(String filterStartVersion);

    String getFilterStartVersion();

    void addFilterEndVersion(String filterEndVersion);

    String getFilterEndVersion();

    void addFilterCreatedAfter(Long filterCreatedAfter);

    Long getFilterCreatedAfter();

    void addFilterCreatedBefore(Long filterCreatedBefore);

    Long getFilterCreatedBefore();

    void addFilterResourceId(Integer filterResourceId);

    Integer getFilterResourceId();

    void addFilterDriftDefinitionId(Integer filterDriftDefId);

    Integer getFilterDriftDefintionId();

    void addFilterCategory(DriftChangeSetCategory filterCategory);

    DriftChangeSetCategory getFilterCategory();

    void fetchDrifts(boolean fetchDrifts);

    boolean isFetchDrifts();

    void addSortVersion(PageOrdering sortVersion);

    PageOrdering getSortVersion();

}
