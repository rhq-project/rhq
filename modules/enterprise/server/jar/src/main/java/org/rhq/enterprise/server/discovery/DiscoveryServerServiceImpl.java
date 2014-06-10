/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.discovery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.discovery.StaleTypeException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.alert.AlertDefinitionCreationException;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.cloud.StatusManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.concurrent.AvailabilityReportSerializer;
import org.rhq.enterprise.server.util.concurrent.InventoryReportSerializer;

/**
 * This is the service that receives inventory data from agents. As agents discover resources, they report back to the
 * server what they have found via this service.
 *
 * @author John Mazzitelli
 */
public class DiscoveryServerServiceImpl implements DiscoveryServerService {
    private Log log = LogFactory.getLog(DiscoveryServerServiceImpl.class);

    /**
     * @see DiscoveryServerService#mergeInventoryReport(InventoryReport)
     */
    @Override
    public MergeInventoryReportResults mergeInventoryReport(InventoryReport report)
        throws InvalidInventoryReportException, StaleTypeException {

        InventoryReportSerializer.getSingleton().lock(report.getAgent().getName());
        try {
            long start = System.currentTimeMillis();
            DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
            MergeInventoryReportResults results;
            try {
                results = discoveryBoss.mergeInventoryReport(report);
            } catch (StaleTypeException e) {
                // There is no need to log this exception as it is part of a normal work flow
                // that occurs as a result of a user deleting a plugin. DiscoveryBossBean
                // already logs a message about the stale types that can be useful for
                // debugging; so, we just need to propagate the exception to the agent.
                throw e;
            } catch (InvalidInventoryReportException e) {
                Agent agent = report.getAgent();
                if (log.isDebugEnabled()) {
                    log.error("Received invalid inventory report from agent [" + agent + "]", e);
                } else {
                    /*
                     * this is expected when the platform is uninventoried, because the agent often has in-flight reports
                     * going to the server at the time the platform's agent is being deleted from the database
                     */
                    log.error("Received invalid inventory report from agent [" + agent + "]: " + e.getMessage());
                }
                throw e;
            } catch (RuntimeException e) {
                log.error("Fatal error occurred during merging of inventory report from agent [" + report.getAgent()
                    + "].", e);
                throw e;
            }

            long elapsed = (System.currentTimeMillis() - start);
            if (elapsed > 30000L) {
                log.warn("Performance: inventory merge (" + elapsed + ")ms");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Performance: inventory merge (" + elapsed + ")ms");
                }
            }

            return results;
        } finally {
            InventoryReportSerializer.getSingleton().unlock(report.getAgent().getName());
        }
    }

    @Override
    public Collection<ResourceSyncInfo> getResourceSyncInfo(int resourceId) {
        long start = System.currentTimeMillis();
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        Collection<ResourceSyncInfo> results;

        results = discoveryBoss.getResourceSyncInfo(resourceId);

        long elapsed = (System.currentTimeMillis() - start);
        if (elapsed > 30000L) {
            log.warn("Performance: get resource sync info (" + elapsed + ")ms");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Performance: get resource sync info (" + elapsed + ")ms");
            }
        }

        return results;
    }

    @Override
    public boolean mergeAvailabilityReport(AvailabilityReport availabilityReport) {
        AvailabilityReportSerializer.getSingleton().lock(availabilityReport.getAgentName());
        try {
            String reportToString = availabilityReport.toString(false);
            if (log.isDebugEnabled())
                log.debug("Processing " + reportToString);

            long start = System.currentTimeMillis();
            AvailabilityManagerLocal availabilityManager = LookupUtil.getAvailabilityManager();
            boolean ok = availabilityManager.mergeAvailabilityReport(availabilityReport);

            long elapsed = (System.currentTimeMillis() - start);
            if (elapsed > 20000L) {
                log.warn("Performance: processed " + reportToString + " - needFull=[" + !ok + "] in (" + elapsed
                    + ")ms");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Performance: processed " + reportToString + " - needFull=[" + !ok + "] in (" + elapsed
                        + ")ms");
                }
            }

            return ok;
        } catch (Exception e) {
            log.info("Error processing availability report from [" + availabilityReport.getAgentName() + "]: "
                + ThrowableUtil.getAllMessages(e));
            return true; // not sure what happened, but avoid infinite recursion during error conditions; do not ask for a full report
        } finally {
            AvailabilityReportSerializer.getSingleton().unlock(availabilityReport.getAgentName());
        }
    }

    @Override
    public Set<Resource> getResources(Set<Integer> resourceIds, boolean includeDescendants) {
        long start = System.currentTimeMillis();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Set<Resource> resources = new HashSet<Resource>();
        for (Integer resourceId : resourceIds) {
            Resource resource = resourceManager.getResourceTree(resourceId, includeDescendants);
            if (isVisibleInInventory(resource)) {
                resource = convertToPojoResource(resource, includeDescendants);
                cleanoutResource(resource);
                resources.add(resource);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Performance: get Resources [" + resourceIds + "], recursive=" + includeDescendants
                + ", timing (" + (System.currentTimeMillis() - start) + ")ms");
        }
        return resources;
    }

    @Override
    public List<Resource> getResourcesAsList(Integer... resourceIds) {
        long start = System.currentTimeMillis();

        ResourceCriteria criteria = new ResourceCriteria();
        // get all of the resources for the supplied ids
        criteria.addFilterIds(resourceIds);
        // filter out any that are not actually in inventory
        criteria.addFilterInventoryStatuses(new ArrayList<InventoryStatus>(InventoryStatus.getInInventorySet()));
        // get all of them, don't limit to default paging
        criteria.clearPaging();
        criteria.fetchResourceType(true);
        criteria.fetchPluginConfiguration(true);

        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<Resource> result = resourceManager.findResourcesByCriteria(overlord, criteria);

        if (log.isDebugEnabled()) {
            log.debug("Performance: get ResourcesAsList [" + resourceIds + "], timing ("
                + (System.currentTimeMillis() - start) + ")ms");
        }

        // Now do some clean out of stuff the agent does not need
        // Perhaps we should limit the query above to only return relevant stuff

        for (Resource resource: result) {
            cleanoutResource(resource);
        }

        return result;
    }

    private void cleanoutResource(Resource resource) {
        resource.setAncestry(null);
        resource.setAlertDefinitions(Collections.EMPTY_SET);
        resource.setLocation(null);
        resource.setDescription(null);
        resource.setAutoGroupBackingGroups(Collections.EMPTY_LIST);
        resource.setExplicitGroups(Collections.EMPTY_SET);
        resource.setCreateChildResourceRequests(Collections.EMPTY_LIST);
        resource.setDeleteResourceRequests(Collections.EMPTY_LIST);
        resource.setImplicitGroups(Collections.EMPTY_SET);
        resource.setInstalledPackageHistory(Collections.EMPTY_LIST);
        resource.setInstalledPackages(Collections.EMPTY_SET);
        resource.setPluginConfigurationUpdates(Collections.EMPTY_LIST);
        resource.setResourceConfigurationUpdates(Collections.EMPTY_LIST);
        if (resource.getPluginConfiguration()!=null) {
            resource.getPluginConfiguration().cleanoutRawConfiguration();
        }


    }

    @Override
    public Map<Integer, InventoryStatus> getInventoryStatus(int rootResourceId, boolean descendents) {
        long start = System.currentTimeMillis();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Map<Integer, InventoryStatus> statuses = resourceManager.getResourceStatuses(rootResourceId, descendents);
        if (log.isDebugEnabled()) {
            log.debug("Performance: get inventory statuses for [" + statuses.size() + "] timing ("
                + (System.currentTimeMillis() - start) + ")ms");
        }
        return statuses;
    }

    @Override
    public void setResourceError(ResourceError resourceError) {
        try {
            ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
            resourceManager.addResourceError(resourceError);
        } catch (RuntimeException re) {
            log.error("Failed to persist Resource error [" + resourceError + "].", re);
            throw re;
        }
    }

    @Override
    public void clearResourceConfigError(int resourceId) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        resourceManager.clearResourceConfigError(resourceId);
    }

    @Override
    public MergeResourceResponse addResource(Resource resource, int creatorSubjectId) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.addResource(resource, creatorSubjectId);
    }

    @Override
    public boolean updateResourceVersion(int resourceId, String version) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.updateResourceVersion(resourceId, version);
    }

    @Override
    public Set<ResourceUpgradeResponse> upgradeResources(Set<ResourceUpgradeRequest> upgradeRequests) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.upgradeResources(upgradeRequests);
    }

    private static Resource convertToPojoResource(Resource resource, boolean includeDescendants) {
        Resource pojoResource = new Resource(resource.getId());
        pojoResource.setUuid(resource.getUuid());
        pojoResource.setResourceKey(resource.getResourceKey());
        pojoResource.setResourceType(resource.getResourceType());
        pojoResource.setMtime(resource.getMtime());
        pojoResource.setInventoryStatus(resource.getInventoryStatus());
        Configuration pcCopy = resource.getPluginConfiguration();
        if (pcCopy != null) {
            pcCopy = pcCopy.deepCopy();
        }
        pojoResource.setPluginConfiguration(pcCopy);
        pojoResource.setName(resource.getName());
        pojoResource.setDescription(resource.getDescription());
        pojoResource.setLocation(resource.getLocation());
        pojoResource.setVersion(resource.getVersion());

        if (resource.getParentResource() != null) {
            pojoResource.setParentResource(convertToPojoResource(resource.getParentResource(), false));
        }
        if (includeDescendants) {
            for (Resource childResource : resource.getChildResources()) {
                if (isVisibleInInventory(childResource)) {
                    pojoResource.addChildResource(convertToPojoResource(childResource, true));
                }
            }
        }
        return pojoResource;
    }

    private static boolean isVisibleInInventory(Resource resource) {
        return resource.getInventoryStatus() != InventoryStatus.DELETED
            && resource.getInventoryStatus() != InventoryStatus.UNINVENTORIED;
    }

    @Override
    public Set<ResourceMeasurementScheduleRequest> postProcessNewlyCommittedResources(Set<Integer> resourceIds) {
        DiscoveryBossLocal boss = LookupUtil.getDiscoveryBoss();
        return boss.postProcessNewlyCommittedResources(resourceIds);
    }

    @Override
    public void setResourceEnablement(int resourceId, boolean setEnabled) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        if (setEnabled) {
            resourceManager.enableResources(overlord, new int[] { resourceId });
        } else {
            resourceManager.disableResources(overlord, new int[] { resourceId });
        }
    }

}
