/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.pc.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.content.ContentAgentService;
import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;
import org.rhq.core.clientapi.server.content.ContentServerService;
import org.rhq.core.clientapi.server.content.DeletePackagesRequest;
import org.rhq.core.clientapi.server.content.DeployPackagesRequest;
import org.rhq.core.clientapi.server.content.RetrievePackageBitsRequest;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.content.transfer.DeployPackageStep;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.pc.ContainerService;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.ServerServices;
import org.rhq.core.pc.agent.AgentService;
import org.rhq.core.pc.inventory.InventoryEventListener;
import org.rhq.core.pc.inventory.InventoryManager;
import org.rhq.core.pc.inventory.ResourceContainer;
import org.rhq.core.pc.util.ComponentUtil;
import org.rhq.core.pc.util.FacetLockType;
import org.rhq.core.pc.util.LoggingThreadFactory;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentFacet;
import org.rhq.core.pluginapi.content.ContentServices;

public class ContentManager extends AgentService implements ContainerService, ContentAgentService, ContentServices {

    private static final int FACET_METHOD_TIMEOUT = 60 * 60 * 1000; // 60 minutes
    private final Log log = LogFactory.getLog(ContentManager.class);

    /**
     * Configuration elements for the running of this manager.
     */
    private PluginContainerConfiguration configuration;

    /**
     * Flag indicating whether or not this instance of the manager should run automatic, scheduled discoveries.
     */
    private boolean scheduledDiscoveriesEnabled;

    /**
     * Executor used in running discoveries.
     */
    private ScheduledThreadPoolExecutor discoveryThreadPoolExecutor;

    /**
     * Executor used for CRUD operations on content.
     */
    private ExecutorService crudExecutor;

    /**
     * Manages the scheduled discoveries, keeping them ordered on next execution time.
     */
    private final Queue<ScheduledContentDiscoveryInfo> scheduledDiscoveries = new PriorityQueue<ScheduledContentDiscoveryInfo>();

    /**
     * Event listener to receive notifications of changes to the inventory.
     */
    private ContentInventoryEventListener inventoryEventListener;

    public ContentManager() {
        super(ContentAgentService.class);
    }

    public void initialize() {
        log.info("Initializing Content Manager...");

        // Determine discovery mode - we only enable discovery if we are inside the agent and the period is positive non-zero
        this.scheduledDiscoveriesEnabled = (configuration.getContentDiscoveryPeriod() > 0);

        // Create thread pool executor. Used in both scheduled and non-scheduled mode for all discoveries.
        int threadPoolSize = configuration.getContentDiscoveryThreadPoolSize();

        discoveryThreadPoolExecutor = new ScheduledThreadPoolExecutor(threadPoolSize, new LoggingThreadFactory(
            "Content.discovery", true));

        discoveryThreadPoolExecutor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        discoveryThreadPoolExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);

