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
package org.rhq.enterprise.server.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;

import org.jboss.remoting.CannotConnectException;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.discovery.StaleTypeException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.CannotConnectToAgentException;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.server.PersistenceUtility;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.cloud.StorageNodeManagerLocal;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.measurement.AvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceAlreadyExistsException;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * SLSB that provides the interface point to the discovery subsystem for the UI layer and the discovery server service.
 *
 * @author Ian Springer
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
@Stateless
public class DiscoveryBossBean implements DiscoveryBossLocal, DiscoveryBossRemote {
    private static final Log LOG = LogFactory.getLog(DiscoveryBossBean.class.getName());

    static private final int MERGE_BATCH_SIZE;

    static {

        int mergeBatchSize = 200;
        try {
            mergeBatchSize = Integer.parseInt(System.getProperty("rhq.server.discovery.merge.batch.size", "200"));
        } catch (Throwable t) {
            //
        }
        MERGE_BATCH_SIZE = mergeBatchSize;
    }

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private DiscoveryBossLocal discoveryBoss; // ourselves for Tx purposes
    @EJB
    private ResourceGroupManagerLocal groupManager;
    @EJB
    private ResourceManagerLocal resourceManager;
    @EJB
    private ResourceAvailabilityManagerLocal resourceAvailabilityManager;
    @EJB
    private ResourceTypeManagerLocal resourceTypeManager;
    @EJB
    private SubjectManagerLocal subjectManager;
    @EJB
    private ProductVersionManagerLocal productVersionManager;
    @EJB
    private SystemManagerLocal systemManager;
    @EJB
    private PluginManagerLocal pluginManager;
    @EJB
    private AvailabilityManagerLocal availabilityManager;
    @EJB
    private StorageNodeManagerLocal storageNodeManager;
    @EJB
    private ConfigurationManagerLocal configurationManager;

    // Do not start in a transaction.  A single transaction may timeout if the report size is too large
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public MergeInventoryReportResults mergeInventoryReport(InventoryReport report)
        throws InvalidInventoryReportException {
        validateInventoryReport(report);

        DeletedResourceTypeFilter filter = new DeletedResourceTypeFilter(subjectManager, resourceTypeManager,
            pluginManager);
        Set<ResourceType> deletedTypes = filter.apply(report);

        if (!deletedTypes.isEmpty()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("The inventory report from " + report.getAgent() + " with added roots "
                    + report.getAddedRoots() + " contains these deleted resource types " + deletedTypes);
            } else {
                LOG.info("The inventory report from " + report.getAgent() + " contains these deleted resource types "
                    + deletedTypes);
            }
            throw new StaleTypeException("The report contains one or more resource types that have been marked for "
                + "deletion.");
        }

        Agent agent = report.getAgent();
        long start = System.currentTimeMillis();

