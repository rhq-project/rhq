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
package org.rhq.core.pc.inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.clientapi.agent.discovery.DiscoveryAgentService;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.discovery.InventoryReportResponse;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.agent.AgentRegistrar;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.operation.OperationServicesAdapter;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.operation.OperationServices;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Manages the process of both auto-detection of servers and runtime detection of services across all plugins. Manages
 * their scheduling and result sending as well as the general inventory model.
 *
 * <p>This is an agent service; its interface is made remotely accessible if this is deployed within the agent.</p>
 *
 * @author Greg Hinkle
 * @author Ian Springer ({@link DiscoveryAgentService#manuallyAddResource})
 */
public class InventoryManager extends AgentService implements ContainerService, DiscoveryAgentService {
    private static final String INVENTORY_THREAD_POOL_NAME = "InventoryManager.discovery";
    private static final String AVAIL_THREAD_POOL_NAME = "InventoryManager.availability";
    private static final int AVAIL_THREAD_POOL_CORE_POOL_SIZE = 1;

    private static final int COMPONENT_STOP_TIMEOUT = 5 * 1000; // 5 seconds

    private final Log log = LogFactory.getLog(InventoryManager.class);

    private PluginContainerConfiguration configuration;

    private ScheduledThreadPoolExecutor inventoryThreadPoolExecutor;
    private ScheduledThreadPoolExecutor availabilityThreadPoolExecutor;

    // The executors are Callable
    private AutoDiscoveryExecutor serverScanExecutor;
    private RuntimeDiscoveryExecutor serviceScanExecutor;
    private AvailabilityExecutor availabilityExecutor;

    private Agent agent;

    /**
     * Root platform resource, required to be root of entire inventory tree in this agent
     */
    private Resource platform;

    /**
     * if the {@link #getPlatform() platform} has inventory status of NEW, this indicates it was committed before but
     * was deleted recently
     */
    private boolean newPlatformWasDeletedRecently = false; // value only is valid/relevant if platform.getInventoryStatus == NEW

    private ReentrantReadWriteLock inventoryLock = new ReentrantReadWriteLock(true);

    /**
     * Used only for the outside the agent model to # resources
     */
    private AtomicInteger temporaryKeyIndex = new AtomicInteger(-1);

    /**
     * UUID to ResourceContainer map
     */
    private Map<String, ResourceContainer> resourceContainers = Collections
        .synchronizedMap(new HashMap<String, ResourceContainer>(1000));

    /**
     * Collection of event listeners to inform of changes to the inventory.
     */
    private Set<InventoryEventListener> inventoryEventListeners = new HashSet<InventoryEventListener>();

    public InventoryManager() {
        super(DiscoveryAgentService.class);
    }

    /**
     * @see ContainerService#initialize()
     */
    public void initialize() {
        inventoryLock.writeLock().lock();

        try {
            log.info("Initializing Inventory Manager...");

            this.agent = new Agent(this.configuration.getContainerName(), null, 0, null, null);

            if (configuration.isInsideAgent()) {
                loadFromDisk();
            }

            executePlatformScan();

            // Never run more than one discovery at a time?
            inventoryThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new LoggingThreadFactory(
                INVENTORY_THREAD_POOL_NAME, true));

            serverScanExecutor = new AutoDiscoveryExecutor(null, this, configuration);
            // After ten seconds, periodically run the autodiscovery scan

            if (configuration.isInsideAgent()) {
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serverScanExecutor, configuration
                    .getServerDiscoveryInitialDelay(), configuration.getServerDiscoveryPeriod(), TimeUnit.SECONDS);
            }

            serviceScanExecutor = new RuntimeDiscoveryExecutor(this, configuration);
            if (configuration.isInsideAgent()) {
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serviceScanExecutor, configuration
                    .getServiceDiscoveryInitialDelay(), configuration.getServiceDiscoveryPeriod(), TimeUnit.SECONDS);
            }

            // do not run more than one availability check at a time
            availabilityThreadPoolExecutor = new ScheduledThreadPoolExecutor(AVAIL_THREAD_POOL_CORE_POOL_SIZE,
                new LoggingThreadFactory(AVAIL_THREAD_POOL_NAME, true));
            availabilityExecutor = new AvailabilityExecutor(this);
            availabilityThreadPoolExecutor.scheduleWithFixedDelay(availabilityExecutor, configuration
                .getAvailabilityScanInitialDelay(), configuration.getAvailabilityScanPeriod(), TimeUnit.SECONDS);
        } finally {
            inventoryLock.writeLock().unlock();
        }

        log.info("InventoryManager initialized");
    }

    /**
     * @see ContainerService#shutdown()
     */
    public void shutdown() {
        inventoryThreadPoolExecutor.shutdownNow();
        availabilityThreadPoolExecutor.shutdownNow();
        if (configuration.isInsideAgent()) {
            this.persistToDisk();
        }
    }

    @Nullable
    public ResourceContainer getResourceContainer(Resource resource) {
        return this.resourceContainers.get(resource.getUuid());
    }

    public ResourceContainer getResourceContainer(Integer resourceId) {
        if ((resourceId == null) || (resourceId.intValue() == 0)) {
            // i've already found one place where passing in 0 was very bad - I want to be very noisy in the log
            // when this happens but not throw an exception, for fear I might break something.
            // I'll just return null instead; hopefully, callers are checking for null appropriately
            log.warn("Cannot get a resource container for an invalid resource ID=" + resourceId);
            if (log.isDebugEnabled()) {
                log.debug("Stack trace follows:", new Throwable("This is where resource ID=[" + resourceId
                    + "] was passed in"));
            }

            return null;
        }

        List<ResourceContainer> containers = new ArrayList<ResourceContainer>(this.resourceContainers.values()); // avoids concurrent mod exception
        for (ResourceContainer container : containers) {
            if (resourceId.equals(container.getResource().getId())) {
                return container;
            }
        }

        return null;
    }

    void executePlatformScan() {
        log.debug("Executing platform scan...");
        Resource discoveredPlatform = discoverPlatform();
        if (this.platform == null) {
            this.platform = discoveredPlatform;
            log.info("Detected new platform " + this.platform);
            initializePlatformComponent();
        } else {
            // If the platform's already in inventory, make sure its version is up-to-date.
            updateResourceVersion(this.platform, discoveredPlatform.getVersion());
        }
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException {
        ResourceContainer container = getResourceContainer(resourceId);
        if (container == null) {
            throw new PluginContainerException("Cannot update plugin configuration for unknown Resource with id ["
                + resourceId + "]");
        }

        Resource resource = container.getResource();
        // First stop the resource component.
        deactivateResource(resource);
        // Then update the resource's plugin config.
        resource.setPluginConfiguration(newPluginConfiguration);
        // And finally restart the resource component.
        try {
            activateResource(resource, container, true);
        } catch (InvalidPluginConfigurationException e) {
            String errorMessage = "Unable to connect to managed resource of type '"
                + resource.getResourceType().getName() + "' using the specified connection properties.";
            log.info(errorMessage, e);
            errorMessage += ((e.getLocalizedMessage() != null) ? (" " + e.getLocalizedMessage()) : "");

            // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
            // stack trace, but append the message from that exception to the message of the exception we throw. This
            // will make for a nicer error message for the server to display in the UI.
            throw new InvalidPluginConfigurationClientException(errorMessage,
                (e.getCause() != null) ? new WrappedRemotingException(e.getCause()) : null);
        }
    }

    public InventoryReport executeServerScanImmediately() {
        try {
            return inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Server scan execution was interrupted");
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public InventoryReport executeServiceScanImmediately() {
        try {
            return inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serviceScanExecutor).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Service scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
        try {
            AvailabilityExecutor availExec = new AvailabilityExecutor(this);

            if (changedOnlyReport) {
                availExec.sendChangedOnlyReportNextTime();
            } else {
                availExec.sendFullReportNextTime();
            }

            return availabilityThreadPoolExecutor.submit((Callable<AvailabilityReport>) availExec).get();
        } catch (InterruptedException e) {
            throw new RuntimeException("Availability scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    public Availability getAvailability(Resource resource) {
        Availability avail = getAvailabilityIfKnown(resource);
        if (avail == null) {
            avail = new Availability(resource, new Date(), null);
        }

        return avail;
    }

    @SuppressWarnings("unchecked")
    public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int ownerSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException {
        // TODO: This is hugely flawed. It assumes discovery components will only return the manually discovered resource, but
        // never says this is required. It then proceeds to auto-import the first resource returned. For discoveries that
        // are process based it works because this passes in a null process scan... also a bad idea.

        // Lookup the full, local resource type (the provided one is just the keys)
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        resourceType = pluginManager.getMetadataManager().getType(resourceType.getName(), resourceType.getPlugin());

        MergeResourceResponse mergeResourceResponse = null;
        Resource resource = null;
        boolean resourceAlreadyExisted = false;
        try {
            // Get the discovery component responsible for discovering resources of the specified resource type.
            PluginComponentFactory pluginComponentFactory = PluginContainer.getInstance().getPluginComponentFactory();
            ResourceDiscoveryComponent discoveryComponent = pluginComponentFactory.getDiscoveryComponent(resourceType);

            List<Configuration> pluginConfigurations = new ArrayList<Configuration>(1);
            pluginConfigurations.add(pluginConfiguration);
            ResourceComponent parentResourceComponent = getResourceContainer(parentResourceId).getResourceComponent();
            ResourceDiscoveryContext<ResourceComponent> resourceDiscoveryContext = new ResourceDiscoveryContext<ResourceComponent>(
                resourceType, parentResourceComponent, SystemInfoFactory.createSystemInfo(),
                new ArrayList<ProcessScanResult>(0), pluginConfigurations, configuration.getContainerName());

            // Ask the plugin's discovery component to find the new resource, throwing exceptions if it cannot be found at all.
            Set<DiscoveredResourceDetails> discoveredResources = discoveryComponent
                .discoverResources(resourceDiscoveryContext);
            if ((discoveredResources == null) || discoveredResources.isEmpty()) {
                log
                    .info("Plugin Warning: discovery component "
                        + discoveryComponent.getClass().getName()
                        + " returned no resources when passed a single plugin configuration (the plugin developer probably did not implement support for manually discovered resources).");
                throw new PluginContainerException("The " + resourceType.getPlugin()
                    + " plugin does not support manual addition of '" + resourceType.getName() + "' resources.");
            }

            // Create the new resource and add it to inventory if it isn't already there.
            DiscoveredResourceDetails discoveredResourceDetails = discoveredResources.iterator().next();
            resource = createNewResource(discoveredResourceDetails);
            Resource parentResource = getResourceContainer(parentResourceId).getResource();
            Resource existingResource = findMatchingChildResource(resource, parentResource);
            if (existingResource != null) {
                log.debug("Manual add for resource type '" + resourceType.getName() + "' and parent resource id "
                    + parentResourceId
                    + " found a resource that already exists in inventory - updating existing resource "
                    + existingResource + "...");
                resourceAlreadyExisted = true;
                resource = existingResource;
                if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
                    resource.setPluginConfiguration(pluginConfiguration);
                }
            } else {
                log.debug("Adding manually discovered resource " + resource + " to inventory...");
                initResourceContainer(resource);
                parentResource.addChildResource(resource);
            }

            // Make sure the resource's component is activated (i.e. started).
            boolean newPluginConfig = true;
            ResourceContainer resourceContainer = getResourceContainer(resource);
            log.debug("Activating resource " + resource + "...");
            activateResource(resource, resourceContainer, newPluginConfig);

            // NOTE: We don't mess with inventory status - that's the server's responsibility.

            // Tell the server to merge the resource into its inventory.
            DiscoveryServerService discoveryServerService = this.configuration.getServerServices()
                .getDiscoveryServerService();
            mergeResourceResponse = discoveryServerService.addResource(resource, ownerSubjectId);

            // Sync our local resource up with the one now in server inventory.
            resource.setId(mergeResourceResponse.getResourceId());
            synchronizeInventory(resource.getId(), EnumSet.allOf(SynchronizationType.class));
        }

        // Catch any other RuntimeExceptions or Errors, so the server doesn't have to worry about deserializing or
        // catching them. Before rethrowing, wrap them in a WrappedRemotingException and then wrap that in either an
        // InvalidPluginConfigurationException or a PluginContainerException.
        catch (Throwable t) {
            if ((resource != null) && !resourceAlreadyExisted && (getResourceContainer(resource) != null)) {
                // If the resource got added to inventory, roll it back (i.e. deactivate it, then remove it from inventory).
                log.debug("Rolling back manual add of resource of type " + resourceType.getName()
                    + "' - removing resource with id " + resource.getId() + " from inventory...");
                deactivateResource(resource);
                removeResource(resource.getId());
            }

            if (t instanceof InvalidPluginConfigurationException) {
                String errorMessage = "Unable to connect to managed resource of type '" + resourceType.getName()
                    + "' using the specified connection properties - resource will not be added to inventory.";
                log.info(errorMessage, t);

                // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
                // stack trace, but append the message from that exception to the message of the exception we throw. This
                // will make for a nicer error message for the server to display in the UI.
                errorMessage += ((t.getLocalizedMessage() != null) ? (" " + t.getLocalizedMessage()) : "");
                throw new InvalidPluginConfigurationClientException(errorMessage,
                    (t.getCause() != null) ? new WrappedRemotingException(t.getCause()) : null);
            } else {
                log.error("Manual add failed for resource of type '" + resourceType.getName()
                    + "' and parent resource id [" + parentResourceId + "].", t);
                throw new PluginContainerException("Failed to add resource with type [" + resourceType.getName()
                    + "] and parent resource id [" + parentResourceId + "].", new WrappedRemotingException(t));
            }
        }

        return mergeResourceResponse;
    }

    static Resource createNewResource(DiscoveredResourceDetails details) {
        Resource resource = new Resource();
        resource.setResourceKey(details.getResourceKey());
        resource.setName(details.getResourceName());
        resource.setVersion(details.getResourceVersion());
        resource.setDescription(details.getResourceDescription());
        resource.setResourceType(details.getResourceType());

        Configuration pluginConfiguration = details.getPluginConfiguration();
        ConfigurationUtility.normalizeConfiguration(details.getPluginConfiguration(), details.getResourceType()
            .getPluginConfigurationDefinition());

        resource.setPluginConfiguration(pluginConfiguration);
        return resource;
    }

    /**
     * Returns the known availability for the resource. If the availability is not known, <code>null</code> is returned.
     *
     * @param  resource the resource whose availability should be returned
     *
     * @return resource availability or <code>null</code> if not known
     */
    @Nullable
    public Availability getAvailabilityIfKnown(Resource resource) {
        ResourceContainer resourceContainer = getResourceContainer(resource);

        if (resourceContainer != null) {
            if (ResourceComponentState.STARTED == resourceContainer.getResourceComponentState()) {
                Availability availability = resourceContainer.getAvailability();
                return availability;
            }
        }

        return null;
    }

    public void handleReport(AvailabilityReport report) {
        // a null report means a non-committed inventory - we are either brand new or our platform was deleted recently
        if (report == null) {
            if ((this.platform != null) && (this.platform.getInventoryStatus() == InventoryStatus.NEW)
                && newPlatformWasDeletedRecently) {
                // let's make sure we are registered; its probable that our platform was deleted and we need to re-register
                log
                    .info("No committed resources to send in our availability report - the platform/agent was deleted, let's re-register again");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }

            return;
        }

        List<Availability> reportAvails = report.getResourceAvailability();

        if (configuration.isInsideAgent() && (reportAvails != null)) {
            // Due to the asynchronous nature of the availability collection,
            // it is possible we may have collected availability of a resource that has just recently been deleted;
            // therefore, as a secondary check, let's remove any availabilities for resources that no longer exist.
            // I suppose after we do this check and before we send the report to the server that a resource could
            // then be deleted, but that time period where that could happen is now very small and thus this will
            // be a rare event.  And even if that does happen, nothing catastrophic would happen on the server,
            // the report would fail, an error would be logged on the server, and the exception thrown would
            // cause us to send a full report next time.
            this.inventoryLock.readLock().lock();
            try {
                Availability[] avails = reportAvails.toArray(new Availability[0]);
                for (Availability avail : avails) {
                    ResourceContainer container = getResourceContainer(avail.getResource());
                    if ((container == null)
                        || (container.getResource().getInventoryStatus() == InventoryStatus.DELETED)) {
                        reportAvails.remove(avail);
                    }
                }
            } finally {
                this.inventoryLock.readLock().unlock();
            }

            if (reportAvails.size() > 0) {
                try {
                    log.info("Sending availability report to server");
                    log.debug("Availability report content: " + report.toString(true));

                    boolean ok = configuration.getServerServices().getDiscoveryServerService().mergeAvailabilityReport(
                        report);
                    if (!ok) {
                        // I guess I could immediately call executeAvailabilityScanImmediately and pass its results to
                        // mergeAvailabilityReport again right now, but what happens if we've queued up a bunch of
                        // changed-only reports and the server is out of sync - each time the server processes those
                        // reports, we'd do an extra round trip with a full report (which will get very expensive).
                        // Let's just flag our executor for the next time it runs to send a full report; this way
                        // if we've got 100 queued changed-only reports, let the server fully process them and only
                        // at the next time we run the avail scan will we send it a full report.  It might make the
                        // server sync up alittle slower than we'd like, but it avoids a potential hammering of the
                        // server with tons of full reports when that would be unnecessary.
                        availabilityExecutor.sendFullReportNextTime();
                    }
                } catch (Exception e) {
                    log.warn("Could not transmit availability report to server", e);
                    availabilityExecutor.sendFullReportNextTime(); // just in case the agent and server are out of sync
                }
            }
        }
    }

    /**
     * Send an inventory report to the server.
     *
     * @param  report
     *
     * @return true if sending the report to the server succeeded, or false otherwise
     */
    public boolean handleReport(InventoryReport report) {
        try {
            if (configuration.isInsideAgent() && (report.getAddedRoots().size() > 0)) {
                log.info("Sending inventory report to server");
                InventoryReportResponse response = configuration.getServerServices().getDiscoveryServerService()
                    .mergeInventoryReport(report);
                syncIds(report, response);
            }

            return true;
        } catch (InvalidInventoryReportException e) {
            log.error("Sending inventory report to server failure - was this agent's platform deleted?", e);

            if ((this.platform != null) && (this.platform.getInventoryStatus() == InventoryStatus.NEW)
                && newPlatformWasDeletedRecently) {
                // let's make sure we are registered; its probable that our platform was deleted and we need to re-register
                log
                    .info("The inventory report was invalid probably because the platform/agent was deleted, let's re-register again");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }

            return false;
        }
    }

    /**
     * Registers the plugin container with a remote server, if there is one. A no-op if we are not talking to a remote
     * server in which we need to be registered.
     */
    private void registerWithServer() {
        AgentRegistrar registrar = PluginContainer.getInstance().getAgentRegistrar();
        if (registrar != null) {
            try {
                registrar.register(10000L);

                // now that we are registered, let's kick off an inventory report
                // just to make sure the server has our initial inventory
                inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor);
            } catch (Exception e) {
                log.error("Cannot re-register with the agent, something bad is happening", e);
            }
        }

        return;
    }

    /**
     * Performs a service scan on the specified resource.
     *
     * @param resourceId resource on which to discover services
     */
    public void performServiceScan(int resourceId) {
        ResourceContainer resourceContainer = getResourceContainer(resourceId);
        Resource resource = resourceContainer.getResource();
        RuntimeDiscoveryExecutor oneTimeExecutor = new RuntimeDiscoveryExecutor(this, configuration, resource);

        try {
            inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) oneTimeExecutor).get();
        } catch (Exception e) {
            throw new RuntimeException("Error submitting service scan", e);
        }
    }

    private void syncIds(InventoryReport report, InventoryReportResponse response) {
        Map<String, Integer> idMap = response.getUuidToIntegerMapping();
        for (String uuid : idMap.keySet()) {
            ResourceContainer container = this.resourceContainers.get(uuid);
            if (container != null) {
                container.getResource().setId(idMap.get(uuid));
                fireResourceActivated(container.getResource());
            }
        }

        // TODO GH: Make the report also tell us what is "committed" so we know to only request synchronization for those roots
        for (Resource root : report.getAddedRoots()) {
            if (response.getUuidToIntegerMapping().size() > 0) {
                synchronizeInventory(root.getId(), EnumSet.allOf(SynchronizationType.class));
            }
        }
    }

    public ResourceComponent getResourceComponent(Resource resource) {
        ResourceContainer resourceContainer = this.resourceContainers.get(resource.getUuid());

        if (resourceContainer == null) {
            return null;
        }

        return resourceContainer.getResourceComponent();
    }

    public void removeResource(int resourceId) {
        boolean scan = removeResourceAndIndicateIfScanIsNeeded(resourceId);

        if (scan) {
            log.info("Deleted resource #[" + resourceId + "] - this will trigger a server scan now");
            inventoryThreadPoolExecutor.submit((Callable<InventoryReport>) this.serverScanExecutor);
        }
    }

    /**
     * Removes the resource and its children and returns true if a scan is needed.
     *
     * @param  resourceId the resource to remove
     *
     * @return true if this method deleted things that requires a scan.
     */
    private boolean removeResourceAndIndicateIfScanIsNeeded(int resourceId) {
        boolean scanIsNeeded = false;

        this.inventoryLock.writeLock().lock();
        try {
            ResourceContainer resourceContainer = getResourceContainer(resourceId);

            if (resourceContainer != null) {
                Resource resource = resourceContainer.getResource();
                boolean isTopLevelServer = (this.platform != null)
                    && (this.platform.equals(resource.getParentResource()))
                    && (resource.getResourceType().getCategory() != ResourceCategory.SERVICE);

                // this will deactivate the resource starting bottom-up - so this ends up as a no-op if we are being called recursively
                // but we need to do this now to ensure everything is stopped prior to removing them from inventory
                deactivateResource(resource);

                Set<Resource> children = new HashSet<Resource>(resource.getChildResources()); // put in new set to avoid concurrent mod exceptions
                for (Resource child : children) {
                    scanIsNeeded |= removeResourceAndIndicateIfScanIsNeeded(child.getId());
                }

                Resource parent = resource.getParentResource();
                if (parent != null) {
                    parent.removeChildResource(resource);
                }

                PluginContainer.getInstance().getMeasurementManager().unscheduleCollection(
                    Collections.singleton(resourceId));

                if (this.resourceContainers.remove(resource.getUuid()) == null) {
                    log.debug("Asked to remove an unknown resource [" + resource + "] with UUID [" + resource.getUuid()
                        + "]");
                }

                // if we just so happened to have removed our top level platform, we need to re-discover it, can't go living without it
                // once we discover the platform, let's schedule an immediate server scan
                if ((this.platform == null) || (this.platform.getId() == resourceId)) {
                    log.debug(resource.getId() + ": Platform discovery is needed");
                    discoverPlatform();
                    newPlatformWasDeletedRecently = true;
                    scanIsNeeded = true;
                } else if (isTopLevelServer) {
                    log.debug(resource.getId() + ": Server discovery is needed");

                    // if we got here, we just deleted a top level server (whose parent is the platform), let's request a scan
                    scanIsNeeded = true;
                }
            } else {
                log.debug(resourceId + ": Could not remove resource because it's container was null");
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }

        return scanIsNeeded;
    }

    public Resource getPlatform() {
        return platform;
    }

    public Agent getAgent() {
        return this.agent;
    }

    /**
     * Inject a new availability
     *
     * @param  resource
     * @param  availabilityType
     *
     * @return
     */
    public Availability updateAvailability(Resource resource, AvailabilityType availabilityType) {
        ResourceContainer resourceContainer = this.resourceContainers.get(resource.getUuid());
        return resourceContainer.updateAvailability(availabilityType);
    }

    public Resource mergeResourceFromDiscovery(Resource resource, Resource parent) throws PluginContainerException {
        Resource existingResource = findMatchingChildResource(resource, parent);
        if (existingResource != null) {
            updateResourceVersion(existingResource, resource.getVersion());
            resource = existingResource;
        } else {
            log.debug("Detected new Resource [" + resource + "]");
            parent.addChildResource(resource);
        }

        if ((!this.configuration.isInsideAgent()) && (resource.getId() == 0)) {
            resource.setId(temporaryKeyIndex.decrementAndGet());
        }

        if (getResourceComponent(resource) == null) {
            log.debug("Adding resource " + resource + " to local inventory...");
            ResourceContainer resourceContainer = initResourceContainer(resource);

            // The chunk below used to automatically set to committed resources that:
            // - Were not children of the platform itself
            // - The resource's parent is committed
            // This was disabled to prevent activation before the resource IDs were properly synced with the plugin
            // container. It is still needed for the embedded console.
            // The following conditions, used in the if statement, provided this ability:
            // || ((!parent.equals(this.platform)) && (parent.getInventoryStatus() == InventoryStatus.COMMITTED))) {
            // jdobies, Apr 8, 2008 - RHQ-255

            // Always commit everything for the embedded console
            if (!this.configuration.isInsideAgent()) {
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                try {
                    activateResource(resource, resourceContainer, true); // just start 'em up as we find 'em for the embedded side
                } catch (InvalidPluginConfigurationException e) {
                    sendInvalidPluginConfigurationResourceError(resource, e);
                }
            }
        }

        return resource;
    }

    private ResourceContainer initResourceContainer(Resource resource) {
        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (resourceContainer == null) {
            resourceContainer = new ResourceContainer(resource);
            if (!this.configuration.isInsideAgent()) {
                resourceContainer.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
            }

            this.resourceContainers.put(resource.getUuid(), resourceContainer);
        }

        return resourceContainer;
    }

    /**
     * Removes resources from the specified parent that were previously found but not found again on a subsequent
     * discovery.
     *
     * @param  parent                resource whose children will be checked in this method.
     * @param  resourceType          type of children being checked for omissions fromt he rediscovered resources list.
     * @param  rediscoveredResources resources that have been found on a recent discovery; resources found in the parent
     *                               but not in this list will be deleted as a result of this method.
     *
     * @return all resources that were removed from this operation
     */
    public Set<Resource> removeStaleResources(Resource parent, ResourceType resourceType,
        Set<Resource> rediscoveredResources) {
        Set<Resource> parentSet = new HashSet<Resource>();
        parentSet.add(parent);

        Set<Resource> existingChildResources = getResourcesWithType(resourceType, parentSet);

        // TODO: jdobies, Mar 01, 2007: This needs to be cleaned up

        /* I tried to use rediscovered.contains(), however it wasn't working correctly. For some reason,
         * the UUID for resources with the same keys were not identical. I believe it has to do with the fact that they
         * are newly discovered. Instead, the key is used directly here in an ugly series of code. There may be another
         * aspect of using the UUID that I'm not aware of yet. jdobies, Mar 01, 2007
         */
        Set<Resource> removedResources = new HashSet<Resource>();

        outer: for (Resource child : existingChildResources) {
            inner: for (Resource rediscovered : rediscoveredResources) {
                if (rediscovered.getResourceKey().equals(child.getResourceKey())) {
                    continue outer;
                }
            }

            removedResources.add(child);
            parent.removeChildResource(child);
        }

        return removedResources;
    }

    /**
     * This will start the resource's plugin component, creating it first if it has not yet been created. If the
     * component is already created and started, this method is a no-op.
     *
     * @param  resource        the resource that the component will manage
     * @param  container       the wrapper around the resource and its component
     * @param  newPluginConfig if <code>true</code>, this will indicate that the resource's plugin configuration is
     *                         known to have changed since the last time the resource component was started
     *
     * @throws InvalidPluginConfigurationException when connecting to the managed resource fails due to an invalid
     *                                             plugin configuration
     * @throws PluginContainerException            for all other errors
     */
    @SuppressWarnings("unchecked")
    public void activateResource(Resource resource, @NotNull
    ResourceContainer container, boolean newPluginConfig) throws InvalidPluginConfigurationException,
        PluginContainerException {
        log.debug("Activating resource component for " + resource + "...");

        ResourceComponent component = container.getResourceComponent();

        // if the component already exists and is started, and the resource's plugin config has not changed, there is
        // nothing to do, so return immediately
        if ((component != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)
            && !newPluginConfig) {
            return;
        }

        // if the component does not even exist yet, we need to instantiate it
        if (component == null) {
            try {
                component = PluginContainer.getInstance().getPluginComponentFactory().buildResourceComponent(
                    resource.getResourceType());
            } catch (Throwable e) {
                throw new PluginContainerException("Could not build component for resource [" + resource + "]", e);
            }
            // give the container wrapper the component instance that is managing the resource
            container.setResourceComponent(component);
        }

        // tell the component what its parent is, if it has a parent
        ResourceComponent parentComponent = null;
        if (resource.getParentResource() != null) {
            parentComponent = getResourceComponent(resource.getParentResource());
        }

        // start the resource but only if its parent component is running
        if ((resource.getParentResource() == null)
            || (getResourceContainer(resource.getParentResource()).getResourceComponentState() == ResourceComponentState.STARTED)) {
            PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();
            ResourceDiscoveryComponent discoveryComponent;
            discoveryComponent = factory.getDiscoveryComponent(resource.getResourceType());

            ResourceContext context = new ResourceContext(resource, // the resource itself
                parentComponent, // its parent component
                discoveryComponent, // the discovery component
                SystemInfoFactory.createSystemInfo(), // for native access
                this.configuration.getTemporaryDirectory(), // location for plugin to write tmp files
                new File(this.configuration.getDataDirectory(), resource.getResourceType().getPlugin()), // plugin's own data dir
                this.configuration.getContainerName(), // the name of the agent/PC
                getEventContext(resource), // for event access
                getOperationContext(resource), // for operation manager access
                getContentContext(resource)); // for content manager access

            ClassLoader startingClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
                component.start(context);
                container.setResourceComponentState(ResourceComponentState.STARTED);
                resource.setConnected(true); // This tells the server-side that the resource has connected successfully.
            } catch (Throwable t) {
                if (newPluginConfig || (t instanceof InvalidPluginConfigurationException)) {
                    if (log.isDebugEnabled())
                        log.debug("Resource has a bad config, waiting for this to go away " + resource);
                    InventoryEventListener iel = new ResourceGotActivatedListener();
                    addInventoryEventListener(iel);
                    throw new InvalidPluginConfigurationException("Failed to start component for resource " + resource
                        + ".", t);
                }
                throw new PluginContainerException("Failed to start component for resource " + resource + ".", t);
            } finally {
                Thread.currentThread().setContextClassLoader(startingClassLoader);
            }

            // We purposefully do not get availability of this resource yet
            // We need availability checked during the normal availability executor timeframe.
            // Otherwise, new resources will not have their availabilities shipped up to the server because
            // they will look like they haven't changed status since the last avail report - but the new
            // resources statuses never got sent up in the last avail report because they didn't exist at that time

            // Finally, inform the rest of the plugin container that this resource has been activated
            fireResourceActivated(resource);
        } else {
            log.debug("Not activating resource [" + resource + "] because its parent isn't started: "
                + getResourceContainer(resource.getParentResource()));
        }
    }

    /**
     * This will send a resource error to the server (if applicable) to indicate that the given resource could not be
     * connected to due to an invalid plugin configuration.
     *
     * @param resource the resource that could not be connected to
     * @param t        the exception that indicates the problem with the plugin configuration
     */
    private void sendInvalidPluginConfigurationResourceError(Resource resource, Throwable t) {
        resource.setConnected(false); // invalid plugin configuration infers the resource component is disconnected

        DiscoveryServerService serverService = configuration.getServerServices().getDiscoveryServerService();
        if (serverService != null) {
            // give the server-side an error message describing the connection failure that can be
            // displayed on the resource's Inventory page.
            ResourceError resourceError = new ResourceError(resource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION,
                t, System.currentTimeMillis());
            try {
                serverService.setResourceError(resourceError);
            } catch (Exception e) {
                log.warn("Cannot inform the server about a resource error [" + resourceError + "]. Cause: " + e);
            }
        }
    }

    private Resource findMatchingChildResource(Resource resource, Resource parent) {
        for (Resource child : parent.getChildResources()) {
            if (((child.getId() != 0) && (child.getId() == resource.getId()))
                || (child.getUuid().equals(resource.getUuid()))
                || (child.getResourceType().equals(resource.getResourceType()) && child.getResourceKey().equals(
                    resource.getResourceKey()))) {
                return child;
            }
        }

        return null;
    }

    /**
     * Lookup all the servers with a particular server type
     *
     * @param  serverType the server type to match against
     *
     * @return the set of servers matching the provided type
     */
    public Set<Resource> getResourcesWithType(ResourceType serverType) {
        return getResourcesWithType(serverType, platform.getChildResources());
    }

    private Set<Resource> getResourcesWithType(ResourceType serverType, Set<Resource> resources) {
        Set<Resource> servers = new HashSet<Resource>();

        if (resources == null) {
            return servers;
        }

        for (Resource server : resources) {
            servers.addAll(getResourcesWithType(serverType, server.getChildResources()));

            if (serverType.equals(server.getResourceType())) {
                servers.add(server);
            }
        }

        return servers;
    }

    private void activateFromDisk(Resource resource) throws PluginContainerException {
        if (resource.getId() == 0) {
            return; // This is for the case of a resource that hadn't been synced to the server (there are probably better places to handle this)
        }

        resource.setAgent(this.agent);
        ResourceContainer container = getResourceContainer(resource.getId());
        if (container.getSynchronizationState() != ResourceContainer.SynchronizationState.SYNCHRONIZED) {
            log.debug("Stopped activating resources at unsynchronized resource [" + resource + "]");
            return;
        }

        try {
            activateResource(resource, container, false);
        } catch (Exception e) {
            log.debug("Failed to activate from disk [" + resource + "]");
        }

        for (Resource child : resource.getChildResources()) {
            activateFromDisk(child);
        }
    }

    private void loadFromDisk() {
        this.inventoryLock.writeLock().lock();

        try {
            File file = new File(this.configuration.getDataDirectory(), "inventory.dat");
            if (file.exists()) {
                long start = System.currentTimeMillis();
                log.info("Loading inventory from persistent data file");

                InventoryFile inventoryFile = new InventoryFile(file);
                inventoryFile.loadInventory();

                this.platform = inventoryFile.getPlatform();
                this.resourceContainers = inventoryFile.getResourceContainers();

                initializePlatformComponent();
                activateFromDisk(this.platform);

                log.info("Inventory size [" + this.resourceContainers.size() + "] initialized from disk in ["
                    + (System.currentTimeMillis() - start) + "ms]");
            }
        } catch (Exception e) {
            log.error("Could not load inventory data from disk", e);
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    /**
     * Shutdown the ResourceComponents from the bottom up.
     */
    private void deactivateResource(Resource resource) {
        this.inventoryLock.writeLock().lock();
        try {
            ResourceContainer container = getResourceContainer(resource);
            if ((container != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)) {
                for (Resource child : resource.getChildResources()) {
                    deactivateResource(child);
                }

                try {
                    ResourceComponent component = container.createResourceComponentProxy(ResourceComponent.class,
                        FacetLockType.WRITE, COMPONENT_STOP_TIMEOUT, true, true);
                    component.stop();
                    log.debug("Successfully deactivated resource with id [" + resource.getId() + "].");
                } catch (Throwable t) {
                    log.warn("Plugin Error: Failed to stop component for [" + resource + "].");
                }

                container.setResourceComponentState(ResourceComponentState.STOPPED);
                log.debug("Set component state to STOPPED for resource with id [" + resource.getId() + "].");
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void persistToDisk() {
        try {
            deactivateResource(this.platform);
            File file = new File(this.configuration.getDataDirectory(), "inventory.dat");
            InventoryFile inventoryFile = new InventoryFile(file);
            inventoryFile.storeInventory(this.platform, this.resourceContainers);
        } catch (Exception e) {
            log.error("Could not persist inventory data to disk", e);
        }
    }

    /**
     * Detects the top platform resource and starts its ResourceComponent.
     *
     * TODO GH: Move this to another class (this one is getting too big)
     */
    @SuppressWarnings("unchecked")
    private Resource discoverPlatform() {
        PluginManager pluginManager = PluginContainer.getInstance().getPluginManager();
        PluginComponentFactory componentFactory = PluginContainer.getInstance().getPluginComponentFactory();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        Set<ResourceType> platformTypes = pluginManager.getMetadataManager().getTypesForCategory(
            ResourceCategory.PLATFORM);

        // this should only ever have 1 or at most 2 resources (always the java fallback platform, and the native platform if supported)
        Set<DiscoveredResourceDetails> allDiscoveredPlatforms = new HashSet<DiscoveredResourceDetails>(2);

        if ((platformTypes != null) && (platformTypes.size() > 0)) {
            // go through all the platform types that are supported and see if they can detect our platform
            for (ResourceType platformType : platformTypes) {
                try {
                    ResourceDiscoveryComponent component = componentFactory.getDiscoveryComponent(platformType);
                    ResourceDiscoveryContext context = new ResourceDiscoveryContext(platformType, null, systemInfo,
                        Collections.EMPTY_LIST, Collections.EMPTY_LIST, configuration.getContainerName());
                    Set<DiscoveredResourceDetails> discoveredResources = null;

                    try {
                        discoveredResources = component.discoverResources(context);
                    } catch (Throwable e) {
                        log.warn("Platform plugin discovery failed - skipping", e);
                    }

                    if (discoveredResources != null) {
                        allDiscoveredPlatforms.addAll(discoveredResources);
                    }
                } catch (Throwable e) {
                    log.error("Error in platform discovery", e);
                }
            }
        } else {
            // this is very strange - there are no platform types - we should never be missing the built-in platform plugins
            log.error("Missing platform plugins - falling back to Java-only impl; this should only occur in tests");
            return createTestPlatform();
        }

        if ((allDiscoveredPlatforms.size() < 1) || (allDiscoveredPlatforms.size() > 2)) {
            log.warn("Platform discovery found too little or too many platforms - "
                + "some platform discovery components need to be fixed so they discover only what they should: "
                + allDiscoveredPlatforms);
        }

        DiscoveredResourceDetails javaPlatform = null;
        DiscoveredResourceDetails nativePlatform = null;

        for (DiscoveredResourceDetails discoveredResource : allDiscoveredPlatforms) {
            // we know the Java resource type in the descriptor is named "Java"
            if (discoveredResource.getResourceType().getName().equalsIgnoreCase("Java")) {
                javaPlatform = discoveredResource;
            } else {
                nativePlatform = discoveredResource;
            }
        }

        DiscoveredResourceDetails platformToUse;

        // in most cases, we will have both (since we support most platforms natively)
        // so use the native platform if we have it; if not, fallback to the java platform
        if (nativePlatform != null) {
            platformToUse = nativePlatform;
        } else if (javaPlatform != null) {
            platformToUse = javaPlatform;
        } else {
            throw new IllegalStateException("Neither a native or java platform was discovered - "
                + "this should never happen. Discovered resources are: " + allDiscoveredPlatforms);
        }

        // build our actual platform resource now that we've discovered it
        Resource platform = createNewResource(platformToUse);
        platform.setAgent(this.agent);

        return platform;
    }

    /**
     * If for some reason the platform plugin is not available, this method can be called to add a "dummy" platform
     * resource. This is normally only used during tests.
     */
    private Resource createTestPlatform() {
        ResourceType type = PluginContainer.getInstance().getPluginManager().getMetadataManager().addTestPlatformType();
        Resource platform = new Resource("testkey" + configuration.getContainerName(), "testplatform", type);
        platform.setAgent(this.agent);
        return platform;
    }

    /**
     * This starts the given platform component. Do not call this method until the {@link #platform} has been determined
     * and set.
     */
    private void initializePlatformComponent() {
        try {
            // now that we started the platform resource component, register it in our list of resource containers
            // first see if we already have an existing ResourceContainer for platform and use this instead of
            // creating a new one
            ResourceContainer platformContainer = getResourceContainer(platform);
            if (platformContainer == null) {
                platformContainer = new ResourceContainer(platform);

                /* Setting inventory status to COMMITTED and SynchronousState to Synchronized
                 * for Embedded Console Platform resource
                 */
                if (!this.configuration.isInsideAgent()) {
                    platformContainer.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
                    platform.setInventoryStatus(InventoryStatus.COMMITTED);
                }

                ResourceComponent platformComponent;
                if (this.platform.getResourceType().equals(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
                    platformComponent = createTestPlatformComponent();
                } else {
                    platformComponent = null;
                }

                platformContainer.setResourceComponent(platformComponent); // If provided, this will be used, otherwise a lookup will be done
                platformContainer.setResourceComponentState(ResourceComponentState.STARTED);
                this.resourceContainers.put(platform.getUuid(), platformContainer);
            }

            activateResource(platform, platformContainer, false);
        } catch (Exception e) {
            // technically this should not happen - what should we do here if it does?
            log.fatal("Platform component failed to start!", e);
            throw new IllegalStateException("For some reason, the platform component can't start!",
                new WrappedRemotingException(e));
        }
    }

    public void synchronizeInventory(int resourceId, EnumSet<SynchronizationType> synchronizationTypes) {
        log.info("Synchronizing server/agent inventories for resource subtree " + resourceId);
        // Get the latest resource data rooted at the given id

        if (synchronizationTypes.contains(DiscoveryAgentService.SynchronizationType.STATUS)) {
            Map<Integer, InventoryStatus> statuses = configuration.getServerServices().getDiscoveryServerService()
                .getInventoryStatus(resourceId, true);

            inventoryLock.writeLock().lock();
            try {
                mergeStatuses(statuses);
            } finally {
                inventoryLock.writeLock().unlock();
            }

            // Resources may have been committed since the last runtime report... do it again
            inventoryThreadPoolExecutor.schedule((Callable<? extends Object>) this.serviceScanExecutor, 5,
                TimeUnit.SECONDS);

            // just imported new things, report on their availabilities immediately
            performAvailabilityChecks(true);
        }

        Resource root = getResourceContainer(resourceId).getResource();
        if (synchronizationTypes.contains(DiscoveryAgentService.SynchronizationType.MEASUREMENT_SCHEDULES)
            && (root.getInventoryStatus() == InventoryStatus.COMMITTED)) {
            if (ResourceCategory.PLATFORM == root.getResourceType().getCategory()) {
                // Get and schedule the latest measurement schedules rooted at the given id
                // This should include disabled schedules to make sure that previously enabled schedules are shut off
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resourceId, false);
                installSchedules(scheduleRequests);
                for (Resource child : root.getChildResources()) {
                    scheduleRequests = configuration.getServerServices().getMeasurementServerService()
                        .getLatestSchedulesForResourceId(child.getId(), true);
                    installSchedules(scheduleRequests);
                }
            } else {
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resourceId, true);
                installSchedules(scheduleRequests);
            }
        }
    }

    private void installSchedules(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        if (PluginContainer.getInstance().getMeasurementManager() != null) {
            PluginContainer.getInstance().getMeasurementManager().scheduleCollection(scheduleRequests);
        } else {
            // MeasurementManager hasn't been started yet
            for (ResourceMeasurementScheduleRequest resourceRequest : scheduleRequests) {
                ResourceContainer resourceContainer = getResourceContainer(resourceRequest.getResourceId());
                resourceContainer.setMeasurementSchedule(resourceRequest.getMeasurementSchedules());
            }
        }
    }

    /**
     * Calling this method will immediately perform an availability check on all inventories resources. The availability
     * checks will be made asynchronously; this method will not block.
     *
     * @param sendFullReport if <code>true</code>, the availability report that is sent will contain availability
     *                       records for all resources; if <code>false</code> the report will only contain records for
     *                       those resources whose availability changed from their last known state.
     */
    private void performAvailabilityChecks(boolean sendFullReport) {
        if (sendFullReport) {
            availabilityExecutor.sendFullReportNextTime();
        }

        availabilityThreadPoolExecutor.schedule((Runnable) availabilityExecutor, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * This merges in a resource tree from the server into the local inventory of the agent. It will insert new
     * resources if needed and update their status as necessary handling change of status.
     */
    private void mergeStatuses(Map<Integer, InventoryStatus> statuses) {
        for (Integer id : statuses.keySet()) {
            InventoryStatus statusFromServer = statuses.get(id);
            ResourceContainer container = getResourceContainer(id);
            if (container == null) {
                // TODO GH: Insert new resource and start it up
            } else {
                // TODO GH: Plan out everything that we will synchronize
                Resource localResource = container.getResource();
                if ((container.getResourceComponentState() != ResourceComponentState.STARTED)
                    || (localResource.getInventoryStatus() != statusFromServer)) {
                    localResource.setInventoryStatus(statusFromServer);
                    switch (localResource.getInventoryStatus()) {
                    case COMMITTED: {
                        try {
                            // committed components are started now
                            boolean newPluginConfig = true;
                            activateResource(localResource, container, newPluginConfig);
                        } catch (InvalidPluginConfigurationException ipce) {
                            sendInvalidPluginConfigurationResourceError(localResource, ipce);
                            log.warn("Cannot activate resource [" + localResource
                                + "] from synchronized merge due to invalid plugin config");
                        } catch (Exception e) {
                            log.warn("Failed to activate resource from synchronized merge [" + localResource + "]");
                        }

                        break;
                    }

                    case DELETED: {
                        removeResource(id);
                        break;
                    }
                    }
                }

                container.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
            }
        }
    }

    /**
     * Instructs the inventory manager to notify the specified listener of inventory change events.
     *
     * @param listener instance to notify of change events
     */
    public void addInventoryEventListener(InventoryEventListener listener) {
        this.inventoryEventListeners.add(listener);
    }

    /**
     * Removes the specified listener from notification of inventory change events.
     *
     * @param listener instance to remove from event notification
     */
    public void removeInventoryEventListener(InventoryEventListener listener) {
        this.inventoryEventListeners.remove(listener);
    }

    /**
     * Notifies all inventory listeners that the specified resources have been added to the inventory.
     *
     * @param resources resources that were added to trigger this event; will not fire an event if this is <code>
     *                  null</code>
     */
    void fireResourcesAdded(Set<Resource> resources) {
        if (resources == null) {
            return;
        }

        for (InventoryEventListener listener : inventoryEventListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourcesAdded(resources);
            } catch (Throwable t) {
                log.error("Error while invoking resources added event on listener", t);
            }
        }
    }

    void fireResourceActivated(Resource resource) {
        if ((resource == null) || (resource.getId() == 0)) {
            log.debug("Not firing activated event for resource: " + resource);
            return;
        }

        log.debug("Firing activated for resource: " + resource);

        for (InventoryEventListener listener : inventoryEventListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourceActivated(resource);
            } catch (Throwable t) {
                log.error("Error while invoking resource activated event on listener", t);
            }
        }
    }

    /**
     * Notifies all inventory listeners that the specified resources have been removed from the inventory.
     *
     * @param resources resources that were removed to trigger this event; will not fire an event if this is <code>
     *                  null</code>
     */
    void fireResourcesRemoved(Set<Resource> resources) {
        if (resources == null) {
            return;
        }

        for (InventoryEventListener listener : inventoryEventListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourcesRemoved(resources);
            } catch (Throwable t) {
                log.error("Error while invoking resources removed event on listener", t);
            }
        }
    }

    public void enableServiceScans(int serverResourceId, Configuration config) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    public void disableServiceScans(int serverResourceId) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    @Nullable
    private EventContext getEventContext(Resource resource) {
        EventContext eventContext;
        if (resource.getResourceType().getEventDefinitions() != null
            && !resource.getResourceType().getEventDefinitions().isEmpty()) {
            eventContext = new EventContextImpl(resource);
        } else {
            eventContext = null;
        }
        return eventContext;
    }

    private OperationContext getOperationContext(Resource resource) {
        if (resource.getResourceType().getOperationDefinitions() == null
            || resource.getResourceType().getOperationDefinitions().isEmpty()) {
            return null;
        }

        if (resource.getId() == 0) {
            log.warn("RESOURCE ID IS 0! Operation features may not work - resource needs to be synced with server");
        }

        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationServices operationServices = new OperationServicesAdapter(operationManager);
        OperationContext operationContext = new OperationContextImpl(resource.getId(), operationServices);
        return operationContext;
    }

    private ContentContext getContentContext(Resource resource) {
        if (resource.getResourceType().getPackageTypes() == null
            || resource.getResourceType().getPackageTypes().isEmpty()) {
            return null;
        }

        if (resource.getId() == 0) {
            log.warn("RESOURCE ID IS 0! Content features may not work - resource needs to be synced with server");
        }

        ContentServices cm = PluginContainer.getInstance().getContentManager();
        ContentContext contentContext = new ContentContextImpl(resource.getId(), cm);
        return contentContext;
    }

    private ResourceComponent createTestPlatformComponent() {
        return new ResourceComponent() {
            public AvailabilityType getAvailability() {
                return AvailabilityType.UP;
            }

            public void start(ResourceContext context) {
            }

            public void stop() {
            }
        };
    }

    private void updateResourceVersion(Resource resource, String version) {
        String existingVersion = resource.getVersion();
        boolean versionChanged = (existingVersion != null) ? !existingVersion.equals(version) : version != null;
        if (versionChanged) {
            log.debug("Discovery reported that version of " + resource + " changed from '" + existingVersion + "' to '"
                + version + "'.");
            boolean versionShouldBeUpdated = resource.getInventoryStatus() != InventoryStatus.COMMITTED
                || updateResourceVersionOnServer(resource, version);
            if (versionShouldBeUpdated) {
                resource.setVersion(version);
                log.info("Version of " + resource + " changed from '" + existingVersion + "' to '" + version + "'.");
            }
        }
    }

    private boolean updateResourceVersionOnServer(Resource resource, String newVersion) {
        boolean versionUpdated = false;
        ServerServices serverServices = this.configuration.getServerServices();
        if (serverServices != null) {
            try {
                DiscoveryServerService discoveryServerService = serverServices.getDiscoveryServerService();
                discoveryServerService.updateResourceVersion(resource.getId(), newVersion);
                // Only update the version in local inventory if the server sync succeeded, otherwise we won't know
                // to try again the next time this method is called.
                versionUpdated = true;
                if (log.isDebugEnabled()) {
                    log.debug("New version for " + resource + " (" + newVersion
                        + ") was successfully synced to the Server.");
                }
            } catch (Exception e) {
                log.error("Failed to sync-to-Server new version for " + resource + ".");
            }
            // TODO: It would be cool to publish a Resource-version-changed Event here. (ips, 02/29/08)
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sync-to-Server of new version for " + resource
                    + " cannot be done, because Plugin Container is not connected to Server.");
            }
        }
        return versionUpdated;
    }

    /**
     * That class implements a listener that gets called when the resource got activated
     * @author hrupp
     *
     */
    class ResourceGotActivatedListener implements InventoryEventListener {

        public void resourceActivated(Resource resource) {
            if (resource != null && resource.getId() > 0) {
                if (log.isDebugEnabled())
                    log.debug("Resource got finally activated, cleaning out config errors " + resource);

                DiscoveryServerService serverService = configuration.getServerServices().getDiscoveryServerService();
                if (serverService != null) {
                    serverService.clearResourceConfigError(resource.getId());
                }
            }
            removeInventoryEventListener(this);
        }

        public void resourcesAdded(Set<Resource> resources) {
            // nothing to do

        }

        public void resourcesRemoved(Set<Resource> resources) {
            // nothing to do

        }

    }
}