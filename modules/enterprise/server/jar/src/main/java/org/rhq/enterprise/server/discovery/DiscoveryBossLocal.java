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

package org.rhq.enterprise.server.discovery;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.discovery.InvalidPluginConfigurationClientException;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InvalidInventoryReportException;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.MergeInventoryReportResults;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.PlatformSyncInfo;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.ImportResourceRequest;
import org.rhq.core.domain.resource.ImportResourceResponse;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.discovery.DiscoveryBossBean.PostMergeAction;

/**
 * The boss interface to the discovery subsystem.
 */
@Local
public interface DiscoveryBossLocal extends DiscoveryBossRemote {
    /**
     * When agents send up results from their discovery components (which notifies the server about newly discovered
     * resources), this method will eventually be called in order to process those inventory reports.
     *
     * @param  report the inventory report to be merged
     *
     * @return the server's response, which will include the information necessary for the agent to
     *         start synchronizing its inventory with the server's inventory. This can return null in one specific
     *         case - if this is a brand new agent and it is currently initializing for the very first time.
     * @throws InvalidInventoryReportException if the inventory report is invalid
     */
    MergeInventoryReportResults mergeInventoryReport(InventoryReport report) throws InvalidInventoryReportException;

    /**
     * <p>Exists for transactional boundary reasons only.</p>
     *
     * Merge In the provided batch of resources.  The list of resources must provide a parent before its child.
     *
     * @param resourceBatch a batch of resources
     * @param agent the agent managing the resources
     * @param postMergeActions a series of actions to perform on newly committed resources.
     *
     * @throws InvalidInventoryReportException
     */
    void mergeResourceInNewTransaction(List<Resource> resourceBatch, Agent agent,
        Map<Resource, Set<PostMergeAction>> postMergeActions)
        throws InvalidInventoryReportException;

    /**
     * Just get the top level server info for the agent's platform.  Then, each top level server
     * can be individually synced
     * @param knownAgent the agent for the platform we want to sync with
     * @return null if platform not found
     */
    PlatformSyncInfo getPlatformSyncInfo(Agent knownAgent);

    /**
     * @param resourceId the root resourceId on which we want to sync
     * @return null if resource not found, otherwise the entire tree rooted at the specified resource, as an
     * unordered collection.  Although not strictly a Set (to save on computation) this collection should not
     * contain duplicates.
     */
    Collection<ResourceSyncInfo> getResourceSyncInfo(int resourceId);

    /**
     * Returns a map of platforms (the keys) and their servers (the values) that are in the auto-discovery queue but not
     * yet imported into inventory. Note that only servers whose direct parent is the platform will appear in the
     * returned data. Embedded servers (i.e. servers that are children of other servers) will be automatically imported
     * when you import their parent server.
     *
     * @param  user the user that wants to see the data
     * @param  pc   used to define the size of the returned map - will determine how many platforms are returned
     *
     * @return the platforms and servers that need to be imported or ignored
     * @deprecated used only by portal war, should be removed when possible.
     */
    @Deprecated
    Map<Resource, List<Resource>> getQueuedPlatformsAndServers(Subject user, PageControl pc);

    /**
     * Like the above method, but can find ignored, committed or both
     * @param user the subject
     * @param statuses the inventory status'
     * @param pc page control
     * @return the queued platforms and servers
     * @deprecated unused, remove when doing a deprecation cleanup
     */
    @Deprecated
    Map<Resource, List<Resource>> getQueuedPlatformsAndServers(Subject user, EnumSet<InventoryStatus> statuses,
        PageControl pc);

    /**
     * This returns all platform resources that either have the given status themselves or one or more of their child
     * servers have that status. Use this to find those platforms that need to be shown in the auto-discovery pages
     * (i.e. those that need to be committed to inventory). Can use this to find platforms that have servers that are
     * ignored.
     *
     * @param  user     the user that wants to see the data
     * @param  statuses the statuses that platform or its child servers must have
     * @param  pc       pagination controls
     *
     * @return the platforms
     *
     * @see    #getQueuedPlatformChildServers
     */
    PageList<Resource> getQueuedPlatforms(Subject user, EnumSet<InventoryStatus> statuses, PageControl pc);