        Agent knownAgent = agentManager.getAgentByName(agent.getName());
        if (knownAgent == null) {
            throw new InvalidInventoryReportException("Unknown Agent named [" + agent.getName()
                + "] sent an inventory report - that report will be ignored. "
                + "This error is harmless and should stop appearing after a short while if the platform of the agent ["
                + agent.getName() + "] was recently removed from the inventory. In any other case this is a bug.");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Received inventory report from RHQ Agent [" + knownAgent + "]. Number of added roots: "
                + report.getAddedRoots().size());
        }

        Set<Resource> roots = report.getAddedRoots();
        LOG.debug(report);

        final Map<String, ResourceType> allTypes = new HashMap<String, ResourceType>();

        for (Resource root : roots) {
            // Make sure all platform, server, and service types are valid. Also, make sure they're fetched - otherwise
            // we'll get persistence exceptions when we try to merge OR persist the platform.
            long rootStart = System.currentTimeMillis();
            if (!initResourceTypes(root, allTypes)) {
                LOG.error("Reported resource [" + root + "] has an unknown type [" + root.getResourceType()
                    + "]. The Agent [" + knownAgent + "] most likely has a plugin named '"
                    + root.getResourceType().getPlugin()
                    + "' installed that is not installed on the Server. Resource will be ignored...");
                continue;
            }

            if (Resource.ROOT != root.getParentResource() && Resource.ROOT_ID == root.getParentResource().getId()) {
                // This is a root resource. Just set it that way
                root.setParentResource(Resource.ROOT);
            }

            mergeResource(root, knownAgent);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Root merged: resource/millis=" + root.getName() + '/'
                    + (System.currentTimeMillis() - rootStart));
            }
        }

        allTypes.clear(); // help GC, we don't need this anymore

        // Prepare the ResourceSyncInfo tree which contains all the info the PC needs to sync itself up with us.
        // The platform can be null in only one scenario.. a brand new agent has connected to the server
        // and that agent is currently trying to upgrade its resources. For that it asks us to send down
        // the current inventory on the server side. But at this point there isn't any since that very
        // agent just registered and is starting up for the very first time and therefore hasn't had
        // a chance yet to send us its full inventory report.
        ResourceSyncInfo syncInfo = discoveryBoss.getResourceSyncInfo(knownAgent);

        // we need to also tell the agent if there were any ignored types - we must provide the agent with
        // ALL types that are ignored, not just for those resources that were in the report
        ResourceTypeCriteria ignoredTypesCriteria = new ResourceTypeCriteria();
        ignoredTypesCriteria.addFilterIgnored(true);
        ignoredTypesCriteria.setPageControl(PageControl.getUnlimitedInstance());
        PageList<ResourceType> ignoredTypes = resourceTypeManager.findResourceTypesByCriteria(
            subjectManager.getOverlord(), ignoredTypesCriteria);

        MergeInventoryReportResults results;
        if (syncInfo != null) {
            results = new MergeInventoryReportResults(syncInfo, ignoredTypes);
        } else {
            results = null;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Inventory merge completed in (" + (System.currentTimeMillis() - start) + ")ms");
        }

        return results;
    }

    @Override
    public ResourceSyncInfo getResourceSyncInfo(Agent knownAgent) {
        Resource platform = resourceManager.getPlatform(knownAgent);
        if (null == platform) {
            return null;
        }

        ResourceSyncInfo result = entityManager.find(ResourceSyncInfo.class, platform.getId());
        return result;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Map<Resource, List<Resource>> getQueuedPlatformsAndServers(Subject user, PageControl pc) {
        return getQueuedPlatformsAndServers(user, EnumSet.of(InventoryStatus.NEW), pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public Map<Resource, List<Resource>> getQueuedPlatformsAndServers(Subject user, EnumSet<InventoryStatus> statuses,
        PageControl pc) {
        // pc.initDefaultOrderingField("res.ctime", PageOrdering.DESC); // this is set in getQueuedPlatforms,

        // maps a platform to a list of child servers
        Map<Resource, List<Resource>> queuedResources = new HashMap<Resource, List<Resource>>();

        List<Resource> queuedPlatforms = getQueuedPlatforms(user, statuses, pc);
        for (Resource platform : queuedPlatforms) {
            List<Resource> queuedServers = new ArrayList<Resource>();
            for (InventoryStatus status : statuses) {
                queuedServers.addAll(getQueuedPlatformChildServers(user, status, platform));
            }
            queuedResources.put(platform, queuedServers);
        }
        return queuedResources;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    @SuppressWarnings("unchecked")
    public PageList<Resource> getQueuedPlatforms(Subject user, EnumSet<InventoryStatus> statuses, PageControl pc) {
        pc.initDefaultOrderingField("res.ctime", PageOrdering.DESC); // show the newest ones first by default

        Query queryCount = PersistenceUtility.createCountQuery(entityManager,
            Resource.QUERY_FIND_QUEUED_PLATFORMS_BY_INVENTORY_STATUS);

        queryCount.setParameter("inventoryStatuses", statuses);
        long count = (Long) queryCount.getSingleResult();

        List<Resource> results;
        if (count > 0) {
            Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
                Resource.QUERY_FIND_QUEUED_PLATFORMS_BY_INVENTORY_STATUS, pc);

            query.setParameter("inventoryStatuses", statuses);
            results = query.getResultList();
        } else
            results = Collections.emptyList();

        return new PageList<Resource>(results, (int) count, pc);
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public List<Resource> getQueuedPlatformChildServers(Subject user, InventoryStatus status, Resource platform) {
        PageList<Resource> childServers = resourceManager.findChildResourcesByCategoryAndInventoryStatus(user,
            platform, ResourceCategory.SERVER, status, PageControl.getUnlimitedInstance());

        return childServers;
    }

    @RequiredPermission(Permission.MANAGE_INVENTORY)
    public void updateInventoryStatus(Subject user, List<Resource> platforms, List<Resource> servers,
        InventoryStatus status) {
        long start = System.currentTimeMillis();

        // need to attach the resources
        List<Resource> attachedPlatforms = new ArrayList<Resource>(platforms.size());
        for (Resource p : platforms) {
            attachedPlatforms.add(entityManager.find(Resource.class, p.getId()));
        }

        platforms = attachedPlatforms;

        List<Resource> attachedServers = new ArrayList<Resource>(servers.size());
        for (Resource s : servers) {
            attachedServers.add(entityManager.find(Resource.class, s.getId()));
        }

        servers = attachedServers;

        // Update and persist the actual inventory statuses
        // This is done is a separate transaction to stop failures in the agent from rolling back the transaction
        discoveryBoss.updateInventoryStatusInNewTransaction(user, platforms, servers, status);

        scheduleAgentInventoryOperationJob(platforms, servers);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Inventory status set to [" + status + "] for [" + platforms.size() + "] platforms and ["
                + servers.size() + "] servers in [" + (System.currentTimeMillis() - start) + "]ms");
        }
    }

    private boolean isJobScheduled(Scheduler scheduler, String name, String group) {
        boolean isScheduled = false;
        try {
            JobDetail jobDetail = scheduler.getJobDetail(name, group);
            if (jobDetail != null) {
                isScheduled = true;
            }
        } catch (SchedulerException se) {
            LOG.error("Error getting job detail", se);
        }
        return isScheduled;
    }

    private void scheduleAgentInventoryOperationJob(List<Resource> platforms, List<Resource> servers) {
        Scheduler scheduler = LookupUtil.getSchedulerBean();
        try {
            final String DEFAULT_JOB_NAME = "AgentInventoryUpdateJob";
            final String DEFAULT_JOB_GROUP = "AgentInventoryUpdateGroup";
            final String TRIGGER_PREFIX = "AgentInventoryUpdateTrigger";

            final String randomSuffix = UUID.randomUUID().toString();

            final String triggerName = TRIGGER_PREFIX + " - " + randomSuffix;
            SimpleTrigger trigger = new SimpleTrigger(triggerName, DEFAULT_JOB_GROUP, new Date());

            JobDataMap jobDataMap = new JobDataMap();
            jobDataMap.put(AgentInventoryStatusUpdateJob.KEY_TRIGGER_NAME, triggerName);
            jobDataMap.put(AgentInventoryStatusUpdateJob.KEY_TRIGGER_GROUP_NAME, DEFAULT_JOB_GROUP);
            AgentInventoryStatusUpdateJob.externalizeJobValues(jobDataMap,
                AgentInventoryStatusUpdateJob.PLATFORMS_COMMA_LIST, platforms);
            AgentInventoryStatusUpdateJob.externalizeJobValues(jobDataMap,
                AgentInventoryStatusUpdateJob.SERVERS_COMMA_LIST, servers);

            trigger.setJobName(DEFAULT_JOB_NAME);
            trigger.setJobGroup(DEFAULT_JOB_GROUP);
            trigger.setJobDataMap(jobDataMap);

            if (isJobScheduled(scheduler, DEFAULT_JOB_NAME, DEFAULT_JOB_GROUP)) {
                scheduler.scheduleJob(trigger);
            } else {
                JobDetail jobDetail = new JobDetail(DEFAULT_JOB_NAME, DEFAULT_JOB_GROUP,
                    AgentInventoryStatusUpdateJob.class);
                scheduler.scheduleJob(jobDetail, trigger);
            }
        } catch (SchedulerException e) {
            LOG.error("Failed to schedule agent inventory update operation.", e);
            updateAgentInventoryStatus(platforms, servers);
        }
    }

    /**
     * Synchronize the agents inventory status for platforms, and then the servers,
     * omitting servers under synced platforms since they will have been handled
     * already. On status change request an agent sync on the affected resources.
     * The agent will sync status and determine what other sync work needs to be
     * performed.
     *
     * @param platforms the platforms in inventory
     * @param servers   the servers in inventory
     */
    public void updateAgentInventoryStatus(List<Resource> platforms, List<Resource> servers) {
        ResourceSyncInfo syncInfo;

        for (Resource platform : platforms) {
            AgentClient agentClient = agentManager.getAgentClient(platform.getAgent());
            if (agentClient != null) {
                try {
                    syncInfo = entityManager.find(ResourceSyncInfo.class, platform.getId());
                    agentClient.getDiscoveryAgentService().synchronizeInventory(syncInfo);
                } catch (Exception e) {
                    LOG.warn("Could not perform commit synchronization with agent for platform [" + platform.getName()
                        + "]", e);
                }
            } else {
                LOG.warn("Could not perform commit sync with agent for platform [" + platform.getName()
                    + "]; will expect agent to do it later");
            }
        }
        for (Resource server : servers) {
            // Only update servers if they haven't already been updated at the platform level
            if (!platforms.contains(server.getParentResource())) {
                AgentClient agentClient = agentManager.getAgentClient(server.getAgent());
                if (agentClient != null) {
                    try {
                        syncInfo = entityManager.find(ResourceSyncInfo.class, server.getId());
                        agentClient.getDiscoveryAgentService().synchronizeInventory(syncInfo);
                    } catch (Exception e) {
                        LOG.warn("Could not perform commit synchronization with agent for server [" + server.getName()
                            + "]", e);
                    }
                } else {
                    LOG.warn("Could not perform commit sync with agent for server [" + server.getName()
                        + "]; will expect agent to do it later");
                }
            }
        }
    }

    public void updateAgentInventoryStatus(String platformsCsvList, String serversCsvList) {
        List<Resource> platforms = new ArrayList<Resource>();
        AgentInventoryStatusUpdateJob.internalizeJobValues(entityManager, platformsCsvList, platforms);

        List<Resource> servers = new ArrayList<Resource>();
        AgentInventoryStatusUpdateJob.internalizeJobValues(entityManager, serversCsvList, servers);

        updateAgentInventoryStatus(platforms, servers);
    }

    /**
     * Updates statuses according to the inventory rules. This is used internally - never call this yourself without
     * knowing what you do. See {@link #updateInventoryStatus(Subject, List, List, InventoryStatus)} for the "public"
     * version.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateInventoryStatusInNewTransaction(Subject user, List<Resource> platforms, List<Resource> servers,
        InventoryStatus status) {
        for (Resource platform : platforms) {
            resourceManager.setResourceStatus(user, platform, status, false);
        }

        for (Resource server : servers) {
            resourceManager.setResourceStatus(user, server, status, true);
        }
        if (status == InventoryStatus.COMMITTED) {
            List<Integer> allResourceIds = new ArrayList<Integer>();
            for (Resource platform : platforms) {
                allResourceIds.add(platform.getId());
            }
            for (Resource server : servers) {
                allResourceIds.add(server.getId());
            }
            resourceAvailabilityManager.insertNeededAvailabilityForImportedResources(allResourceIds);
        }
    }

    public Resource manuallyAddResource(Subject subject, int resourceTypeId, int parentResourceId,
        Configuration pluginConfiguration) throws Exception {

        Resource result = null;

        ResourceType resourceType = this.resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
        // the subsequent call to manuallyAddResource requires a detached ResourceType param so clear
        entityManager.clear();
        MergeResourceResponse response = manuallyAddResource(subject, resourceType, parentResourceId,
            pluginConfiguration);
        result = this.resourceManager.getResourceById(subject, response.getResourceId());

        return result;
    }

    public MergeResourceResponse manuallyAddResource(Subject user, ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration) throws InvalidPluginConfigurationClientException, PluginContainerException {
        if (!this.authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parentResourceId)) {
            throw new PermissionException("You do not have permission on resource with id " + parentResourceId
                + " to manually add child resources.");
        }

        Resource parentResource = this.resourceManager.getResourceById(user, parentResourceId);

        if (!resourceType.isSupportsManualAdd()) {
            throw new RuntimeException("Cannot manually add " + resourceType + " child Resource under parent "
                + parentResource + ", since the " + resourceType + " type does not support manual add.");
        }

        abortResourceManualAddIfExistingSingleton(parentResource, resourceType);

        MergeResourceResponse mergeResourceResponse;
        try {
            AgentClient agentClient = this.agentManager.getAgentClient(parentResource.getAgent());
            DiscoveryAgentService discoveryAgentService = agentClient.getDiscoveryAgentService();
            mergeResourceResponse = discoveryAgentService.manuallyAddResource(resourceType, parentResourceId,
                pluginConfiguration, user.getId());
        } catch (CannotConnectException e) {
            throw new CannotConnectToAgentException("Error adding [" + resourceType + "] Resource to inventory as "
                + "a child of " + parentResource + " - cause: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error adding [" + resourceType + "] Resource to inventory as a child of "
                + parentResource + " - cause: " + e, e);
        }

        return mergeResourceResponse;
    }

    public MergeResourceResponse addResource(Resource resource, int creatorSubjectId) {
        MergeResourceResponse mergeResourceResponse;
        try {
            validateResource(resource);

        } catch (InvalidInventoryReportException e) {
            throw new IllegalStateException("Plugin Container sent an invalid Resource - " + e.getLocalizedMessage());
        }
        if (!initResourceTypes(resource)) {
            throw new IllegalStateException("Plugin Container sent a Resource with an unknown type - "
                + resource.getResourceType());
        }

        Resource existingResource = findExistingResource(resource, null);
        if (existingResource != null) {
            mergeResourceResponse = new MergeResourceResponse(existingResource.getId(), true);
        } else {
            Subject creator = this.subjectManager.getSubjectById(creatorSubjectId);
            try {
                creator = this.subjectManager.loginUnauthenticated(creator.getName());
            } catch (LoginException e) {
                throw new IllegalStateException(
                    "Unable to temporarily login to provided resource creator user for resource creation", e);
            }

            Resource parentResource = this.resourceManager.getResourceById(creator, resource.getParentResource()
                .getId());
            resource.setAgent(parentResource.getAgent());
            resource.setModifiedBy(creator.getName());

            // Manually added resources are auto-committed.
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            resource.setItime(System.currentTimeMillis());
            try {
                this.resourceManager.createResource(creator, resource, parentResource.getId());
            } catch (ResourceAlreadyExistsException e) {
                throw new IllegalStateException(e);
            }

            mergeResourceResponse = new MergeResourceResponse(resource.getId(), false);
        }

        return mergeResourceResponse;
    }

    public boolean updateResourceVersion(int resourceId, String version) {
        Resource existingResource = this.entityManager.find(Resource.class, resourceId);
        if (existingResource != null) {
            boolean changed = updateResourceVersion(existingResource, version);
            if (changed) {
                this.entityManager.merge(existingResource);
            }
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    public Set<ResourceUpgradeResponse> upgradeResources(Set<ResourceUpgradeRequest> upgradeRequests) {
        Set<ResourceUpgradeResponse> result = new HashSet<ResourceUpgradeResponse>();

        boolean allowGenericPropertiesUpgrade = Boolean.parseBoolean(systemManager.getSystemConfiguration(
            subjectManager.getOverlord()).getProperty(RHQConstants.AllowResourceGenericPropertiesUpgrade, "false"));

        for (ResourceUpgradeRequest request : upgradeRequests) {
            Resource existingResource = this.entityManager.find(Resource.class, request.getResourceId());
            if (existingResource != null) {
                try {
                    ResourceUpgradeResponse upgradedData = upgradeResource(existingResource, request,
                        allowGenericPropertiesUpgrade);
                    if (upgradedData != null) {
                        result.add(upgradedData);
                    }
                } catch (Exception e) {
                    LOG.error("Failed to process upgrade request for resource " + existingResource + ".", e);
                }
            }
        }
        return result;
    }

    /**
     * Convenience method that looks at <code>resource</code> and if its version is not
     * the same as <code>newVersion</code>, its version string will be set to it. If
     * the resource's version was different and was changed by this method, <code>true</code>
     * will be returned.
     *
     * @param resource the resource whose version is to be checked
     * @param newVersion what the version of the resource should be
     *
     * @return <code>true</code> if the resource's version was not <code>newVersion</code> and was
     *         changed to it. <code>false</code> if the version was already the same as <code>newVersion</code>
     *         or <code>resource</code> was <code>null</code>. In other words, this returns <code>true</code>
     *         if the resource's version was actually changed.
     */
    private boolean updateResourceVersion(Resource resource, String newVersion) {
        boolean versionChanged = false;
        if (resource != null) {
            String oldVersion = resource.getVersion();

            // we consider null and "" versions the same - and they should not be product versions
            if (oldVersion == null) {
                oldVersion = "";
            }

            if (newVersion == null) {
                newVersion = "";
            }

            versionChanged = !oldVersion.equals(newVersion);

            if (versionChanged) {
                LOG.info("Resource [" + resource + "] changed its version from [" + oldVersion + "] to [" + newVersion
                    + "]");
                resource.setVersion(newVersion);

                ProductVersion productVersion = null;
                if (newVersion.length() > 0) {
                    productVersion = productVersionManager.addProductVersion(resource.getResourceType(), newVersion);
                }
                resource.setProductVersion(productVersion);
            }
        }
        return versionChanged;
    }

    /**
     * @param resource NotNull
     * @param upgradeRequest
     * @param allowGenericPropertiesUpgrade name and description are only upgraded if this is true
     * @return response to the upgrade request detailing what has been accepted on the server side
     */
    private ResourceUpgradeResponse upgradeResource(Resource resource, ResourceUpgradeRequest upgradeRequest,
        boolean allowGenericPropertiesUpgrade) {
        if (upgradeRequest.getUpgradeErrorMessage() != null) {
            ResourceError error = new ResourceError(resource, ResourceErrorType.UPGRADE,
                upgradeRequest.getUpgradeErrorMessage(), upgradeRequest.getUpgradeErrorStackTrace(),
                upgradeRequest.getTimestamp());
            resourceManager.addResourceError(error);
            return null;
        }

        ResourceUpgradeResponse ret = new ResourceUpgradeResponse();
        ret.setResourceId(resource.getId());

        if (upgradeRequest.hasSomethingToUpgrade()) {

            String resourceKey = upgradeRequest.getNewResourceKey();
            String name = upgradeRequest.getNewName();
            String description = upgradeRequest.getNewDescription();

            StringBuilder logMessage = new StringBuilder("Resource [").append(resource.toString()).append(
                "] upgraded its ");

            if (needsUpgrade(resource.getResourceKey(), resourceKey)) {
                resource.setResourceKey(resourceKey);
                logMessage.append("resourceKey, ");
                ret.setUpgradedResourceKey(resource.getResourceKey());
            }

            if (allowGenericPropertiesUpgrade && needsUpgrade(resource.getName(), name)) {
                resource.setName(name);
                logMessage.append("name, ");
                ret.setUpgradedResourceName(resource.getName());
            }

            if (allowGenericPropertiesUpgrade && needsUpgrade(resource.getDescription(), description)) {
                resource.setDescription(description);
                logMessage.append("description, ");
                ret.setUpgradedResourceDescription(resource.getDescription());
            }

            // If provided, assume the new plugin config should replace the old plugin config in its entirety.
            // Use a deep copy without ids as the updgardeRequest config may contain entity config props.
            // Note: we explicitly do not call configurationManager.updatePluginConfiguration() because the
            // agent is already updated to the new configuration. Instead we call the dedicated local method
            // supporting this use case.
            Configuration pluginConfig = upgradeRequest.getNewPluginConfiguration();
            if (null != pluginConfig) {
                pluginConfig = pluginConfig.deepCopy(false);
                PluginConfigurationUpdate update = configurationManager.upgradePluginConfiguration(
                    subjectManager.getOverlord(), resource.getId(), pluginConfig);
                ret.setUpgradedResourcePluginConfiguration(update.getResource().getPluginConfiguration());
            }

            // finally let's remove the potential previous upgrade error. we've now successfully
            // upgraded the resource.
            List<ResourceError> upgradeErrors = resourceManager.findResourceErrors(subjectManager.getOverlord(),
                resource.getId(), ResourceErrorType.UPGRADE);
            for (ResourceError error : upgradeErrors) {
                entityManager.remove(error);
            }

            logMessage.replace(logMessage.length() - 1, logMessage.length(), "to become [").append(resource.toString())
                .append("]");

            LOG.info(logMessage.toString());
        }

        return ret;
    }

    private void validateInventoryReport(InventoryReport report) throws InvalidInventoryReportException {
        for (Resource root : report.getAddedRoots()) {
            validateResource(root);
        }
    }

    /**
     * @param resource This can be a detached object
     * @throws InvalidInventoryReportException
     */
    private void validateResource(Resource resource) throws InvalidInventoryReportException {
        if (resource.getResourceType() == null) {
            throw new InvalidInventoryReportException("Reported resource [" + resource + "] has a null type.");
        }

        if (resource.getResourceKey() == null) {
            throw new InvalidInventoryReportException("Reported resource [" + resource + "] has a null key.");
        }

        if (resource.getInventoryStatus() == InventoryStatus.DELETED) {
            throw new InvalidInventoryReportException(
                "Reported resource ["
                    + resource
                    + "] has an illegal inventory status of 'DELETED' - agents are not allowed to delete platforms from inventory.");
        }

        // Recursively validate all the resource's descendants.
        for (Resource childResource : resource.getChildResources()) {
            validateResource(childResource);
        }
    }

    enum PostMergeAction {
        LINK_STORAGE_NODE
    };

    /**
     * <p>Should Not Be Called With Existing Transaction !!!</p>
     *
     * <p>Merges the specified resource and its children into inventory. If the resource already exists in inventory,
     * it is updated; if it does not already exist in inventory, it is added and its parent is set to the specified, already inventoried,
     * parent resource.</p>
     *
     * <p>Does not require an existing transaction.  The resource and each child will be merged in an isolated
     * transaction</p>
     *
     * @param  resource       NotNull pojo, the resource to be merged, should have parent and children pojos set
     * @param  agent          NotNull detached entity, the agent that should be set on the resource being merged
     *
     * @throws InvalidInventoryReportException if a critical field in the resource is missing or invalid
     */
    private void mergeResource(Resource resource, Agent agent) throws InvalidInventoryReportException {

        long start = System.currentTimeMillis();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Merging [" + resource + "]...");
        }

        // We don't merge the entire resource tree. Instead we batch them in order to reduce transaction overhead
        // while ensuring no transaction is too big (and thus risks timeout). To do this we need to flatten the
        // tree and chunk through it.  Parents must be merged before children, so use a breadth first approach.
        // NOTE: this will also strip out all resources that are to be ignored; thus, ignored resources won't get merged
        List<Resource> resourceList = treeToBreadthFirstList(resource);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Preparing to merge [" + resourceList.size() + "] Resources with a batch size of ["
                + MERGE_BATCH_SIZE + "]");
        }

        Map<Resource, Set<PostMergeAction>> postMergeActions = new HashMap<Resource, Set<PostMergeAction>>();
        while (!resourceList.isEmpty()) {
            int size = resourceList.size();
            int end = (MERGE_BATCH_SIZE < size) ? MERGE_BATCH_SIZE : size;

            List<Resource> resourceBatch = resourceList.subList(0, end);
            discoveryBoss.mergeResourceInNewTransaction(resourceBatch, agent, postMergeActions);
            if (!postMergeActions.isEmpty()) {
                performPostMergeActions(postMergeActions);
            }

            // Advance our progress and possibly help GC. This will remove the processed resources from the backing list
            resourceBatch.clear();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Resource and children merged: resource/millis=" + resource.getName() + '/'
                + (System.currentTimeMillis() - start));
        }

        return;
    }

    private void performPostMergeActions(Map<Resource, Set<PostMergeAction>> postMergeActions) {
        for (Resource r : postMergeActions.keySet()) {
            for (PostMergeAction a : postMergeActions.get(r)) {
                switch (a) {
                case LINK_STORAGE_NODE:
                    storageNodeManager.linkResource(r);
                }
            }
        }
        postMergeActions.clear();
    }

    private List<Resource> treeToBreadthFirstList(Resource resource) {
        // if we are to ignore this resource's type, don't bother doing anything since all is to be ignored
        if (resource.getResourceType().isIgnored()) {
            return new ArrayList<Resource>(0);
        }

        List<Resource> result = new ArrayList<Resource>(MERGE_BATCH_SIZE);

        LinkedList<Resource> queue = new LinkedList<Resource>();
        queue.add(resource);
        while (!queue.isEmpty()) {
            Resource node = queue.remove();

            // if this node is to be ignored, don't traverse it and don't add it to the returned results
            if (node.getResourceType().isIgnored()) {
                continue;
            }

            result.add(node);
            for (Resource child : node.getChildResources()) {
                queue.add(child);
            }
        }

        return result;
    }

    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void mergeResourceInNewTransaction(List<Resource> resourceBatch, Agent agent,
        Map<Resource, Set<PostMergeAction>> postMergeActions) throws InvalidInventoryReportException {

        long batchStart = System.currentTimeMillis();
        boolean isDebugEnabled = LOG.isDebugEnabled();
        // Cache parent resources we've already fetched from the DB, many resources will have the same parent
        Map<Integer, Resource> parentMap = new HashMap<Integer, Resource>();

        for (Resource resource : resourceBatch) {
            Resource existingResource = null;
            long start = System.currentTimeMillis();

            existingResource = findExistingResource(resource, parentMap);

            // Does this resource already exist in inventory? If so, update, otherwise add
            if (null != existingResource) {
                updateExistingResource(resource, existingResource);

            } else {
                presetAgent(resource, agent);
                persistResource(resource, parentMap, postMergeActions);
            }

            if (isDebugEnabled) {
                LOG.debug("Single Resource merged: resource/millis=" + resource.getName() + '/'
                    + (System.currentTimeMillis() - start));
            }
        }

        // Help out the GC
        parentMap.clear();

        if (isDebugEnabled) {
            long delta = (System.currentTimeMillis() - batchStart);
            LOG.debug("Resource Batch merged: size/average/millis=" + resourceBatch.size() + "/" + delta
                / resourceBatch.size() + "/" + delta);
        }
    }

    /**
     * Recursively set the agent on the resource tree.
     *
     * @param resource pojo, the parent
     * @param agent pojo, the agent
     */
    private void presetAgent(Resource resource, Agent agent) {
        resource.setAgent(agent);
        for (Resource child : resource.getChildResources()) {
            presetAgent(child, agent);
        }
    }

    /**
     * <p>Requires A Transaction</p>
     *
     * Given a resource, will attempt to find it in the server's inventory (that is, finds it in the database). If the
     * given resource's ID does not exist in the database, it will be looked up by its resource key. If the resource
     * cannot be found either via ID or resource key then SIDE EFFECT: the given resource's ID will be reset to 0 and null
     * will be returned.
     *
     * @param resource Pojo containing resourceId, key, and parentResoure (if applicable)
     * @param parentMap, if supplied, holds previously fetched parent pojos. useful when the calling code does many
     * finds for a few parents.  If not found in the map the db will be searched, the map will be updated if possible.
     * @return the Resource entity found in the database and matching the given resource.
     */
    private Resource findExistingResource(Resource resource, Map<Integer, Resource> parentMap) {

        boolean isDebugEnabled = LOG.isDebugEnabled();

        if (isDebugEnabled) {
            LOG.debug("getExistingResource processing for [" + resource + "]");
        }

        Resource existingResource = null;

        if (resource.getId() != 0) {
            if (isDebugEnabled) {
                LOG.debug("Agent claims resource is already in inventory. Id=" + resource.getId());
            }

            // This maybe could be more efficient using a named query that pulls some lazy data, but this should be fine
            existingResource = entityManager.find(Resource.class, resource.getId());
            if (isDebugEnabled) {
                if (null != existingResource) {
                    LOG.debug("Found resource already in inventory. Id=" + resource.getId());
                } else {
                    // agent lied - agent's copy of JON server inventory must be stale.
                    LOG.debug("However, no resource exists with the specified id. Id=" + resource.getId());
                }
            }
        } else {
            LOG.debug("Agent reported resource with id of 0");
        }

        // If necessary double-check for an existing resource using the business key.
        // this will happen if the agent found the resource (non-zero id) but the DB didn't know about it,
        // or if the agent didn't know about it to begin with (id was 0).
        if (existingResource == null) {
            if (isDebugEnabled) {
                LOG.debug("Checking if a resource exists with the specified business key. Id=" + resource.getId()
                    + ", key=" + resource.getResourceKey());
            }

            // (jshaughn) I'm not 100% sure but I believe this loop has to do with either or both of:
            // - protecting against the agent merging a resource it thinks is new but actually exists
            // - handling the case in which a resource type has moved (see f74b22044) and trying to  relocate the parent.
            // Anyway, I'm not going to touch it even though it slows things down.
            ResourceType resourceType = resource.getResourceType();
            Resource parent = resource;

            while (null != parent && null == existingResource) {
                parent = parent.getParentResource();

                // check if the parent is in inventory. This might not be the case during initial sync-up for resource upgrade.
                Resource existingParent = null;
                if (null != parent) {
                    int parentId = parent.getId();

                    if (parentId <= 0) {
                        LOG.warn("Expected potential parent resource to have a valid ID. Parent=" + parent + ", Child="
                            + resource);
                    }

                    // See if we already fetched this parent and it's in our cache map, if so, use it.
                    if (null != parentMap) {
                        existingParent = parentMap.get(parentId);
                    }

                    if (null == existingParent) {
                        // I think getReference may be slightly faster here but it's likely negligible
                        existingParent = entityManager.getReference(Resource.class, parentId);
                        if (null != existingParent) {
                            if (null != parentMap) {
                                parentMap.put(parentId, existingParent);
                            }
                        } else {
                            // this parent is not known to the server, so there's no point in trying to find a child of it...
                            continue;
                        }
                    }
                }

                // We found the parent in inventory, so now see if we can find this resource in inventory by using
                // the parent, the resource key (unique among siblings), the plugin and the type.
                Query query = entityManager.createNamedQuery(Resource.QUERY_FIND_BY_PARENT_AND_KEY);
                query.setParameter("parent", existingParent);
                query.setParameter("key", resource.getResourceKey());
                query.setParameter("plugin", resourceType.getPlugin());
                query.setParameter("typeName", resourceType.getName());
                try {
                    existingResource = (Resource) query.getSingleResult();
                } catch (NoResultException e) {
                    existingResource = null;
                }
            }

            if (null != existingResource) {
                // We found it - reset the id to what it should be.
                resource.setId(existingResource.getId());
                if (isDebugEnabled) {
                    LOG.debug("Found resource already in inventory with specified business key, Id=" + resource.getId());
                }

            } else {
                LOG.debug("Unable to find the agent-reported resource by id or business key.");

                if (resource.getId() != 0) {
                    // existingResource is still null at this point, the resource does not exist in inventory.
                    LOG.error("Resetting the resource's id to zero. Previous Id=" + resource.getId());
                    resource.setId(0);
                    // TODO: Is there anything else we should do here to inform the agent it has an out-of-sync resource?

                } else {
                    LOG.debug("Resource's id was already zero, nothing to do for the merge.");
                }
            }
        }

        return existingResource;
    }

    /**
     * <p>Requires A Transaction.</p>
     * @param updatedResource pojo
     * @param existingResource attached entity
     * @throws InvalidInventoryReportException
     */
    private void updateExistingResource(Resource updatedResource, Resource existingResource)
        throws InvalidInventoryReportException {
        /*
         * there exists a small window of time after the synchronous part of the uninventory and before the async
         * quartz job comes along to perform the actual removal of the resource from the database, that an inventory
         * report can come across the wire and !OVERWROTE! the UNINVENTORIED status back to COMMITTED.  if we find,
         * during an inventory report merge, that the existing resource was already uninventoried (indicating that
         * the quartz job has not yet come along to remove this resource from the database) we should stop all
         * processing from this node and return immediately.  this short-cuts the processing for the entire sub-tree
         * under this resource, but that's OK because the in-band uninventory logic will have marked entire sub-tree
         * for uninventory atomically.  in other words, all of the descendants under a resource would also be marked
         * for async uninventory too.
         */
        if (existingResource.getInventoryStatus() == InventoryStatus.UNINVENTORIED) {
            return;
        }

        Resource existingParent = existingResource.getParentResource();
        Resource updatedParent = updatedResource.getParentResource();
        ResourceType existingResourceParentType = (existingParent != null) ? existingResource.getParentResource()
            .getResourceType() : null;
        ResourceType updatedResourceParentType = (updatedParent != null) ? updatedResource.getParentResource()
            .getResourceType() : null;
        Set<ResourceType> validParentTypes = existingResource.getResourceType().getParentResourceTypes();

        if (validParentTypes != null && !validParentTypes.isEmpty()
            && !validParentTypes.contains(existingResourceParentType)) {

            // The existing Resource has an invalid parent ResourceType. This may be because its ResourceType was moved
            // to a new parent ResourceType, but its new parent was not yet discovered at the time of the type move. See
            // if the Resource reported by the Agent has a valid parent type, and, if so, update the existing Resource's
            // parent to that type.

            if (validParentTypes.contains(updatedResourceParentType)) {
                if (existingResource.getParentResource() != null) {
                    existingResource.getParentResource().removeChildResource(existingResource);
                }
                if (updatedParent != Resource.ROOT) {
                    // get attached entity for parent so we can add the child
                    updatedParent = entityManager.find(Resource.class, updatedParent.getId());
                    updatedParent.addChildResource(existingResource);
                } else {
                    existingResource.setParentResource(Resource.ROOT);
                }

            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Existing Resource " + existingResource + " has invalid parent type ("
                        + existingResourceParentType + ") and so does plugin-reported Resource " + updatedResource
                        + " (" + updatedResourceParentType + ") - valid parent types are [" + validParentTypes + "].");
                }
            }
        }

        // The below block is for Resources that were created via the RHQ GUI, whose descriptions will be null.
        if (existingResource.getDescription() == null && updatedResource.getDescription() != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Setting description of existing resource with id " + existingResource.getId() + " to '"
                    + updatedResource.getDescription() + "' (as reported by agent)...");
            }
            existingResource.setDescription(updatedResource.getDescription());
        }

        // Log a warning if the agent says the Resource key has changed (should rarely happen).
        if ((existingResource.getResourceKey() != null)
            && !existingResource.getResourceKey().equals(updatedResource.getResourceKey())) {

            LOG.warn("Agent reported that key for " + existingResource + " has changed from '"
                + existingResource.getResourceKey() + "' to '" + updatedResource.getResourceKey() + "'.");
        }

        updateResourceVersion(existingResource, updatedResource.getVersion());

        // If the resource was marked as deleted, reactivate it again.
        if (existingResource.getInventoryStatus() == InventoryStatus.DELETED) {
            existingResource.setInventoryStatus(InventoryStatus.COMMITTED);
            existingResource.setPluginConfiguration(updatedResource.getPluginConfiguration());
            existingResource.setAgentSynchronizationNeeded();
        }

        return;
    }

    private boolean initResourceTypes(Resource resource) {
        final HashMap<String, ResourceType> types = new HashMap<String, ResourceType>();
        try {
            return initResourceTypes(resource, types);
        } finally {
            types.clear(); // help GC
        }
    }

    /**
     * recursively assign (detached) ResourceType entities to the resource tree
     * @param resource
     * @param loadedTypeMap Empty map to start, filled as we go to minimize DB fetches
     * @return false if a resource's type is unknown; true if all types were successfully loaded
     */
    private boolean initResourceTypes(Resource resource, Map<String, ResourceType> loadedTypeMap) {

        String plugin = resource.getResourceType().getPlugin();
        String name = resource.getResourceType().getName();
        StringBuilder key = new StringBuilder(plugin);
        key.append(":::");
        key.append(name);
        ResourceType resourceType = loadedTypeMap.get(key.toString());

        if (null == resourceType) {
            try {
                resourceType = this.resourceTypeManager.getResourceTypeByNameAndPlugin(name, plugin);
            } catch (RuntimeException e) {
                resourceType = null;
            }

            if (null == resourceType) {
                LOG.error("Reported resource [" + resource + "] has an unknown type [" + resource.getResourceType()
                    + "]. The Agent most likely has a plugin named '" + plugin
                    + "' installed that is not installed on the Server. Resource will be ignored...");
                return false;
            } else {
                loadedTypeMap.put(key.toString(), resourceType);
            }
        }

        resource.setResourceType(resourceType);

        // don't bother looking at the children if we are just going to ignore this resource
        if (resourceType.isIgnored()) {
            return true;
        }

        for (Iterator<Resource> childIterator = resource.getChildResources().iterator(); childIterator.hasNext();) {
            Resource child = childIterator.next();
            if (!initResourceTypes(child, loadedTypeMap)) {
                childIterator.remove();
            }
        }

        return true;
    }

    private void persistResource(Resource resource, Map<Integer, Resource> parentMap,
        Map<Resource, Set<PostMergeAction>> postMergeActions) {

        // Id of detached parent resource
        Integer parentId = (null != resource.getParentResource()) ? resource.getParentResource().getId() : null;

        // attached parentResource
        Resource parentResource = null;

        if (null != parentId) {
            // look in our map cache first
            if (null != parentMap) {
                parentResource = parentMap.get(parentId);
            }
            // if not in cache, try the DB
            if (null == parentResource) {
                // Find the parent resource entity
                parentResource = entityManager.find(Resource.class, parentId);
            }
            // if the parent exists, create the parent-child relationship and add it to the map
            if (null != parentResource) {
                parentResource.addChildResource(resource);
                parentMap.put(parentId, parentResource);
            }
        }

        entityManager.persist(resource);

        // Add a product version entry for the new resource.
        if ((resource.getVersion() != null) && (resource.getVersion().length() > 0)) {
            ResourceType type = resource.getResourceType();
            ProductVersion productVersion = productVersionManager.addProductVersion(type, resource.getVersion());
            resource.setProductVersion(productVersion);
        }

        // Ensure the new resource has an owner and modifier of superUser.
        Subject overlord = subjectManager.getOverlord();

        resource.setItime(System.currentTimeMillis());
        resource.setModifiedBy(overlord.getName());

        setInventoryStatus(parentResource, resource, postMergeActions);

        // Extend implicit (recursive) group membership of the parent to the new child
        if (null != parentResource) {
            groupManager.updateImplicitGroupMembership(overlord, resource);
        }
    }

    // Resources are set to either NEW or COMMITTED
    // We autocommit in the following scenarios:
    // - The parent resource is not a platform and is already committed
    // - The resource is a platform and has an RHQ Storage Node child
    // - The resource is an RHQ Storage Node child
    // Ensure the new resource has the proper inventory status
    private void setInventoryStatus(Resource parentResource, Resource resource,
        Map<Resource, Set<PostMergeAction>> postMergeActions) {

        // never autocommit a platform
        if (null == parentResource) {
            resource.setInventoryStatus(InventoryStatus.NEW);
            return;
        }

        ResourceType resourceType = resource.getResourceType();
        boolean isParentCommitted = InventoryStatus.COMMITTED == parentResource.getInventoryStatus();
        boolean isService = ResourceCategory.SERVICE == resourceType.getCategory();
        boolean isParentServer = ResourceCategory.SERVER == parentResource.getResourceType().getCategory();

        // always autocommit non-top-level-server children of committed parents
        if (isParentCommitted && (isService || isParentServer)) {
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
            return;
        }

        // always autocommit top-level-server if it's an RHQ Storage Node (and the platform, if necessary)
        boolean isStorageNodePlugin = "RHQStorage".equals(resourceType.getPlugin());
        boolean isStorageNode = (isStorageNodePlugin && "RHQ Storage Node".equals(resourceType.getName()));

        if (isStorageNode) {
            resource.setInventoryStatus(InventoryStatus.COMMITTED);

            if (!isParentCommitted) {
                parentResource.setInventoryStatus(InventoryStatus.COMMITTED);
            }

            addPostMergeAction(postMergeActions, resource, PostMergeAction.LINK_STORAGE_NODE);

            return;
        }

        // otherwise, set NEW
        resource.setInventoryStatus(InventoryStatus.NEW);

        return;
    }

    private void addPostMergeAction(Map<Resource, Set<PostMergeAction>> postMergeActions, Resource resource,
        PostMergeAction action) {
        if (postMergeActions.containsKey(resource)) {
            postMergeActions.get(resource).add(action);
        } else {
            postMergeActions.put(resource, EnumSet.of(PostMergeAction.LINK_STORAGE_NODE));
        }
    }

    public void importResources(Subject subject, int[] resourceIds) {
        if (resourceIds == null || resourceIds.length == 0) {
            return;
        }
        checkStatus(subject, resourceIds, InventoryStatus.COMMITTED, EnumSet.of(InventoryStatus.NEW));
    }

    public void ignoreResources(Subject subject, int[] resourceIds) {
        if (resourceIds == null || resourceIds.length == 0) {
            return;
        }

        checkStatus(subject, resourceIds, InventoryStatus.IGNORED,
            EnumSet.of(InventoryStatus.NEW, InventoryStatus.COMMITTED));

        // We want to set all availabilities for the ignored resources to "unknown" since we won't be tracking them anymore.
        // This is more of a convienence; if it fails for any reason, just log an error but don't roll back ignoring the resources.
        try {
            this.availabilityManager.setResourceAvailabilities(resourceIds, AvailabilityType.UNKNOWN);
        } catch (Exception e) {
            LOG.error("Failed to reset availabilities for resources being ignored: " + ThrowableUtil.getAllMessages(e));
        }

        return;
    }

    public void unignoreResources(Subject subject, int[] resourceIds) {
        if (resourceIds == null || resourceIds.length == 0) {
            return;
        }
        checkStatus(subject, resourceIds, InventoryStatus.NEW, EnumSet.of(InventoryStatus.IGNORED));
    }

    public void unignoreAndImportResources(Subject subject, int[] resourceIds) {
        if (resourceIds == null || resourceIds.length == 0) {
            return;
        }
        checkStatus(subject, resourceIds, InventoryStatus.COMMITTED, EnumSet.of(InventoryStatus.IGNORED));
    }

    @SuppressWarnings("unchecked")
    private void checkStatus(Subject subject, int[] resourceIds, InventoryStatus target,
        EnumSet<InventoryStatus> validStatuses) {
        Query query = entityManager.createQuery("" //
            + "  SELECT res.inventoryStatus " //
            + "    FROM Resource res " //
            + "   WHERE res.id IN ( :resourceIds ) " //
            + "GROUP BY res.inventoryStatus ");
        List<Integer> resourceIdList = ArrayUtils.wrapInList(resourceIds);

        // Do one query per 1000 Resource id's to prevent Oracle from failing because of an IN clause with more
        // than 1000 items.
        // After the below while loop completes, this Set will contain the statuses represented by the Resources with
        // the passed in id's.
        Set<InventoryStatus> statuses = EnumSet.noneOf(InventoryStatus.class);
        int fromIndex = 0;
        while (fromIndex < resourceIds.length) {
            int toIndex = (resourceIds.length < (fromIndex + 1000)) ? resourceIds.length : (fromIndex + 1000);

            List<Integer> resourceIdSubList = resourceIdList.subList(fromIndex, toIndex);
            query.setParameter("resourceIds", resourceIdSubList);
            List<InventoryStatus> batchStatuses = query.getResultList();
            statuses.addAll(batchStatuses);

            fromIndex = toIndex;
        }

        if (!validStatuses.containsAll(statuses)) {
            throw new IllegalArgumentException("Can only set inventory status to [" + target
                + "] for Resources with current inventory status of one of [" + validStatuses + "].");
        }

        // Do one query per 1000 Resource id's to prevent Oracle from failing because of an IN clause with more
        // than 1000 items.
        List<Resource> resources = new ArrayList<Resource>(resourceIds.length);
        fromIndex = 0;
        while (fromIndex < resourceIds.length) {
            int toIndex = (resourceIds.length < (fromIndex + 1000)) ? resourceIds.length : (fromIndex + 1000);

            int[] resourceIdSubArray = Arrays.copyOfRange(resourceIds, fromIndex, toIndex);
            PageList<Resource> batchResources = resourceManager.findResourceByIds(subject, resourceIdSubArray, false,
                PageControl.getUnlimitedInstance());
            resources.addAll(batchResources);

            fromIndex = toIndex;
        }

        // Split the Resources into two lists - one for platforms and one for servers, since that's what
        // updateInventoryStatus() expects.
        List<Resource> platforms = new ArrayList<Resource>();
        List<Resource> servers = new ArrayList<Resource>();
        for (Resource resource : resources) {
            ResourceCategory category = resource.getResourceType().getCategory();
            if (category == ResourceCategory.PLATFORM) {
                if (target == InventoryStatus.IGNORED && (resource.getInventoryStatus() == InventoryStatus.COMMITTED)) {
                    LOG.warn("Cannot ignore a committed platform - skipping request to ignore:" + resource);
                } else {
                    platforms.add(resource);
                }
            } else {
                servers.add(resource); // we include services in here now (see BZ 535289)
            }
        }

        updateInventoryStatus(subject, platforms, servers, target);
    }

    private <T> boolean needsUpgrade(T oldValue, T newValue) {
        return newValue != null && (oldValue == null || !newValue.equals(oldValue));
    }

    private void abortResourceManualAddIfExistingSingleton(Resource parentResource, ResourceType resourceType) {
        if (resourceType.isSingleton()) {
            ResourceCriteria resourceCriteria = new ResourceCriteria();
            resourceCriteria.addFilterParentResourceId(parentResource.getId());
            resourceCriteria.addFilterResourceTypeId(resourceType.getId());
            resourceCriteria.clearPaging();//Doc: disable paging as the code assumes all the results will be returned.

            PageList<Resource> childResourcesOfType = resourceManager.findResourcesByCriteria(
                subjectManager.getOverlord(), resourceCriteria);
            if (childResourcesOfType.size() >= 1) {
                throw new RuntimeException("Cannot manually add " + resourceType + " child Resource under parent "
                    + parentResource + ", since " + resourceType
                    + " is a singleton type, and there is already a child Resource of that type. "
                    + "If the existing child Resource corresponds to a managed Resource which no longer exists, "
                    + "uninventory it and then try again.");
            }
        }
    }

}
