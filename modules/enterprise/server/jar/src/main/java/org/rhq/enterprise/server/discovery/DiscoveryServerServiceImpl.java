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
package org.rhq.enterprise.server.discovery;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.AvailabilityReport;
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

/**
 * This is the service that receives inventory data from agents. As agents discover resources, they report back to the
 * server what they have found via this service. // TODO GH: Should this be renamed inventory server service?
 *
 * @author John Mazzitelli
 */
public class DiscoveryServerServiceImpl implements DiscoveryServerService {
    private Log log = LogFactory.getLog(DiscoveryServerServiceImpl.class);

    /**
     * @see DiscoveryServerService#mergeInventoryReport(InventoryReport)
     */
    public ResourceSyncInfo mergeInventoryReport(InventoryReport report) throws InvalidInventoryReportException {
        long start = System.currentTimeMillis();
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        ResourceSyncInfo syncInfo;
        try {
            syncInfo = discoveryBoss.mergeInventoryReport(report);
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
            log.error(
                "Fatal error occurred during merging of inventory report from agent [" + report.getAgent() + "].", e);
            throw e;
        }

        long elapsed = (System.currentTimeMillis() - start);
        if (elapsed > 30000L) {
            log.info("Performance: inventory merge (" + elapsed + ")ms");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Performance: inventory merge (" + elapsed + ")ms");
            }
        }

        return syncInfo;
    }

    // TODO GH: Should this be on the measurement server service?
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

            // log at INFO level if we are going to ask for a full report, its a full report or it took a long time, DEBUG otherwise
            if (!ok || !availabilityReport.isChangesOnlyReport() || (elapsed > 20000L)) {
                log.info("Processed " + reportToString + " - need full=[" + !ok + "] in (" + elapsed + ")ms");
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Processed " + reportToString + " - need full=[" + !ok + "] in (" + elapsed + ")ms");
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

    public Set<Resource> getResources(Set<Integer> resourceIds, boolean includeDescendants) {
        long start = System.currentTimeMillis();
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        Set<Resource> resources = new HashSet<Resource>();
        for (Integer resourceId : resourceIds) {
            Resource resource = resourceManager.getResourceTree(resourceId, includeDescendants);
            resource = convertToPojoResource(resource, includeDescendants);
            resources.add(resource);
        }
        if (log.isDebugEnabled()) {
            log.debug("Performance: get Resources [" + resourceIds + "], recursive=" + includeDescendants
                + ", timing (" + (System.currentTimeMillis() - start) + ")ms");
        }
        return resources;
    }

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

    public void setResourceError(ResourceError resourceError) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        resourceManager.addResourceError(resourceError);
    }

    public void clearResourceConfigError(int resourceId) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        resourceManager.clearResourceConfigError(resourceId);
    }

    public MergeResourceResponse addResource(Resource resource, int creatorSubjectId) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.addResource(resource, creatorSubjectId);
    }

    public boolean updateResourceVersion(int resourceId, String version) {
        DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();
        return discoveryBoss.updateResourceVersion(resourceId, version);
    }

    private static Resource convertToPojoResource(Resource resource, boolean includeDescendants) {
        Resource pojoResource = new Resource(resource.getId());
        pojoResource.setUuid(resource.getUuid());
        pojoResource.setResourceKey(resource.getResourceKey());
        pojoResource.setResourceType(resource.getResourceType());
        pojoResource.setMtime(resource.getMtime());
        pojoResource.setInventoryStatus(resource.getInventoryStatus());
        pojoResource.setPluginConfiguration(resource.getPluginConfiguration());
        pojoResource.setName(resource.getName());
        pojoResource.setDescription(resource.getDescription());
        pojoResource.setLocation(resource.getLocation());
        if (resource.getParentResource() != null) {
            pojoResource.setParentResource(convertToPojoResource(resource.getParentResource(), false));
        }
        if (includeDescendants) {
            for (Resource childResource : resource.getChildResources()) {
                pojoResource.addChildResource(convertToPojoResource(childResource, true));
            }
        }
        return pojoResource;
    }

    public Set<ResourceMeasurementScheduleRequest> postProcessNewlyCommittedResources(Set<Integer> resourceIds) {
        if (log.isDebugEnabled()) {
            log.debug("Post-processing " + resourceIds.size() + "newly committed resources");
            log.debug("Ids were: " + resourceIds);
        }

        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        AlertTemplateManagerLocal alertTemplateManager = LookupUtil.getAlertTemplateManager();
        MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();
        AgentManagerLocal agentManager = LookupUtil.getAgentManager();
        StatusManagerLocal statusManager = LookupUtil.getStatusManager();

        long start = System.currentTimeMillis();

        // do this in one fell swoop, instead of one resource at a time
        Set<ResourceMeasurementScheduleRequest> results = scheduleManager.findSchedulesForResourceAndItsDescendants(
            ArrayUtils.unwrapCollection(resourceIds), false);

        long time = (System.currentTimeMillis() - start);

        if (time >= 10000L) {
            log.info("Performance: commit resource, create schedules timing: resourceCount/millis="
                + resourceIds.size() + '/' + time);
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: commit resource, create schedules timing: resourceCount/millis="
                + resourceIds.size() + '/' + time);
        }

        start = System.currentTimeMillis();

        for (Integer resourceId : resourceIds) {
            // apply alert templates
            try {
                alertTemplateManager.updateAlertDefinitionsForResource(overlord, resourceId);
            } catch (AlertDefinitionCreationException adce) {
                /* should never happen because AlertDefinitionCreationException is only ever
                 * thrown if updateAlertDefinitionsForResource isn't called as the overlord
                 *
                 * but we'll log it anyway, just in case, so it isn't just swallowed
                 */
                log.error(adce);
            } catch (Throwable t) {
                log.debug("Could not apply alert templates for resourceId = " + resourceId, t);
            }
        }

        try {
            if (resourceIds.size() > 0) {
                // they all come from the same agent, so pick any old one
                int anyResourceIdFromNewlyCommittedSet = resourceIds.iterator().next();
                int agentId = agentManager.getAgentIdByResourceId(anyResourceIdFromNewlyCommittedSet);
                statusManager.updateByAgent(agentId);
            }
        } catch (Throwable t) {
            log.debug("Could not reload caches for newly committed resources", t);
        }

        time = (System.currentTimeMillis() - start);

        if (time >= 10000L) {
            log.info("Performance: commit resource, apply alert templates timing: resourceCount/millis="
                + resourceIds.size() + '/' + time);
        } else if (log.isDebugEnabled()) {
            log.debug("Performance: commit resource, apply alert templates timing: resourceCount/millis="
                + resourceIds.size() + '/' + time);
        }

        return results;
    }

    /**
     * Method implementation for prototyping of dynamically adding new ResourceTypes 
     */
    public void addNewResourceType(String resourceTypeName, String metricName) {

        //Method call to add a new resourceType
        LookupUtil.getResourceMetadataManager().addNewResourceType(resourceTypeName, metricName);

    }
}