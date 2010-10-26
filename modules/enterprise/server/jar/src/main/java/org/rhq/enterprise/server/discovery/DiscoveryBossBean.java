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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.discovery.StaleTypeException;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.ProductVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.core.util.collection.ArrayUtils;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.agentclient.AgentClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.core.AgentManagerLocal;
import org.rhq.enterprise.server.resource.ProductVersionManagerLocal;
import org.rhq.enterprise.server.resource.ResourceAlreadyExistsException;
import org.rhq.enterprise.server.resource.ResourceAvailabilityManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;

/**
 * SLSB that provides the interface point to the discovery subsystem for the UI layer and the discovery server service.
 *
 * @author Ian Springer
 * @author Greg Hinkle
 */
@Stateless
public class DiscoveryBossBean implements DiscoveryBossLocal, DiscoveryBossRemote {
    private final Log log = LogFactory.getLog(DiscoveryBossBean.class.getName());

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AgentManagerLocal agentManager;
    @EJB
    private AuthorizationManagerLocal authorizationManager;
    @EJB
    private DiscoveryBossLocal discoveryBoss; // ourself for Tx purposes
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

    public ResourceSyncInfo mergeInventoryReport(InventoryReport report) throws InvalidInventoryReportException {
        validateInventoryReport(report);

        InventoryReportFilter filter = new DeletedResourceTypeFilter(subjectManager, resourceTypeManager);
        if (!filter.accept(report)) {
            throw new StaleTypeException("The report contains one or more resource types that have been marked for " +
                "deletion.");
        }

        Agent agent = report.getAgent();
        long start = System.currentTimeMillis();

        Agent knownAgent = agentManager.getAgentByName(agent.getName());
        if (knownAgent == null) {
            throw new InvalidInventoryReportException("Unknown Agent named [" + agent.getName()
                + "] sent an inventory report - that report will be ignored");
        }

        if (log.isDebugEnabled()) {
            log.debug("Received inventory report from RHQ Agent [" + knownAgent + "]. Number of added roots: "
                + report.getAddedRoots().size());
        }

        Set<Resource> roots = report.getAddedRoots();
        log.debug(report);

        for (Resource root : roots) {
            // Make sure all platform, server, and service types are valid. Also, make sure they're fetched - otherwise
            // we'll get persistence exceptions when we try to merge OR persist the platform.
            long rootStart = System.currentTimeMillis();
            if (!initResourceTypes(root)) {
                continue;
            }
            if ((root.getParentResource() != Resource.ROOT) && (root.getParentResource().getId() != Resource.ROOT_ID)) {
                // This is a new resource that has a parent that already exists.
                Resource parent = getExistingResource(root.getParentResource());
                assert parent != null;
                mergeResource(root, parent, knownAgent);
            } else {
                // This is a root resource.
                mergeResource(root, Resource.ROOT, knownAgent);
            }

            // Do NOT delete this flush+clear - it greatly improves performance.
            entityManager.flush();
            entityManager.clear();

            if (log.isDebugEnabled()) {
                log.debug("Root merged: resource/millis=" + root.getName() + '/'
                    + (System.currentTimeMillis() - rootStart));
            }
        }

        // Prepare the ResourceSyncInfo tree which contains all the info the PC needs to sync itself up with us.
        Resource platform = this.resourceManager.getPlatform(knownAgent);

        //the platform can be null in only one scenario.. a brand new agent has connected to the server
        //and that agent is currently trying to upgrade its resources. For that it asks us to send down
        //the current inventory on the server side. But at this point there isn't any since that very
        //agent just registered and is starting up for the very first time and therefore hasn't had
        //a chance yet to send us its full inventory report.
        ResourceSyncInfo syncInfo = platform != null ? this.entityManager
            .find(ResourceSyncInfo.class, platform.getId()) : null;

        if (log.isDebugEnabled()) {
            log.debug("Inventory merge completed in (" + (System.currentTimeMillis() - start) + ")ms");
        }

        return syncInfo;
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
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager,
            Resource.QUERY_FIND_QUEUED_PLATFORMS_BY_INVENTORY_STATUS, pc);