        crudExecutor = new ThreadPoolExecutor(1, 5, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10000),
            new LoggingThreadFactory("Content.crud", true));

        // When running in scheduled mode, create and schedule the thread pool for discovering content
        if (scheduledDiscoveriesEnabled) {
            log.info("Initializing scheduled content discovery...");

            // Without specifying a particular piece of work, this runner will request the next piece of work from
            // the scheduled items queue
            ContentDiscoveryRunner runner = new ContentDiscoveryRunner(this);

            // Begin the automatic discovery thread
            long initialDelay = configuration.getContentDiscoveryInitialDelay();
            long discoveryPeriod = configuration.getContentDiscoveryPeriod();

            discoveryThreadPoolExecutor.scheduleAtFixedRate(runner, initialDelay, discoveryPeriod, TimeUnit.SECONDS);

            // Add inventory event listener so we can keep the scheduled discoveries consistent with the resources
            inventoryEventListener = new ContentInventoryEventListener();

            // the inventory manager has probably already activated some resources, so let's prepopulate our schedules
            InventoryManager im = PluginContainer.getInstance().getInventoryManager();
            im.notifyForAllActivatedResources(inventoryEventListener);

            // now ask that the inventory manager tell us about resources that will be activated in the future
            im.addInventoryEventListener(inventoryEventListener);
        }
        log.info("Content Manager initialized...");
    }

    public void shutdown() {
        log.info("Shutting down Content Manager...");
        PluginContainer pluginContainer = PluginContainer.getInstance();
        pluginContainer.shutdownExecutorService(discoveryThreadPoolExecutor, true);
        // pass false, so we don't interrupt a plugin in the middle of a content update
        pluginContainer.shutdownExecutorService(crudExecutor, false);
        pluginContainer.getInventoryManager().removeInventoryEventListener(inventoryEventListener);
    }

    public void setConfiguration(PluginContainerConfiguration configuration) {
        this.configuration = configuration;
    }

    public Set<ResourcePackageDetails> getLastDiscoveredResourcePackages(int resourceId) {
        // Get the resource component
        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = inventoryManager.getResourceContainer(resourceId);

        // Nothing to do if the container doesn't exist or isn't running, so punch out
        if ((container == null)
            || (ResourceContainer.ResourceComponentState.STARTED != container.getResourceComponentState())) {
            throw new RuntimeException("Container is non-existent or is not running for resource id [" + resourceId
                + "]");
        }

        return container.getInstalledPackages();
    }

    public ContentDiscoveryReport executeResourcePackageDiscoveryImmediately(int resourceId, String packageTypeName)
        throws PluginContainerException {
        // Load the package type object
        PackageType packageType = findPackageType(resourceId, packageTypeName);
        if (packageType == null) {
            throw new PluginContainerException("Could not load package type [" + packageTypeName + "] for resource: "
                + resourceId);
        }

        // Create a new runner that is scoped to the resource/package type specified
        ScheduledContentDiscoveryInfo discoveryInfo = new ScheduledContentDiscoveryInfo(resourceId, packageType);

        ContentDiscoveryRunner oneTimeRunner = new ContentDiscoveryRunner(this, discoveryInfo);

        ContentDiscoveryReport results;
        try {
            results = discoveryThreadPoolExecutor.submit((Callable<ContentDiscoveryReport>) oneTimeRunner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Exception occurred during execution of discovery", e);
        }

        return results;
    }

    public void deployPackages(DeployPackagesRequest request) {
        Runnable runner = new CreateContentRunner(this, request);
        crudExecutor.submit(runner);
    }

    public DeployPackagesResponse deployPackagesImmediately(DeployPackagesRequest request)
        throws PluginContainerException {
        Callable<DeployPackagesResponse> runner = new CreateContentRunner(this, request);
        try {
            return crudExecutor.submit(runner).get();
        } catch (Exception e) {
            throw new PluginContainerException("Error during deployment of packages. request: " + request, e);
        }
    }

    public void deletePackages(DeletePackagesRequest request) {
        DeleteContentRunner runner = new DeleteContentRunner(this, request);
        crudExecutor.submit(runner);
    }

    public void retrievePackageBits(RetrievePackageBitsRequest request) {
        RetrieveContentBitsRunner runner = new RetrieveContentBitsRunner(this, request);
        crudExecutor.submit(runner);
    }

    public List<DeployPackageStep> translateInstallationSteps(int resourceId, ResourcePackageDetails packageDetails)
        throws PluginContainerException {
        List<DeployPackageStep> steps;
        try {
            ContentFacet contentFacet = findContentFacet(resourceId);
            steps = contentFacet.generateInstallationSteps(packageDetails);
        } catch (Exception e) {
            throw new PluginContainerException("Error translating the package installation steps", e);
        }

        return steps;
    }

    // ContentServices Implementation  --------------------------------------------

    public long downloadPackageBitsForChildResource(ContentContext context, String childResourceTypeName,
        PackageDetailsKey key, OutputStream outputStream) {

        ContentContextImpl contextImpl = (ContentContextImpl) context;
        ContentServerService serverService = getContentServerService();
        outputStream = remoteOutputStream(outputStream);

        long count = serverService.downloadPackageBitsForChildResource(contextImpl.getResourceId(),
            childResourceTypeName, key, outputStream);

        return count;
    }

    public long downloadPackageBits(ContentContext context, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, boolean resourceExists) {
        ContentContextImpl contextImpl = (ContentContextImpl) context; // this has to be of this type, we gave it to the plugin
        ContentServerService serverService = getContentServerService();

        // we need to load the content to server before we will start download the content
        // it is because of timeout on remoteStreams
        serverService.preLoadRemoteContent(contextImpl.getResourceId(), packageDetailsKey);

        outputStream = remoteOutputStream(outputStream);
        long count = 0;
        if (resourceExists) {
            count = serverService.downloadPackageBitsGivenResource(contextImpl.getResourceId(), packageDetailsKey,
                outputStream);
        } else {
            // TODO: Figure out how to support this; the APIs require the resource to get the bits
        }
        return count;
    }

    public long downloadPackageBitsRange(ContentContext context, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte, boolean resourceExists) {
        ContentContextImpl contextImpl = (ContentContextImpl) context; // this has to be of this type, we gave it to the plugin
        ContentServerService serverService = getContentServerService();
        outputStream = remoteOutputStream(outputStream);
        long count = 0;
        if (resourceExists) {
            count = serverService.downloadPackageBitsRangeGivenResource(contextImpl.getResourceId(), packageDetailsKey,
                outputStream, startByte, endByte);
        } else {
            // TODO: Figure out how to support this; the APIs require the resource to get the bits
        }
        return count;
    }

    public long getPackageBitsLength(ContentContext context, PackageDetailsKey packageDetailsKey) {
        ContentContextImpl contextImpl = (ContentContextImpl) context; // this has to be of this type, we gave it to the plugin
        ContentServerService serverService = getContentServerService();
        long size = serverService.getPackageBitsLength(contextImpl.getResourceId(), packageDetailsKey);
        return size;
    }

    public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(ContentContext context, PageControl pc) {
        ContentContextImpl contextImpl = (ContentContextImpl) context; // this has to be of this type, we gave it to the plugin
        ContentServerService serverService = getContentServerService();
        PageList<PackageVersionMetadataComposite> metadata = serverService.getPackageVersionMetadata(contextImpl
            .getResourceId(), pc);
        return metadata;
    }

    public String getResourceSubscriptionMD5(ContentContext context) {
        ContentContextImpl contextImpl = (ContentContextImpl) context; // this has to be of this type, we gave it to the plugin
        ContentServerService serverService = getContentServerService();
        String metadataMD5 = serverService.getResourceSubscriptionMD5(contextImpl.getResourceId());
        return metadataMD5;
    }

    // Package  --------------------------------------------

    /**
     * Returns the next content discovery to take place. This method will check to ensure the discovery can occur; its
     * next discovery time is not scheduled for the future.
     *
     * @return information needed to trigger a discovery; <code>null</code> if no discoveries are necessary.
     */
    synchronized ScheduledContentDiscoveryInfo getNextScheduledDiscovery() {
        // Check to see if the current time has passed the next discovery time
        ScheduledContentDiscoveryInfo next = scheduledDiscoveries.peek();

        if ((next == null) || (next.getNextDiscovery() > System.currentTimeMillis())) {
            return null;
        } else {
            return scheduledDiscoveries.poll();
        }
    }

    /**
     * Sets the next discovery time for the specified discovery.
     *
     * @param discoveryInfo discovery being rescheduled; cannot be <code>null</code>.
     */
    synchronized void rescheduleDiscovery(ScheduledContentDiscoveryInfo discoveryInfo) {
        // Sanity check
        if (!scheduledDiscoveriesEnabled) {
            log.warn("An attempt was made to reschedule a content discovery "
                + "while not running in scheduled discovery mode - returning...");
            return;
        }

        /* This is to prevent a race condition where the resource associated with this item is removed while this
         * discovery is being run. In such a case, this item cannot be removed from the queue (as it is out of the queue
         * while running) but will still be added at the end of the this call.
         */

        // Make sure the resource still exists, otherwise don't bother rescheduling
        ResourceContainer resourceContainer = PluginContainer.getInstance().getInventoryManager().getResourceContainer(
            discoveryInfo.getResourceId());

        if (resourceContainer != null) {
            boolean debugEnabled = log.isDebugEnabled();

            if (discoveryInfo.getInterval() > 0) {
                if (debugEnabled) {
                    log.debug("Rescheduling [" + discoveryInfo + "]...");
                }
                discoveryInfo.setNextDiscovery(System.currentTimeMillis() + discoveryInfo.getInterval());
                addToQueue(discoveryInfo);
                if (debugEnabled) {
                    log.debug("Finished rescheduling: " + discoveryInfo);
                }
            } else {
                if (debugEnabled) {
                    log.debug("Will not reschedule content discovery: " + discoveryInfo);
                }
            }
        }
    }

    /**
     * Unschedules any discoveries that are currently in the queue to be executed against the specified resource.
     *
     * @param resource resource whose discoveries to remove; cannot be <code>null</code>
     */
    synchronized void unscheduleDiscoveries(Resource resource) {
        if (log.isDebugEnabled()) {
            log.debug("Unscheduling content discoveries for resource id [" + resource + ']');
        }

        // Find all scheduled items for this resource
        Set<ScheduledContentDiscoveryInfo> unscheduleUs = new HashSet<ScheduledContentDiscoveryInfo>();
        for (ScheduledContentDiscoveryInfo scheduledItem : scheduledDiscoveries) {
            if (scheduledItem.getResourceId() == resource.getId()) {
                unscheduleUs.add(scheduledItem);
            }
        }

        // Remove all matching items from the queue
        for (ScheduledContentDiscoveryInfo removeMe : unscheduleUs) {
            scheduledDiscoveries.remove(removeMe);
        }
    }

    /**
     * Performs a content discovery for the provided type against the resource.
     *
     * @param  resourceId resource whose content are being discovered
     * @param  type       type of content to discover
     *
     * @return content that were discovered by this discovery
     *
     * @throws Exception if the plugin is incorrectly configured or throws an error while attempting discovery
     */
    ContentDiscoveryReport performContentDiscovery(int resourceId, PackageType type) throws Exception {
        // Perform the discovery
        // Use only a read-locked component proxy
        ContentFacet contentFacet = ComponentUtil.getComponent(resourceId, ContentFacet.class, FacetLockType.READ,
            FACET_METHOD_TIMEOUT, false, true);

        Set<ResourcePackageDetails> details = contentFacet.discoverDeployedPackages(type);

        if (log.isDebugEnabled()) {
            log.debug("Discovered [" + ((details != null) ? details.size() : 0) + "] packages of type=" + type);
        }

        // Process the results
        ContentDiscoveryReport report = handleDiscoveredContent(details, resourceId);
        return report;
    }

    /**
     * Performs a call to the ContentFacet to create a new package with the specified details.
     *
     * @param  resourceId       resource against which the content will be created
     * @param  packagesToDeploy describes the packages being deployed to the resource
     *
     * @return response from the facet
     *
     * @throws Exception                if the plugin throws an error while creating the content
     * @throws PluginContainerException if there is an error in the plugin container gathering the required data to
     *                                  perform the create
     */
    DeployPackagesResponse performPackageDeployment(int resourceId, Set<ResourcePackageDetails> packagesToDeploy)
        throws Exception {
        // Perform the create
        ContentFacet contentFacet = findContentFacet(resourceId);
        DeployPackagesResponse response = contentFacet.deployPackages(packagesToDeploy, this);

        return response;
    }

    /**
     * Performs a call to the <code>ContentFacet</code> to have the plugin delete the specified resource.
     *
     * @param  resourceId       resource in which the content exists
     * @param  packagesToDelete describes the packages being deleted from the resource
     *
     * @return response object from the facet
     *
     * @throws Exception if the plugin throws an error while trying to delete the content
     */
    RemovePackagesResponse performPackageDelete(int resourceId, Set<ResourcePackageDetails> packagesToDelete)
        throws Exception {
        // Perform the delete
        ContentFacet contentFacet = findContentFacet(resourceId);
        RemovePackagesResponse response = contentFacet.removePackages(packagesToDelete);

        return response;
    }

    InputStream performGetPackageBits(int resourceId, ResourcePackageDetails packageToRetrieve) throws Exception {
        // Perform the retrieval
        ContentFacet contentFacet = findContentFacet(resourceId);
        InputStream contentStream = contentFacet.retrievePackageBits(packageToRetrieve);

        // Wrap the content stream for sending to the original ArtifactAgentService caller
        // There is no need to check for agent mode here; the method call will wrap appropriately
        contentStream = remoteInputStream(contentStream);

        return contentStream;
    }

    /**
     * Returns the server handle to use to complete requests.
     *
     * @return server service implementation if one is registered; <code>null</code> otherwise
     */
    ContentServerService getContentServerService() {
        ContentServerService serverService = null;
        ServerServices serverServices = configuration.getServerServices();
        if (serverServices != null) {
            serverService = serverServices.getContentServerService();
        }
        return serverService;
    }

    // Private  --------------------------------------------

    /**
     * Schedules any necessary discoveries for the specified resource.
     * This is called when a resource is newly activated - as when a plugin configuration change is made.
     * If a schedule is already in place, it will remain.
     * 
     * @param resource resource for which discoveries are being scheduled
     */
    private synchronized void scheduleDiscoveries(Resource resource) {
        // Sanity check
        if (!scheduledDiscoveriesEnabled) {
            log.warn("Attempting to schedule a discovery for a resource while not running in scheduled discovery mode");
            return;
        }

        ResourceType resourceType = resource.getResourceType();
        Set<PackageType> packageTypes = resourceType.getPackageTypes();

        if ((packageTypes != null) && (packageTypes.size() > 0)) {

            int resourceId = resource.getId();

            // Check the queue to make sure we haven't already scheduled anything for this resource.
            // If a schedule already exists, we'll remove it so we reschedule it to trigger soon.
            Iterator<ScheduledContentDiscoveryInfo> iterator = scheduledDiscoveries.iterator();
            while (iterator.hasNext()) {
                ScheduledContentDiscoveryInfo contentDiscoveryInfo = iterator.next();
                if (contentDiscoveryInfo.getResourceId() == resourceId) {
                    if (log.isDebugEnabled()) {
                        log.debug("Already found scheduled content discovery for resource id [" + resourceId
                            + "], package type=[" + contentDiscoveryInfo.getPackageType()
                            + "]. Will reschedule to be triggered soon.");
                    }
                    iterator.remove();
                }
            }

            if (log.isDebugEnabled()) {
                log.debug("Scheduling [" + packageTypes.size() + "] content discoveries for resource id [" + resourceId
                    + "]");
            }

            // Add discovery items for each type. Since intervals will vary per type, create separate scheduled
            // items for each package type that can be found for this resource
            for (PackageType type : packageTypes) {
                ScheduledContentDiscoveryInfo contentDiscovery;
                contentDiscovery = new ScheduledContentDiscoveryInfo(resourceId, type);
                contentDiscovery.setNextDiscovery(System.currentTimeMillis()); // schedule the first one as soon as possible
                addToQueue(contentDiscovery);
            }
        }

        return;
    }

    /**
     * Adds a new scheduled item to the queue, taking care to synchronize on the queue and not face concurrency issues.
     * This will only add the item if we are running in the agent mode (and use the automatic scheduler)
     *
     * @param item new item to add to the queue
     */
    private synchronized void addToQueue(ScheduledContentDiscoveryInfo item) {
        // Make sure we're in scheduled mode before adding to queue
        if (scheduledDiscoveriesEnabled) {
            scheduledDiscoveries.offer(item);
        } else {
            log.warn("Attempting to add a scheduled item to the queue when not running in scheduled mode: " + item);
        }
    }

    /**
     * Handles the results received from the call to the facet to discover content. See
     * {@link ContentFacet#discoverDeployedPackages(org.rhq.core.domain.content.PackageType)}.
     *
     * @param  details    description of content that was returned from the facet
     * @param  resourceId resource against which the content were found
     *
     * @return domain model representation of the details specified
     *
     * @throws Exception if there is an error from any subsequent calls made to the facet
     */
    private ContentDiscoveryReport handleDiscoveredContent(Set<ResourcePackageDetails> details, int resourceId)
        throws Exception {
        // The plugin should at least return an empty set, but check for null too.
        if (details == null) {
            return null;
        }

        InventoryManager inventoryManager = PluginContainer.getInstance().getInventoryManager();
        ResourceContainer container = inventoryManager.getResourceContainer(resourceId);

        Set<ResourcePackageDetails> updatedPackageSet = new HashSet<ResourcePackageDetails>(details);
        Set<ResourcePackageDetails> existingInstalledPackagesSet = container.getInstalledPackages();
        if (existingInstalledPackagesSet == null) {
            existingInstalledPackagesSet = new HashSet<ResourcePackageDetails>();
        }

        // Strip out content that have been removed (i.e. not returned on the latest discovery)
        int originalPackageCount = existingInstalledPackagesSet.size();
        existingInstalledPackagesSet.retainAll(updatedPackageSet);
        int removedPackagesCount = originalPackageCount - existingInstalledPackagesSet.size();
        if (removedPackagesCount > 0) {
            if (log.isDebugEnabled()) {
                log.debug("Removed [" + removedPackagesCount + "] obsolete packages for resource id [" + resourceId
                    + "]");
            }
        }

        // Strip from updated list content that are already known for the resource, we don't need to do anything
        updatedPackageSet.removeAll(existingInstalledPackagesSet);

        // Remaining content in updated list are "new" content
        if (!updatedPackageSet.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Found [" + updatedPackageSet.size() + "] new packages for resource id [" + resourceId + "]");
            }
        }

        // Add new content (yes, existingInstalledPackagesSet is same as details, but use the container's reference)
        existingInstalledPackagesSet.addAll(updatedPackageSet);

        // Add merged (current) list to the resource container
        container.setInstalledPackages(existingInstalledPackagesSet);

        // Package and send to server
        ContentDiscoveryReport report = new ContentDiscoveryReport();
        report.addAllDeployedPackages(existingInstalledPackagesSet);
        report.setResourceId(resourceId);

        ContentServerService contentServerService = getContentServerService();
        if (contentServerService != null) {
            // if there are 1+ installed packages to report OR there are 0 but there used to be packages installed,
            // then send up the report to be merged
            if (!existingInstalledPackagesSet.isEmpty() || originalPackageCount != 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Merging [" + existingInstalledPackagesSet.size()
                        + "] discovered packages for resource id [" + resourceId + "] with Server");
                }
                contentServerService.mergeDiscoveredPackages(report);
            }
        }

        return report;
    }

    /**
     * Returns the <code>ContentFacet</code> for the component associated with the specified resource ID.
     *
     * @param  resourceId resource whose facet is being found
     *
     * @return <code>ContentFacet</code> to contact for the specified resource ID; this will never be <code>null</code>.
     *
     * @throws Exception if the <code>ContentFacet</code> cannot be retrieved
     */
    private ContentFacet findContentFacet(int resourceId) throws Exception {
        // in case some calls to here need only the read lock - for now, always lock down with the write lock
        return ComponentUtil.getComponent(resourceId, ContentFacet.class, FacetLockType.WRITE, FACET_METHOD_TIMEOUT,
            false, true);
    }

    /**
     * Finds a package type defined in the resource type of the specified resource.
     *
     * @param  resourceId      resource whose definition will be checked for the type
     * @param  packageTypeName name of the type being retrieved
     *
     * @return type instance if one is found for the specified name; <code>null</code> otherwise
     *
     * @throws PluginContainerException if the resource id is invalid
     */
    private PackageType findPackageType(int resourceId, String packageTypeName) throws PluginContainerException {
        ResourceType resourceType = ComponentUtil.getResourceType(resourceId);
        for (PackageType type : resourceType.getPackageTypes()) {
            if (type.getName().equals(packageTypeName)) {
                return type;
            }
        }

        return null;
    }

    // Inner Classes  --------------------------------------------

    /**
     * Listens for inventory change events and adjusts the scheduled items accordingly. That is, adds new scheduled
     * content scans for new resources and removes discoveries for resources that have been removed from inventory. This
     * class has no effect when the PC is running in embedded mode and should not be registered as a listener.
     */
    private class ContentInventoryEventListener implements InventoryEventListener {

        public void resourceActivated(Resource resource) {
            ContentManager.this.scheduleDiscoveries(resource);
        }

        public void resourceDeactivated(Resource resource) {
            ContentManager.this.unscheduleDiscoveries(resource);
        }

        public void resourcesAdded(Set<Resource> resources) {
            // when activated, we'll add the schedules
        }

        public void resourcesRemoved(Set<Resource> resources) {
            for (Resource removeMe : resources) {
                ContentManager.this.unscheduleDiscoveries(removeMe);
            }
        }
    }
}
