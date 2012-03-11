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
package org.rhq.core.pc.inventory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jetbrains.annotations.NotNull;
import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;

/**
* @author Greg Hinkle
* @author Ian Springer
*/
public class RuntimeDiscoveryExecutor implements Runnable, Callable<InventoryReport> {
    private Log log = LogFactory.getLog(RuntimeDiscoveryExecutor.class);

    private InventoryManager inventoryManager;
    private PluginContainerConfiguration pluginContainerConfiguration;

    /**
     * Resource to scan. If null, the entire platform will be scanned.
     */
    private Resource resource;

    public RuntimeDiscoveryExecutor(InventoryManager inventoryManager,
        PluginContainerConfiguration pluginContainerConfiguration) {
        this.inventoryManager = inventoryManager;
        this.pluginContainerConfiguration = pluginContainerConfiguration;
    }

    /**
     * Creates a new <code>RuntimeDiscoveryExecutor</code> instance that will run a discovery scoped to the specified
     * agent.
     *
     * @param inventoryManager             hook back to the inventory manager
     * @param pluginContainerConfiguration configuration of this executor
     * @param resource                     scopes the runtime scan to a particular resource
     */
    public RuntimeDiscoveryExecutor(InventoryManager inventoryManager,
        PluginContainerConfiguration pluginContainerConfiguration, Resource resource) {
        this(inventoryManager, pluginContainerConfiguration);
        this.resource = resource;
    }

    public void run() {
        call();
    }

    @NotNull
    public InventoryReport call() {
        String target = (resource != null) ? this.resource.toString() : "platform";
        log.info("Executing runtime discovery scan rooted at [" + target + "]...");
        InventoryReport report = new InventoryReport(inventoryManager.getAgent());

        try {
            report.setRuntimeReport(true);
            report.setStartTime(System.currentTimeMillis());
            runtimeDiscover(report);
            report.setEndTime(System.currentTimeMillis());

            if (log.isDebugEnabled()) {
                log.debug(String.format("Runtime discovery scan took %d ms.", (report.getEndTime() - report
                    .getStartTime())));
            }

            // TODO: This is always zero for embedded because we don't populate the report.
            log.info("Scanned platform and " + report.getAddedRoots().size() + " server(s) and discovered "
                + (report.getResourceCount() - report.getAddedRoots().size()) + " new descendant Resource(s).");

            // TODO GH: This is principally valuable only until we work out the last of the data transfer situations.
            if (log.isTraceEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(10000);
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(report);
                log.trace("Runtime report contains " + report.getResourceCount() + " Resources with a size of "
                    + baos.size() + " bytes");
            }

            this.inventoryManager.handleReport(report);
        } catch (Exception e) {
            log.warn("Exception caught while executing runtime discovery scan rooted at [" + target + "].", e);
            report.addError(new ExceptionPackage(Severity.Warning, e));
        }

        return report;
    }

    private void runtimeDiscover(InventoryReport report) throws PluginContainerException {
        // Always start out by refreshing availabilities, since we will only scan servers that are available.
        this.inventoryManager.executeAvailabilityScanImmediately(true);
        if (this.resource == null) {
            // Run a full scan for all resources in the inventory
            Resource platform = this.inventoryManager.getPlatform();

            // Discover platform services here
            discoverForResource(platform, report, false);

            // Next discover all other services and non-top-level servers, recursively down the hierarchy
            discoverForResourceRecursive(platform, report);
        } else {
            // Run a single scan for just a resource and its descendants
            discoverForResource(resource, report, false);
        }

        return;
    }

    private void discoverForResourceRecursive(Resource parent, InventoryReport report) throws PluginContainerException {
        for (Resource child : parent.getChildResources()) {
            // See if the child has new children itself. Then we check those children to see if there are grandchildren.
            // Note that if the child has already been added to the report, there is no need to process it again, so skip it.
            boolean alreadyProcessed = report.getAddedRoots().contains(child);
            if (!alreadyProcessed) {
                discoverForResource(child, report, alreadyProcessed);
                // We need to recurse here even though discoverForResource recurses over child, too.
                // This is because that discovery above only goes over newly discovered resources.
                // It is possible this child has already existing children (e.g. previously manually added)
                // that they themselves might have additional new children that need discovering.
                discoverForResourceRecursive(child, report);
            }
        }

        return;
    }

