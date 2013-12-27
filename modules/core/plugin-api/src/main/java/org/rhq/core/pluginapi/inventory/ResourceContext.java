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
package org.rhq.core.pluginapi.inventory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.ProcessScan;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.availability.AvailabilityCollectorRunnable;
import org.rhq.core.pluginapi.availability.AvailabilityContext;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.component.ComponentInvocationContext;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.operation.OperationContext;
import org.rhq.core.pluginapi.upgrade.ResourceUpgradeContext;
import org.rhq.core.system.ProcessInfo;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.system.pquery.ProcessInfoQuery;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * The context object that {@link ResourceComponent} objects will have access - it will have all the information that
 * the resource components needs during their lifetime.
 *
 * <p>This context class is currently designed to be an immutable object. Instances of this context object are to be
 * created by the plugin container only.</p>
 *
 * @param  <T> the parent resource component type for this component. This means you can nest a hierarchy of resource
 *             components that mimic the resource type hierarchy as defined in a plugin deployment descriptor.
 *
 * @author John Mazzitelli
 */
@SuppressWarnings("unchecked")
public class ResourceContext<T extends ResourceComponent<?>> {

    private static final Log LOG = LogFactory.getLog(ResourceContext.class);

    private final T parentResourceComponent;
    private final ResourceContext<?> parentResourceContext;
    private final Configuration pluginConfiguration;
    private final SystemInfo systemInformation;
    private final ResourceDiscoveryComponent<T> resourceDiscoveryComponent;
    private final Resource resource;
    private File temporaryDirectory; // Lazily evaluated
    private final File baseDataDirectory; // base data directory from system
    private final String pluginContainerName;
    private final EventContext eventContext;
    private final OperationContext operationContext;
    private final ContentContext contentContext;
    private final AvailabilityContext availabilityContext;
    private final InventoryContext inventoryContext;
    private final PluginContainerDeployment pluginContainerDeployment;
    private final ResourceTypeProcesses trackedProcesses;
    private final ComponentInvocationContext componentInvocationContext;

    private static class Children {
        public final ResourceType resourceType;
        public final String parentResourceUuid;

        public Children(String parentResourceUuid, ResourceType resourceType) {
            this.parentResourceUuid = parentResourceUuid;
            this.resourceType = resourceType;
        }

        @Override
        public int hashCode() {
            int uuidHashCode = parentResourceUuid == null ? 1 : parentResourceUuid.hashCode();
            return 31 * uuidHashCode * resourceType.getId();
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }

            if (!(other instanceof Children)) {
                return false;
            }

            Children o = (Children) other;

