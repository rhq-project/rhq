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

import java.util.List;

import org.rhq.core.domain.drift.DriftCategory;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.util.PageOrdering;

/**
 * <p>
 * This is the API for criteria based searching on {@link org.rhq.core.domain.drift.DriftChangeSet DriftChangeSet}.
 * </p>
 * <p>
 * Criteria based queries are used extensively throughout RHQ. To maintain a consistent
 * querying approach, drift server plugins need to querying based on this API. The API is
 * written in a way to support different plugin implementations as easily as possible.
 * </p>
 *
 * @author Jay Shaughnessy
 * @author John Sanda
 */
public interface DriftChangeSetCriteria extends BaseCriteria {

    /**
     * @param filterId The change set id
     */
    void addFilterId(String filterId);

    /**
     * @return The change set id filter or null if not set
     */
    String getFilterId();

    /**
     * @param filterVersion The change set version.
     */
    void addFilterVersion(String filterVersion);

    /**
     * @return The change set version filter or null if not set
     */
    String getFilterVersion();

    /**
     * Allows for searching for change sets by version range.
     *
     * @param filterStartVersion The starting version of the change set(s). This should be
     * inclusive.
     */
    void addFilterStartVersion(String filterStartVersion);

    /**
     * @return The starting version or null if not set
     */
    String getFilterStartVersion();

    /**
     * Allows for searching for change sets by version range.
     *
     * @param filterEndVersion The ending version of the change set(s). This should be
     * inclusive.
     */
    void addFilterEndVersion(String filterEndVersion);

    /**
     * @return The ending version or null if not set.
     */
    String getFilterEndVersion();

    /**
     * Allows for searching change sets by timestamp range where the timestamp corresponds
     * to the creation time of the change set.
     *
     * @param filterCreatedAfter The starting time of the range. This should be inclusive.
     */
    void addFilterCreatedAfter(Long filterCreatedAfter);

    /**
     * @return The starting creation timestamp or null if not set.
     */
    Long getFilterCreatedAfter();

    /**
     * Allows for searching change sets by timestamp range where the timestamp corresponds
     * to the creation time of the change set.
     *
     * @param filterCreatedBefore The ending time of the range. This should be inclusive.
     */
    void addFilterCreatedBefore(Long filterCreatedBefore);

    /**
     * @return The ending creation timestamp or null if not set.
     */
    Long getFilterCreatedBefore();

    /**
     * @param filterResourceId The id which uniquely identifies the resource to which the
     * change set(s) belong. Note that persistence of resources is managed by the RHQ core
     * server so it assumed that no other resource-specific information other than the id
     * is maintained/tracked by drift server plugin implementations.
     */
    void addFilterResourceId(Integer filterResourceId);

    /**
     * @return The resource id filter or null if not set.
     */
    Integer getFilterResourceId();

    /**
     * @param filterDriftDefId The id which uniquely identifies the drift definition to
     * which the change set(s) belong. Note that persistence of resources is managed by the
     * RHQ core server so it assumed that no other resource-specific information other than
     * the id is maintained/tracked by drift server plugin implementations.
     */
    void addFilterDriftDefinitionId(Integer filterDriftDefId);

    /**
     * @return The drift definition id filter or null if not set.
     */
    Integer getFilterDriftDefinitionId();

    /**
     * Allows for filtering on change set type. If not set, it can be assumed that the
     * category is {@link DriftChangeSetCategory#DRIFT} which means that the query results
     * should only include delta change sets.
     *
     * @param filterCategory The change set type filter
     */
    void addFilterCategory(DriftChangeSetCategory filterCategory);

    /**
     * @return The change set category (i.e., type) filter
     */
    DriftChangeSetCategory getFilterCategory();

    /**
     * Allows for filtering on the type of drift contained in the change sets. Each of the
     * specified categories must found in a change set in order for it to be considered a
     * match.
     *
     * @param filterDriftCategories Drift type or categories on which to filter.
     * @see DriftCategory
     */
    void addFilterDriftCategories(DriftCategory... filterDriftCategories);

    /**
     * @return A list of {@link DriftCategory} filters or an empty list if not set.
     */
    List<DriftCategory> getFilterDriftCategories();

    /**
     * Allows for filtering on a specific directory. A change set should be considered a match
     * only if it contains {@link org.rhq.core.domain.drift.Drift Drift} with a directory that
     * IS CASE SENSITIVE EQUAL TO the specified string. All substring matching should use the
     * {@link #addFilterDriftPath(String)}. Setting this filter non-null will force
     * {@link #setStrict(boolean)} to true. 
     *
     * @param filterDriftDirectory A directory substring on which to filter
     */
    void addFilterDriftDirectory(String filterDriftDirectory);

    /**
     * @return The drift directory substring filter
     */
    String getFilterDriftDirectory();

    /**
     * Allows for filtering on a specific path. A change set should be considered a match
     * only if it contains {@link org.rhq.core.domain.drift.Drift Drift} with a path that
     * contains the specified substring.

     * @param filterDriftPath A path substring on which to filter
     */
    void addFilterDriftPath(String filterDriftPath);

    /**
     * @return The drift path substring filter
     */
    String getFilterDriftPath();

    /**
     * @param fetchDrifts set to true if the drifts that make up the change set should be
     * loaded and returned with the change set.
     */
    void fetchDrifts(boolean fetchDrifts);

    /**
     * @return true if drifts are to be retrieved
     */
    boolean isFetchDrifts();

    /**
     * Allows for sorting on change set version
     *
     * @param sortVersion Specifies whether the sort is ascending or descending
     */
    void addSortVersion(PageOrdering sortVersion);

    /**
     * @return The type of sort by version or null if no sorting has been specified.
     */
    PageOrdering getSortVersion();

}
