/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.criteria.Criteria.Restriction;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 * 
 * @author Joseph Marques
 */
public class SavedSearchResultCountRecalculationJob extends AbstractStatefulJob {

    private final static Log LOG = LogFactory.getLog(SavedSearchResultCountRecalculationJob.class);

    private SavedSearchManagerLocal savedSearchManager = LookupUtil.getSavedSearchManager();
    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceGroupManagerLocal resourceGroupManager = LookupUtil.getResourceGroupManager();

    private Subject overlord = LookupUtil.getSubjectManager().getOverlord();

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        List<SavedSearch> staleSavedSearches = getSavedSearchesNeedingRecomputation();

        int errors = 0;
        int updated = 0;
        long totalMillis = 0;
        for (SavedSearch next : staleSavedSearches) {
            if (next.isGlobal()) {
                continue;
            }
            try {

                if (next.getSearchSubsystem() == SearchSubsystem.RESOURCE) {
                    ResourceCriteria criteria = new ResourceCriteria();
                    criteria.setRestriction(Restriction.COUNT_ONLY);
                    criteria.setSearchExpression(next.getPattern());

                    totalMillis -= System.currentTimeMillis();
                    PageList<Resource> results = resourceManager.findResourcesByCriteria(overlord, criteria);
                    totalMillis += System.currentTimeMillis();

                    if (processResults(next, results)) {
                        updated++;
                    }

                } else if (next.getSearchSubsystem() == SearchSubsystem.GROUP) {
                    ResourceGroupCriteria criteria = new ResourceGroupCriteria();
                    criteria.setRestriction(Restriction.COUNT_ONLY);
                    criteria.setSearchExpression(next.getPattern());

                    totalMillis -= System.currentTimeMillis();
                    PageList<ResourceGroup> results = resourceGroupManager.findResourceGroupsByCriteria(overlord,
                        criteria);
                    totalMillis += System.currentTimeMillis();

                    if (processResults(next, results)) {
                        updated++;
                    }
                }
            } catch (Throwable t) {
                // TODO: mark this saved search as "broken" so that future computation is suppressed for it
                errors++;
                LOG.error("Could not calculate result count for SavedSearch[name=" + next.getName() + ", pattern='"
                    + next.getPattern() + "']: " + t.getMessage());
                LOG.debug(t);
            }
        }
        if (updated > 0) {
            // only print non-zero stats
            LOG.debug("Statistics: updated " + updated + " in " + totalMillis + " ms (" + errors + " errors)");
        }
    }

    private boolean processResults(SavedSearch next, PageList<?> results) {
        // TODO: should recent count be computed at the time of update/save for this saved search?
        //       it would obviate the need for null checking here as well as in the UI for conditional 
        //        display of the result count
        if (next.getResultCount() == null || results.getTotalSize() != next.getResultCount()) {
            next.setResultCount((long) results.getTotalSize());
            savedSearchManager.updateSavedSearch(overlord, next);
            return true;
        }
        return false;
    }

    private List<SavedSearch> getSavedSearchesNeedingRecomputation() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);

        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterLastComputeTimeMax(fiveMinutesAgo);

        List<SavedSearch> results = savedSearchManager.findSavedSearchesByCriteria(overlord, criteria);
        return results;
    }
}
