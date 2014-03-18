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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.inventory;

import static org.rhq.core.util.StringUtil.isNotBlank;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
import org.rhq.core.clientapi.agent.metadata.ResourceTypeNotEnabledException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.DiscoveryServerService;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.clientapi.server.discovery.StaleTypeException;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeInventoryReportResults.ResourceTypeFlyweight;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.PlatformSyncInfo;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.ResourceUpgradeReport;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.agent.AgentRegistrar;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.agent.AgentServiceStreamRemoter;
import org.rhq.core.pc.availability.AvailabilityContextImpl;
import org.rhq.core.pc.component.ComponentInvocationContextImpl;
import org.rhq.core.pc.configuration.ConfigurationCheckExecutor;
import org.rhq.core.pc.content.ContentContextImpl;
import org.rhq.core.pc.content.ContentManager;
import org.rhq.core.pc.drift.sync.DriftSyncManager;
import org.rhq.core.pc.event.EventContextImpl;
import org.rhq.core.pc.event.EventManager;
import org.rhq.core.pc.inventory.ResourceContainer.ResourceComponentState;
import org.rhq.core.pc.measurement.MeasurementManager;
import org.rhq.core.pc.operation.OperationContextImpl;
import org.rhq.core.pc.operation.OperationManager;
import org.rhq.core.pc.plugin.BlacklistedException;
import org.rhq.core.pc.plugin.CanonicalResourceKey;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.core.pc.upgrade.DiscoverySuspendedException;
import org.rhq.core.pc.upgrade.ResourceUpgradeDelegate;
import org.rhq.core.pc.util.DiscoveryComponentProxyFactory;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.inventory.ClassLoaderFacet;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.InventoryContext;
import org.rhq.core.pluginapi.inventory.ManualAddFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryCallback;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeFacet;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.StopWatch;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.exception.WrappedRemotingException;

