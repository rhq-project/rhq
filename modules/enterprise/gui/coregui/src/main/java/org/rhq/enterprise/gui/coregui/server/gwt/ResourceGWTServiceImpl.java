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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.ConfigurationTemplate;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
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
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.discovery.DiscoveryBossLocal;
import org.rhq.enterprise.server.measurement.MeasurementProblemManagerLocal;
import org.rhq.enterprise.server.resource.ResourceFactoryManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class ResourceGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceGWTService {

    static final long serialVersionUID = 1L;

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
    private ResourceFactoryManagerLocal resourceFactoryManager = LookupUtil.getResourceFactoryManager();
    private DiscoveryBossLocal discoveryBoss = LookupUtil.getDiscoveryBoss();

    public static final String[] importantFields = { //
    "serialVersionUID", //
        //                    "ROOT                            \n" +
        //                    "ROOT_ID                         \n" +
        "id", //
        "uuid", // This is important, because it is what Resource's equals() and hashCode() impls use.
        "resourceKey", //
        "name", //
        "ancestry", //
        //"connected", //
        "version", //
        "description", //
        "ctime", //
        "mtime", //
        "itime", //
        "modifiedBy", //
        "location", //
        "resourceType", //
        "inventoryStatus", //
        "childResources", //
        "parentResource", //
        //                    "resourceConfiguration           \n" +
        //                    "pluginConfiguration             \n" +
        //                    "agent                           \n" +
        //                    "alertDefinitions                \n" +
        //                    "resourceConfigurationUpdates    \n" +
        //                    "pluginConfigurationUpdates      \n" +
        //                    "implicitGroups                  \n" +
        "explicitGroups", //
        //                    "contentServiceRequests          \n" +
        //                    "createChildResourceRequests     \n" +
        //                    "deleteResourceRequests          \n" +
        //                    "operationHistories              \n" +
        //                    "installedPackages               \n" +
        //                    "installedPackageHistory         \n" +
        //                    "resourceRepos                   \n" +
        "schedules", //
        //                    "availability                    \n" +
        "currentAvailability", //
        //                    "resourceErrors                  \n" +
        //                    "eventSources                    \n" +
        //                    "productVersion                  "}
        "tags", //
        "driftDefinitions" };

    public static Set<String> importantFieldsSet = new HashSet<String>(Arrays.asList(importantFields));

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Log.info("Loading GWT RPC Services");
    }

    public ResourceGWTServiceImpl() {
    }

    public ResourceAvailabilitySummary getResourceAvailabilitySummary(int resourceId) throws RuntimeException {
        try {
            ResourceAvailabilitySummary result;
            result = resourceManager.getAvailabilitySummary(getSessionSubject(), resourceId);
            return result;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public ResourceAvailability getLiveResourceAvailability(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceManager.getLiveResourceAvailability(getSessionSubject(), resourceId), "ResourceService.getLiveResourceAvailability");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) throws RuntimeException {
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(getSessionSubject(), criteria);

            ObjectFilter.filterFieldsInCollection(result, importantFieldsSet);

            return SerialUtility.prepare(result, "ResourceService.findResourcesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<Resource> findResourcesByCriteriaBounded(ResourceCriteria criteria, int maxResources,
        int maxResourcesByType) throws RuntimeException {
        try {
            List<Resource> result = resourceManager.findResourcesByCriteriaBounded(getSessionSubject(), criteria,
                maxResources, maxResourcesByType);

            ObjectFilter.filterFieldsInCollection(result, importantFieldsSet);

            return SerialUtility.prepare(result, "ResourceService.findResourcesByCriteriaBounded");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<ResourceComposite> findResourceCompositesByCriteria(ResourceCriteria criteria)
        throws RuntimeException {
        try {
            PageList<ResourceComposite> result = resourceManager.findResourceCompositesByCriteria(getSessionSubject(),
                criteria);
            if (result.size() > 0) {
                List<Resource> resources = new ArrayList<Resource>(result.size());
                for (ResourceComposite composite : result) {
                    resources.add(composite.getResource());
                }
                ObjectFilter.filterFieldsInCollection(resources, importantFieldsSet);
            }

            return SerialUtility.prepare(result, "ResourceService.findResourceCompositesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<ResourceInstallCount> findResourceComplianceCounts() throws RuntimeException {
        try {
            List<ResourceInstallCount> result = resourceManager.findResourceComplianceCounts(getSessionSubject());
            return SerialUtility.prepare(result, "ResourceService.findResourceComplianceCounts");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<ResourceInstallCount> findResourceInstallCounts(boolean groupByVersions) throws RuntimeException {
        try {
            List<ResourceInstallCount> result = resourceManager.findResourceInstallCounts(getSessionSubject(),
                groupByVersions);
            return SerialUtility.prepare(result, "ResourceService.findResourceInstallCounts");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    /** Locate ProblemResourcesComposites and generate the disambiguation reports for them.
     *  Criteria passed in not currently used.
     */
    public PageList<ProblemResourceComposite> findProblemResources(long ctime, int maxItems) throws RuntimeException {
        try {
            MeasurementProblemManagerLocal problemManager = LookupUtil.getMeasurementProblemManager();
            PageList<ProblemResourceComposite> result = problemManager.findProblemResources(getSessionSubject(), ctime,
                new PageControl(0, maxItems));

            return SerialUtility.prepare(result, "ResourceService.findProblemResources");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<ResourceLineageComposite> getResourceLineageAndSiblings(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(
                resourceManager.getResourceLineageAndSiblings(getSessionSubject(), resourceId),
                "ResourceService.getResourceLineageAndSiblings");

        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Map<Integer, String> getResourcesAncestry(Integer[] resourceIds, ResourceAncestryFormat format)
        throws RuntimeException {
        try {
            return resourceManager.getResourcesAncestry(getSessionSubject(), resourceIds, format);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Resource getPlatformForResource(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceManager.getRootResourceForResource(resourceId),
                "ResourceService.getPlatformForResource");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<RecentlyAddedResourceComposite> findRecentlyAddedResources(long ctime, int maxItems)
        throws RuntimeException {
        try {
            List<RecentlyAddedResourceComposite> platforms = resourceManager.findRecentlyAddedPlatforms(
                getSessionSubject(), ctime, maxItems);

            for (RecentlyAddedResourceComposite platform : platforms) {
                List<RecentlyAddedResourceComposite> servers = resourceManager.findRecentlyAddedServers(
                    getSessionSubject(), ctime, platform.getId());
                platform.setChildren(servers);
            }

            return SerialUtility.prepare(platforms, "ResourceService.findRecentlyAddedResources");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void uninventoryAllResourcesByAgent(Agent[] agents) throws RuntimeException {
        try {
            if (agents != null && agents.length > 0) {
                // we want to uninventory them one at a time to ensure our server-side doesn't do too much in a single SLSB method/tx
                // If one fails and throws an exception, we abort the whole thing. This may or may not be a good thing.
                for (Agent agent : agents) {
                    resourceManager.uninventoryAllResourcesByAgent(getSessionSubject(), agent);
                }
            }
            return;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<Integer> uninventoryResources(int[] resourceIds) throws RuntimeException {
        try {
            return resourceManager.uninventoryResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void updateResource(Resource resource) throws RuntimeException {
        try {
            resourceManager.updateResource(getSessionSubject(), resource);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration newResourceConfiguration, Integer timeout) throws RuntimeException {
        try {
            ConfigurationDefinition pluginConfigDefinition = LookupUtil.getConfigurationManager()
                .getPluginConfigurationDefinitionForResourceType(getSessionSubject(), newResourceTypeId);
            Configuration pluginConfig = null;
            if (pluginConfigDefinition != null) {
                ConfigurationTemplate pluginConfigTemplate = pluginConfigDefinition.getDefaultTemplate();
                pluginConfig = (pluginConfigTemplate != null) ? pluginConfigTemplate.createConfiguration()
                    : new Configuration();

                // TODO GH: Is this still necessary now that we don't blow up on non-normalized configs
                // ConfigurationUtility.normalizeConfiguration(pluginConfig, pluginConfigDefinition);
            }

            resourceFactoryManager.createResource(getSessionSubject(), parentResourceId, newResourceTypeId,
                newResourceName, pluginConfig, newResourceConfiguration, timeout);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void createResource(int parentResourceId, int newResourceTypeId, String newResourceName,
        Configuration deploymentTimeConfiguration, int packageVersionId, Integer timeout) throws RuntimeException {
        try {
            ConfigurationDefinition pluginConfigDefinition = LookupUtil.getConfigurationManager()
                .getPluginConfigurationDefinitionForResourceType(getSessionSubject(), newResourceTypeId);
            Configuration pluginConfig = null;
            if (pluginConfigDefinition != null) {
                ConfigurationTemplate pluginConfigTemplate = pluginConfigDefinition.getDefaultTemplate();
                pluginConfig = (pluginConfigTemplate != null) ? pluginConfigTemplate.createConfiguration()
                    : new Configuration();

                // TODO GH: Is this still necessary now that we don't blow up on non-normalized configs
                // ConfigurationUtility.normalizeConfiguration(pluginConfig, pluginConfigDefinition);
            }

            resourceFactoryManager.createPackageBackedResourceViaPackageVersion(getSessionSubject(), parentResourceId,
                newResourceTypeId, newResourceName, pluginConfig, deploymentTimeConfiguration, packageVersionId,
                timeout);
        } catch (Throwable t) {
            if (t instanceof CannotConnectToAgentException) {
                throw (CannotConnectToAgentException) t;
            }
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<DeleteResourceHistory> deleteResources(int[] resourceIds) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceFactoryManager.deleteResources(getSessionSubject(), resourceIds),
                "ResourceService.deleteResources");
        } catch (Throwable t) {
            if (t instanceof CannotConnectToAgentException) {
                throw (CannotConnectToAgentException) t;
            }
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<Integer> disableResources(int[] resourceIds) throws RuntimeException {
        try {
            return resourceManager.disableResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<Integer> enableResources(int[] resourceIds) throws RuntimeException {
        try {
            return resourceManager.enableResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<CreateResourceHistory> findCreateChildResourceHistory(int parentId, Long beginDate, Long endDate,
        PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceFactoryManager.findCreateChildResourceHistory(getSessionSubject(),
                parentId, beginDate, endDate, pc), "ResourceService.findCreateChildResourceHistory");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<DeleteResourceHistory> findDeleteChildResourceHistory(int parentId, Long beginDate, Long endDate,
        PageControl pc) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceFactoryManager.findDeleteChildResourceHistory(getSessionSubject(),
                parentId, beginDate, endDate, pc), "ResourceService.findDeleteChildResourceHistory");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Map<Resource, List<Resource>> getQueuedPlatformsAndServers(HashSet<InventoryStatus> statuses, PageControl pc)
        throws RuntimeException {
        try {
            return SerialUtility.prepare(
                discoveryBoss.getQueuedPlatformsAndServers(getSessionSubject(), EnumSet.copyOf(statuses), pc),
                "ResourceService.getQueuedPlatformsAndServers");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void importResources(int[] resourceIds) throws RuntimeException {
        try {
            discoveryBoss.importResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void ignoreResources(int[] resourceIds) throws RuntimeException {
        try {
            discoveryBoss.ignoreResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void unignoreResources(int[] resourceIds) throws RuntimeException {
        try {
            discoveryBoss.unignoreResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void unignoreAndImportResources(int[] resourceIds) throws RuntimeException {
        try {
            discoveryBoss.unignoreAndImportResources(getSessionSubject(), resourceIds);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public List<ResourceError> findResourceErrors(int resourceId) throws RuntimeException {
        try {
            return SerialUtility.prepare(resourceManager.findResourceErrors(getSessionSubject(), resourceId),
                "ResourceService.getResourceErrors");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void deleteResourceErrors(int[] resourceErrorIds) throws RuntimeException {
        try {
            for (int doomedId : resourceErrorIds) {
                resourceManager.deleteResourceError(getSessionSubject(), doomedId);
            }
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public Resource manuallyAddResource(int resourceTypeId, int parentResourceId, Configuration pluginConfiguration)
        throws RuntimeException {
        try {
            Resource result = discoveryBoss.manuallyAddResource(getSessionSubject(), resourceTypeId, parentResourceId,
                pluginConfiguration);
            return SerialUtility.prepare(result, "ResourceService.manuallyAddResource");
        } catch (Throwable t) {
            if (t instanceof CannotConnectToAgentException) {
                throw (CannotConnectToAgentException) t;
            }
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public PageList<Resource> findGroupMemberCandidateResources(ResourceCriteria criteria,
        int[] alreadySelectedResourceIds) throws RuntimeException {
        try {
            PageList<Resource> result = resourceManager.findGroupMemberCandidateResources(getSessionSubject(),
                criteria, alreadySelectedResourceIds);
            ObjectFilter.filterFieldsInCollection(result, importantFieldsSet);
            return SerialUtility.prepare(result, "ResourceService.findResourcesByCriteria");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }
}