        queryCount.setParameter("inventoryStatuses", statuses);
        long count = (Long) queryCount.getSingleResult();

        query.setParameter("inventoryStatuses", statuses);
        List<Resource> results = query.getResultList();

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
        discoveryBoss.updateInventoryStatus(user, status, platforms, servers);

        // on status change request an agent sync on the affected resources.  The agent will sync status and determine
        // what other sync work needs to be performed. Synchronize the platforms, then the servers, omitting servers
        // under synced platforms since they will have been handled already.        
        for (Resource platform : platforms) {
            AgentClient agentClient = agentManager.getAgentClient(platform.getAgent());
            try {
                agentClient.getDiscoveryAgentService().synchronizeInventory(
                    entityManager.find(ResourceSyncInfo.class, platform.getId()));
            } catch (Exception e) {
                log.warn("Could not perform commit synchronization with agent for platform [" + platform.getName()
                    + "]", e);
            }
        }

        for (Resource server : servers) {
            // Only update servers if they haven't already been updated at the platform level
            if (!platforms.contains(server.getParentResource())) {
                AgentClient agentClient = agentManager.getAgentClient(server.getAgent());
                try {
                    agentClient.getDiscoveryAgentService().synchronizeInventory(
                        entityManager.find(ResourceSyncInfo.class, server.getId()));
                } catch (Exception e) {
                    log.warn("Could not perform commit synchronization with agent for server [" + server.getName()
                        + "]", e);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Inventory status set to [" + status + "] for [" + platforms.size() + "] platforms and ["
                + servers.size() + "] servers in [" + (System.currentTimeMillis() - start) + "]ms");
        }
    }

    /**
     * Updates statuses according to the inventory rules. This is used internally - never call this yourself without
     * knowing what you do. See {@link #updateInventoryStatus(Subject, List, List, InventoryStatus)} for the "public"
     * version.
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void updateInventoryStatus(Subject user, InventoryStatus status, List<Resource> platforms,
        List<Resource> servers) {
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

        try {
            ResourceType resourceType = this.resourceTypeManager.getResourceTypeById(subject, resourceTypeId);
            // the subsequent call to manuallyAddResource requires a detached ResourceType param so clear 
            entityManager.clear();
            MergeResourceResponse response = manuallyAddResource(subject, resourceType, parentResourceId,
                pluginConfiguration);
            result = this.resourceManager.getResourceById(subject, response.getResourceId());
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return result;
    }

    @NotNull
    public MergeResourceResponse manuallyAddResource(Subject user, ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration) throws InvalidPluginConfigurationClientException, PluginContainerException {
        if (!this.authorizationManager.hasResourcePermission(user, Permission.CREATE_CHILD_RESOURCES, parentResourceId)) {
            throw new PermissionException("You do not have permission on resource with id " + parentResourceId
                + " to manually add child resources.");
        }

        MergeResourceResponse mergeResourceResponse;
        try {
            Resource parentResource = this.resourceManager.getResourceById(user, parentResourceId);
            AgentClient agentClient = this.agentManager.getAgentClient(parentResource.getAgent());
            mergeResourceResponse = agentClient.getDiscoveryAgentService().manuallyAddResource(resourceType,
                parentResourceId, pluginConfiguration, user.getId());
        } catch (RuntimeException e) {
            throw new RuntimeException("Error adding " + resourceType.getName()
                + " resource to inventory as a child of the resource with id " + parentResourceId + " - cause: "
                + e.getLocalizedMessage(), e);
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

        Resource existingResource = getExistingResource(resource);
        if (existingResource != null) {
            mergeResourceResponse = new MergeResourceResponse(existingResource.getId(), true);
        } else {
            Subject creator = this.subjectManager.getSubjectById(creatorSubjectId);
            try {
                creator = this.subjectManager.loginUnauthenticated(creator.getName(), true);
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

    public Set<ResourceUpgradeResponse> upgradeResources(Set<ResourceUpgradeRequest> upgradeRequests) {
        Set<ResourceUpgradeResponse> result = new HashSet<ResourceUpgradeResponse>();

        boolean allowGenericPropertiesUpgrade = Boolean.parseBoolean(systemManager.getSystemConfiguration()
            .getProperty(RHQConstants.AllowResourceGenericPropertiesUpgrade, "false"));

        for (ResourceUpgradeRequest request : upgradeRequests) {
            Resource existingResource = this.entityManager.find(Resource.class, request.getResourceId());
            if (existingResource != null) {
                ResourceUpgradeResponse upgradedData = upgradeResource(existingResource, request,
                    allowGenericPropertiesUpgrade);
                if (upgradedData != null) {
                    result.add(upgradedData);
                }
            }
        }
        return result;
    }

    /**
     * Convienence method that looks at <code>resource</code> and if its version is not
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
     *         iff the resource's version was actually changed.
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
                log.info("Resource [" + resource + "] changed its version from [" + oldVersion + "] to [" + newVersion
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
     * @param existingResource
     * @param upgradeRequest
     * @param allowGenericPropertiesUpgrade name and description are only upgraded if this is true
     * @return response to the upgrade request detailing what has been accepted on the server side
     */
    private ResourceUpgradeResponse upgradeResource(@NotNull Resource resource, ResourceUpgradeRequest upgradeRequest,
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

        String resourceKey = upgradeRequest.getNewResourceKey();
        String name = upgradeRequest.getNewName();
        String description = upgradeRequest.getNewDescription();

        if (resourceKey != null || name != null || description != null) {
            StringBuilder logMessage = new StringBuilder("Resource [").append(resource.toString()).append(
                "] upgraded its ");

            if (needsUpgrade(resource.getResourceKey(), resourceKey)) {
                resource.setResourceKey(resourceKey);
                logMessage.append("resourceKey, ");
            }
            ret.setUpgradedResourceKey(resource.getResourceKey());

            if (allowGenericPropertiesUpgrade && needsUpgrade(resource.getName(), name)) {
                resource.setName(name);
                logMessage.append("name, ");
            }
            ret.setUpgradedResourceName(resource.getName());

            if (allowGenericPropertiesUpgrade && needsUpgrade(resource.getDescription(), description)) {
                resource.setDescription(description);
                logMessage.append("description, ");
            }
            ret.setUpgradedResourceDescription(resource.getDescription());

            //finally let's remove the potential previous upgrade error. we've now successfully
            //upgraded the resource.
            List<ResourceError> upgradeErrors = resourceManager.findResourceErrors(subjectManager.getOverlord(),
                resource.getId(), ResourceErrorType.UPGRADE);
            for (ResourceError error : upgradeErrors) {
                entityManager.remove(error);
            }

            logMessage.replace(logMessage.length() - 1, logMessage.length(), "to become [").append(resource.toString())
                .append("]");

            log.info(logMessage.toString());
        }
        return ret;
    }

    private void validateInventoryReport(InventoryReport report) throws InvalidInventoryReportException {
        for (Resource root : report.getAddedRoots()) {
            validateResource(root);
        }
    }

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

    /**
     * Merges the specified resource into inventory. If the resource already exists in inventory, it is updated; if it
     * does not already exist in inventory, it is added and its parent is set to the specified, already inventoried,
     * parent resource.
     *
     * @param  resource       the resource to be merged
     * @param  parentResource the inventoried resource that should be the parent of the resource to be merged
     * @param  agent          the agent that should be set on the resource being merged
     *
     * @throws InvalidInventoryReportException if a critical field in the resource is missing or invalid
     */
    private void mergeResource(@NotNull Resource resource, @Nullable Resource parentResource, @NotNull Agent agent)
        throws InvalidInventoryReportException {
        long start = System.currentTimeMillis();

        log.debug("Merging [" + resource + "]...");
        Resource existingResource = getExistingResource(resource);

        if (existingResource != null) {
            updatePreviouslyInventoriedResource(resource, existingResource, parentResource);
        } else {
            presetAgent(resource, agent);
            addResourceToInventory(resource, parentResource);
        }

        if (log.isDebugEnabled()) {
            log.debug("Resource merged: resource/millis=" + resource.getName() + '/'
                + (System.currentTimeMillis() - start));
        }
        return;
    }

    private void presetAgent(Resource resource, Agent agent) {
        resource.setAgent(agent);
        for (Resource child : resource.getChildResources()) {
            presetAgent(child, agent);
        }
    }

    /**
     * Given a resource, will attempt to find it in the server's inventory (that is, finds it in the database). If the
     * given resource's ID does not exist in the database, it will be looked up by its resource key. If the resource
     * cannot be found either via ID or resource key, the given resource's ID will be reset to 0 and null will be
     * returned.
     *
     * @param  resource the resource to find in the server's inventory (the database)
     *
     * @return the existing resource found in the database that matches that of the given resource
     */
    private Resource getExistingResource(Resource resource) {
        Resource existingResource = null;

        log.debug("getExistingResource processing for [" + resource + "]");

        String idLogMsg = "id=" + resource.getId();

        if (resource.getId() != 0) {
            log.debug(idLogMsg + ": Agent claims resource is already in inventory.");

            /* agent says this resource is already in inventory.
             *
             * note: we intentionally do not use ResourceManager.getResourceById() here, because if it were to throw a
             *  ResourceNotFoundException, it would cause a tx rollback (ips, 05/09/07).
             */
            existingResource = entityManager.find(Resource.class, resource.getId());
            if (existingResource == null) {
                // agent lied - agent's copy of JON server inventory must be stale.
                log.debug(idLogMsg + ": However, no resource exists with the specified id.");
            } else {
                log.debug(idLogMsg + ": Found resource already in inventory with specified id");
            }
        } else {
            log.debug(idLogMsg + ": Agent reported resource with id of 0.");
        }

        if (existingResource == null) {
            log.debug(idLogMsg + ": Checking if a resource exists with the specified business key.");

            /*
             * double-check for an existing resource using the business key.
             *
             * this will happen if the agent found the resource (non-zero id) but the entityManager didn't know about it,
             * or if the agent didn't know about it to begin with (id was 0).
             */
            ResourceType resourceType = resource.getResourceType();
            Resource parent = resource;
            Subject overlord = subjectManager.getOverlord();
            while (parent != null && existingResource == null) {
                parent = parent.getParentResource();
                //check if the parent itself is inventoried. This might not be the case
                //during initial sync-up for resource upgrade.
                Resource existingParent = null;
                if (parent != null) {
                    existingParent = entityManager.find(Resource.class, parent.getId());
                    if (existingParent == null) {
                        //well, this parent is not known to the server, so there's no
                        //point in trying to find a child of it...
                        continue;
                    }
                }
                existingResource = resourceManager.getResourceByParentAndKey(overlord, existingParent,
                    resource.getResourceKey(), resourceType.getPlugin(), resourceType.getName());
            }

            if (existingResource != null) {
                // We found it - reset the id to what it should be.
                resource.setId(existingResource.getId());
                log.debug(idLogMsg + ": Found resource already in inventory with specified business key");
            } else {
                log.debug(idLogMsg + ": Unable to find the agent-reported resource by id or business key.");

                if (resource.getId() != 0) {
                    // existingResource is still null at this point, the resource does not exist in inventory.
                    log.error(idLogMsg + ": Resetting the resource's id to zero.");
                    resource.setId(0);
                    // TODO: Is there anything else we should do here to inform the agent it has an out-of-sync resource?
                } else {
                    log.debug(idLogMsg + ": Resource's id was already zero, nothing to do for the merge.");
                }
            }
        }

        if (existingResource != null) {
            existingResource.getChildResources().size(); // eager load child resources to avoid later failures in adding children
        }

        return existingResource;
    }

    private void updatePreviouslyInventoriedResource(Resource resource, Resource existingResource,
        Resource parentResource) throws InvalidInventoryReportException {
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

        assert (parentResource == null) || (parentResource.getId() != 0);

        ResourceType existingResourceParentType = (existingResource.getParentResource() != null) ? existingResource
            .getParentResource().getResourceType() : null;
        ResourceType resourceParentType = (resource.getParentResource() != null) ? resource.getParentResource()
            .getResourceType() : null;
        Set<ResourceType> validParentTypes = existingResource.getResourceType().getParentResourceTypes();
        if (validParentTypes != null && !validParentTypes.isEmpty()
            && !validParentTypes.contains(existingResourceParentType)) {
            // The existing Resource has an invalid parent ResourceType. This may be because its ResourceType was moved
            // to a new parent ResourceType, but its new parent was not yet discovered at the time of the type move. See
            // if the Resource reported by the Agent has a valid parent type, and, if so, update the existing Resource's
            // parent to that type.

            if (validParentTypes.contains(resourceParentType)) {
                if (existingResource.getParentResource() != null) {
                    existingResource.getParentResource().removeChildResource(existingResource);
                }
                if (resource.getParentResource() != null) {
                    parentResource = getExistingResource(resource.getParentResource());
                    parentResource.addChildResource(existingResource);
                }
                existingResource.setParentResource(resource.getParentResource());
            } else {
                log.debug("Existing Resource " + existingResource + " has invalid parent type ("
                    + existingResourceParentType + ") and so does plugin-reported Resource " + resource + " ("
                    + resourceParentType + ") - valid parent types are [" + validParentTypes + "].");
            }
        }

        // The below block is for Resources that were created via the RHQ GUI, whose descriptions will be null.
        if (existingResource.getDescription() == null && resource.getDescription() != null) {
            log.debug("Setting description of existing resource with id " + existingResource.getId() + " to '"
                + resource.getDescription() + "' (as reported by agent)...");
            existingResource.setDescription(resource.getDescription());
        }

        // Log a warning if the agent says the Resource key has changed (should rarely happen).
        if ((existingResource.getResourceKey() != null)
            && !existingResource.getResourceKey().equals(resource.getResourceKey())) {
            log.warn("Agent reported that key for " + existingResource + " has changed from '"
                + existingResource.getResourceKey() + "' to '" + resource.getResourceKey() + "'.");
        }

        updateResourceVersion(existingResource, resource.getVersion());

        // If the resource was marked as deleted, reactivate it again.
        if (existingResource.getInventoryStatus() == InventoryStatus.DELETED) {
            existingResource.setInventoryStatus(InventoryStatus.COMMITTED);
            existingResource.setPluginConfiguration(resource.getPluginConfiguration());
        }

        for (Resource childResource : resource.getChildResources()) {
            // It's important to specify the existing Resource, which is an attached entity bean, as the parent.
            mergeResource(childResource, existingResource, existingResource.getAgent());
        }
        return;
    }

    private boolean initResourceTypes(Resource resource) {
        ResourceType resourceType;
        try {
            resourceType = this.resourceTypeManager.getResourceTypeByNameAndPlugin(subjectManager.getOverlord(),
                resource.getResourceType().getName(), resource.getResourceType().getPlugin());
        } catch (RuntimeException e) {
            log.error("Failed to lookup Resource type [" + resource.getResourceType() + "] for reported Resource ["
                + resource + "] - this should not have happened.");
            return false;
        }
        if (resourceType == null) {
            log.error("Reported resource [" + resource + "] has an unknown type [" + resource.getResourceType()
                + "]. The Agent most likely has a plugin named '" + resource.getResourceType().getPlugin()
                + "' installed that is not installed on the Server. Resource will be ignored...");
            return false;
        }

        resource.setResourceType(resourceType);
        for (Iterator<Resource> childIterator = resource.getChildResources().iterator(); childIterator.hasNext();) {
            Resource child = childIterator.next();
            if (!initResourceTypes(child)) {
                childIterator.remove();
            }
        }
        return true;
    }

    private void addResourceToInventory(Resource resource, Resource parentResource) {
        log.debug("New resource [" + resource + "] reported - adding to inventory with status 'NEW'...");
        initAutoDiscoveredResource(resource, parentResource);
        entityManager.persist(resource);

        if (parentResource != null) {
            parentResource.addChildResource(resource);
        }

        // Add a product version entry for the new resource.
        addProductVersionsRecursively(resource);

        if (parentResource != null) {
            groupManager.updateImplicitGroupMembership(subjectManager.getOverlord(), resource);
        }

        // do NOT delete this flush/clear - it greatly improves performance
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Ensures the resource has the proper relationship to its product version. This method will recursively dig
     * into child resources, updating their versions as well.
     *
     * @param resource resource (along with its children) to which to add product version references
     */
    private void addProductVersionsRecursively(Resource resource) {
        if ((resource.getVersion() != null) && (resource.getVersion().length() > 0)) {
            ResourceType type = resource.getResourceType();
            ProductVersion productVersion = productVersionManager.addProductVersion(type, resource.getVersion());
            resource.setProductVersion(productVersion);
        }

        for (Resource child : resource.getChildResources()) {
            addProductVersionsRecursively(child);
        }
    }

    private void initAutoDiscoveredResource(Resource resource, Resource parent) {
        // Before adding a new auto-discovered resource to inventory, ensure that it, and all its descendants, has
        // the proper inventory status and an owner and modifier of superUser.
        if ((resource.getParentResource() != null)
            && (resource.getParentResource().getInventoryStatus() == InventoryStatus.COMMITTED)
            && ((resource.getResourceType().getCategory() == ResourceCategory.SERVICE) || (resource.getParentResource()
                .getResourceType().getCategory() == ResourceCategory.SERVER))) {
            // Auto-commit services whose parent resources have already been imported by the user
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
        } else {
            resource.setInventoryStatus(InventoryStatus.NEW);
        }

        resource.setItime(System.currentTimeMillis());
        resource.setModifiedBy(subjectManager.getOverlord().getName());
        for (Resource childResource : resource.getChildResources()) {
            initAutoDiscoveredResource(childResource, resource);
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
        checkStatus(subject, resourceIds, InventoryStatus.IGNORED, EnumSet.of(InventoryStatus.NEW));
    }

    public void unignoreResources(Subject subject, int[] resourceIds) {
        if (resourceIds == null || resourceIds.length == 0) {
            return;
        }
        checkStatus(subject, resourceIds, InventoryStatus.NEW, EnumSet.of(InventoryStatus.IGNORED));
    }

    @SuppressWarnings("unchecked")
    private void checkStatus(Subject subject, int[] resourceIds, InventoryStatus target,
        EnumSet<InventoryStatus> validStatuses) {
        Query query = entityManager.createQuery("" //
            + "  SELECT res.inventoryStatus " //
            + "    FROM Resource res " //
            + "   WHERE res.id IN ( :resourceIds ) " //
            + "GROUP BY res.inventoryStatus ");
        query.setParameter("resourceIds", ArrayUtils.wrapInList(resourceIds));
        List<InventoryStatus> results = query.getResultList();

        for (InventoryStatus expected : validStatuses) {
            results.remove(expected);
        }
        if (results.size() > 0) {
            throw new IllegalArgumentException("Can only commit resources with status: " + results);
        }

        PageList<Resource> resources = resourceManager.findResourceByIds(subject, resourceIds, false,
            PageControl.getUnlimitedInstance());
        List<Resource> platforms = new ArrayList<Resource>();
        List<Resource> servers = new ArrayList<Resource>();
        for (Resource res : resources) {
            ResourceCategory category = res.getResourceType().getCategory();
            if (category == ResourceCategory.PLATFORM) {
                platforms.add(res);
            } else if (category == ResourceCategory.SERVER) {
                servers.add(res);
            } else {
                throw new IllegalArgumentException("Can not directly change the inventory status of a service");
            }
        }

        updateInventoryStatus(subject, platforms, servers, target);
    }

    private static <T> boolean needsUpgrade(T oldValue, T newValue) {
        return newValue != null && (oldValue == null || !newValue.equals(oldValue));
    }
}