    /**
     * Given a platform resource, this returns all of its child server resources that have been auto-discovered and have
     * the given status.
     *
     * @param  user     the user that wants to see the data
     * @param  status   the status that platform or its child servers must have
     * @param  platform the resource whose auto-discovered child servers must have the given status
     *
     * @return the give platform's top-level server children that have the given status
     */
    List<Resource> getQueuedPlatformChildServers(Subject user, InventoryStatus status, Resource platform);

    /**
     * This method is used to change the inventory status of a set of platforms and servers (e.g. when users import new
     * resources into inventory).
     *
     * @param user      the user that wants to change the status
     * @param platforms identifies the platforms that are to be updated
     * @param servers   identifies the servers that are to be updated
     * @param status    the new status the given resources will have
     */
    void updateInventoryStatus(Subject user, List<Resource> platforms, List<Resource> servers, InventoryStatus status);

    /**
     * This is used internally. Never call this yourself without knowing what you do. See
     * {@link #updateInventoryStatus(Subject, List, List, InventoryStatus)} for the "public" version.
     *
     * @param user      the user that wants to change the status
     * @param platforms identifies the platforms that are to be updated
     * @param servers   identifies the servers that are to be updated
     * @param status    the new status the given resources will have
     */
    void updateInventoryStatusInNewTransaction(Subject user, List<Resource> platforms, List<Resource> servers,
        InventoryStatus status);

    /**
     * Manually Add the resource to inventory using the type and plugin configuration (i.e. connection properties)
     * specified in <code>importResourceRequest</code>. This will not only create a new resource, but it will also
     * ensure the resource component is activated (and thus connects to the managed resource).
     *
     *
     * @param  subject              the user making the request
     * @param importResourceRequest the request
     *
     * @return The response. Note that the resource may have existed already if given the provided pluginConfiguration
     *         leads to a previously defined resource.
     *
     * @throws InvalidPluginConfigurationClientException if connecting to the underlying managed resource failed due to
     *                                                   an invalid plugin configuration
     * @throws PluginContainerException                  if the manual discovery fails for any other reason
     */
    ImportResourceResponse manuallyAddResource(Subject subject, ImportResourceRequest importResourceRequest)
        throws InvalidPluginConfigurationClientException, PluginContainerException;

    /**
     * Adds the specified resource to inventory, *auto-committing it*.
     *
     * @param  resource       the resource to be merged
     * @param  ownerSubjectId the user who should be the owner of the new Resource
     *
     * @return a response containing the merged resource, as well as whether the resource already existed in inventory
     */
    MergeResourceResponse addResource(Resource resource, int ownerSubjectId);

    /**
     * Updates the version of the specified Resource in inventory, if it is indeed in inventory.
     * If the resource is already in inventory and its version is already <code>version</code>, then
     * this method does nothing and returns <code>true</code>.
     *
     * @param resourceId the id of the Resource to be updated
     * @param version the new version
     *
     * @return <code>true</code> if the Resource was in inventory and its version is now that of <code>version</code>.
     *         <code>false</code> if the Resource was not in inventory
     */
    boolean updateResourceVersion(int resourceId, String version);

    /**
     * Upgrades the data of the resources according to the provided reports.
     * The server is free to ignore or modify the requests and will provide the
     * true changes made to the resources on the server-side in the result of this method.
     *
     * @param upgradeRequests contains the information about the upgrade of individual resources.
     * @return details on what resources have been upgraded with what data.
     */
    Set<ResourceUpgradeResponse> upgradeResources(Set<ResourceUpgradeRequest> upgradeRequests);

    void updateAgentInventoryStatus(String platformsList, String serversList);
}