    /**
     * @param  parent         The parent resource to look for children of
     * @param  report         The report to add the resource to
     * @param  parentReported true if the resources parent is already in the inventory report and therefore will include
     *                        this resource and its descendants in the report under that root
     *
     * @throws PluginContainerException on error
     */
    private void discoverForResource(Resource parent, InventoryReport report, boolean parentReported)
        throws PluginContainerException {
        // TODO GH: If resource.isRuntimeDiscoveryEnabled
        // TODO GH: If resource.isInventoryStatusCommitted

        log.debug("Discovering child Resources for " + parent + "...");

        ResourceContainer parentContainer = this.inventoryManager.getResourceContainer(parent);
        if (parentContainer == null) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot perform service scan on parent [" + parent + "] without a container");
            }
            return;
        }

        if (parentContainer.getResourceComponentState() != ResourceContainer.ResourceComponentState.STARTED) {
            if (log.isTraceEnabled()) {
                log.trace("Parent [" + parent + "] is not STARTED - not performing service scan");
            }
            return;
        }

        if (parent.getInventoryStatus() != InventoryStatus.COMMITTED) {
            if (log.isDebugEnabled()) {
                log.debug("Parent [" + parent + "] must be imported/committed before service scan can run.");
            }
            return;
        }

        ResourceComponent parentComponent = parentContainer.getResourceComponent();
        if (parentComponent == null) {
            if (log.isDebugEnabled()) {
                log.debug("Parent component for [" + parent + "] was null; cannot perform service scan.");
            }
            return;
        }

        // Do a live check of availability here. This won't set the availability anywhere but will allow us
        // to find nested resources, i.e. children of resources we've found during our recursive call 
        // to discoverForResource(). Without this live check, the availability of these newly discovered
        // resources would be null, so we would just return without checking for their children.
        AvailabilityType availability;
        ClassLoader originalCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(parentContainer.getResourceClassLoader());
            availability = parentComponent.getAvailability();
        } catch (Exception e) {
            availability = AvailabilityType.DOWN;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCL);
        }

        if (availability != AvailabilityType.UP) {
            if (log.isDebugEnabled()) {
                log.debug("Availability of [" + parent + "] is not UP, cannot perform service scan on it.");
            }
            return;
        }

        // For each child resource type of the server, do a discovery for resources of that type
        PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();
        Set<ResourceType> childResourceTypes = parent.getResourceType().getChildResourceTypes();
        for (ResourceType childResourceType : childResourceTypes) {
            try {
                // Make sure we have a discovery component for that type, otherwise there is nothing to do
                ResourceDiscoveryComponent discoveryComponent = null;
                try {
                    discoveryComponent = factory.getDiscoveryComponent(childResourceType, parentContainer);
                } catch (PluginContainerException pce) {
                    log.error("Unable to obtain discovery component for [" + childResourceType + "]", pce);
                }

                if (discoveryComponent == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resource not discoverable, no component found: " + childResourceType);
                    }
                    continue; // Assume its not discoverable
                }

                // For this resource type, discover all resources of that type on this parent resource
                if (log.isDebugEnabled()) {
                    log.debug("Running service scan on parent resource [" + parent + "] looking for children of type ["
                        + childResourceType + "]");
                }
                Set<Resource> childResources = this.inventoryManager.executeComponentDiscovery(childResourceType,
                    discoveryComponent, parentContainer, Collections.<ProcessScanResult> emptyList());

                // For each discovered resource, update it in the inventory manager and recursively discover its child resources
                Map<String, Resource> mergedResources = new HashMap<String, Resource>();

                for (Resource childResource : childResources) {
                    boolean thisInReport = false;
                    Resource mergedResource;
                    mergedResource = this.inventoryManager.mergeResourceFromDiscovery(childResource, parent);
                    mergedResources.put(mergedResource.getUuid(), mergedResource);
                    if ((mergedResource.getId() == 0) && !parentReported) {
                        report.addAddedRoot(parent);
                        thisInReport = true;
                        parentReported = true;
                    }
                    discoverForResource(mergedResource, report, thisInReport);
                }
                removeStaleResources(parent, childResourceType, mergedResources);
            } catch (Throwable t) {
                report.getErrors().add(new ExceptionPackage(Severity.Severe, t));
                log.error("Error in runtime discovery", t);
            }
        }

        return;
    }

    // TODO: Move this to InventoryManager, so it can be used by AutoDiscoveryExecutor too.
    private void removeStaleResources(Resource parent, ResourceType childResourceType,
        Map<String, Resource> mergedResources) {
        Set<Resource> existingChildResources = parent.getChildResources();
        for (Resource existingChildResource : existingChildResources) {
            // NOTE: If inside Agent, only remove Resources w/ id == 0. Other Resources may still exist in the
            //       the Server's inventory.
            if (existingChildResource.getResourceType().equals(childResourceType)
                && !mergedResources.containsKey(existingChildResource.getUuid())
                && (existingChildResource.getId() == 0 || !this.pluginContainerConfiguration.isInsideAgent())) {
                log.info("Removing stale resource [" + existingChildResource + "]");
                this.inventoryManager.removeResourceAndIndicateIfScanIsNeeded(existingChildResource);
            }
        }
    }
}