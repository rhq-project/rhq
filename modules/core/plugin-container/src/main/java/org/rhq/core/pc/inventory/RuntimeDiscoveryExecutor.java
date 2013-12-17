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
import org.rhq.core.clientapi.agent.metadata.ResourceTypeNotEnabledException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.PluginContainerConfiguration;
import org.rhq.core.pc.plugin.PluginComponentFactory;
import org.rhq.core.pluginapi.availability.AvailabilityFacet;
import org.rhq.core.pluginapi.inventory.ProcessScanResult;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.util.exception.ExceptionPackage;
import org.rhq.core.util.exception.Severity;

/**
 * This should probably be renamed to ServiceDiscoveryExecutor or maybe ChildDiscoveryExecutor.  It is responsible for
 * discovering children of existing resources.  It recursively walks the hierarchy looking for new resources, which
 * are typically services (but could be non-top-level servers).  It is complemented by {@link AutoDiscoveryExecutor}
 * which looks for new top level servers.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class RuntimeDiscoveryExecutor implements Runnable, Callable<InventoryReport> {
    private Log log = LogFactory.getLog(RuntimeDiscoveryExecutor.class);

    private final InventoryManager inventoryManager;
    private final PluginContainerConfiguration pluginContainerConfiguration;

    /**
     * Resource to scan. If null, the entire platform will be scanned.
     */
    private Resource rootResource;

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
     * @param rootResource                 scopes the runtime scan to a particular resource
     */
    public RuntimeDiscoveryExecutor(InventoryManager inventoryManager,
        PluginContainerConfiguration pluginContainerConfiguration, Resource rootResource) {
        this(inventoryManager, pluginContainerConfiguration);
        this.rootResource = rootResource;
    }

    public void run() {
        call();
    }

    @NotNull
    public InventoryReport call() {
        String target = (rootResource != null) ? this.rootResource.toString() : "platform";
        log.info("Executing runtime discovery scan rooted at [" + target + "]...");
        InventoryReport report = new InventoryReport(inventoryManager.getAgent());

        try {
            report.setRuntimeReport(true);
            report.setStartTime(System.currentTimeMillis());
            runtimeDiscover(report);
            report.setEndTime(System.currentTimeMillis());

            if (log.isDebugEnabled()) {
                log.debug(String.format("Runtime discovery scan took %d ms.",
                    (report.getEndTime() - report.getStartTime())));
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

        if (this.rootResource == null) {
            // Run a full scan for all resources in the inventory
            Resource platform = this.inventoryManager.getPlatform();

            // Discover platform services here
            discoverForResource(platform, report, false);

        } else {
            // Run a single scan for just a resource and its descendants
            discoverForResource(rootResource, report, false);
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

        // For each child resource type of the server, do a discovery for resources of that type
        Set<ResourceType> childResourceTypes = parent.getResourceType().getChildResourceTypes();
        if (null == childResourceTypes || childResourceTypes.isEmpty()) {
            // I'm not sure it's possible, but just in case, make sure it doesn't have children. If it does, keep going
            if (parent.getChildResources().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("Parent resource type [" + parent + "] has no child types; cannot perform service scan.");
                }
                return;
            }
        }

        // At this point we used to always do a live check of availability.  This buys us very little and
        // costs us a lot.  For a platform-rooted scan this ends up being an avail check for all but leaf
        // nodes of the tree.  That is costly on top of the discovery check itself, and is antithetical to
        // the whole staggered avail-check approach we now have in place.  We can't even update the container
        // with the avail check result, because all changes in avail need to be detected and reported by the
        // AvailabilityExecutor.  We did the avail check for two reasons.  If there is no current avail we
        // need to establish one because it may be for a resource newly discovered by this scan, and we need to know
        // if we can in turn perform discovery on it.  We still need to do this check.  The second was to perform
        // discovery only on UP resources.  We can keep this logic but just use the current availability
        // stored in the container.  It may be stale, but it is likely valid, as avail does not often change.
        // An argument could be made to always use the currently stored avail, but currently we've decided
        // to still perform the check in two cases: if the current avail is not UP or if the resource category is
        // SERVER.  This means we won't miss an opportunity to do discovery for stale DOWN resource, and we won't
        // waste time doing discovery on a stale UP SERVER, which can be time consuming.  Since most resources are
        // SERVICEs, and also are typically UP and stay UP, performing checks in these two situations should
        // not add much overhead. Finally, make sure to use facet proxy to do the avail check, this allows us to use
        // a timeout, and therefore not hang discovery if the avail check is slow.
        Availability currentAvailability = parentContainer.getAvailability();
        AvailabilityType currentAvailabilityType = (null == currentAvailability) ? AvailabilityType.DOWN
            : currentAvailability.getAvailabilityType();

        // If there is no current avail, or this is a SERVER, we must perfom the live check.
        if (AvailabilityType.UP != currentAvailabilityType
            || ResourceCategory.SERVER == parentContainer.getResource().getResourceType().getCategory()) {

            AvailabilityFacet parentAvailabilityProxy = parentContainer.getAvailabilityProxy();

            try {
                currentAvailabilityType = parentAvailabilityProxy.getAvailability();
            } catch (Exception e) {
                currentAvailabilityType = AvailabilityType.DOWN;
            }
        }

        if (AvailabilityType.UP != currentAvailabilityType) {
            if (log.isDebugEnabled()) {
                log.debug("Availability of [" + parent + "] is not UP, cannot perform service scan on it.");
            }
            return;
        }

        PluginComponentFactory factory = PluginContainer.getInstance().getPluginComponentFactory();

        try {

            for (ResourceType childResourceType : childResourceTypes) {
                // Make sure we have a discovery component for that type, otherwise there is nothing to do
                ResourceDiscoveryComponent discoveryComponent = null;
                try {
                    discoveryComponent = factory.getDiscoveryComponent(childResourceType, parentContainer);
                } catch (ResourceTypeNotEnabledException rtne) {
                    if (log.isDebugEnabled()) {
                        log.debug("Resource not discoverable, type is disabled: " + childResourceType);
                    }
                    continue; // do not discovery anything for this component
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
                Set<Resource> discoveredChildResources = this.inventoryManager
                    .executeComponentDiscovery(childResourceType, discoveryComponent, parentContainer,
                        Collections.<ProcessScanResult> emptyList());

                // For each discovered child resource, update it in the inventory manager
                Map<String, Resource> mergedResources = new HashMap<String, Resource>();
                for (Resource discoveredChildResource : discoveredChildResources) {
                    Resource mergedResource;
                    mergedResource = this.inventoryManager.mergeResourceFromDiscovery(discoveredChildResource, parent);
                    mergedResources.put(mergedResource.getUuid(), mergedResource);
                    if ((mergedResource.getId() == 0) && !parentReported) {
                        report.addAddedRoot(parent);
                        parentReported = true;
                    }
                }

                // get rid of any child resources of this type that were not yet committed and are now gone
                removeStaleResources(parent, childResourceType, mergedResources);

            }

            // now, recursively perform discovery on all of the parent's children, which includes the newly
            // merged children as well as previously existing children.
            for (Resource childResource : parent.getChildResources()) {
                discoverForResource(childResource, report, parentReported);
            }

        } catch (Throwable t) {
            report.getErrors().add(new ExceptionPackage(Severity.Severe, t));
            log.error("Error in runtime discovery", t);
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