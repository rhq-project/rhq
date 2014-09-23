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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.core.clientapi.agent.discovery;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.PlatformSyncInfo;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * The interface to a RHQ Agent's Resource discovery subsystem.
 */
public interface DiscoveryAgentService {
    /**
     * This will update the plugin configuration for the resource with the given ID. This effectively will change the
     * way the resource component connects to/communicates with the resource, so it will require the resource component
     * to be restarted.
     *
     * @param  resourceId             identifies the resource whose plugin configuration is to be updated
     * @param  newPluginConfiguration the new plugin configuration
     *
     * @throws InvalidPluginConfigurationClientException if failed to update the plugin configuration or failed to
     *                                                   restart the component due to a bad plugin configuration
     * @throws PluginContainerException                  if failed for some other reason
     */
    void updatePluginConfiguration(int resourceId, Configuration newPluginConfiguration)
        throws InvalidPluginConfigurationClientException, PluginContainerException;

    /**
     * Called by the server when requesting a full platform sync.  The provided info will guide the subsequent
     * agent-initiated sync.
     *
     * @param syncInfo for the platform to be synchronized with the server.
     */
    void synchronizePlatform(PlatformSyncInfo syncInfo);

    /**
     * Called by the server to update the agent with changed for specified top level server. The agent will
     * synchronize its inventory for the server and its subtree given the provided information.
     *
     * @param syncInfo for the top level server to be synchronized with the server.
     */
    void synchronizeServer(int resourceId, Collection<ResourceSyncInfo> toplevelServerSyncInfo);

    /**
     * Access to the current inventory managed by the plugin container.
     *
     * @return the platform that is managed by this plugin container
     */
    Resource getPlatform();

    /**
     * Executes an immediate plugin discovery scan for top-level servers. This looks for servers not yet in inventory.
     *
     * @return the inventory report
     *
     * @throws PluginContainerException if the server scan fails
     */
    @NotNull
    InventoryReport executeServerScanImmediately() throws PluginContainerException;

    /**
     * Executes an immediate plugin discovery scan for services and non-top-level servers. This looks for platform
     * services and for servers and services that are children of servers or services already in inventory.
     *
     * @return the inventory report
     *
     * @throws PluginContainerException if the service scan fails
     */
    @NotNull
    InventoryReport executeServiceScanImmediately() throws PluginContainerException;

    /**
     * This method asks that a service scan be performed, but it does not wait for the results of that scan. The
     * scan is not performed if a service scan is already in progress.
     *
     * @return true if the scan is launched, false if the scan was skipped due to a scan being in progress.
     */
    boolean executeServiceScanDeferred();

    /**
     * This method asks that a service scan be performed, rooted at the specified Resource,
     * but it does not wait for the results of that scan.
     */
    void executeServiceScanDeferred(int resourceId);

    /**
     * Checks the availability of all resources and returns a report on their availability statuses. This method blocks
     * until all availabilities have been checked at which point in time the report is built and returned.
     *
     * @param  changedOnlyReport if <code>true</code>, the report returned will only contain statuses for those
     *                           resources that have changed availability status from their last known state. If <code>
     *                           false</code>, the report will contain information on all known resources (which will
     *                           make the report much larger than had <code>true</code> been passed in).
     *
     * @return the report
     */
    AvailabilityReport executeAvailabilityScanImmediately(boolean changedOnlyReport);

    /**
     * Return an availability report for the specified root resource and its descendants.
     * <p/>
     * The returned report may contain no results if {@code changesOnly} is set to true.  Otherwise it will return
     * the availability of the root resource and its descendants.  Note, a live availability check (i.e. a call
     * to getAvailability()) is always performed on the root resource.  Only descendants normally eligible for
     * availability collection at the time of this call will also have live availability. Others will report their
     * most recently reported availability.
     * <p/>
     * Also note that the availability types of the resources in the report may have any of the following values from
     * the {@link AvailabilityType} enum - it may happen that the availability of the resource is
     * {@link AvailabilityType#UNKNOWN} if the resource component is not started.
     * <p/>
     * <b>IMPORTANT:</b> The report is NOT sent up to the server as a consequence of calling this method (unlike for
     * example in the case of {@link #executeAvailabilityScanImmediately(boolean)}). The caller itself is responsible to
     * correctly handle the report within the server.
     *
     * @param resource the resource to return the availability of.
     * @param changesOnly if true, only changes in availability will be reported. if false the report will contain
     *                    the availabilities of the root resource and all descendants, whether their availability
     *                    changed or not.
     * @return an availability report populated as described in the above options.
     */
    @NotNull
    AvailabilityReport getCurrentAvailability(Resource resource, boolean changesOnly);

    /**
     * This call will request that the agent produce a full availability report on its next availability scan.
     */
    void requestFullAvailabilityReport();

    /**
     * Manually discover the resource of the specified type using the specified plugin configuration (i.e. connection
     * properties). This will not only create a new resource, but it will also ensure the resource component is
     * activated (and thus connects to the managed resource). If an error occurs, but the caller can still process the
     * results, the returned object will contain a ResourceError that is associated with the new resource (this occurs
     * when the new resource was created but its component could not be activated).
     *
     * @param  resourceType        the type of resource to be manually added
     * @param  parentResourceId    the id of the resource that will be the parent of the manually discovered resource
     * @param  pluginConfiguration the properties that should be used to connect to the underlying managed resource
     * @param  creatorSubjectId    the {@link org.rhq.core.domain.auth.Subject} id of the JON user that requested the
     *                             addition of the resource
     *
     * @return the newly discovered resource with any associated {@link ResourceError} that might have occurred during
     *         the activation of the resource
     *
     * @throws InvalidPluginConfigurationClientException if connecting to the underlying managed resource failed due to
     *                                                   an invalid plugin configuration.
     * @throws PluginContainerException                  if the manual discovery fails for any other reason
     */
    MergeResourceResponse manuallyAddResource(ResourceType resourceType, int parentResourceId,
        Configuration pluginConfiguration, int creatorSubjectId) throws InvalidPluginConfigurationClientException,
        PluginContainerException;

    /**
     * Shuts down and removes a Resource and its descendants from the PC's inventory.
     *
     * @param resourceId the id of the Resource to remove
     */
    void uninventoryResource(int resourceId);

    // TODO GH: Everything below here is not used yet
    /**
     * Enable periodic scans for new services for the specified server, using the specified discovery configuration. If
     * service scans were already enabled, the server's discovery configuration is updated on the agent.
     *
     * @param serverResourceId resource id of the server
     * @param config           discovery configuration for the server
     */
    void enableServiceScans(int serverResourceId, Configuration config);

    /**
     * Disable periodic scans for new services for the specified server. If service scans were not enabled, this method
     * is a no-op.
     *
     * @param serverResourceId resource id of the server
     */
    void disableServiceScans(int serverResourceId);
}