/**
 * Manages the process of both auto-detection of servers and runtime detection of services across all plugins. Manages
 * their scheduling and result sending as well as the general inventory model.
 * <p>
 * This is an Agent service; its DiscoveryAgentService interface is made remotely accessible if it is deployed within
 * the Agent.
 * </p>
 *
 * @author Greg Hinkle
 * @author Ian Springer
 * @author Jay Shaughnessy
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class InventoryManager extends AgentService implements ContainerService, DiscoveryAgentService {
    private static final Log log = LogFactory.getLog(InventoryManager.class);

    private static final String INVENTORY_THREAD_POOL_NAME = "InventoryManager.discovery";
    private static final String AVAIL_THREAD_POOL_NAME = "InventoryManager.availability";
    private static final int AVAIL_THREAD_POOL_CORE_POOL_SIZE = 1;

    private static final int COMPONENT_START_TIMEOUT = 60 * 1000; // 60 seconds
    private static final int COMPONENT_STOP_TIMEOUT = 5 * 1000; // 5 seconds

    static private final int SYNC_BATCH_SIZE;

    static {

        int syncBatchSize = 500;
        try {
            syncBatchSize = Integer.parseInt(System.getProperty("rhq.agent.sync.batch.size", "500"));
        } catch (Throwable t) {
            //
        }
        SYNC_BATCH_SIZE = syncBatchSize;
    }

    private final PluginContainerConfiguration configuration;

    private ScheduledThreadPoolExecutor inventoryThreadPoolExecutor;
    private ScheduledThreadPoolExecutor availabilityThreadPoolExecutor;

    // The executors are Callable
    private final AutoDiscoveryExecutor serverScanExecutor;
    private final RuntimeDiscoveryExecutor serviceScanExecutor;
    private final AvailabilityExecutor availabilityExecutor;

    private final Agent agent;

    /**
     * Child scanning process.
     */
    private volatile Future<?> serviceScan = null;

    /**
     * Root platform resource, required to be root of entire inventory tree in this agent
     */
    private Resource platform;

    /**
     * if the {@link #getPlatform() platform} has inventory status of NEW, this indicates it was committed before but
     * was deleted recently
     */
    private boolean newPlatformWasDeletedRecently = false; // value only is valid/relevant if platform.getInventoryStatus == NEW

    private final ReentrantReadWriteLock inventoryLock = new ReentrantReadWriteLock(true);

    /**
     * Used only for the outside the agent model to # resources
     */
    private final AtomicInteger temporaryKeyIndex = new AtomicInteger(-1);

    /**
     * UUID to ResourceContainer map
     */
    private final Map<String, ResourceContainer> resourceContainersByUUID = new ConcurrentHashMap<String, ResourceContainer>(
        500);

    /**
     * ResourceID to ResourceContainer map
     */
    private final TIntObjectMap<ResourceContainer> resourceContainerByResourceId = new TIntObjectHashMap<ResourceContainer>(
        500);

    /**
     * Collection of event listeners to inform of changes to the inventory.
     */
    private final Set<InventoryEventListener> inventoryEventListeners = new CopyOnWriteArraySet<InventoryEventListener>();

    private final PluginManager pluginManager;

    private final DiscoveryComponentProxyFactory discoveryComponentProxyFactory;

    /**
     * Handles the resource upgrade during the initialization of the inventory manager.
     */
    private final ResourceUpgradeDelegate resourceUpgradeDelegate = new ResourceUpgradeDelegate(this);
    private final PluginComponentFactory pluginFactory;
    private final EventManager eventManager;
    private MeasurementManager measurementManager;

    /**
     * Constructs a new instance.
     * Call {@link #initialize()} once constructed.
     */
    public InventoryManager(PluginContainerConfiguration configuration, AgentServiceStreamRemoter streamRemoter,
        PluginManager pluginManager, EventManager eventManager) {
        super(DiscoveryAgentService.class, streamRemoter);
        this.configuration = configuration;
        if (pluginManager == null)
            throw new NullPointerException("pluginManager is null");
        this.pluginManager = pluginManager;
        this.pluginFactory = new PluginComponentFactory(this, pluginManager);
        this.eventManager = eventManager;
        availabilityExecutor = new AvailabilityExecutor(this);
        serviceScanExecutor = new RuntimeDiscoveryExecutor(this, configuration);
        serverScanExecutor = new AutoDiscoveryExecutor(null, this);
        discoveryComponentProxyFactory = new DiscoveryComponentProxyFactory(pluginFactory);
        agent = new Agent(this.configuration.getContainerName(), null, 0, null, null);
    }

    /**
     * Loads the inventory, sets up the platform, measurement manager, and
     * upgrades the resources, and schedules periodic tasks.
     *
     * Because of dependency circularity, initialization has to happen
     * outside of the constructor.
     *
     * ClassLoaderManager depends on InventoryManager to return the platform resource.
     * InventoryManager calls PluginComponentFactory which then calls ClassLoaderManager to load classes.
     */
    public void initialize() {
        inventoryLock.writeLock().lock();

        try {
            log.info("Initializing Inventory Manager...");

            discoveryComponentProxyFactory.initialize();

            if (configuration.isInsideAgent()) {
                loadFromDisk();
            }

            // Discover the platform first thing.
            executePlatformScan();

            // Initialize measurement manager
            measurementManager = new MeasurementManager(configuration, getStreamRemoter(), this);

            //try the resource upgrade before we have any schedulers set up
            //so that we don't get any interventions from concurrently running
            //discoveries.
            activateAndUpgradeResources();

            // Never run more than one avail check at a time.
            availabilityThreadPoolExecutor = new ScheduledThreadPoolExecutor(AVAIL_THREAD_POOL_CORE_POOL_SIZE,
                new LoggingThreadFactory(AVAIL_THREAD_POOL_NAME, true));

            // Never run more than one discovery scan at a time (service and service scans share the same pool).
            inventoryThreadPoolExecutor = new ScheduledThreadPoolExecutor(1, new LoggingThreadFactory(
                INVENTORY_THREAD_POOL_NAME, true));

            // Only schedule periodic discovery scans and avail checks if we are running inside the RHQ Agent (versus
            // inside EmbJopr).
            if (configuration.isInsideAgent()) {
                // After an initial delay (5s by default), periodically run an availability check (every 1m by default).
                availabilityThreadPoolExecutor.scheduleWithFixedDelay(availabilityExecutor,
                    configuration.getAvailabilityScanInitialDelay(), configuration.getAvailabilityScanPeriod(),
                    TimeUnit.SECONDS);

                // After an initial delay (10s by default), periodically run a server discovery scan (every 15m by default).
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serverScanExecutor,
                    configuration.getServerDiscoveryInitialDelay(), configuration.getServerDiscoveryPeriod(),
                    TimeUnit.SECONDS);

                // After an initial delay (20s by default), periodically run a service discovery scan (every 1d by default).
                inventoryThreadPoolExecutor.scheduleWithFixedDelay(serviceScanExecutor,
                    configuration.getServiceDiscoveryInitialDelay(), configuration.getServiceDiscoveryPeriod(),
                    TimeUnit.SECONDS);
            }
        } finally {
            inventoryLock.writeLock().unlock();
        }

        log.info("Inventory Manager initialized.");
    }

    /**
     * @see ContainerService#shutdown()
     */
    @Override
    public void shutdown() {
        PluginContainer.shutdownExecutorService(this.inventoryThreadPoolExecutor, true);
        PluginContainer.shutdownExecutorService(this.availabilityThreadPoolExecutor, true);
        if (this.configuration.isInsideAgent()) {
            this.persistToDisk();
        }
        this.discoveryComponentProxyFactory.shutdown();
        this.inventoryEventListeners.clear();
        this.resourceContainersByUUID.clear();
        this.resourceContainerByResourceId.clear();
    }

    /**
     * Invokes the given discovery component in order to discover resources. This will return
     * the discovered resources' details as returned by the discovery component. This may return
     * an empty set if nothing is discovered. This may return <code>null</code> if for some reason
     * we could not invoke the discovery component.
     *
     * @param parentResourceContainer the container of the resource under which we are going to execute the discovery
     * @param component the discovery component that will actually go out and discover resources
     * @param context the context for use by the discovery component
     * @return the details of all discovered resources, may be empty or <code>null</code>
     *
     * @throws DiscoverySuspendedException if the discovery is suspended due to a resource upgrade failure
     * @throws Exception if the discovery component threw an exception
     */
    public Set<DiscoveredResourceDetails> invokeDiscoveryComponent(ResourceContainer parentResourceContainer,
        ResourceDiscoveryComponent component, ResourceDiscoveryContext context) throws DiscoverySuspendedException,
        Exception {

        Resource parentResource = parentResourceContainer == null ? null : parentResourceContainer.getResource();

        if (resourceUpgradeDelegate.hasUpgradeFailedInChildren(parentResource, context.getResourceType())) {
            String message = "Discovery of [" + context.getResourceType() + "] has been suspended under "
                + (parentResource == null ? " the platform " : parentResource)
                + " because some of its siblings failed to upgrade.";

            log.debug(message);

            throw new DiscoverySuspendedException(message);
        }

        long timeout = getDiscoveryComponentTimeout(context.getResourceType());

        Set<DiscoveredResourceDetails> results;

        try {
            ResourceDiscoveryComponent proxy = this.discoveryComponentProxyFactory.getDiscoveryComponentProxy(
                context.getResourceType(), component, timeout, parentResourceContainer);
            results = proxy.discoverResources(context);
        } catch (TimeoutException te) {
            log.warn("Discovery for Resources of [" + context.getResourceType() + "] has been running for more than "
                + timeout + " milliseconds. This may be a plugin bug.", te);
            results = null;
        } catch (BlacklistedException be) {
            // Discovery did not run, because the ResourceType was blacklisted during a prior discovery scan.
            if (log.isDebugEnabled()) {
                log.debug(ThrowableUtil.getAllMessages(be));
            }
            results = null;
        }

        if (results == null || results.isEmpty()) {
            return results;
        }

        // find the discovery callbacks defined, if there are none, just return the results as-is
        Map<String, List<String>> callbacks = this.pluginManager.getMetadataManager().getDiscoveryCallbacks(
            context.getResourceType());
        if (callbacks == null || callbacks.isEmpty()) {
            return results;
        }

        // funnel the results through the discovery callbacks that are defined on the discovered resource type
        // we do them one discovered resource details as a time, giving each callback the opportunity to handle each
        ResourceDiscoveryCallback.DiscoveryCallbackResults callbackResults;
        PluginComponentFactory pluginComponentFactory = PluginContainer.getInstance().getPluginComponentFactory();
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();

        for (Iterator<DiscoveredResourceDetails> detailsIterator = results.iterator(); detailsIterator.hasNext();) {
            DiscoveredResourceDetails details = detailsIterator.next();
            int callbackCount = 0;
            boolean stopProcessing = false; // if true, a callback told us he found a details that he modified and we should stop
            boolean abortDiscovery = false; // if true, multiple callbacks claimed ownership which isn't allowed - its discovery will be aborted
            boolean vetoDiscovery = false; // if true, a callback veto'ed the resource - it should not be discovered at all
            for (Map.Entry<String, List<String>> entry : callbacks.entrySet()) {
                String pluginName = entry.getKey();
                List<String> callbackClassNames = entry.getValue();
                for (String className : callbackClassNames) {
                    ResourceDiscoveryCallback callback = pluginComponentFactory.getDiscoveryCallback(pluginName,
                        className);
                    try {
                        Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());
                        callbackResults = callback.discoveredResources(details);// inline in our calling thread - no time outs or anything; hopefully the plugin plays nice
                        callbackCount++;
                        if (log.isDebugEnabled()) {
                            log.debug("Discovery callback [{" + pluginName + "}" + className + "] returned ["
                                + callbackResults + "] #invocations=" + callbackCount);
                        }
                        switch (callbackResults) {
                        case PROCESSED: {
                            if (stopProcessing) {
                                abortDiscovery = true;
                                log.warn("Another discovery callback [{" + pluginName + "}" + className
                                    + "] processed details [" + details
                                    + "]. This is not allowed. Discovery will be aborted for that resource");
                            } else {
                                stopProcessing = true;
                            }
                            break;
                        }
                        case VETO: {
                            vetoDiscovery = true;
                            log.warn("Discovery callback [{" + pluginName + "}" + className + "] vetoed resource ["
                                + details
                                + "]. Discovery will be skipped for that resource and it will not be inventoried.");
                            break;
                        }
                        default: {
                            // callback left the details unprocessed, nothing to do.
                            break;
                        }
                        }
                        // note that we keep going, even if we set stopProcessing is true - this is because we
                        // want to keep calling callbacks and check if they, too, think they can identify the details. If
                        // they can, something is wrong and we want to abort since more than one callback should not be
                        // claiming ownership of the same details. We could, in the future, not do this check and just
                        // stop calling callbacks the first time a callback tells us it processed details
                    } catch (Throwable t) {
                        log.error("Discovery callback [{" + pluginName + "}" + className + "] failed", t);
                    } finally {
                        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
                    }
                } // for each callback
            } // for each plugin

            if (abortDiscovery) {
                // provide a backdoor escape hatch - you can avoid aborting by setting this sysprop to true
                if (Boolean.getBoolean("rhq.agent.discovery-callbacks.never-abort") == false) {
                    detailsIterator.remove(); // we do not want to process this details
                }
            }

            if (vetoDiscovery) {
                detailsIterator.remove();
            }
        }

        return results;
    }

    /**
     * Invokes the given discovery component in order to manually add a Resource with the specified plugin config. This
     * will return the discovered resource's detail as returned by the discovery component, or it may return
     * an empty set if nothing is discovered. This may return <code>null</code> if for some reason
     * we could not invoke the discovery component.
     *
     *
     * @param component the discovery component that will actually go out and discover resources
     * @param pluginConfig the plugin configuration to be used to connect to the resource to be discovered
     * @param context the context for use by the discovery component
     * @param parentResourceContainer
     * @return the details of all discovered resources, may be empty or <code>null</code>
     *
     * @throws Exception if the discovery component threw an exception
     */
    private DiscoveredResourceDetails discoverResource(ResourceDiscoveryComponent component,
        Configuration pluginConfig, ResourceDiscoveryContext context, ResourceContainer parentResourceContainer)
        throws Exception {
        long timeout = getDiscoveryComponentTimeout(context.getResourceType());

        try {
            ManualAddFacet proxy = this.discoveryComponentProxyFactory.getDiscoveryComponentProxy(
                context.getResourceType(), component, timeout, ManualAddFacet.class, parentResourceContainer);
            DiscoveredResourceDetails result = proxy.discoverResource(pluginConfig, context);
            return result;
        } catch (TimeoutException te) {
            log.warn("Manual add of Resource of type [" + context.getResourceType() + "] with plugin configuration ["
                + pluginConfig.toString(true) + "] has been running for more than " + timeout
                + " milliseconds. This may be a plugin bug.", te);
            return null;
        } catch (BlacklistedException be) {
            if (log.isDebugEnabled()) {
                log.debug(ThrowableUtil.getAllMessages(be));
            }
            return null;
        }
    }

    /**
     * Invokes the given discovery component's ClassLoaderFacet in order to obtain
     * additional jars for the resource's classloader. This will return
     * the discovered resources' details as returned.
     *
     * @param resource the resource whose component is to be invoked
     * @param component the discovery component that will actually go out and discover resources
     * @param parentContainer the activated parent container
     * @return the additional jars for the resource's classloader
     *
     * @throws Throwable if the discovery component threw an exception
     */
    public List<URL> invokeDiscoveryComponentClassLoaderFacet(Resource resource, ResourceDiscoveryComponent component,
        ResourceContainer parentContainer) throws Throwable {

        ResourceComponent parentComponent = parentContainer.getResourceComponent();
        ResourceContext parentResourceContext = parentContainer.getResourceContext();

        ResourceType resourceType = resource.getResourceType();
        long timeout = getDiscoveryComponentTimeout(resourceType);

        ClassLoaderFacet proxy = this.discoveryComponentProxyFactory.getDiscoveryComponentProxy(resourceType,
            component, timeout, ClassLoaderFacet.class, parentContainer);

        ResourceDiscoveryContext discoveryContext = new ResourceDiscoveryContext(resourceType, parentComponent,
            parentResourceContext, SystemInfoFactory.createSystemInfo(), null, null,
            this.configuration.getContainerName(), this.configuration.getPluginContainerDeployment());

        // Configurations are not immutable, so clone the plugin config, so the plugin will not be able to change the
        // actual PC-managed plugin config.
        Configuration pluginConfigClone = resource.getPluginConfiguration().deepCopy(false);
        // TODO (ips): Clone the ResourceType too for the same reason.

        DiscoveredResourceDetails details = new DiscoveredResourceDetails(resourceType, resource.getResourceKey(),
            resource.getName(), resource.getVersion(), resource.getDescription(), pluginConfigClone, null); // TODO: I have a feeling we'll need process info, how to get it??

        List<URL> results = proxy.getAdditionalClasspathUrls(discoveryContext, details);

        return results;
    }

    public <T extends ResourceComponent<?>> ResourceUpgradeReport invokeDiscoveryComponentResourceUpgradeFacet(
        ResourceType resourceType, ResourceDiscoveryComponent<T> component,
        ResourceUpgradeContext<T> inventoriedResource, ResourceContainer parentResourceContainer) throws Throwable {

        long timeout = getDiscoveryComponentTimeout(resourceType);
        try {
            ResourceUpgradeFacet<T> proxy = this.discoveryComponentProxyFactory.getDiscoveryComponentProxy(
                resourceType, component, timeout, ResourceUpgradeFacet.class, parentResourceContainer);

            return proxy.upgrade(inventoriedResource);
        } catch (BlacklistedException e) {
            log.debug(e);
            return null;
        }
    }

    public DiscoveryComponentProxyFactory getDiscoveryComponentProxyFactory() {
        return this.discoveryComponentProxyFactory;
    }

    private long getDiscoveryComponentTimeout(ResourceType type) {
        // TODO: remove this system property and put the timeout in the plugin descriptor;
        // use the type to find what timeout to use since each type can have metadata determining the timeout
        // default should be 300000.
        long timeout = Long.parseLong(System.getProperty("rhq.test.discovery-timeout", "300000"));
        return timeout;
    }

    @Nullable
    public ResourceContainer getResourceContainer(String uuid) {
        return this.resourceContainersByUUID.get(uuid);
    }

    @Nullable
    public ResourceContainer getResourceContainer(CanonicalResourceKey canonicalId) {
        ResourceContainer resourceContainer = null;
        for (Map.Entry<String, ResourceContainer> entry : resourceContainersByUUID.entrySet()) {
            ResourceContainer container = entry.getValue();
            Resource resource = container.getResource();
            if (resource != null) {
                Resource parent = resource.getParentResource();
                if (parent != null) {
                    try {
                        CanonicalResourceKey currentCanonicalId = new CanonicalResourceKey(resource, parent);
                        if (currentCanonicalId.equals(canonicalId)) {
                            resourceContainer = container;
                            break;
                        }
                    } catch (PluginContainerException ignore) {
                        // TODO not sure what to do here, when would this ever happen? for now, ignore
                    }
                }
            }
        }
        return resourceContainer;
    }

    @Nullable
    public ResourceContainer getResourceContainer(Resource resource) {
        String uuid = resource.getUuid();
        if (uuid == null)
            return null;
        return this.resourceContainersByUUID.get(uuid);
    }

    @Nullable
    public ResourceContainer getResourceContainer(int resourceId) {
        if (resourceId == 0) {
            // I've already found one place where passing in 0 was very bad - I want to be very noisy in the log
            // when this happens but not throw an exception, for fear I might break something.
            // I'll just return null instead; hopefully, callers are checking for null appropriately.
            log.warn("Cannot get a resource container for an invalid resource ID=" + resourceId);
            if (log.isDebugEnabled()) {
                //noinspection ThrowableInstanceNeverThrown
                log.debug("Stack trace follows:", new Throwable("This is where resource ID=[" + resourceId
                    + "] was passed in"));
            }

            return null;
        }

        ResourceContainer resourceContainer = this.resourceContainerByResourceId.get(resourceId);
        if (resourceContainer != null) {
            return resourceContainer;
        }
        // We did not find the UUID in above map.
        // Check on the classical way if the container is present and populate the
        // resourceId -> resourceContainer map for the next call into this method.
        ResourceContainer retContainer = null;
        for (ResourceContainer container : resourceContainersByUUID.values()) {
            if (resourceId == container.getResource().getId()) {
                retContainer = container;
                this.resourceContainerByResourceId.put(resourceId, retContainer);
                break;
            }
        }
        return retContainer;
    }

    void executePlatformScan() {
        log.debug("Executing platform scan...");
        Resource discoveredPlatform = discoverPlatform();
        try {
            mergeResourceFromDiscovery(discoveredPlatform, null);
        } catch (PluginContainerException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
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
            // TODO: What about re-activating the Resource's descendants?
        } catch (InvalidPluginConfigurationException e) {
            String errorMessage = "Unable to connect to managed resource of type ["
                + resource.getResourceType().getName() + "] using the specified connection properties.";
            log.info(errorMessage, e);
            errorMessage += ((e.getLocalizedMessage() != null) ? (" " + e.getLocalizedMessage()) : "");

            // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
            // stack trace, but append the message from that exception to the message of the exception we throw. This
            // will make for a nicer error message for the server to display in the UI.
            throw new InvalidPluginConfigurationClientException(errorMessage,
                (e.getCause() != null) ? new WrappedRemotingException(e.getCause()) : null);
        }
    }

    private InventoryReport submit(Callable<InventoryReport> c) {
        try {
            return inventoryThreadPoolExecutor.submit(c).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Scan execution was interrupted");
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    @NotNull
    public InventoryReport executeServerScanImmediately() {
        return submit(serverScanExecutor);
    }

    @Override
    @NotNull
    public InventoryReport executeServiceScanImmediately() {
        return submit(serviceScanExecutor);
    }

    @NotNull
    public InventoryReport executeServiceScanImmediately(Resource resource) {
        RuntimeDiscoveryExecutor discoveryExecutor = new RuntimeDiscoveryExecutor(this, this.configuration, resource);
        return submit(discoveryExecutor);
    }

    @Override
    public boolean executeServiceScanDeferred() {
        // if there is a scan in progress, cancel it and start over.  No need to bog down the agent with multiple
        // concurrent service scans.  The one in progress is now dated, because we likely have just added agent-side
        // resources.
        if ((null != serviceScan) && !serviceScan.isDone()) {
            boolean isCanceled = serviceScan.cancel(true);
            if (isCanceled) {
                log.info("Canceled in-progress service scan in favor of starting new scan. Partial scan will still be processed.");
            }
        }
        serviceScan = this.inventoryThreadPoolExecutor.schedule((Runnable) this.serviceScanExecutor,
            configuration.getChildResourceDiscoveryDelay(), TimeUnit.SECONDS);

        return true;
    }

    @Override
    public void executeServiceScanDeferred(int resourceId) {
        Resource resource = getResourceContainer(resourceId).getResource();
        RuntimeDiscoveryExecutor discoveryExecutor = new RuntimeDiscoveryExecutor(this, this.configuration, resource);
        inventoryThreadPoolExecutor.submit((Runnable) discoveryExecutor);
    }

    /**
     * This method implicitly calls {@link #handleReport(AvailabilityReport)} so any report generating entries
     * *will be sent to the server*.  Callers should subsequently *NOT* send the report.
     *
     * @param changedOnlyReport
     * @return The report, for inspection
     */
    @Override
    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport) {
        return executeAvailabilityScanImmediately(changedOnlyReport, false);
    }

    /**
     * This method implicitly calls {@link #handleReport(AvailabilityReport)} so any report generating entries
     * *will be sent to the server*.  Callers should subsequently *NOT* send the report.
     *
     * @param changedOnlyReport
     * @param forceChecks
     * @return The report, for inspection
     */
    public AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport, boolean forceChecks) {
        try {
            AvailabilityExecutor availExec = (forceChecks) ? new ForceAvailabilityExecutor(this)
                : new AvailabilityExecutor(this);

            if (changedOnlyReport) {
                availExec.sendChangesOnlyReportNextTime();
            } else {
                availExec.sendFullReportNextTime();
            }

            AvailabilityReport availabilityReport = availabilityThreadPoolExecutor.submit(
                (Callable<AvailabilityReport>) availExec).get();

            // make sure the server is notified of any changes in availability
            handleReport(availabilityReport);

            return availabilityReport;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Availability scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    @Override
    @NotNull
    public AvailabilityReport getCurrentAvailability(Resource resource, boolean changesOnly) {
        try {
            //make sure we have the full version of the resource
            ResourceContainer container = getResourceContainer(resource.getId());
            if (container == null) {
                //don't bother doing anything
                return new AvailabilityReport(changesOnly, getAgent().getName());
            }
            resource = container.getResource();

            AvailabilityExecutor availExec = new CustomScanRootAvailabilityExecutor(this, resource, true);
            if (changesOnly) {
                availExec.sendChangesOnlyReportNextTime();
            } else {
                availExec.sendFullReportNextTime();
            }

            return availabilityThreadPoolExecutor.submit((Callable<AvailabilityReport>) availExec).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Availability scan execution was interrupted", e);
        } catch (ExecutionException e) {
            // Should never happen, reports are always generated, even if they're just to report the error
            throw new RuntimeException("Unexpected exception", e);
        }
    }

    /**
     * Same as calling {@link #requestAvailabilityCheck(Resource, boolean)} as:
     * <p/>
     *
     * <code>requestAvailabilityCheck(resource, false);</code>
     */
    public void requestAvailabilityCheck(Resource resource) {
        requestAvailabilityCheck(resource, false);
    }

    /**
     * Request an ASAP avail check for the provided resource. If recursive, do the same for the children.
     *
     * @param resource
     * @param recursive
     */
    public void requestAvailabilityCheck(Resource resource, boolean recursive) {
        if (null == resource) {
            return;
        }

        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (null != resourceContainer) {
            // set the avail schedule time to 1 to ensure the resource will have an avail check performed on
            // the next availability scan.  (note, do not set to 0, that has special semantics that will actually
            // push the avail check out one cycle.
            resourceContainer.setAvailabilityScheduleTime(1L);
        }

        if (recursive && null != resource.getChildResources()) {
            for (Resource child : resource.getChildResources()) {
                requestAvailabilityCheck(child, true);
            }
        }
    }

    public void setResourceEnablement(int resourceId, boolean setEnabled) {
        configuration.getServerServices().getDiscoveryServerService().setResourceEnablement(resourceId, setEnabled);
    }

    @Override
    public MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int ownerSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException {
        // TODO (ghinkle): This is hugely flawed. It assumes discovery components will only return the manually discovered
        // resource, but never says this is required. It then proceeds to auto-import the first resource returned. For
        // discoveries that are process based it works because this passes in a null process scan... also a bad idea.

        // Look up the full Resource type (the one provided by the Server is just the keys).
        String resourceTypeString = resourceType.toString();
        resourceType = this.pluginManager.getMetadataManager().getType(resourceType);
        if (resourceType == null) {
            throw new IllegalStateException("Server specified unknown Resource type: " + resourceTypeString);
        }

        MergeResourceResponse mergeResourceResponse;
        Resource resource = null;
        boolean resourceAlreadyExisted = false;

        try {
            ResourceContainer parentResourceContainer = getResourceContainer(parentResourceId);
            ResourceComponent parentResourceComponent = parentResourceContainer.getResourceComponent();

            // Get the discovery component responsible for discovering resources of the specified resource type.
            PluginComponentFactory pluginComponentFactory = PluginContainer.getInstance().getPluginComponentFactory();
            ResourceDiscoveryComponent discoveryComponent = pluginComponentFactory.getDiscoveryComponent(resourceType,
                parentResourceContainer);

            DiscoveredResourceDetails discoveredResourceDetails;
            if (discoveryComponent instanceof ManualAddFacet) {
                // The plugin is using the new manual add API.
                ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext = new ResourceDiscoveryContext<ResourceComponent<?>>(
                    resourceType, parentResourceComponent, parentResourceContainer.getResourceContext(),
                    SystemInfoFactory.createSystemInfo(), new ArrayList<ProcessScanResult>(0),
                    new ArrayList<Configuration>(0), this.configuration.getContainerName(),
                    this.configuration.getPluginContainerDeployment());

                // Ask the plugin's discovery component to find the new resource, throwing exceptions if it cannot be
                // found at all.
                discoveredResourceDetails = discoverResource(discoveryComponent, pluginConfiguration, discoveryContext,
                    parentResourceContainer);
                if (discoveredResourceDetails == null) {
                    log.info("Plugin Error: During manual add, discovery component method ["
                        + discoveryComponent.getClass().getName() + ".discoverResource()] returned null "
                        + "(either the Resource type was blacklisted or the plugin developer "
                        + "did not implement support for manually discovered Resources correctly).");
                    throw new PluginContainerException("The [" + resourceType.getPlugin()
                        + "] plugin does not properly support manual addition of [" + resourceType.getName()
                        + "] Resources.");
                }
            } else {
                // The plugin is using the old manual add API, which we must continue to support to maintain
                // backward compatibility.
                log.info("Plugin Warning: Resource type '" + resourceType.getName() + "' from '"
                    + resourceType.getPlugin() + "' is still using the deprecated manual Resource add API, "
                    + "rather than the new ManualAddFacet interface.");
                List<Configuration> pluginConfigurations = new ArrayList<Configuration>(1);
                pluginConfigurations.add(pluginConfiguration);
                ResourceDiscoveryContext<ResourceComponent<?>> discoveryContext = new ResourceDiscoveryContext<ResourceComponent<?>>(
                    resourceType, parentResourceComponent, parentResourceContainer.getResourceContext(),
                    SystemInfoFactory.createSystemInfo(), new ArrayList<ProcessScanResult>(0), pluginConfigurations,
                    this.configuration.getContainerName(), this.configuration.getPluginContainerDeployment());

                // Ask the plugin's discovery component to find the new resource, throwing exceptions if it cannot be
                // found at all.
                try {
                    Set<DiscoveredResourceDetails> discoveredResources = invokeDiscoveryComponent(
                        parentResourceContainer, discoveryComponent, discoveryContext);
                    if ((discoveredResources == null) || discoveredResources.isEmpty()) {
                        log.info("Plugin Error: During manual add, discovery component method ["
                            + discoveryComponent.getClass().getName() + ".discoverResources()] returned "
                            + discoveredResources + " when passed a single plugin configuration "
                            + "(either the resource type was blacklisted or the plugin developer "
                            + "did not implement support for manually discovered resources correctly).");
                        throw new PluginContainerException("The [" + resourceType.getPlugin()
                            + "] plugin does not properly support manual addition of [" + resourceType.getName()
                            + "] resources.");
                    }
                    discoveredResourceDetails = discoveredResources.iterator().next();
                } catch (DiscoverySuspendedException e) {
                    String message = "The discovery class ["
                        + discoveryComponent.getClass().getName()
                        + "]"
                        + " uses a legacy implementation of \"manual add\" functionality. Some of the child resources"
                        + " with the resource type ["
                        + resourceType
                        + "] under the parent resource ["
                        + parentResourceContainer.getResource()
                        + "]"
                        + " failed to upgrade, which makes it impossible to support the legacy manual-add implementation. Either upgrade the plugin ["
                        + resourceType.getPlugin()
                        + "] to successfully upgrade all resources or consider implementing the ManualAdd facet.";
                    log.info(message);
                    throw new PluginContainerException(message, e);
                }
            }

            // Create the new Resource and add it to inventory if it isn't already there.
            resource = createNewResource(discoveredResourceDetails);
            Resource parentResource = getResourceContainer(parentResourceId).getResource();
            Resource existingResource = findMatchingChildResource(resource, parentResource);
            if (existingResource != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Manual add for resource type [" + resourceType.getName() + "] and parent resource id ["
                        + parentResourceId
                        + "] found a resource that already exists in inventory - updating existing resource ["
                        + existingResource + "]");
                }
                resourceAlreadyExisted = true;
                resource = existingResource;
                if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
                    resource.setPluginConfiguration(pluginConfiguration);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Adding manually discovered resource [" + resource + "] to inventory...");
                }
                resource.setInventoryStatus(InventoryStatus.COMMITTED);
                parentResource.addChildResourceWithoutAncestry(resource);
                initResourceContainer(resource);
            }

            // Make sure the resource's component is activated (i.e. started).
            boolean newPluginConfig = true;
            ResourceContainer resourceContainer = getResourceContainer(resource);
            if (log.isDebugEnabled()) {
                log.debug("Activating resource [" + resource + "]...");
            }

            // NOTE: We don't mess with inventory status - that's the server's responsibility.

            // Tell the server to merge the resource into its inventory.
            DiscoveryServerService discoveryServerService = this.configuration.getServerServices()
                .getDiscoveryServerService();
            mergeResourceResponse = discoveryServerService.addResource(resource, ownerSubjectId);

            // Sync our local resource up with the one now in server inventory. Treat this like a newlyCommittedResource
            // - set mtime (same as ctime for a new resource) to ensure this does not get picked up in an inventory sync
            //   pass, we know we're currently in sync with the server.
            resource.setId(mergeResourceResponse.getResourceId());
            resource.setMtime(mergeResourceResponse.getMtime());
            Set newResources = new LinkedHashSet<Resource>();
            newResources.add(resource);
            postProcessNewlyCommittedResources(newResources);
            performServiceScan(resource.getId());

            // Note that it is important to activate the resource *AFTER* it has been synced with the
            // server so that the resource has a valid id (which is needed by at least the content
            // subsystem).
            try {
                activateResource(resource, resourceContainer, newPluginConfig);
            } catch (Throwable t) {
                // if it fails to start keep going, we already have the resource in inventory and
                // we are in sync with the server. The new resource will be unavailable but at least
                // it will be accessible and editable by the user. Report the start exception at the end.
                handleInvalidPluginConfigurationResourceError(resource, t);
                throw new PluginContainerException("The resource [" + resource
                    + "] has been added but could not be started. Verify the supplied configuration values: ", t);
            }
        }

        // Catch any other RuntimeExceptions or Errors, so the server doesn't have to worry about deserializing or
        // catching them. Before rethrowing, wrap them in a WrappedRemotingException and then wrap that in either an
        // InvalidPluginConfigurationException or a PluginContainerException.
        catch (Throwable t) {
            if ((resource != null) && !resourceAlreadyExisted && (getResourceContainer(resource) != null)) {
                // If the resource got added to inventory, roll it back (i.e. deactivate it, then remove it from inventory).
                if (log.isDebugEnabled()) {
                    log.debug("Rolling back manual add of resource of type [" + resourceType.getName()
                        + "] - removing resource with id [" + resource.getId() + "] from inventory...");
                }
                deactivateResource(resource);
                uninventoryResource(resource.getId());
            }

            if (t instanceof InvalidPluginConfigurationException) {
                String errorMessage = "Unable to connect to managed resource of type [" + resourceType.getName()
                    + "] using the specified connection properties - resource will not be added to inventory.";
                log.info(errorMessage, t);

                // In the exception we throw over to the server, strip the InvalidPluginConfigurationException out of the
                // stack trace, but append the message from that exception to the message of the exception we throw. This
                // will make for a nicer error message for the server to display in the UI.
                errorMessage += ((t.getLocalizedMessage() != null) ? (" " + t.getLocalizedMessage()) : "");
                throw new InvalidPluginConfigurationClientException(errorMessage,
                    (t.getCause() != null) ? new WrappedRemotingException(t.getCause()) : null);
            } else {
                log.error("Manual add failed for resource of type [" + resourceType.getName()
                    + "] and parent resource id [" + parentResourceId + "]", t);
                throw new PluginContainerException("Failed to add resource with type [" + resourceType.getName()
                    + "] and parent resource id [" + parentResourceId + "]", new WrappedRemotingException(t));
            }
        }

        return mergeResourceResponse;
    }

    static Resource createNewResource(DiscoveredResourceDetails details) {
        // Use a CopyOnWriteArraySet for childResources to allow the field to be concurrently accessed safely
        // (i.e. to avoid ConcurrentModificationExceptions).
        Set<Resource> childResources = new CopyOnWriteArraySet<Resource>();
        Resource resource = new Resource(childResources);

        resource.setUuid(UUID.randomUUID().toString());
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

    private Resource cloneResourceWithoutChildren(Resource resourceFromServer) {
        // Use a CopyOnWriteArraySet for childResources to allow the field to be concurrently accessed safely
        // (i.e. to avoid ConcurrentModificationExceptions).
        Set<Resource> childResources = new CopyOnWriteArraySet<Resource>();
        Resource resource = new Resource(childResources);

        resource.setId(resourceFromServer.getId());
        resource.setUuid(resourceFromServer.getUuid());
        resource.setResourceKey(resourceFromServer.getResourceKey());
        resource.setResourceType(resourceFromServer.getResourceType());
        resource.setMtime(resourceFromServer.getMtime());
        resource.setInventoryStatus(resourceFromServer.getInventoryStatus());
        resource.setPluginConfiguration(resourceFromServer.getPluginConfiguration());
        resource.setVersion(resourceFromServer.getVersion());

        resource.setName(resourceFromServer.getName());
        resource.setDescription(resourceFromServer.getDescription());
        resource.setLocation(resourceFromServer.getLocation());

        compactResource(resource);
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
                log.info("No committed resources to send in our availability report - the platform/agent was deleted, let's re-register again");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }

            return;
        }

        List<AvailabilityReport.Datum> reportAvails = report.getResourceAvailability();

        if (configuration.isInsideAgent() && reportAvails.size() > 0) {
            // Due to the asynchronous nature of the availability collection,
            // it is possible we may have collected availability of a resource that has just recently been deleted;
            // therefore, as a secondary check, let's remove any availabilities for resources that no longer exist.
            // I suppose after we do this check and before we send the report to the server that a resource could
            // then be deleted, but that time period where that could happen is now very small and thus this will
            // be a rare event.  And even if that does happen, nothing catastrophic would happen on the server,
            // the report would be accepted normally, a message would be inserted in the server log indicating an empty
            // report was received, and the rest of the handling would be short-circuited.
            this.inventoryLock.readLock().lock();
            try {
                AvailabilityReport.Datum[] avails = reportAvails.toArray(new AvailabilityReport.Datum[reportAvails
                    .size()]);
                for (AvailabilityReport.Datum avail : avails) {
                    int resourceId = avail.getResourceId();
                    ResourceContainer container = getResourceContainer(resourceId);

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
                    log.info("Sending availability report to Server...");
                    if (log.isDebugEnabled()) {
                        log.debug("Availability report content: " + report.toString(log.isTraceEnabled()));
                    }

                    boolean ok = configuration.getServerServices().getDiscoveryServerService()
                        .mergeAvailabilityReport(report);
                    if (!ok) {
                        // I guess I could immediately call executeAvailabilityScanImmediately and pass its results to
                        // mergeAvailabilityReport again right now, but what happens if we've queued up a bunch of
                        // changed-only reports and the server is out of sync - each time the server processes those
                        // reports, we'd do an extra round trip with a full report (which will get very expensive).
                        // Let's just flag our executor for the next time it runs to send a full report; this way
                        // if we've got 100 queued changed-only reports, let the server fully process them and only
                        // at the next time we run the avail scan will we send it a full report.  It might make the
                        // server sync up a little slower than we'd like, but it avoids a potential hammering of the
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
     * Send an inventory report to the Server.
     *
     * @param  report the inventory report to be sent
     * @return true if sending the report to the Server succeeded, or false otherwise
     */
    public boolean handleReport(InventoryReport report) {
        if (!configuration.isInsideAgent()) {
            return true;
        }

        PlatformSyncInfo platformSyncInfo;
        Collection<ResourceTypeFlyweight> ignoredTypes;
        try {
            String reportType = (report.isRuntimeReport()) ? "runtime" : "server";
            log.info("Sending [" + reportType + "] inventory report to Server...");
            long startTime = System.currentTimeMillis();
            DiscoveryServerService discoveryServerService = configuration.getServerServices()
                .getDiscoveryServerService();
            MergeInventoryReportResults results = discoveryServerService.mergeInventoryReport(report);
            if (results != null) {
                platformSyncInfo = results.getPlatformSyncInfo();
                ignoredTypes = results.getIgnoredResourceTypes();
            } else {
                platformSyncInfo = null;
                ignoredTypes = null;
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Server DONE merging inventory report [%d] ms.",
                    (System.currentTimeMillis() - startTime)));
            }
        } catch (StaleTypeException e) {
            log.error("Failed to merge inventory report with server. The report contains one or more resource types "
                + "that have been marked for deletion. Notifying the plugin container that a reboot is needed to purge "
                + "stale types.");
            PluginContainer.getInstance().notifyRebootRequestListener();
            return false;
        } catch (InvalidInventoryReportException e) {
            log.error("Failure sending inventory report to Server - was this Agent's platform deleted?", e);
            if ((this.platform != null) && (this.platform.getInventoryStatus() == InventoryStatus.NEW)
                && newPlatformWasDeletedRecently) {
                // let's make sure we are registered; its probable that our platform was deleted and we need to re-register
                log.info("The inventory report was invalid probably because the platform/Agent was deleted; let's re-register...");
                registerWithServer();
                newPlatformWasDeletedRecently = false; // we've tried to recover from our platform being deleted, let's not do it again
            }
            return false;
        }

        // tell our metadata manager what resource types are to be ignored
        PluginMetadataManager metadataMgr = this.pluginManager.getMetadataManager();
        if (ignoredTypes != null && !ignoredTypes.isEmpty()) {
            Collection<ResourceType> ignoredSet = new HashSet<ResourceType>(ignoredTypes.size());
            for (ResourceTypeFlyweight ignoredType : ignoredTypes) {
                ignoredSet.add(new ResourceType(ignoredType.getName(), ignoredType.getPlugin(), null, null));
            }
            metadataMgr.setIgnoredResourceTypes(ignoredSet);
        } else {
            metadataMgr.setIgnoredResourceTypes(null);
        }

        //sync info can be null if the server hasn't received a full inventory report
        //from us yet. This can happen if this method is being invoked from inside the
        //resource upgrade executor to sync up with the server side inventory *JUST AFTER*
        //this agent registered with the server for the very first time. In that case
        //the server hasn't received any info from us yet.
        //Another (rare) scenario where this would happen would be when the platform resource type
        //would change.
        //In either case, let's sync up with the server - if it's got nothing, neither should the agent.
        if (platformSyncInfo != null) {
            syncPlatform(platformSyncInfo);
        } else {
            purgeObsoleteResources(Collections.<String> emptySet());

            //can't live without a platform, but we just deleted it. Let's rediscover it.
            discoverPlatform();
        }

        return true;
    }

    /**
     * Performs a full platform sync so that resources passed in are reflected in the agent's inventory.
     *
     * @param platformSyncInfo sync info on the platform and references to the top level servers. not null.
     */
    private void syncPlatform(PlatformSyncInfo platformSyncInfo) {
        final Set<String> allServerSideUuids = new HashSet<String>();
        boolean hadSyncedResources = false;
        ResourceSyncInfo platformResourceSyncInfo = platformSyncInfo.getPlatform();

        // sync the platform because it does not get included in the top level server sync
        allServerSideUuids.add(platformResourceSyncInfo.getUuid());
        // sync the top level service hierarchy
        addAllUuids(platformSyncInfo.getServices(), allServerSideUuids);

        // Add the platform sync info to the service hierarchy in order to process in one batch
        Collection<ResourceSyncInfo> syncInfos = platformSyncInfo.getServices();
        syncInfos.add(platformResourceSyncInfo);

        log.info("Sync Starting: Platform [" + platformSyncInfo.getPlatform().getId() + "]");

        log.info("Sync Starting: Platform Top level services [" + platformSyncInfo.getPlatform().getId() + "]");
        hadSyncedResources = syncResources(platformResourceSyncInfo.getId(), syncInfos);
        log.info("Sync Complete: Platform Top level services [" + platformSyncInfo.getPlatform().getId()
            + "] Local inventory changed: [" + hadSyncedResources + "]");

        syncInfos = null; // release to GC

        // then sync the top level servers by calling back to the server for the sync info for each. We
        // do this one at a time to avoid forcing the whole inventory into active memory at one time during the sync.
        Collection<Integer> topLevelServerIds = platformSyncInfo.getTopLevelServerIds();
        if (null != topLevelServerIds) {
            DiscoveryServerService service = configuration.getServerServices().getDiscoveryServerService();

            for (Integer topLevelServerId : topLevelServerIds) {
                syncInfos = service.getResourceSyncInfo(topLevelServerId);
                if (null != syncInfos) {
                    addAllUuids(syncInfos, allServerSideUuids);
                    log.info("Sync Starting: Top Level Server  [" + topLevelServerId + "]");
                    boolean serverHadSyncedResources = syncResources(topLevelServerId, syncInfos);
                    log.info("Sync Complete: Top Level Server  [" + topLevelServerId + "] Local inventory changed: ["
                        + serverHadSyncedResources + "]");
                    hadSyncedResources = serverHadSyncedResources || hadSyncedResources;

                    syncInfos = null; // release to GC

                    // (jshaughn) Is this really necessary?
                    try {
                        // Wait a little to get other tasks room for breathing
                        Thread.sleep(800L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        purgeObsoleteResources(allServerSideUuids);

        log.info("Sync Complete: Platform [" + platformSyncInfo.getPlatform().getId() + "].");

        // kick off a service scan to scan synced Resources for new child Resources.
        //
        // Do this only if we are finished with resource upgrade because no availability checks
        // or discoveries can happen during upgrade. This is to ensure maximum consistency of the
        // inventory with the server side as well as to disallow any other server-agent traffic during
        // the upgrade phase. Not to mention the fact that no thread pools are initialized yet by the
        // time the upgrade kicks in.
        if (hadSyncedResources && !isResourceUpgradeActive()) {
            log.info("Sync changes detected, requesting full availability report and service discovery: Platform ["
                + platformSyncInfo.getPlatform().getId() + "]");

            // Additionally, request a full avail report.  This ensures each newly synced resource has its
            // availability determined on the next avail run (typically < 30s away).  Note that this is not
            // particularly inefficient.  A full report is different than a full scan.  Although each resource's
            // latest avail will be in the report sent to the server, only new and eligible resources will have their
            // avail checked.
            requestFullAvailabilityReport();

            executeServiceScanDeferred();
        }
    }

    private void addAllUuids(Collection<ResourceSyncInfo> syncInfos, Set<String> allServerSideUuids) {
        for (ResourceSyncInfo syncInfo : syncInfos) {
            allServerSideUuids.add(syncInfo.getUuid());
        }
    }

    /**
     * Performs a synch on only the single resource and its descendants. This is assumed to be a partial
     * inventory.  To synch on the full inventory call {@link #syncPlatform(PlatformSyncInfo)}
     *
     * @param syncInfos the resource's sync data
     * @return true if any resources needed synchronization, false otherwise
     */
    private boolean syncResources(int rootResourceId, Collection<ResourceSyncInfo> syncInfos) {
        boolean result = false;
        final long startTime = System.currentTimeMillis();
        final Set<Resource> syncedResources = new LinkedHashSet<Resource>();
        final Set<ResourceSyncInfo> unknownResourceSyncInfos = new LinkedHashSet<ResourceSyncInfo>();
        final Set<Integer> modifiedResourceIds = new LinkedHashSet<Integer>();
        final Set<Integer> deletedResourceIds = new LinkedHashSet<Integer>();
        final Set<Resource> newlyCommittedResources = new LinkedHashSet<Resource>();
        final Set<Resource> ignoredResources = new LinkedHashSet<Resource>();

        // rhq-980 Adding agent-side logging to report any unexpected synch failure.
        try {
            log.debug("Processing Server sync info...");

            processSyncInfo(syncInfos, syncedResources, unknownResourceSyncInfos, modifiedResourceIds,
                deletedResourceIds, newlyCommittedResources, ignoredResources);

            if (log.isDebugEnabled()) {
                log.debug(String.format("DONE Processing sync info: [%d] ms: synced [%d] resources: "
                    + "[%d] unknown, [%d] modified, [%d] deleted, [%d] newly committed",
                    (System.currentTimeMillis() - startTime), syncedResources.size(), unknownResourceSyncInfos.size(),
                    modifiedResourceIds.size(), deletedResourceIds.size(), newlyCommittedResources.size()));
            }

            Set<Resource> unknownResources = mergeUnknownResources(unknownResourceSyncInfos);
            Set<Resource> modifiedResources = mergeModifiedResources(modifiedResourceIds);
            purgeIgnoredResources(ignoredResources);

            postProcessNewlyCommittedResources(newlyCommittedResources);
            if (log.isDebugEnabled()) {
                if (!deletedResourceIds.isEmpty()) {
                    log.debug("Ignored [" + deletedResourceIds.size() + "] DELETED resources.");
                }
                log.debug(String.format("DONE syncing local inventory [%d] ms.",
                    (System.currentTimeMillis() - startTime)));
            }

            result = !syncedResources.isEmpty() || !unknownResources.isEmpty() || !modifiedResources.isEmpty();

        } catch (Throwable t) {
            log.warn("Failed to synchronize local inventory with Server inventory for Resource [" + rootResourceId
                + "] and its descendants: " + t.getMessage());
            // convert to runtime exception so as not to change the api
            throw new RuntimeException(t);
        }

        return result;
    }

    @Override
    public void synchronizePlatform(PlatformSyncInfo syncInfo) {
        syncPlatform(syncInfo);
        performServiceScan(syncInfo.getPlatform().getId()); // NOTE: This will block (the initial scan blocks).
        // TODO: (jshaughn) should we also request a full avail scan?
    }

    @Override
    public void synchronizeServer(int resourceId, Collection<ResourceSyncInfo> topLevelServerSyncInfo) {
        log.info("Synchronizing local inventory with Server inventory for Resource [" + resourceId
            + "] and its descendants...");
        syncResources(resourceId, topLevelServerSyncInfo);
        performServiceScan(resourceId); // NOTE: This will block (the initial scan blocks).
        // TODO: (jshaughn) should we also request a full avail scan?
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
                inventoryThreadPoolExecutor.submit((Runnable) this.serverScanExecutor);
            } catch (Exception e) {
                log.error("Cannot re-register with the agent, something bad is happening", e);
            }
        }

        return;
    }

    /**
     * Performs a service scan on the specified Resource, waiting until the scan completes to return.
     *
     * @param resourceId the id of the Resource for which to discover child services
     */
    public InventoryReport performServiceScan(int resourceId) {
        ResourceContainer resourceContainer = getResourceContainer(resourceId);
        if (resourceContainer == null) {
            // TODO (ips, 05/16/12): Shouldn't we throw an exception here??
            if (log.isDebugEnabled()) {
                log.debug("No resource container for Resource with id [" + resourceId
                    + "] found - not performing a child service scan.");
            }
            return new InventoryReport(agent);
        }
        Resource resource = resourceContainer.getResource();
        RuntimeDiscoveryExecutor oneTimeExecutor = new RuntimeDiscoveryExecutor(this, configuration, resource);

        if (log.isDebugEnabled()) {
            log.debug("Scheduling child service scan for " + resource + " and waiting for it to complete...");
        }
        try {
            Future<InventoryReport> future = inventoryThreadPoolExecutor
                .submit((Callable<InventoryReport>) oneTimeExecutor);
            return future.get();
        } catch (Exception e) {
            throw new RuntimeException("Error submitting child service scan for " + resource + ".", e);
        }
    }

    @Nullable
    public ResourceComponent<?> getResourceComponent(Resource resource) {
        ResourceContainer resourceContainer = this.resourceContainersByUUID.get(resource.getUuid());

        if (resourceContainer == null) {
            return null;
        }

        return resourceContainer.getResourceComponent();
    }

    @Override
    public void uninventoryResource(int resourceId) {
        ResourceContainer resourceContainer = getResourceContainer(resourceId);
        if (resourceContainer == null) {
            if (log.isDebugEnabled()) {
                log.debug("Could not remove Resource [" + resourceId + "] because its container was null.");
            }
            return;
        }
        boolean scan = removeResourceAndIndicateIfScanIsNeeded(resourceContainer.getResource());

        //only actually schedule the scanning when we are finished with resource upgrade. The resource upgrade
        //happens before any scanning infrastructure is established.
        if (!isResourceUpgradeActive() && scan) {
            log.info("Deleted resource #[" + resourceId + "] - this will trigger a server scan now");
            inventoryThreadPoolExecutor.submit((Runnable) this.serverScanExecutor);
        }
    }

    /**
     * Removes the resource and its children and returns true if a scan is needed.
     *
     * @param resource the Resource to be removed
     * @return true if this method deleted things that requires a scan.
     */
    boolean removeResourceAndIndicateIfScanIsNeeded(Resource resource) {
        boolean scanIsNeeded = false;

        this.inventoryLock.writeLock().lock();
        try {
            if (log.isDebugEnabled()) {
                log.debug("Removing [" + resource + "] from local inventory...");
            }

            // this will deactivate the resource starting bottom-up - so this ends up as a no-op if we are being called
            // recursively, but we need to do this now to ensure everything is stopped prior to removing them from inventory
            deactivateResource(resource);

            // see BZ 801432
            if (log.isDebugEnabled()) {
                if (!resource.getChildResources().getClass().getName().contains("Collections$SetFromMap")) {
                    Exception e = new Exception(
                        "Unexpected child set - if you see this, please notify support or log it in bugzilla"
                            + resource.getChildResources().getClass().getName() + ":" + resource.getId() + ":"
                            + resource.getName());
                    log.debug("[BZ 801432]", e);
                }
            }

            Set<Resource> children = getContainerChildren(resource);
            Set<Resource> tmp = new HashSet<Resource>(children);
            for (Resource child : tmp) {
                scanIsNeeded |= removeResourceAndIndicateIfScanIsNeeded(child);
            }

            Resource parent = resource.getParentResource();
            if (parent != null) {
                parent.removeChildResource(resource);
            }

            measurementManager.unscheduleCollection(Collections.singleton(resource.getId()));

            if (this.resourceContainersByUUID.remove(resource.getUuid()) == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Asked to remove an unknown Resource [" + resource + "] with UUID [" + resource.getUuid()
                        + "]");
                }
            } else {
                this.resourceContainerByResourceId.remove(resource.getId());
            }

            // Notify InventoryEventListeners a Resource has been removed.
            fireResourcesRemoved(Collections.singleton(resource));

            // if we just so happened to have removed our top level platform, we need to re-discover it, can't go living without it
            // once we discover the platform, let's schedule an immediate server scan
            if ((this.platform == null) || (this.platform.getId() == resource.getId())) {
                if (log.isDebugEnabled()) {
                    log.debug("Platform [" + resource.getId() + "] was deleted - running platform scan now...");
                }
                this.platform = null;
                executePlatformScan();
                newPlatformWasDeletedRecently = true;
                scanIsNeeded = true;
            } else {
                boolean isTopLevelServer = (this.platform.equals(resource.getParentResource()))
                    && (resource.getResourceType().getCategory() != ResourceCategory.SERVICE);
                if (isTopLevelServer) {
                    if (log.isDebugEnabled()) {
                        log.debug("Top-level server [" + resource.getId()
                            + "] was deleted - server discovery is needed.");
                    }
                    // if we got here, we just deleted a top level server (whose parent is the platform), let's request a scan
                    scanIsNeeded = true;
                }
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }

        return scanIsNeeded;
    }

    /**
     * Get the parent resource's children, ensuring we use the resource container version of the resource, because
     * the container's resource is guaranteed to be up to date.
     *
     * @param parentResource
     * @return the children. parentResouce children if there is no container. May be empty. Not null.
     */
    public Set<Resource> getContainerChildren(Resource parentResource) {
        return getContainerChildren(parentResource, getResourceContainer(parentResource));
    }

    /**
     * Get the parent resource's children, ensuring we use the resource container version of the resource, because
     * the container's resource is guaranteed to be up to date.
     *
     * @param parentResource
     * @param container
     * @return the children, empty if parentContainer is null or there are no children. not null.
     * @return the children, may be empty, not null.
     */
    public Set<Resource> getContainerChildren(Resource parentResource, ResourceContainer container) {
        if (null == container) {
            return parentResource.getChildResources();
        }

        Resource parentContainerResource = container.getResource();

        // this is just to log whether there was an reason to actually call this method
        if (parentResource != parentContainerResource && log.isDebugEnabled()) {
            Set<Resource> containerChildren = parentContainerResource.getChildResources();
            Set<Resource> localChildren = parentResource.getChildResources();
            if (containerChildren.equals(localChildren)) {
                log.debug("Container resource different from local resource.\n Container: " + parentContainerResource
                    + "\n     Local: " + parentResource);
            } else {
                log.debug("Container resource different from local resource and children differ!\n Container:"
                    + container + containerChildren + "\n      Local:" + parentResource + localChildren);
            }
        }

        return parentContainerResource.getChildResources();
    }

    @Override
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
        ResourceContainer resourceContainer = resourceContainersByUUID.get(resource.getUuid());
        return resourceContainer.updateAvailability(availabilityType);
    }

    public void mergeResourcesFromUpgrade(Set<ResourceUpgradeRequest> upgradeRequests) throws Exception {
        Set<ResourceUpgradeResponse> serverUpdates = null;
        try {
            ServerServices serverServices = this.configuration.getServerServices();
            if (serverServices != null) {
                DiscoveryServerService discoveryServerService = serverServices.getDiscoveryServerService();

                serverUpdates = discoveryServerService.upgradeResources(upgradeRequests);
            }
        } catch (Exception e) {
            log.error("Failed to process resource upgrades on the server.", e);
            throw e;
        }

        if (serverUpdates != null) {
            for (ResourceUpgradeResponse upgradeResponse : serverUpdates) {
                String resourceKey = upgradeResponse.getUpgradedResourceKey();
                String name = upgradeResponse.getUpgradedResourceName();
                String description = upgradeResponse.getUpgradedResourceDescription();
                Configuration pluginConfig = upgradeResponse.getUpgradedResourcePluginConfiguration();

                //only bother if there's something to upgrade at all on this resource.
                if (resourceKey != null || name != null || description != null || pluginConfig != null) {
                    ResourceContainer existingResourceContainer = getResourceContainer(upgradeResponse.getResourceId());
                    if (existingResourceContainer != null) {
                        Resource existingResource = existingResourceContainer.getResource();

                        StringBuilder logMessage = new StringBuilder("Resource [").append(existingResource.toString())
                            .append("] upgraded its ");

                        if (resourceKey != null) {
                            existingResource.setResourceKey(resourceKey);
                            logMessage.append("resourceKey, ");
                        }

                        if (name != null) {
                            existingResource.setName(name);
                            logMessage.append("name, ");
                        }

                        if (description != null) {
                            existingResource.setDescription(description);
                            logMessage.append("description, ");
                        }

                        if (pluginConfig != null) {
                            existingResource.setPluginConfiguration(pluginConfig);
                            logMessage.append("pluginConfiguration, ");
                        }

                        logMessage.replace(logMessage.length() - 1, logMessage.length(), "to become [")
                            .append(existingResource.toString()).append("]");

                        log.info(logMessage.toString());
                    } else {
                        log.error("Upgraded a resource that is not present on the agent. This should not happen. "
                            + "The id of the missing resource is: " + upgradeResponse.getResourceId());
                    }
                }
            }
        }
    }

    public Resource mergeResourceFromDiscovery(Resource resource, Resource parent) throws PluginContainerException {
        // If the Resource is already in inventory, make sure its version is up-to-date, then simply return the
        // existing Resource.
        Resource existingResource = findMatchingChildResource(resource, parent);
        if (existingResource != null) {
            updateResourceVersion(existingResource, resource.getVersion());
            return existingResource;
        }

        // Auto-generate id and auto-commit if embedded within JBossAS.
        if (!this.configuration.isInsideAgent()) {
            resource.setId(this.temporaryKeyIndex.decrementAndGet());
            resource.setInventoryStatus(InventoryStatus.COMMITTED);
        }

        // Add the Resource to the Resource hierarchy.
        // (log services at DEBUG, servers and platforms at INFO)
        String logMessage = String.format("Detected new %s [%s] - adding to local inventory...", resource
            .getResourceType().getCategory(), resource);
        if (parent != null) {
            switch (resource.getResourceType().getCategory()) {
            case SERVICE:
                log.debug(logMessage);
                break;
            case SERVER:
                log.info(logMessage);
                break;
            case PLATFORM:
                throw new IllegalStateException(
                    "An attempt was made to add a platform Resource as a child of another Resource.");
            }
            parent.addChildResourceWithoutAncestry(resource);
        } else {
            if (resource.getResourceType().getCategory() != ResourceCategory.PLATFORM)
                throw new IllegalStateException(
                    "An attempt was made to add a non-platform Resource as the root Resource.");
            log.info(logMessage);
            this.platform = resource;
        }

        // Initialize a container for the Resource.
        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (resourceContainer != null) {
            // This should never happen...
            log.warn("Resource container already existed for Resource that was supposed to be NEW: " + resource);
        } else {
            resourceContainer = initResourceContainer(resource);
        }

        // Auto-activate if embedded within JBossAS (if within Agent, we need to wait until the Resource has been
        // imported into the Server's inventory before activating it).
        if (!this.configuration.isInsideAgent()) {
            try {
                activateResource(resource, resourceContainer, true); // just start 'em up as we find 'em for the embedded side
            } catch (InvalidPluginConfigurationException e) {
                log.error("Failed to activate " + resource + ": " + e.getLocalizedMessage());
                // TODO: I don't think it makes any sense to call the below method w/in the embedded console.
                // (ips, 07/16/08)
                handleInvalidPluginConfigurationResourceError(resource, e);
            }
        }

        // Notify InventoryEventListeners a Resource has been added.
        fireResourcesAdded(Collections.singleton(resource));

        return resource;
    }

    /**
     * During initialization time, the inventory manager will active resources after loading them
     * from disk. Any other manager that starts up and is initialized after the Inventory Manager
     * is initialized will miss the activation notifications. This method is here so those managers
     * can be notified during their initialization phase by simply passing in their listener
     * which will be called for every resource currently activated in inventory.
     *
     * @param listener the listener that will be notified for every resource currently active
     */
    public void notifyForAllActivatedResources(InventoryEventListener listener) {
        List<Resource> activatedResources = new ArrayList<Resource>();
        this.inventoryLock.readLock().lock();
        try {
            for (ResourceContainer container : this.resourceContainersByUUID.values()) {
                if ((container != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)) {
                    activatedResources.add(container.getResource());
                }
            }

            for (Resource resource : activatedResources) {
                try {
                    listener.resourceActivated(resource);
                } catch (Throwable t) {
                    log.warn("Listener [" + listener + "] of activated resource [" + resource + "] failed", t);
                }
            }
        } finally {
            this.inventoryLock.readLock().unlock();
            activatedResources.clear();
        }

        return;
    }

    private ResourceContainer initResourceContainer(Resource resource) {
        ResourceContainer resourceContainer = getResourceContainer(resource);
        if (resourceContainer == null) {
            ClassLoader classLoader = getResourceClassLoader(resource);
            resourceContainer = new ResourceContainer(resource, classLoader);
            if (!this.configuration.isInsideAgent()) {
                // Auto-sync if the PC is running within the embedded JBossAS console.
                resourceContainer.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
            }
            this.resourceContainersByUUID.put(resource.getUuid().intern(), resourceContainer);
            this.resourceContainerByResourceId.put(resource.getId(), resourceContainer);
        } else {
            // container already exists, but make sure the classloader exists too
            if (resourceContainer.getResourceClassLoader() == null) {
                ClassLoader classLoader = getResourceClassLoader(resource);
                resourceContainer.setResourceClassLoader(classLoader);
            }
        }
        return resourceContainer;
    }

    private ClassLoader getResourceClassLoader(Resource resource) {
        ClassLoader classLoader;
        try {
            classLoader = pluginFactory.getResourceClassloader(resource);
        } catch (PluginContainerException e) {
            if (log.isTraceEnabled()) {
                log.trace("Access to resource [" + resource + "] will fail due to missing classloader.", e);
            } else if (log.isDebugEnabled()) {
                log.debug("Access to resource [" + resource + "] will fail due to missing classloader - cause: " + e);
            }
            classLoader = null;
        }
        return classLoader;
    }

    /**
     * This method prepares the resource and container for activation.
     * <p>
     * After this method has processed the resource and container, it is enough
     * to call ResourceComponent.start(). All the datastructures needed for that
     * call are prepared in the container by this method.
     *
     * @param resource the resource that we are activating
     * @param container the container to hold the datastructures
     * @throws InvalidPluginConfigurationException
     * @throws PluginContainerException
     * @return true the resource has been successfully prepared and can be started. False if the resource should not be started.
     */
    private boolean prepareResourceForActivation(Resource resource, @NotNull
    ResourceContainer container, boolean forceReinitialization) throws InvalidPluginConfigurationException,
        PluginContainerException {

        // don't bother doing anything if this resource is of a disabled/ignored type
        if (this.pluginManager.getMetadataManager().isDisabledOrIgnoredResourceType(resource.getResourceType())) {
            return false;
        }

        if (resourceUpgradeDelegate.hasUpgradeFailed(resource)) {
            if (log.isTraceEnabled()) {
                log.trace("Skipping activation of " + resource + " - it has failed to upgrade.");
            }

            return false;
        }

        ResourceComponent component = container.getResourceComponent();
        ResourceComponentState state = container.getResourceComponentState();

        // state is a transient field, so reinitialize it just in case this is invoked just after loadFromDisk()
        if (state == null) {
            container.setResourceComponentState(ResourceComponentState.STOPPED);
            state = ResourceComponentState.STOPPED;
        }

        // if the component exists and is not stopped then we may not have to do anything
        if ((component != null) && (state != ResourceComponentState.STOPPED)) {

            // if STARTED and we are forced to restart (e.g. plugin config change), then stop the component
            // and continue. If STARTING just let it continue to start as interruption could put us in a bad state.
            if (forceReinitialization) {
                switch (state) {
                case STARTED:
                    if (log.isDebugEnabled()) {
                        log.debug("Forcing re-initialization of an already started resource: " + resource);
                    }
                    deactivateResource(resource);
                    break;
                case STARTING:
                    log.warn("Could not force initialization of component for resource [" + resource.getId()
                        + "] as it is already in the process of starting.");

                    return false;
                default:
                    log.error("Unexpected state [" + state.name() + "], returning false...");

                    return false;
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("No need to prepare the activation of resource " + resource
                        + " - its component is already started and its plugin "
                        + "config has not been updated since it was last started.");
                }

                return false;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Preparing component for [" + resource + "] for activation, current state=["
                + container.getResourceComponentState() + "], forcing reinitialization=[" + forceReinitialization
                + "]...");
        }

        ResourceContainer parentResourceContainer;
        Resource parentResource = resource.getParentResource();
        if (parentResource == null) {
            parentResourceContainer = null;
        } else {
            parentResourceContainer = getResourceContainer(parentResource);
            if (parentResourceContainer == null) {
                // The parent probably just got uninventoried - log a DEBUG message and abort.
                if (log.isDebugEnabled()) {
                    log.debug(resource + " not being prepared for activation - container not found for parent "
                        + parentResource + ".");
                }
                return false;
            }
        }

        // If the component does not even exist yet, we need to instantiate it and set it on the container.
        if (component == null) {
            if (log.isDebugEnabled()) {
                log.debug("Creating component for [" + resource + "]...");
            }
            try {
                // should not throw ResourceTypeNotEnabledException because we checked for that above - if it does, just handle it as an error
                component = pluginFactory.buildResourceComponent(resource);
            } catch (Throwable e) {
                throw new PluginContainerException("Could not build component for Resource [" + resource + "]", e);
            }
            container.setResourceComponent(component);
        }

        // Start the resource, but only if its parent component is running. If the parent is null, that means
        // the resource is, itself, the root platform, which we always activate.
        boolean isParentStarted = (parentResourceContainer == null)
            || (parentResourceContainer.getResourceComponentState() == ResourceComponentState.STARTED);

        if (isParentStarted) {
            ResourceType type = resource.getResourceType();

            // wrap the discovery component in a proxy to allow us to timeout discovery invocations
            ResourceDiscoveryComponent discoveryComponent;
            try {
                // should not throw ResourceTypeNotEnabledException because we checked for that above - if it does, just handle it as an error
                discoveryComponent = pluginFactory.getDiscoveryComponent(type, parentResourceContainer);
                discoveryComponent = this.discoveryComponentProxyFactory.getDiscoveryComponentProxy(type,
                    discoveryComponent, getDiscoveryComponentTimeout(type), parentResourceContainer);
            } catch (Exception e) {
                discoveryComponent = null;
                log.warn("Cannot give activated resource its discovery component. Cause: " + e);
            }

            ConfigurationUtility.normalizeConfiguration(resource.getPluginConfiguration(),
                type.getPluginConfigurationDefinition());

            ResourceComponent<?> parentComponent = null;
            ResourceContext<?> parentResourceContext = null;
            if (parentResource != null) {
                ResourceContainer rc = getResourceContainer(parentResource);

                parentComponent = rc.getResourceComponent();
                parentResourceContext = rc.getResourceContext();
            }

            ResourceContext context = createResourceContext(resource, parentComponent, parentResourceContext,
                discoveryComponent);
            container.setResourceContext(context);

            return true;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Resource [" + resource + "] not being prepared for activation; parent isn't started: "
                    + parentResourceContainer);
            }

            return false;
        }
    }

    /**
     * This will start the resource's plugin component, creating it first if it has not yet been created. If the
     * component is already created and started, this method is a no-op.
     *
     * @param  resource        the resource that the component will manage
     * @param  container       the wrapper around the resource and its component
     * @param  updatedPluginConfig if <code>true</code>, this will indicate that the resource's plugin configuration is
     *                         known to have changed since the last time the resource component was started
     *
     * @throws InvalidPluginConfigurationException when connecting to the managed resource fails due to an invalid
     *                                             plugin configuration
     * @throws PluginContainerException            for all other errors
     */
    public void activateResource(Resource resource, @NotNull
    ResourceContainer container, boolean updatedPluginConfig) throws InvalidPluginConfigurationException,
        PluginContainerException {

        if (resourceUpgradeDelegate.hasUpgradeFailed(resource)) {
            if (log.isTraceEnabled()) {
                log.trace("Skipping activation of " + resource + " - it has failed to upgrade.");
            }
            return;
        }

        if (prepareResourceForActivation(resource, container, updatedPluginConfig)) {
            container.setResourceComponentState(ResourceComponentState.STARTING);

            ResourceContext context;
            ResourceComponent component;

            try {
                context = container.getResourceContext();

                // Wrap the component in a proxy that will provide locking and a timeout for the call to start().
                component = container.createResourceComponentProxy(ResourceComponent.class, FacetLockType.READ,
                    COMPONENT_START_TIMEOUT, true, false, true);
            } catch (Throwable t) {
                container.setResourceComponentState(ResourceComponentState.STOPPED);
                throw new PluginContainerException("Failed getting proxy for resource " + resource + ".", t);
            }

            try {
                component.start(context);
                container.setResourceComponentState(ResourceComponentState.STARTED);
                resource.setConnected(true); // This tells the server-side that the resource has connected successfully.

            } catch (Throwable t) {
                // Don't leave in a STARTING state. Don't actually call component.stop(),
                // because we're not actually STARTED
                container.setResourceComponentState(ResourceComponentState.STOPPED);

                StringBuilder messageBuilder = new StringBuilder("Failed to start component for ").append(resource);
                if (isNotBlank(t.getMessage())) {
                    messageBuilder.append(" - ").append(t.getMessage());
                }
                if (t.getCause() != null) {
                    messageBuilder.append(" - Cause: ").append(t.getClass().getName());
                    if (isNotBlank(t.getCause().getMessage())) {
                        messageBuilder.append(": ").append(t.getCause().getMessage());
                    }
                }
                String message = messageBuilder.toString();

                if (updatedPluginConfig || (t instanceof InvalidPluginConfigurationException)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resource has a bad config, waiting for this to go away: " + resource);
                    }
                    InventoryEventListener iel = new ResourceGotActivatedListener();
                    addInventoryEventListener(iel);

                    throw new InvalidPluginConfigurationException(message);
                }

                throw new PluginContainerException(message);
            }

            // We purposefully do not get availability of this resource yet
            // We need availability checked during the normal availability executor timeframe.
            // Otherwise, new resources will not have their availabilities shipped up to the server because
            // they will look like they haven't changed status since the last avail report - but the new
            // resources statuses never got sent up in the last avail report because they didn't exist at that time

            // Finally, inform the rest of the plugin container that this resource has been activated
            fireResourceActivated(resource);
        }
    }

    /**
     * @return The location for [plugins] to write data files
     */
    public File getDataDirectory() {
        return this.configuration.getDataDirectory();
    }

    private <T extends ResourceComponent<?>> ResourceContext<T> createResourceContext(Resource resource,
        T parentComponent, ResourceContext<?> parentResourceContext, ResourceDiscoveryComponent<T> discoveryComponent) {

        return new ResourceContext<T>(resource, // the resource itself
            parentComponent, // its parent component
            parentResourceContext, //the resource context of the parent
            discoveryComponent, // the discovery component (this is actually the proxy to it)
            SystemInfoFactory.createSystemInfo(), // for native access
            this.configuration.getTemporaryDirectory(), // location for plugin to write temp files
            this.configuration.getDataDirectory(), // base location for plugin to write data files
            this.configuration.getContainerName(), // the name of the agent/PC
            getEventContext(resource), // for event access
            getOperationContext(resource), // for operation manager access
            getContentContext(resource), // for content manager access
            getAvailabilityContext(resource), getInventoryContext(resource),
            this.configuration.getPluginContainerDeployment(), // helps components make determinations of what to do
            new ComponentInvocationContextImpl());
    }

    public <T extends ResourceComponent<?>> ResourceUpgradeContext<T> createResourceUpgradeContext(Resource resource,
        ResourceContext<?> parentResourceContext, T parentComponent, ResourceDiscoveryComponent<T> discoveryComponent) {

        return new ResourceUpgradeContext<T>(resource, // the resource itself
            parentResourceContext, //the context of its parent resource
            parentComponent, // its parent component
            discoveryComponent, // the discovery component (this is actually the proxy to it)
            SystemInfoFactory.createSystemInfo(), // for native access
            this.configuration.getTemporaryDirectory(), // location for plugin to write temp files
            this.configuration.getDataDirectory(), // base location for plugin to write data files
            this.configuration.getContainerName(), // the name of the agent/PC
            getEventContext(resource), // for event access
            getOperationContext(resource), // for operation manager access
            getContentContext(resource), // for content manager access
            getAvailabilityContext(resource), // for components that want avail manager access
            getInventoryContext(resource), this.configuration.getPluginContainerDeployment()); // helps components make determinations of what to do
    }

    /**
     * This will send a resource error to the server (if applicable) to indicate that the given resource could not be
     * connected to due to an invalid plugin configuration.
     *
     * @param resource the resource that could not be connected to
     * @param t        the exception that indicates the problem with the plugin configuration
     * @return         true if the error was sent successfully, or false otherwise
     */
    public boolean handleInvalidPluginConfigurationResourceError(Resource resource, Throwable t) {
        resource.setConnected(false); // invalid plugin configuration infers the resource component is disconnected
        // Give the server-side an error message describing the connection failure that can be
        // displayed on the resource's Inventory page.
        ResourceError resourceError = new ResourceError(resource, ResourceErrorType.INVALID_PLUGIN_CONFIGURATION,
            t.getLocalizedMessage(), ThrowableUtil.getStackAsString(t), System.currentTimeMillis());
        return sendResourceErrorToServer(resourceError);
    }

    boolean sendResourceErrorToServer(ResourceError resourceError) {
        boolean errorSent = false;

        DiscoveryServerService serverService = null;
        ServerServices serverServices = this.configuration.getServerServices();
        if (serverServices != null) {
            serverService = serverServices.getDiscoveryServerService();
        }

        if (serverService != null) {
            try {
                // use light-weight proxy to Resource, so that the entire hierarchy doesn't get serialized
                resourceError.setResource(new Resource(resourceError.getResource().getId()));
                serverService.setResourceError(resourceError);
                errorSent = true;
            } catch (RuntimeException e) {
                log.warn("Cannot inform the Server about a Resource error [" + resourceError + "]. Cause: " + e);
            }
        }
        return errorSent;
    }

    private Resource findMatchingChildResource(Resource resource, Resource parent) {
        if (resource != null) {
            if (parent == null) {
                // Resource must be a platform - see if it matches our local platform
                if (this.platform != null && matches(resource, this.platform)) {
                    return this.platform;
                }
            } else {
                // don't use container children here, the caller is providing the desired resources
                for (Resource child : parent.getChildResources()) {
                    if (child != null && matches(resource, child)) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private boolean matches(Resource newResource, Resource existingResource) {
        try {
            return ((existingResource.getId() != 0) && (existingResource.getId() == newResource.getId()))
                || (existingResource.getUuid().equals(newResource.getUuid()))
                || (existingResource.getResourceType().equals(newResource.getResourceType()) && existingResource
                    .getResourceKey().equals(newResource.getResourceKey()));
        } catch (RuntimeException e) {
            log.error("Runtime error while attempting to compare existing Resource " + existingResource
                + " to new Resource " + newResource);
            throw e;
        }
    }

    /**
     * Lookup all the resources with a particular type
     *
     * @param  type the type to match against
     *
     * @return the set of resources matching the provided type
     */
    public Set<Resource> getResourcesWithType(ResourceType type) {
        return getResourcesWithType(type, getContainerChildren(this.platform));
    }

    private Set<Resource> getResourcesWithType(ResourceType type, Set<Resource> resources) {
        Set<Resource> result = new HashSet<Resource>();

        if (resources == null) {
            return result;
        }

        for (Resource resource : resources) {
            // If we're looking for SERVERs and we've hit a SERVICE, skip it as it doesn't match and
            // we won't find SERVERs below a SERVICE
            if (ResourceCategory.SERVER == type.getCategory()
                && ResourceCategory.SERVICE == resource.getResourceType().getCategory()) {
                continue;
            }

            Set<Resource> children = getContainerChildren(resource);
            result.addAll(getResourcesWithType(type, children));

            if (type.equals(resource.getResourceType())) {
                ResourceContainer container = getResourceContainer(resource);
                result.add((container == null) ? resource : container.getResource());
            }
        }

        return result;
    }

    public boolean isDiscoveryScanInProgress() {
        return (this.inventoryThreadPoolExecutor.getActiveCount() >= 1);
    }

    // commenting out dead code, leaving for reference -jshaughn
    //
    //    private void activateFromDisk(Resource resource) throws PluginContainerException {
    //        if (resource.getId() == 0) {
    //            return; // This is for the case of a resource that hadn't been synced to the server (there are probably better places to handle this)
    //        }
    //
    //        resource.setAgent(this.agent);
    //        ResourceContainer container = getResourceContainer(resource.getId());
    //        if (container == null) {
    //            if (log.isDebugEnabled()) {
    //                log.debug("Could not find a container for resource: " + resource);
    //            }
    //            return;
    //        }
    //        if (container.getSynchronizationState() != ResourceContainer.SynchronizationState.SYNCHRONIZED) {
    //            if (log.isDebugEnabled()) {
    //                log.debug("Stopped activating resources at unsynchronized resource [" + resource + "]");
    //            }
    //            return;
    //        }
    //
    //        try {
    //            activateResource(resource, container, false);
    //        } catch (Exception e) {
    //            log.debug("Failed to activate from disk [" + resource + "]");
    //        }
    //
    //        for (Resource child : resource.getChildResources()) {
    //            initResourceContainer(child);
    //            activateFromDisk(child);
    //        }
    //    }

    /**
     * Tries to load an existing inventory from the file data/inventory.dat
     */
    private void loadFromDisk() {
        this.inventoryLock.writeLock().lock();

        File file = null;
        try {
            file = new File(this.configuration.getDataDirectory(), "inventory.dat");
            if (file.exists()) {
                long start = System.currentTimeMillis();
                log.info("Loading inventory from data file [" + file + "]...");

                InventoryFile inventoryFile = new InventoryFile(file, this);
                inventoryFile.loadInventory();

                this.platform = inventoryFile.getPlatform();
                this.resourceContainersByUUID.clear();
                this.resourceContainerByResourceId.clear();
                for (String uuid : inventoryFile.getResourceContainers().keySet()) {
                    ResourceContainer resourceContainer = inventoryFile.getResourceContainers().get(uuid);
                    this.resourceContainersByUUID.put(uuid, resourceContainer);
                    Resource resource = resourceContainer.getResource();
                    this.resourceContainerByResourceId.put(resource.getId(), resourceContainer);
                    compactResource(resource);
                }

                log.info("Inventory with size [" + this.resourceContainersByUUID.size()
                    + "] loaded from data file in [" + (System.currentTimeMillis() - start) + "ms]");
            }
        } catch (Exception e) {
            this.platform = null;
            this.resourceContainersByUUID.clear();
            this.resourceContainerByResourceId.clear();
            if (file != null) {
                file.renameTo(new File(file.getAbsolutePath() + ".invalid")); // move it out of the way if we can, retain it for later analysis
            }
            log.error(
                "Could not load inventory from data file. The agent has lost knowledge of its previous inventory - "
                    + "it will resync its inventory once it can reconnect with a server.", e);
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    /**
     * Shutdown the ResourceComponents from the bottom up.
     * @param resource The resource to deactivate
     */
    public void deactivateResource(Resource resource) {
        this.inventoryLock.writeLock().lock();
        try {
            ResourceContainer container = getResourceContainer(resource);
            if ((container != null) && (container.getResourceComponentState() == ResourceComponentState.STARTED)) {
                // traverse the hierarchy using the container's resource, which should be up to date
                for (Resource child : getContainerChildren(resource, container)) {
                    deactivateResource(child);
                }

                try {
                    ResourceComponent<?> component = container.createResourceComponentProxy(ResourceComponent.class,
                        FacetLockType.WRITE, COMPONENT_STOP_TIMEOUT, true, true, true);
                    component.stop();
                    if (log.isDebugEnabled()) {
                        log.debug("Successfully deactivated resource with id [" + resource.getId() + "].");
                    }
                } catch (Throwable t) {
                    log.warn("Plugin Error: Failed to stop component for [" + resource + "].", t);
                }

                container.setResourceComponentState(ResourceComponentState.STOPPED);
                if (log.isDebugEnabled()) {
                    log.debug("Set component state to STOPPED for resource with id [" + resource.getId() + "].");
                }

                fireResourceDeactivated(resource);
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void persistToDisk() {
        try {
            deactivateResource(this.platform);
            File dataDir = this.configuration.getDataDirectory();
            if (!dataDir.exists()) {
                if (!dataDir.mkdirs()) {
                    throw new RuntimeException("Failed to create data directory [" + dataDir + "].");
                }
            }
            File file = new File(dataDir, "inventory.dat");
            InventoryFile inventoryFile = new InventoryFile(file, this);
            inventoryFile.storeInventory(this.platform, this.resourceContainersByUUID);
        } catch (Exception e) {
            log.error("Could not persist inventory data to disk", e);
        }
    }

    /**
     * Detects the top platform resource and starts its ResourceComponent.
     *
     * TODO GH: Move this to another class (this one is getting too big)
     * @return The discovered platform (which might be a dummy in case of testing)
     */
    private Resource discoverPlatform() {
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();
        PluginMetadataManager metadataManager = pluginManager.getMetadataManager();
        Set<ResourceType> platformTypes = metadataManager.getTypesForCategory(ResourceCategory.PLATFORM);

        // This should only ever have 1 or, at most, 2 Resources
        // (always the Java fallback platform, and the native platform if supported).
        Set<DiscoveredResourceDetails> allDiscoveredPlatforms = new HashSet<DiscoveredResourceDetails>(2);

        if ((platformTypes != null) && (platformTypes.size() > 0)) {

            //check for fake testing type. If the test platform type is being used, it is always going to be
            //the sole platform type available.
            if (platformTypes.size() == 1 && platformTypes.contains(PluginMetadataManager.TEST_PLATFORM_TYPE)) {
                return getTestPlatform();
            }

            // Go through all the platform types that are supported and see if they can detect our platform.
            for (ResourceType platformType : platformTypes) {
                try {
                    ResourceDiscoveryComponent component = pluginFactory.getDiscoveryComponent(platformType, null);
                    ResourceDiscoveryContext context = new ResourceDiscoveryContext(platformType, null, null,
                        systemInfo, Collections.EMPTY_LIST, Collections.EMPTY_LIST, configuration.getContainerName(),
                        this.configuration.getPluginContainerDeployment());
                    Set<DiscoveredResourceDetails> discoveredResources = null;

                    try {
                        discoveredResources = invokeDiscoveryComponent(null, component, context);
                    } catch (DiscoverySuspendedException e) {
                        log.error("Discovery seems to be suspended for platforms due to upgrade error.", e);
                    } catch (Throwable e) {
                        log.warn("Platform plugin discovery failed - skipping", e);
                    }

                    if (discoveredResources != null) {
                        allDiscoveredPlatforms.addAll(discoveredResources);
                    }
                } catch (ResourceTypeNotEnabledException rtne) {
                    if (log.isDebugEnabled()) {
                        log.debug("Skipping platform discovery - its type is disabled: " + platformType);
                    }
                } catch (Throwable e) {
                    log.error("Error in platform discovery", e);
                }
            }
        } else {
            // This is very strange - there are no platform types - we should never be missing the built-in platform plugin.
            log.error("Missing platform plugin(s) - falling back to dummy platform impl; this should only occur in tests!");
            // TODO: Set sysprop (e.g. rhq.test.mode=true) in integration tests,
            //       and throw a runtime exception here if that sysprop is not set.
            return getTestPlatform();
        }

        if (allDiscoveredPlatforms.isEmpty()) {
            throw new IllegalStateException("Neither a native nor a Java platform was discovered - "
                + "this should never happen. Known platform types are " + platformTypes + ".");
        }

        if (allDiscoveredPlatforms.size() > 2) {
            log.warn("Platform discovery reported too many platforms - "
                + "the platform discovery components for platform types " + platformTypes + " "
                + "should be fixed so together they report no more than 2 platforms total. " + "Reported platforms: "
                + allDiscoveredPlatforms + ".");
        }

        DiscoveredResourceDetails javaPlatform = null;
        DiscoveredResourceDetails nativePlatform = null;
        for (DiscoveredResourceDetails discoveredPlatform : allDiscoveredPlatforms) {
            // We know the Java resource type in the descriptor is named "Java".
            if (discoveredPlatform.getResourceType().getName().equalsIgnoreCase("Java")) {
                javaPlatform = discoveredPlatform;
            } else {
                nativePlatform = discoveredPlatform;
            }
        }

        // In most cases, we will have both (since we support most platforms natively),
        // so use the native platform if we have it; if not, fall back to the Java platform.
        DiscoveredResourceDetails platformToUse = (nativePlatform != null) ? nativePlatform : javaPlatform;

        // Build our actual platform resource now that we've discovered it.
        Resource platform = createNewResource(platformToUse);
        platform.setAgent(this.agent);

        return platform;
    }

    /**
     * If for some reason the platform plugin is not available, this method can be called to add a "dummy" platform
     * resource. This is normally only used during tests.
     * @return A dummy platform for testing purposes only.
     */
    private Resource getTestPlatform() {
        ResourceType type = this.pluginManager.getMetadataManager().addTestPlatformType();
        if (this.platform != null && this.platform.getResourceType() == type) {
            return this.platform;
        }
        Set<Resource> childResources = Collections.newSetFromMap(new ConcurrentHashMap<Resource, Boolean>());
        Resource platform = new Resource(childResources);
        platform.setResourceKey("testkey" + configuration.getContainerName());
        platform.setName("testplatform");
        platform.setResourceType(type);
        platform.setUuid(UUID.randomUUID().toString());
        platform.setAgent(this.agent);
        return platform;
    }

    /**
     * This method is called for a resource tree that exists in the server inventory but
     * not in the agent's inventory.
     *
     * @param resource
     */
    private void syncSchedulesRecursively(Resource resource) {
        if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
            if (resource.getResourceType().getCategory() == ResourceCategory.PLATFORM) {
                // Get and schedule the latest measurement schedules rooted at the given id.
                // This should include disabled schedules to make sure that previously enabled schedules are shut off.
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resource.getId(), false);
                installSchedules(scheduleRequests);

                // performing syncing of the children schedules in one fell swoop
                Set<Integer> childrenIds = new HashSet<Integer>();
                for (Resource child : getContainerChildren(resource)) {
                    childrenIds.add(child.getId());
                }
                scheduleRequests = configuration.getServerServices().getMeasurementServerService()
                    .getLatestSchedulesForResourceIds(childrenIds, true);
                installSchedules(scheduleRequests);
            } else {
                Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
                    .getMeasurementServerService().getLatestSchedulesForResourceId(resource.getId(), true);
                installSchedules(scheduleRequests);
            }
        }
    }

    private void syncDriftDefinitionsRecursively(Resource resource) {
        if (resource.getInventoryStatus() != InventoryStatus.COMMITTED) {
            return;
        }

        Deque<Resource> resources = new LinkedList<Resource>();
        resources.push(resource);

        Set<Integer> resourceIds = new HashSet<Integer>();

        while (!resources.isEmpty()) {
            Resource r = resources.pop();
            if (supportsDriftManagement(r)) {
                resourceIds.add(r.getId());
            }

            Set<Resource> children = getContainerChildren(r);
            for (Resource child : children) {
                resources.push(child);
            }
        }

        DriftSyncManager driftSyncMgr = createDriftSyncManager();
        driftSyncMgr.syncWithServer(resourceIds);
    }

    private boolean supportsDriftManagement(Resource r) {
        PluginMetadataManager metaDataMgr = this.pluginManager.getMetadataManager();
        ResourceType type = metaDataMgr.getType(r.getResourceType());
        return type != null && type.getDriftDefinitionTemplates() != null
            && !type.getDriftDefinitionTemplates().isEmpty();
    }

    private void syncSchedules(Set<Resource> resources) {
        if (log.isDebugEnabled()) {
            log.debug("syncSchedules(Set<Resource>) for resources: " + resources);
        }

        if (resources.isEmpty()) {
            return;
        }

        Set<Integer> committedResourceIds = new HashSet<Integer>();
        for (Resource resource : resources) {
            if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
                committedResourceIds.add(resource.getId());
            }
        }

        Set<ResourceMeasurementScheduleRequest> scheduleRequests = configuration.getServerServices()
            .getMeasurementServerService().getLatestSchedulesForResourceIds(committedResourceIds, false);
        installSchedules(scheduleRequests);
    }

    private void syncDriftDefinitions(Set<Resource> resources) {
        if (log.isDebugEnabled()) {
            log.debug("Syncing drift definitions for " + resources);
        }

        if (resources.isEmpty()) {
            return;
        }

        Set<Integer> committedResourceIds = new HashSet<Integer>();
        for (Resource resource : resources) {
            if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
                committedResourceIds.add(resource.getId());
            }
        }

        DriftSyncManager driftSyncMgr = createDriftSyncManager();
        driftSyncMgr.syncWithServer(committedResourceIds);
    }

    private void postProcessNewlyCommittedResources(Set<Resource> resources) {
        if (log.isDebugEnabled()) {
            log.debug("Post-processing newly committed resources: " + resources);
        }

        if (resources.isEmpty()) {
            return;
        }

        Set<Integer> newlyCommittedResourceIds = new HashSet<Integer>();
        for (Resource resource : resources) {
            if (resource.getInventoryStatus() == InventoryStatus.COMMITTED) {
                newlyCommittedResourceIds.add(resource.getId());
            }
        }
        Set<ResourceMeasurementScheduleRequest> schedules = configuration.getServerServices()
            .getDiscoveryServerService().postProcessNewlyCommittedResources(newlyCommittedResourceIds);
        installSchedules(schedules);
    }

    private void installSchedules(Set<ResourceMeasurementScheduleRequest> scheduleRequests) {
        this.measurementManager.scheduleCollection(scheduleRequests);
    }

    private DriftSyncManager createDriftSyncManager() {
        DriftSyncManager mgr = new DriftSyncManager();
        mgr.setDriftServer(configuration.getServerServices().getDriftServerService());
        mgr.setDataDirectory(configuration.getDataDirectory());
        mgr.setDriftManager(PluginContainer.getInstance().getDriftManager());
        mgr.setInventoryManager(this);
        return mgr;
    }

    @Override
    public void requestFullAvailabilityReport() {
        if (null != availabilityExecutor) {
            availabilityExecutor.sendFullReportNextTime();
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
     * @return true if the inventory manager failed to merge the upgrade requests with the server during startup.
     */
    public boolean hasUpgradeMergeFailed() {
        return resourceUpgradeDelegate.hasUpgradeMergeFailed();
    }

    /**
     * The resource upgrade should only occur during the {@link #initialize()} method and should be
     * switched off at all other times.
     *
     * @return true if resource upgrade is currently active, false otherwise
     */
    public boolean isResourceUpgradeActive() {
        return resourceUpgradeDelegate.enabled();
    }

    /**
     * Always use this before accessing the event listeners because this ensures
     * thread safety.
     *
     * @return all inventory event listeners
     */
    private Set<InventoryEventListener> getInventoryEventListeners() {
        return this.inventoryEventListeners;
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

        Set<InventoryEventListener> iteratorSafeListeners = getInventoryEventListeners();
        for (InventoryEventListener listener : iteratorSafeListeners) {
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
            if (log.isDebugEnabled()) {
                log.debug("Not firing activated event for resource: " + resource);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Firing activated for resource: " + resource);
        }

        Set<InventoryEventListener> iteratorSafeListeners = getInventoryEventListeners();
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourceActivated(resource);
            } catch (Throwable t) {
                log.error("Error while invoking resource activated event on listener", t);
            }
        }
        return;
    }

    private void fireResourceDeactivated(Resource resource) {
        if ((resource == null) || (resource.getId() == 0)) {
            if (log.isDebugEnabled()) {
                log.debug("Not firing deactivated event for resource: " + resource);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("Firing deactivated for resource: " + resource);
        }

        Set<InventoryEventListener> iteratorSafeListeners = getInventoryEventListeners();
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourceDeactivated(resource);
            } catch (Throwable t) {
                log.error("Error while invoking resource deactivated event on listener", t);
            }
        }
        return;
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

        Set<InventoryEventListener> iteratorSafeListeners = getInventoryEventListeners();
        for (InventoryEventListener listener : iteratorSafeListeners) {
            // Catch anything to make sure we don't stop firing to other listeners
            try {
                listener.resourcesRemoved(resources);
            } catch (Throwable t) {
                log.error("Error while invoking resources removed event on listener", t);
            }
        }
    }

    @Override
    public void enableServiceScans(int serverResourceId, Configuration config) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    @Override
    public void disableServiceScans(int serverResourceId) {
        throw new UnsupportedOperationException("not implemented yet"); // TODO: Implement this method.
    }

    @NotNull
    Set<Resource> executeComponentDiscovery(ResourceType resourceType, ResourceDiscoveryComponent discoveryComponent,
        ResourceContainer parentContainer, List<ProcessScanResult> processScanResults) {

        ResourceContext parentResourceContext = parentContainer.getResourceContext();
        ResourceComponent parentComponent = parentContainer.getResourceComponent();
        Resource parentResource = parentContainer.getResource();

        long startTime = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Executing discovery for [" + resourceType.getName() + "] Resources...");
        }
        Set<Resource> newResources;
        try {
            ResourceDiscoveryContext context = new ResourceDiscoveryContext(resourceType, parentComponent,
                parentResourceContext, SystemInfoFactory.createSystemInfo(), processScanResults,
                Collections.EMPTY_LIST, this.configuration.getContainerName(),
                this.configuration.getPluginContainerDeployment());
            newResources = new HashSet<Resource>();
            try {
                Set<DiscoveredResourceDetails> discoveredResources = invokeDiscoveryComponent(parentContainer,
                    discoveryComponent, context);
                if ((discoveredResources != null) && (!discoveredResources.isEmpty())) {
                    IdentityHashMap<Configuration, DiscoveredResourceDetails> pluginConfigObjects = new IdentityHashMap<Configuration, DiscoveredResourceDetails>();
                    for (DiscoveredResourceDetails discoveredResource : discoveredResources) {
                        if (discoveredResource == null) {
                            throw new IllegalStateException("Plugin error: Discovery class "
                                + discoveryComponent.getClass().getName()
                                + " returned a Set containing one or more null items.");
                        }
                        if (!discoveredResource.getResourceType().equals(resourceType)) {
                            throw new IllegalStateException("Plugin error: Discovery class "
                                + discoveryComponent.getClass().getName()
                                + " returned a DiscoveredResourceDetails with an incorrect ResourceType (was "
                                + discoveredResource.getResourceType().getName() + " but should have been "
                                + resourceType.getName());
                        }
                        if (null != pluginConfigObjects.put(discoveredResource.getPluginConfiguration(),
                            discoveredResource)) {
                            throw new IllegalStateException(
                                "The plugin component "
                                    + discoveryComponent.getClass().getName()
                                    + " returned multiple resources that point to the same plugin configuration object on the "
                                    + "resource type [" + resourceType + "]. This is not allowed, please use "
                                    + "ResourceDiscoveryContext.getDefaultPluginConfiguration() "
                                    + "for each discovered resource.");
                        }
                        Resource newResource = InventoryManager.createNewResource(discoveredResource);
                        newResources.add(newResource);
                    }
                }
            } catch (DiscoverySuspendedException e) {
                //ok, the discovery is suspended for this resource type under this parent.
                //but we can continue the discovery in the child resource types of the existing resources.
                //we can therefore pretend that the discovery returned the existing resources so that
                //we can recurse into their children up in the call-chain.
                for (Resource existingResource : getContainerChildren(parentResource)) {
                    if (resourceType.equals(existingResource.getResourceType())) {
                        newResources.add(existingResource);
                    }
                }
            }
        } catch (Throwable e) {
            // TODO GH: Add server/parent - up/down semantics so this won't happen just because a server is not up
            long elapsedTime = System.currentTimeMillis() - startTime;
            if (!PluginContainer.getInstance().isRunning()) {
                log.warn("Could not complete discovery, plugin container was shut down.");
            } else {
                log.warn("Failure during discovery for [" + resourceType.getName() + "] Resources - failed after "
                    + elapsedTime + " ms.", e);
            }
            return Collections.EMPTY_SET;
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime > 20000)
            log.info("Discovery for [" + resourceType.getName() + "] resources took [" + elapsedTime + "] ms");
        else
            log.debug("Discovery for [" + resourceType.getName() + "] resources completed in [" + elapsedTime + "] ms");
        return newResources;
    }

    @Nullable
    private EventContext getEventContext(Resource resource) {
        EventContext eventContext;
        if (resource.getResourceType().getEventDefinitions() != null
            && !resource.getResourceType().getEventDefinitions().isEmpty()) {
            eventContext = new EventContextImpl(resource, eventManager);
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
            log.info("Resource ID is 0! Operation features will not work until the resource is synced with server");
        }

        OperationManager operationManager = PluginContainer.getInstance().getOperationManager();
        OperationContext operationContext = new OperationContextImpl(resource.getId(), operationManager);
        return operationContext;
    }

    private ContentContext getContentContext(Resource resource) {
        ResourceType resourceType = resource.getResourceType();
        boolean hasPackageTypes = (resourceType.getPackageTypes() != null && !resourceType.getPackageTypes().isEmpty());
        boolean hasContentBasedCreatableChildren = false;
        if (resourceType.getChildResourceTypes() != null) {
            for (ResourceType childResourceType : resourceType.getChildResourceTypes()) {
                if (childResourceType.isCreatable()
                    && childResourceType.getCreationDataType() == ResourceCreationDataType.CONTENT) {
                    hasContentBasedCreatableChildren = true;
                    break;
                }
            }
        }
        // Only give the ResourceComponent a ContentContext if its metadata says it actually needs one.
        if (!hasPackageTypes && !hasContentBasedCreatableChildren) {
            return null;
        }
        if (resource.getId() == 0) {
            log.info("Resource ID is 0! Content features will not work until the resource is synced with server");
        }
        ContentManager contentManager = PluginContainer.getInstance().getContentManager();
        ContentContext contentContext = new ContentContextImpl(resource.getId(), contentManager);
        return contentContext;
    }

    //    private ResourceComponent<?> createTestPlatformComponent() {
    //        return new ResourceComponent() {
    //            public AvailabilityType getAvailability() {
    //                return AvailabilityType.UP;
    //            }
    //
    //            public void start(ResourceContext context) {
    //            }
    //
    //            public void stop() {
    //            }
    //        };
    //    }

    private AvailabilityContext getAvailabilityContext(Resource resource) {
        if (null == resource.getUuid() || resource.getUuid().isEmpty()) {
            log.error("RESOURCE UUID IS NOT SET! Availability features may not work!");
        }

        AvailabilityContext availabilityContext = new AvailabilityContextImpl(resource, this);
        return availabilityContext;
    }

    /**
     * Create inventory context for a resource.
     *
     * @param resource the resource
     * @return the inventory context
     */
    private InventoryContext getInventoryContext(Resource resource) {
        if (null == resource.getUuid() || resource.getUuid().isEmpty()) {
            log.error("RESOURCE UUID IS NOT SET! Inventory features may not work!");
        }

        InventoryContext inventoryContext = new InventoryContextImpl(resource);
        return inventoryContext;
    }

    private void updateResourceVersion(Resource resource, String version) {
        String existingVersion = resource.getVersion();
        boolean versionChanged = (existingVersion != null) ? !existingVersion.equals(version) : version != null
            && !version.isEmpty();
        if (versionChanged) {
            if (log.isDebugEnabled()) {
                log.debug("Discovery reported that version of [" + resource + "] changed from [" + existingVersion
                    + "] to [" + version + "]");
            }
            boolean versionShouldBeUpdated = resource.getInventoryStatus() != InventoryStatus.COMMITTED
                || updateResourceVersionOnServer(resource, version);
            if (versionShouldBeUpdated) {
                resource.setVersion(version);
                log.info("Version of [" + resource + "] changed from [" + existingVersion + "] to [" + version + "]");
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
                    log.debug("New version for [" + resource + "] (" + newVersion
                        + ") was successfully synced to the Server.");
                }
            } catch (Exception e) {
                log.error("Failed to sync-to-Server new version for [" + resource + "]");
            }
            // TODO: It would be cool to publish a Resource-version-changed Event here. (ips, 02/29/08)
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Sync-to-Server of new version for [" + resource
                    + "] cannot be done, because Plugin Container is not connected to Server.");
            }
        }
        return versionUpdated;
    }

    private void processSyncInfo(Collection<ResourceSyncInfo> syncInfos, Set<Resource> syncedResources,
        Set<ResourceSyncInfo> unknownResourceSyncInfos, Set<Integer> modifiedResourceIds,
        Set<Integer> deletedResourceIds, Set<Resource> newlyCommittedResources, Set<Resource> ignoredResources) {

        for (ResourceSyncInfo syncInfo : syncInfos) {
            if (InventoryStatus.DELETED == syncInfo.getInventoryStatus()) {
                // A previously deleted resource still being reported by the server. Support for this option can
                // be removed if the server is ever modified to not report deleted resources. It is happening currently
                // because deleted resources are kept to support resource history. The deleted resources are rightfully not
                // in the PC inventory, and so must be handled separately, and not as unknown resources.
                deletedResourceIds.add(syncInfo.getId());
            } else {
                ResourceContainer container = getResourceContainer(syncInfo.getUuid());
                if (container == null) {
                    // Either a manually added Resource or just something we haven't discovered.
                    // If this unknown resource is to be ignored, then don't bother to do anything.
                    if (InventoryStatus.IGNORED != syncInfo.getInventoryStatus()) {
                        unknownResourceSyncInfos.add(syncInfo);
                    } else {
                        log.info("Got an unknown but ignored resource - ignoring it: " + syncInfo.getId());
                    }
                } else {
                    Resource resource = container.getResource();
                    // Ensure the Resource classloader is initialized on the Resource container.
                    initResourceContainer(resource);

                    if (log.isDebugEnabled()) {
                        log.debug("Local Resource: id=" + resource.getId() + ", status="
                            + resource.getInventoryStatus() + ", mtime=" + resource.getMtime());
                        log.debug("Sync Resource: " + syncInfo.getId() + ", status=" + syncInfo.getInventoryStatus()
                            + ", mtime=" + syncInfo.getMtime());
                    }

                    final boolean ignoreResource = (InventoryStatus.IGNORED == syncInfo.getInventoryStatus());
                    final boolean ignoreResourceType = this.pluginManager.getMetadataManager()
                        .isDisabledOrIgnoredResourceType(resource.getResourceType());
                    if (ignoreResource || ignoreResourceType) {
                        // a resource or its type has been tagged to be ignored - we need to remove it from our inventory
                        ignoredResources.add(resource);
                    } else {
                        if (resource.getInventoryStatus() != InventoryStatus.COMMITTED
                            && syncInfo.getInventoryStatus() == InventoryStatus.COMMITTED) {
                            newlyCommittedResources.add(resource);
                        }

                        if (resource.getId() == 0) {
                            // This must be a Resource we just reported to the server. Just update its id, mtime, and status.
                            resource.setId(syncInfo.getId());
                            resource.setMtime(syncInfo.getMtime());
                            resource.setInventoryStatus(syncInfo.getInventoryStatus());
                            refreshResourceComponentState(container, true);
                            syncedResources.add(resource);
                        } else {
                            // It's a resource that was already synced at least once.
                            if (resource.getId() != syncInfo.getId()) {
                                // This really should never happen, but check for it just to be bulletproof.
                                log.error("PC Resource id (" + resource.getId()
                                    + ") does not match Server Resource id (" + syncInfo.getId()
                                    + ") for Resource with uuid " + resource.getUuid() + ": " + resource);
                                modifiedResourceIds.add(syncInfo.getId());
                            }
                            // See if it's been modified on the Server since the last time we synced.
                            else if (resource.getMtime() < syncInfo.getMtime()) {
                                modifiedResourceIds.add(resource.getId());
                            } else {
                                // Only try to start up the component if the Resource has *not* been modified on the Server.
                                // Otherwise, hold off until we've synced the Resource with the Server.
                                refreshResourceComponentState(container, false);
                            }
                        }
                    }
                }
            }
        }
    }

    private void compactResource(final Resource resource) {
        resource.setAncestry(null);
        resource.setAlertDefinitions(null);
        resource.setLocation(null);
        resource.setModifiedBy(null);
        resource.setOperationHistories(Collections.EMPTY_LIST);
        resource.setTags(Collections.EMPTY_SET);
        resource.setInstalledPackages(Collections.EMPTY_SET);
        resource.setInstalledPackageHistory(Collections.EMPTY_LIST);
        resource.setDescription(null);
        resource.setImplicitGroups(Collections.EMPTY_SET);
        resource.setExplicitGroups(Collections.EMPTY_SET);
        resource.setCreateChildResourceRequests(Collections.EMPTY_LIST);
        resource.setDeleteResourceRequests(Collections.EMPTY_LIST);
        resource.setAutoGroupBackingGroups(Collections.EMPTY_LIST);
        if (resource.getSchedules() != null) { // TODO used at all in the agent?
            if (resource.getSchedules().size() == 0) {
                resource.setSchedules(Collections.EMPTY_SET);
            }
        }

        if (resource.getVersion() != null) {
            resource.setVersion(resource.getVersion().intern());
        }

        if (resource.getName() != null) {
            resource.setName(resource.getName().intern());
        }

        Configuration pluginConfiguration = resource.getPluginConfiguration();
        if (pluginConfiguration != null) {
            pluginConfiguration.cleanoutRawConfiguration();
            compactConfiguration(pluginConfiguration);
        }

        Configuration resourceConfiguration = resource.getResourceConfiguration();
        if (resourceConfiguration != null) {
            resourceConfiguration.cleanoutRawConfiguration();

            boolean persisted = ConfigurationCheckExecutor.persistConfigurationToFile(this, resource.getId(),
                resourceConfiguration, log);
            if (persisted) {
                resource.setResourceConfiguration(null);
            }
        }
    }

    public static Configuration getResourceConfiguration(Resource agentSideResource) {
        return ConfigurationCheckExecutor.getResourceConfiguration(PluginContainer.getInstance().getInventoryManager(),
            agentSideResource);
    }

    private void compactConfiguration(Configuration config) {
        if (config == null) {
            return;
        }
        if (config.getProperties() == null) {
            return;
        }
        for (Property prop : config.getProperties()) {
            if (prop.getName() != null) {
                prop.setName(prop.getName().intern());
            }
        }
        config.resize();
    }

    private Set<Resource> mergeModifiedResources(Set<Integer> modifiedResourceIds) {
        if (null == modifiedResourceIds || modifiedResourceIds.isEmpty()) {
            return Collections.<Resource> emptySet();
        }

        if (log.isDebugEnabled()) {
            log.debug("Merging [" + modifiedResourceIds.size() + "] modified Resources into local inventory...");
        }

        Set<Resource> modifiedResources = configuration.getServerServices().getDiscoveryServerService()
            .getResources(modifiedResourceIds, false);

        syncSchedules(modifiedResources); // RHQ-792, mtime is the indicator that schedules should be sync'ed too
        syncDriftDefinitions(modifiedResources);
        for (Resource modifiedResource : modifiedResources) {
            compactResource(modifiedResource);
            mergeResource(modifiedResource);
        }

        return modifiedResources;
    }

    private Set<Resource> mergeUnknownResources(Set<ResourceSyncInfo> unknownResourceSyncInfos) {
        if (null == unknownResourceSyncInfos || unknownResourceSyncInfos.isEmpty()) {
            return Collections.<Resource> emptySet();
        }

        if (log.isDebugEnabled()) {
            log.debug("Retrieving unknown Resources for [" + unknownResourceSyncInfos.size()
                + "] unknownResourceSyncInfos...");
        }

        boolean result = false;
        PluginMetadataManager pmm = this.pluginManager.getMetadataManager();
        Set<Resource> unknownResources = getResourcesFromSyncInfos(unknownResourceSyncInfos);

        if (log.isDebugEnabled()) {
            log.debug("Merging [" + unknownResources.size() + "] unknown Resources (and descendents) into inventory...");
        }

        for (Resource unknownResource : unknownResources) {
            ResourceType resourceType = pmm.getType(unknownResource.getResourceType());
            if (resourceType != null) {
                log.info("Got unknown resource: " + unknownResource.getId());
                result = true;
                compactResource(unknownResource);
                mergeResource(unknownResource);
                syncSchedulesRecursively(unknownResource);
                syncDriftDefinitionsRecursively(unknownResource);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("During an inventory sync, the server gave us Resource [" + unknownResource
                        + "] but its type is disabled in the agent; skipping it...");
                }
                // report this so that the user knows to uninventory the dead resource
                String msg = "This resource should be uninventoried.  The agent has disabled this resource's type.  The resource is no longer being managed.";
                ResourceError resourceError = new ResourceError(unknownResource, ResourceErrorType.DISABLED_TYPE, msg,
                    msg, System.currentTimeMillis());
                sendResourceErrorToServer(resourceError);
            }
        }

        return unknownResources;
    }

    private Set<Resource> getResourcesFromSyncInfos(Set<ResourceSyncInfo> syncInfos) {
        /////
        // First we need to get a list of the unknown resource ids
        List<Integer> resourceIdList = new ArrayList<Integer>(syncInfos.size());
        for (ResourceSyncInfo syncInfo : syncInfos) {
            resourceIdList.add(syncInfo.getId());
        }

        /////
        // Now we need to loop over batches of the resource ID list - asking the server for their resource representations.
        // When we get the resources from the server, we put them in our resourceMap, keyed on ID.

        final boolean isDebugEnabled = log.isDebugEnabled();
        final StopWatch stopWatch = new StopWatch();
        String marker = null;
        Map<Integer, Resource> resourceMap = new HashMap<Integer, Resource>(resourceIdList.size());
        int batchNumber = 0;
        while (!resourceIdList.isEmpty()) {
            // Our current batch starts at the head of the list, but
            // we need to determine how big our current batch should be and where in the list of IDs that batch ends
            int size = resourceIdList.size();
            int end = (SYNC_BATCH_SIZE < size) ? SYNC_BATCH_SIZE : size;
            batchNumber++;

            // Determine the content of our current batch - this is simply a sublist of our IDs list.
            // Note that we use .clear() once we get the batch array in order to advance our progress and help GC.
            // This usage of .clear() will remove the processed resources from the backing list.
            String markerPrefix = null;
            if (isDebugEnabled) {
                markerPrefix = String.format("a. Batch [%03d] (%d): ", batchNumber, syncInfos.size());
                marker = String.format("%sGet resource ID sublist - %d of %d remaining", markerPrefix, end, size);
                stopWatch.markTimeBegin(marker);
            }

            List<Integer> resourceIdBatch = resourceIdList.subList(0, end);
            Integer[] resourceIdArray = resourceIdBatch.toArray(new Integer[resourceIdBatch.size()]);

            resourceIdBatch.clear();

            if (isDebugEnabled) {
                stopWatch.markTimeEnd(marker);

                marker = markerPrefix + "Get sublist of resources from server";
                stopWatch.markTimeBegin(marker);
            }

            // Ask the server for the resource representation of all resource IDs in our batch.
            // This is a potentially expensive operation depending on the size of the batch and the content of the resources.
            List<Resource> resourceBatch = configuration.getServerServices().getDiscoveryServerService()
                .getResourcesAsList(resourceIdArray);

            if (isDebugEnabled) {
                stopWatch.markTimeEnd(marker);

                marker = markerPrefix + "Store sublist of resources to map";
                stopWatch.markTimeBegin(marker);
            }

            // Now that the server told us the resources in our batch, we add them to our master map.
            // Note our usage of clear on the batch - this is to help GC.
            for (Resource r : resourceBatch) {
                //  protect against childResources notNull assumptions downstream
                if (null == r.getChildResources()) {
                    r.setChildResources(Collections.EMPTY_SET); // this will actually initialize to an empty Set
                }
                compactResource(r);
                resourceMap.put(r.getId(), r);
            }
            resourceBatch.clear();

            if (isDebugEnabled) {
                stopWatch.markTimeEnd(marker);
            }
        }

        if (syncInfos.size() != resourceMap.size()) {
            log.warn("Expected [" + syncInfos.size() + "] but found [" + resourceMap.size()
                + "] resources when fetching from server");
        }

        /////
        // We now have all the resources associated with all sync infos in a map.
        // We need to build the full resource tree using the resource parent info as the blueprint for how to
        // link the resources in the tree.
        if (isDebugEnabled) {
            marker = "b. Build the resource hierarchies";
            stopWatch.markTimeBegin(marker);
        }

        // The root resources to be merged (i.e. the resources whose parents are not in the map)
        Set<Resource> result = new HashSet<Resource>();
        for (Resource resource : resourceMap.values()) {
            if (null == resource.getParentResource()) {
                result.add(resource); // the platform, make sure we have this
                continue;
            }
            Resource parent = resourceMap.get(resource.getParentResource().getId());
            if (null != parent) {
                parent.addChildResource(resource);
            } else {
                result.add(resource);
            }
        }

        if (isDebugEnabled) {
            stopWatch.markTimeEnd(marker);

            log.debug("Resource trees built from map - performance: " + stopWatch);
        }

        return result;
    }

    //    private void print(Resource resourceTreeNode, int level) {
    //        StringBuilder builder = new StringBuilder();
    //        for (int i = 0; i < level; i++) {
    //            builder.append("   ");
    //        }
    //        log.info(builder.toString() + resourceTreeNode.getId() + " " + resourceTreeNode.getUuid());
    //        for (Resource child : resourceTreeNode.getChildResources()) {
    //            print(child, level + 1);
    //        }
    //    }

    private void mergeResource(Resource resourceFromServer) {
        if (log.isDebugEnabled()) {
            log.debug("Merging " + resourceFromServer + " into local inventory...");
        }

        // Replace the stripped-down ResourceType that came from the Server with the full ResourceType - it's critical
        // to do this before merging the Resource, because the plugin container and plugins rely on the type being fully
        // initialized.
        if (!hydrateResourceType(resourceFromServer)) {
            return;
        }

        // Find the Resource's parent in our inventory.
        Resource parentResource;
        Resource parentResourceFromServer = resourceFromServer.getParentResource();
        if (parentResourceFromServer != null) {
            ResourceContainer parentResourceContainer = getResourceContainer(parentResourceFromServer);
            if (parentResourceContainer == null) {
                // must get the resContainer via Id here, the uuid is not necessarily set in the parent
                parentResourceContainer = getResourceContainer(parentResourceFromServer.getId());
            }
            if (parentResourceContainer != null) {
                parentResource = parentResourceContainer.getResource();
            } else {
                log.debug("Skipping merge of " + resourceFromServer
                    + " into local inventory, since a container was not found for its parent "
                    + parentResourceFromServer + ".");
                return;
            }
        } else {
            // A null parent means this is the platform.
            parentResource = null;
        }

        // See if the Resource already exists in our inventory.
        Resource existingResource = findMatchingChildResource(resourceFromServer, parentResource);
        if ((existingResource == null)
            && (resourceFromServer.getResourceType().getCategory() == ResourceCategory.PLATFORM)) {
            // This should never happen, but add a check so we'll know if it ever does.
            log.error("Existing platform [" + this.platform + "] has different Resource type and/or Resource key than "
                + "platform in Server inventory: " + resourceFromServer);
        }
        boolean pluginConfigUpdated;

        Resource mergedResource;
        ResourceContainer resourceContainer;
        this.inventoryLock.writeLock().lock();
        try {
            if (existingResource != null) { // modified Resource
                log.debug("Modifying " + existingResource + " in local inventory - Resource from Server is "
                    + resourceFromServer + ".");

                // First grab the existing Resource's container, so we can reuse it.
                resourceContainer = this.resourceContainersByUUID.remove(existingResource.getUuid());
                if (resourceContainer != null) {
                    this.resourceContainerByResourceId.remove(existingResource.getId());
                    this.resourceContainersByUUID.put(resourceFromServer.getUuid().intern(), resourceContainer);
                    this.resourceContainerByResourceId.put(resourceFromServer.getId(), resourceContainer);
                } else {
                    log.error("No ResourceContainer found for existing " + existingResource + ".");
                    return;
                }
                if (parentResource != null) {
                    // It's critical to remove the existing Resource from the parent's child Set if the UUID has
                    // changed (i.e. altering the hashCode of an item in a Set == BAD), so just always remove it.
                    parentResource.removeChildResource(existingResource);
                }
                // Now merge the Resource from the Server into the existing Resource...
                pluginConfigUpdated = mergeResource(resourceFromServer, existingResource);
                mergedResource = existingResource;
            } else { // unknown Resource
                log.debug("Adding unknown " + resourceFromServer + " to local inventory.");
                pluginConfigUpdated = false;
                mergedResource = cloneResourceWithoutChildren(resourceFromServer);
            }

            if (parentResource != null) {
                parentResource.addChildResourceWithoutAncestry(mergedResource);
            } else {
                this.platform = mergedResource;
            }

            resourceContainer = initResourceContainer(mergedResource);
        } finally {
            this.inventoryLock.writeLock().unlock();
        }

        refreshResourceComponentState(resourceContainer, pluginConfigUpdated);

        // Recursively merge the children. Note - don't recurse using containers, we're merging the
        // the provided hierarchy not traversing the existing hierarchy
        for (Resource childResource : resourceFromServer.getChildResources()) {
            mergeResource(childResource);
        }
    }

    private boolean hydrateResourceType(Resource resourceFromServer) {
        ResourceType fullResourceType = this.pluginManager.getMetadataManager().getType(
            resourceFromServer.getResourceType());
        if (fullResourceType == null) {
            log.error(resourceFromServer + " being synced from Server has an unknown type ["
                + resourceFromServer.getResourceType() + "] - the [" + resourceFromServer.getResourceType().getPlugin()
                + "] plugin is most likely not up to date in the Agent - try updating the Agent's plugins.");
            return false;
        }
        resourceFromServer.setResourceType(fullResourceType);
        return true;
    }

    private boolean mergeResource(Resource sourceResource, Resource targetResource) {
        if (targetResource.getId() != 0 && targetResource.getId() != sourceResource.getId()) {
            log.warn("Id for " + targetResource + " changed from [" + targetResource.getId() + "] to ["
                + sourceResource.getId() + "].");
        }
        targetResource.setId(sourceResource.getId());
        targetResource.setUuid(sourceResource.getUuid());
        if (!targetResource.getResourceKey().equals(sourceResource.getResourceKey())) {
            log.warn("Resource key for " + targetResource + " changed from [" + targetResource.getResourceKey()
                + "] to [" + sourceResource.getResourceKey() + "].");
        }
        targetResource.setResourceKey(sourceResource.getResourceKey());
        targetResource.setResourceType(sourceResource.getResourceType());
        targetResource.setMtime(sourceResource.getMtime());
        targetResource.setInventoryStatus(sourceResource.getInventoryStatus());
        boolean pluginConfigUpdated = (!targetResource.getPluginConfiguration().equals(
            sourceResource.getPluginConfiguration()));
        targetResource.setPluginConfiguration(sourceResource.getPluginConfiguration());

        targetResource.setName(sourceResource.getName());
        compactResource(targetResource);

        return pluginConfigUpdated;
    }

    private void purgeIgnoredResources(Set<Resource> ignoredResources) {
        if (ignoredResources == null || ignoredResources.isEmpty()) {
            return;
        }

        log.debug("Purging [" + ignoredResources.size() + "] ignored resources...");
        this.inventoryLock.writeLock().lock();
        try {
            for (Resource ignoredResource : ignoredResources) {
                uninventoryResource(ignoredResource.getId());
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void purgeObsoleteResources(Set<String> allUuids) {
        // Remove previously synchronized Resources that no longer exist in the Server's inventory...
        log.debug("Purging obsolete Resources...");
        if (this.resourceContainersByUUID == null) {
            log.debug("No containers present, immediately returning ..");
            return;
        }
        this.inventoryLock.writeLock().lock();
        try {
            int removedResources = 0;
            /*
             * use a separate map for iterating, so that later calls to resourceContainers.remove(ResourceContainer)
             * can be called later without throwing ConcurrentModificationException
             */
            Map<String, ResourceContainer> mapForIterating = new HashMap<String, ResourceContainer>(
                this.resourceContainersByUUID);
            for (String uuid : mapForIterating.keySet()) {
                if (!allUuids.contains(uuid)) {
                    ResourceContainer resourceContainer = this.resourceContainersByUUID.get(uuid);
                    if (resourceContainer != null) {
                        Resource resource = resourceContainer.getResource();
                        // Only purge stuff that was synchronized at some point. Other stuff may just be newly discovered.
                        if (resource.getId() != 0) {
                            uninventoryResource(resource.getId());
                            removedResources++;
                        }
                    } else {
                        log.debug("No obsolete resource to purge - no container for uuid: " + uuid);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Purged [" + removedResources + "] obsolete Resources.");
            }
        } finally {
            this.inventoryLock.writeLock().unlock();
        }
    }

    private void refreshResourceComponentState(ResourceContainer container, boolean pluginConfigUpdated) {
        if (isResourceUpgradeActive()) {
            //don't do anything during upgrade. The resources are only started during the upgrade process.
            return;
        }

        Resource resource = container.getResource();
        switch (resource.getInventoryStatus()) {
        case COMMITTED: {
            try {
                if (pluginConfigUpdated) {
                    deactivateResource(resource);
                }
                activateResource(resource, container, pluginConfigUpdated);
            } catch (InvalidPluginConfigurationException ipce) {
                handleInvalidPluginConfigurationResourceError(resource, ipce);
                log.warn("Cannot start component for " + resource
                    + " from synchronized merge due to invalid plugin config: " + ipce.getLocalizedMessage());
            } catch (Exception e) {
                log.error("Failed to start component for " + resource + " from synchronized merge.", e);
            }
            break;
        }
        default:
            // nothing to do for other states
            break;
        }

        container.setSynchronizationState(ResourceContainer.SynchronizationState.SYNCHRONIZED);
    }

    private void activateAndUpgradeResources() {
        try {

            log.info("Starting resource activation and upgrade.");

            long start = System.currentTimeMillis();

            log.info("Executing the initial inventory synchronization before upgrade.");

            boolean syncResult = handleReport(new InventoryReport(getAgent()));
            if (!syncResult) {
                log.warn("Failed to sync up the inventory with the server. The resource upgrade will be disabled.");
            }

            log.info("Starting to activate (and upgrade) resources.");

            activateAndUpgradeResourceRecursively(getPlatform(), syncResult);

            log.info("Inventory activated and upgrade requests gathered in " + (System.currentTimeMillis() - start)
                + "ms.");

            log.info("Sending the upgrade requests to the server.");
            resourceUpgradeDelegate.sendRequests();

            log.info("Resource activation and upgrade finished.");
        } catch (Throwable t) {
            log.error(
                "Resource activation or upgrade failed with an exception. An attempt to merely activate the resources will be made now.",
                t);

            //make sure to at least activate the resources
            activateAndUpgradeResourceRecursively(getPlatform(), false);
        } finally {
            resourceUpgradeDelegate.disable();
        }
    }

    private void activateAndUpgradeResourceRecursively(Resource resource, boolean doUpgrade) {
        ResourceContainer container = initResourceContainer(resource);

        boolean activate = true;

        //only do upgrade inside the agent. it does not make sense in embedded mode.
        if (doUpgrade && configuration.isInsideAgent()) {
            try {
                activate = prepareResourceForActivation(resource, container, false);
                activate = activate && resourceUpgradeDelegate.processAndQueue(container);
            } catch (Throwable t) {
                log.error("Exception thrown while upgrading [" + resource + "].", t);
                activate = false;
            }
        }

        if (activate) {
            try {
                activateResource(resource, container, false);
                for (Resource child : getContainerChildren(resource, container)) {
                    activateAndUpgradeResourceRecursively(child, doUpgrade);
                }
            } catch (InvalidPluginConfigurationException e) {
                log.debug("Failed to activate resource [" + resource + "] due to invalid plugin configuration.", e);
                handleInvalidPluginConfigurationResourceError(resource, e);
            } catch (Throwable t) {
                log.error("Exception thrown while activating [" + resource + "].", t);
                handleInvalidPluginConfigurationResourceError(resource, t);
            }
        }
    }

    /**
     * That class implements a listener that gets called when the resource got activated
     * @author hrupp
     *
     */
    class ResourceGotActivatedListener implements InventoryEventListener {

        @Override
        public void resourceActivated(Resource resource) {
            if (resource != null && resource.getId() > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Resource got finally activated, cleaning out config errors: " + resource);
                }

                DiscoveryServerService serverService = configuration.getServerServices().getDiscoveryServerService();
                if (serverService != null) {
                    serverService.clearResourceConfigError(resource.getId());
                }
            }
            removeInventoryEventListener(this);
        }

        @Override
        public void resourceDeactivated(Resource resource) {
            // nothing to do
        }

        @Override
        public void resourcesAdded(Set<Resource> resources) {
            // nothing to do
        }

        @Override
        public void resourcesRemoved(Set<Resource> resources) {
            // nothing to do
        }
    }

    public PluginComponentFactory getPluginComponentFactory() {
        return this.pluginFactory;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public MeasurementManager getMeasurementManager() {
        return measurementManager;
    }
}
