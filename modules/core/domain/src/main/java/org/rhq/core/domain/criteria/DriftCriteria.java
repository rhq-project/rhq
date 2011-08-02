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

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * To support pluggable drift server implementations this Interface provides the contract required for
 * Criteria based searching on Drift.  
 * 
 * @author Jay Shaughnessy
 * 
 */
public interface DriftCriteria extends BaseCriteria {

    void addFilterId(String filterId);

    String getFilterId();

    void addFilterCategories(DriftCategory... filterCategories);

    DriftCategory[] getFilterCategories();

    void addFilterChangeSetId(String filterChangeSetId);

    String getFilterChangeSetId();

    void addFilterPath(String filterPath);

    String getFilterPath();

    void addFilterResourceIds(Integer... filterResourceIds);

    Integer[] getFilterResourceIds();

    void addFilterStartTime(Long filterStartTime);

    Long getFilterStartTime();

    void addFilterEndTime(Long filterEndTime);

    Long getFilterEndTime();

    void fetchChangeSet(boolean fetchChangeSet);

    boolean isFetchChangeSet();

    void addSortCtime(PageOrdering sortCtime);

    PageOrdering getSortCtime();

}
