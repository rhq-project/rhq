/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.ProblemResourceComposite;
import org.rhq.core.domain.resource.composite.RecentlyAddedResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.resource.composite.ResourceLineageComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */
public interface ResourceGWTService extends RemoteService {

    void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration newResourceConfiguration, Integer timeout) throws RuntimeException;

    void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration deploymentTimeConfiguration, int packageVersionId, Integer timeout) throws RuntimeException;

    List<DeleteResourceHistory> deleteResources(int[] resourceIds) throws RuntimeException;

    List<Integer> disableResources(int[] resourceIds) throws RuntimeException;

    List<Integer> enableResources(int[] resourceIds) throws RuntimeException;

    PageList<CreateResourceHistory> findCreateChildResourceHistory(int parentId, Long beginDate, Long endDate,
        PageControl pc) throws RuntimeException;

    PageList<DeleteResourceHistory> findDeleteChildResourceHistory(int parentId, Long beginDate, Long endDate,
        PageControl pc) throws RuntimeException;

    List<RecentlyAddedResourceComposite> findRecentlyAddedResources(long ctime, int maxItems) throws RuntimeException;

    ResourceAvailabilitySummary getResourceAvailabilitySummary(int resourceId) throws RuntimeException;

    ResourceAvailability getLiveResourceAvailability(int resourceId) throws RuntimeException;

    PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) throws RuntimeException;

    List<Resource> findResourcesByCriteriaBounded(ResourceCriteria criteria, int maxResources, int maxResourcesByType)
        throws RuntimeException;

    PageList<ResourceComposite> findResourceCompositesByCriteria(ResourceCriteria criteria) throws RuntimeException;

    List<ResourceError> findResourceErrors(int resourceId) throws RuntimeException;

    void deleteResourceErrors(int[] resourceErrorIds) throws RuntimeException;

    PageList<ProblemResourceComposite> findProblemResources(long ctime, int maxItems) throws RuntimeException;

    List<ResourceInstallCount> findResourceComplianceCounts() throws RuntimeException;

    List<ResourceInstallCount> findResourceInstallCounts(boolean groupByVersions) throws RuntimeException;

    Resource getPlatformForResource(int resourceId) throws RuntimeException;

    Map<Resource, List<Resource>> getQueuedPlatformsAndServers(HashSet<InventoryStatus> statuses, PageControl pc)
        throws RuntimeException;

    Map<Integer, String> getResourcesAncestry(Integer[] resourceIds, ResourceAncestryFormat format)
        throws RuntimeException;

    List<ResourceLineageComposite> getResourceLineageAndSiblings(int resourceId) throws RuntimeException;

    void ignoreResources(int[] resourceIds) throws RuntimeException;

    void importResources(int[] resourceIds) throws RuntimeException;

    Resource manuallyAddResource(int resourceTypeId, int parentResourceId, Configuration pluginConfiguration)
        throws RuntimeException;

    void updateResource(Resource resource) throws RuntimeException;

    void unignoreResources(int[] resourceIds) throws RuntimeException;

    void unignoreAndImportResources(int[] resourceIds) throws RuntimeException;

    void uninventoryAllResourcesByAgent(Agent[] agents) throws RuntimeException;

    List<Integer> uninventoryResources(int[] resourceIds) throws RuntimeException;

    PageList<Resource> findGroupMemberCandidateResources(ResourceCriteria criteria, int[] alreadySelectedResourceIds)
        throws RuntimeException;

}