            return (parentResourceUuid == null ? o.parentResourceUuid == null : parentResourceUuid
                .equals(o.parentResourceUuid)) && resourceType.equals(o.resourceType);
        }
    }

    private static Map<Children, ResourceTypeProcesses> PROCESSES_PER_PARENT_PER_RESOURCE_TYPE = new HashMap<Children, ResourceTypeProcesses>();

    /**
     * Creates a new {@link ResourceContext} object with a default {@link ComponentInvocationContext}.
     *
     * @deprecated as of RHQ 4.9.
     */
    @Deprecated
    public ResourceContext(Resource resource, T parentResourceComponent, ResourceContext<?> parentResourceContext,
        ResourceDiscoveryComponent<T> resourceDiscoveryComponent, SystemInfo systemInfo, File temporaryDirectory,
        File baseDataDirectory, String pluginContainerName, EventContext eventContext, OperationContext operationContext,
        ContentContext contentContext, AvailabilityContext availabilityContext, InventoryContext inventoryContext,
        PluginContainerDeployment pluginContainerDeployment) {
        this(resource, parentResourceComponent, parentResourceContext, resourceDiscoveryComponent, systemInfo,
            temporaryDirectory, baseDataDirectory, pluginContainerName, eventContext, operationContext, contentContext,
            availabilityContext, inventoryContext, pluginContainerDeployment, new ComponentInvocationContext() {
                @Override
                public boolean isInterrupted() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void markInterrupted() {
                    throw new UnsupportedOperationException();
                }
            });
    }

    /**
     * Creates a new {@link ResourceContext} object.
     *
     * <b>NOTE:</b> The plugin container is responsible for instantiating these objects; plugin writers should never
     * have to actually create context objects.
     *
     * @param resource                   the resource whose {@link org.rhq.core.pluginapi.inventory.ResourceComponent}
     *                                   will be given this context object of the plugin
     * @param parentResourceComponent    the parent component of the context's associated resource component (or null if parent resource is null)
     * @param parentResourceContext      the resource context of the parent resource (or null if parent resource is null)
     * @param resourceDiscoveryComponent the discovery component that can be used to detect other resources of the same
     *                                   type as this resource (may be <code>null</code>)
     * @param systemInfo                 information about the system on which the plugin and its plugin container are
     *                                   running
     * @param temporaryDirectory         a temporary directory for plugin use that is destroyed at plugin container shutdown
     * @param baseDataDirectory              a directory where plugins can store persisted data that survives plugin container restarts
     * @param pluginContainerName        the name of the plugin container in which the discovery component is running.
     *                                   Components can be assured this name is unique across <b>all</b> plugin
     *                                   containers/agents running in the RHQ environment.
     * @param eventContext               an {@link EventContext}, if the resource supports one or more types of
     *                                   {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise
     * @param operationContext           an {@link OperationContext} the plugin can use to interoperate with the
     *                                   operation manager
     * @param contentContext             a {@link ContentContext} the plugin can use to interoperate with the content
     *                                   manager
     * @param availabilityContext        a {@link AvailabilityContext} the plugin can use to interoperate with the
     *                                   plugin container inventory manager
     * @param pluginContainerDeployment  indicates where the plugin container is running
     * @param componentInvocationContext a {@link ComponentInvocationContext} the plugin can use to determine if the
     *                                   current component invocation has been canceled or timed out.
     */
    public ResourceContext(Resource resource, T parentResourceComponent, ResourceContext<?> parentResourceContext,
        ResourceDiscoveryComponent<T> resourceDiscoveryComponent, SystemInfo systemInfo, File temporaryDirectory,
        File baseDataDirectory, String pluginContainerName, EventContext eventContext, OperationContext operationContext,
        ContentContext contentContext, AvailabilityContext availabilityContext, InventoryContext inventoryContext,
        PluginContainerDeployment pluginContainerDeployment, ComponentInvocationContext componentInvocationContext) {

        this.resource = resource;
        this.parentResourceComponent = parentResourceComponent;
        this.parentResourceContext = parentResourceContext;
        this.resourceDiscoveryComponent = resourceDiscoveryComponent;
        this.systemInformation = systemInfo;
        this.pluginConfiguration = resource.getPluginConfiguration();
        this.baseDataDirectory = baseDataDirectory;
        this.pluginContainerName = pluginContainerName.intern();
        this.pluginContainerDeployment = pluginContainerDeployment;
        this.temporaryDirectory = temporaryDirectory;

        this.eventContext = eventContext;
        this.operationContext = operationContext;
        this.contentContext = contentContext;
        this.availabilityContext = availabilityContext;
        this.inventoryContext = inventoryContext;

        String parentResourceUuid = "";
        if (resource.getParentResource() != null) {
            parentResourceUuid = resource.getParentResource().getUuid();
        }
        this.trackedProcesses = getTrackedProcesses(parentResourceUuid, resource.getResourceType());

        this.componentInvocationContext = componentInvocationContext;
    }

    /**
     * The {@link Resource#getResourceKey() resource key} of the resource this context is associated with. This resource
     * key is unique across all of the resource's siblings. That is to say, this resource key is unique among all
     * children of the {@link #getParentResourceComponent() parent}.
     *
     * @return resource key of the associated resource
     */
    public String getResourceKey() {
        return this.resource.getResourceKey();
    }

    /**
     * The {@link Resource#getResourceType() resource type} of the resource this context is associated with.
     *
     * @return type of the associated resource
     */
    public ResourceType getResourceType() {
        return this.resource.getResourceType();
    }

    /**
     * The {@link Resource#getVersion() version} of the resource this context is associated with.
     *
     * @return the resource's version string
     *
     * @since 1.2
     */
    public String getVersion() {
        return this.resource.getVersion();
    }

    /**
     * The data directory of the resource this context is associated with.
     *
     * @return resource data directory
     */
    public File getResourceDataDirectory() {
        File resourceDataDirectory = new File(baseDataDirectory, this.getAncestryBasedResourceKey());

        try {
            File oldResourceDataDirectory = new File(baseDataDirectory, this.resource.getUuid());
            if (oldResourceDataDirectory.exists()) {
                oldResourceDataDirectory.renameTo(resourceDataDirectory);
            }
        } catch (Exception e) {
            //Just prevent an exception related to renaming of the old
            //data resource directory from causing this method fail.
            //This method should continue and create the new folder
            //as if the old folder never existed.
        }

        if (!resourceDataDirectory.exists()) {
            resourceDataDirectory.mkdirs();
        }

        return resourceDataDirectory;
    }

    /**
     * The data directory of a child to be created for the resource this context is associated with.
     *
     * @return child resource data directory
     */
    public File getFutureChildResourceDataDirectory(String childResourceKey) {
        File childResourceDataDirectory = new File(baseDataDirectory, this.getAncestryBasedResourceKey(childResourceKey));
        if (!childResourceDataDirectory.exists()) {
            childResourceDataDirectory.mkdirs();
        }

        return childResourceDataDirectory;
    }

    /**
     * The parent of the resource component that is associated with this context.
     *
     * @return parent component of the associated resource component
     */
    public T getParentResourceComponent() {
        return this.parentResourceComponent;
    }

    /**
     * Returns the resource context of the parent resource or null if there is no parent resource.
     * <p>
     * (This method is protected to be able to share that information with the {@link ResourceUpgradeContext}
     * but at the same time to not pollute the ResourceContext public API with data that doesn't belong
     * to it).
     *
     * @return
     */
    protected ResourceContext<?> getParentResourceContext() {
        return this.parentResourceContext;
    }

    /**
     * Returns a {@link SystemInfo} object that contains information about the platform/operating system that the
     * resource is running on. With this object, you can natively obtain things such as the operating system name, its
     * hostname,and other things. Please refer to the javadoc on {@link SystemInfo} for more details on the types of
     * information you can access.
     *
     * @return system information object
     */
    public SystemInfo getSystemInformation() {
        return this.systemInformation;
    }

    /**
     * Returns the resource's plugin configuration. This is used to configure the subsystem that is used to actually
     * talk to the managed resource. Do not confuse this with the <i>resource configuration</i>, which is the actual
     * configuration settings for the managed resource itself.
     *
     * @return plugin configuration
     */
    public Configuration getPluginConfiguration() {
        return this.pluginConfiguration.deepCopy();
    }

    /**
     * Returns the information on the native operating system process in which the managed resource is running. If
     * native support is not available or the process for some reason can no longer be found, this may return <code>
     * null</code>.
     *
     * The returned {@link ProcessInfo} always has a fresh snapshot of non static data: it's whether newly created
     * or got refreshed in order to determine if the process was still running.
     *
     * @return information on the resource's process, or null if process was not found
     */
    public ProcessInfo getNativeProcess() {
        ProcessInfo processInfo = null;

        synchronized (trackedProcesses) {
            //right, we've entered the critical section...
            //we might have waited for another thread to actually fill in the tracked processes
            //so let's check again if we really need to run the discovery
            processInfo = trackedProcesses.getProcessInfo(resource.getResourceKey());

            if (isRediscoveryRequired(processInfo)) {

                if (LOG.isTraceEnabled()) {
                    LOG.trace("getNativeProcess(): recheck for rediscovery confirmed the need for it");
                }

                try {
                    Set<DiscoveredResourceDetails> details = Collections.emptySet();

                    List<ProcessScanResult> processes = getNativeProcessesForType();
                    if (!processes.isEmpty()) {
                        ResourceDiscoveryContext<T> context;

                        context = new ResourceDiscoveryContext<T>(this.resource.getResourceType(), this.parentResourceComponent,
                            this.parentResourceContext, this.systemInformation, processes, Collections.EMPTY_LIST,
                            getPluginContainerName(), getPluginContainerDeployment());

                        details = this.resourceDiscoveryComponent.discoverResources(context);
                    }

                    trackedProcesses.update(details);
                    processInfo = trackedProcesses.getProcessInfo(resource.getResourceKey());
                } catch (Exception e) {
                    LOG.warn("Cannot get native process for resource [" + this.resource.getResourceKey() + "] - discovery failed", e);
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("getNativeProcess(): rediscovery done");
        }

        return processInfo;
    }

    private boolean isRediscoveryRequired(ProcessInfo processInfo) {
        return processInfo == null || !processInfo.freshSnapshot().isRunning();
    }

    /**
     * Scans the current list of running processes and returns information on all processes that may contain resources
     * of the {@link #getResourceType() same type as this resource}. More specifically, this method will scan all the
     * processes and try to match them up with the {@link ResourceType#getProcessScans() PIQL queries} associated with
     * this resource's type.
     *
     * @return information on the processes that may be running this resource or other resources of the same type
     *
     * @see    ResourceType#getProcessScans()
     */
    public List<ProcessScanResult> getNativeProcessesForType() {
        // perform auto-discovery PIQL queries now to see if we can auto-detect resources that are running now of this type
        List<ProcessScanResult> scanResults = new ArrayList<ProcessScanResult>();
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        try {
            Set<ProcessScan> processScans = this.resource.getResourceType().getProcessScans();
            if (processScans != null && !processScans.isEmpty()) {
                ProcessInfoQuery piq = new ProcessInfoQuery(systemInfo.getAllProcesses());
                for (ProcessScan processScan : processScans) {
                    List<ProcessInfo> queryResults = piq.query(processScan.getQuery());
                    if ((queryResults != null) && (queryResults.size() > 0)) {
                        for (ProcessInfo autoDiscoveredProcess : queryResults) {
                            scanResults.add(new ProcessScanResult(processScan, autoDiscoveredProcess));
                        }
                    }
                }
            }
        } catch (UnsupportedOperationException uoe) {
        }

        return scanResults;
    }

    /**
     * A temporary directory for plugin use that is destroyed at plugin container shutdown. Plugins should use this if they need to
     * write temporary files that they do not expect to remain after the plugin container is restarted. This directory is shared
     * among all plugins - plugins must ensure they write unique files here, as other plugins may be using this same
     * directory. Typically, plugins will use the {@link File#createTempFile(String, String, File)} API when writing to
     * this directory.
     *
     * @return location for plugin temporary files
     */
    public File getTemporaryDirectory() {
        if (this.temporaryDirectory==null) {
            this.temporaryDirectory = new File(System.getProperty("java.io.tmpdir"), "AGENT_TMP");
            this.temporaryDirectory.mkdirs();
        }
        return temporaryDirectory;
    }

    /**
     * Directory where plugins can store persisted data that survives plugin container restarts. Each plugin will have their own
     * data directory. The returned directory may not yet exist - it is up to each individual plugin to manage this
     * directory as they see fit (this includes performing the initial creation when the directory is first needed).
     *
     * @return location for plugins to store persisted data
     */
    public File getDataDirectory() {
        return new File(baseDataDirectory, resource.getResourceType().getPlugin());
    }

    /**
     * The name of the plugin container in which the resource component is running. Components
     * can be assured this name is unique across <b>all</b> plugin containers/agents running
     * in the RHQ environment.
     *
     * @return the name of the plugin container
     */
    public String getPluginContainerName() {
        return pluginContainerName;
    }

    /**
     * Indicates where the plugin container (and therefore where the plugins) are deployed and running.
     * See {@link PluginContainerDeployment} for more information on what the return value means.
     *
     * @return indicator of where the plugin container is deployed and running
     *
     * @since 1.3
     */
    public PluginContainerDeployment getPluginContainerDeployment() {
        return pluginContainerDeployment;
    }

    /**
     * Returns an {@link EventContext}, if the resource supports one or more types of
     * {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise.
     *
     * @return an <code>EventContext</code>, if the resource supports one or more types of
     *         {@link org.rhq.core.domain.event.Event}s, or <code>null</code> otherwise
     */
    public EventContext getEventContext() {
        return eventContext;
    }

    /**
     * Returns an {@link OperationContext} that allows the plugin to access the operation functionality provided by the
     * plugin container.
     *
     * @return operation context object
     */
    public OperationContext getOperationContext() {
        return operationContext;
    }

    /**
     * Returns a {@link ContentContext} that allows the plugin to access the content functionality provided by the
     * plugin container.
     *
     * @return content context object
     */
    public ContentContext getContentContext() {
        return contentContext;
    }

    /**
     * Returns an {@link AvailabilityContext} that allows the plugin to access the availability functionality provided by the
     * plugin container.
     *
     * @return availability context object
     */
    public AvailabilityContext getAvailabilityContext() {
        return availabilityContext;
    }

    /**
      * Returns an {@link InventoryContext} that allows the plugin to access inventory related functionality provided by the
      * plugin container.
      *
      * @return the inventory context
      */
    public InventoryContext getInventoryContext() {
        return inventoryContext;
    }

    /**
     * @deprecated Use {@link AvailabilityContext#createAvailabilityCollectorRunnable(AvailabilityFacet, long)}
     */
    @Deprecated
    public AvailabilityCollectorRunnable createAvailabilityCollectorRunnable(AvailabilityFacet availChecker,
        long interval) {

        return getAvailabilityContext().createAvailabilityCollectorRunnable(availChecker, interval);
    }

    /**
     * Returns a shared object representing the processes detected for given resource type under given parent.
     * Note that this comes from a static field so it is shared by any resource contexts representing a
     * resource of the same type under a single parent. This is to reduce the number of needed discoveries
     * to a minimum.
     *
     * @param parentResourceUuid
     * @param resourceType
     * @return
     */
    private static ResourceTypeProcesses getTrackedProcesses(String parentResourceUuid, ResourceType resourceType) {
        synchronized (PROCESSES_PER_PARENT_PER_RESOURCE_TYPE) {
            Children key = new Children(parentResourceUuid, resourceType);
            ResourceTypeProcesses ret = PROCESSES_PER_PARENT_PER_RESOURCE_TYPE.get(key);
            if (ret == null) {
                ret = new ResourceTypeProcesses();
                PROCESSES_PER_PARENT_PER_RESOURCE_TYPE.put(key, ret);
            }

            return ret;
        }
    }

    /**
     * Calculates a unique key based on parents' resource keys. The final key is the SHA256
     * all the ancestry resource keys.
     *
     * @return key
     */
    private String getAncestryBasedResourceKey() {
        return this.getAncestryBasedResourceKey(null);
    }

    /**
     * Calculates a unique key based on parents' resource keys.
     *
     * @param prefixKey extra key to be appended at the beginning of the digest process
     * @return key
     */
    private String getAncestryBasedResourceKey(String prefixKey) {
        MessageDigestGenerator messageDigest = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

        if (prefixKey != null) {
            messageDigest.add(prefixKey.getBytes());
        }

        messageDigest.add(this.resource.getResourceKey().getBytes());

        ResourceContext<?> ancestor = this.parentResourceContext;
        while (ancestor != null) {
            messageDigest.add(ancestor.getResourceKey().getBytes());
            ancestor = ancestor.getParentResourceContext();
        }

        return messageDigest.getDigestString();
    }

    /**
     * Return the {@link ComponentInvocationContext} object which plugins can use to determine if the component
     * invocation has been interrupted.
     *
     * @return a {@link ComponentInvocationContext} object
     */
    public ComponentInvocationContext getComponentInvocationContext() {
        return componentInvocationContext;
    }

    /**
     * Returns the {@link String} representation of the underlying resource.
     * @return a {@link String} representation of the underlying resource
     */
    public String getResourceDetails() {
        return resource.toString();
    }
}
