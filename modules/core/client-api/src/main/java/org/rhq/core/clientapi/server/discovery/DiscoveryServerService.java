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
package org.rhq.core.clientapi.server.discovery;

import java.util.Map;
import java.util.Set;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.communications.command.annotation.Timeout;
import org.rhq.core.domain.discovery.AvailabilityReport;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeRequest;
import org.rhq.core.clientapi.agent.upgrade.ResourceUpgradeResponse;
import org.rhq.core.clientapi.server.discovery.InventoryReport;
import org.rhq.core.domain.discovery.MergeResourceResponse;
import org.rhq.core.domain.discovery.ResourceSyncInfo;
import org.rhq.core.domain.measurement.ResourceMeasurementScheduleRequest;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.ResourceErrorType;

/**
 * The interface to a JON server's resource discovery subsystem.
 */
public interface DiscoveryServerService {
    String CONCURRENCY_LIMIT_INVENTORY_REPORT = "rhq.server.concurrency-limit.inventory-report";
    String CONCURRENCY_LIMIT_AVAILABILITY_REPORT = "rhq.server.concurrency-limit.availability-report";
    String CONCURRENCY_LIMIT_INVENTORY_SYNC = "rhq.server.concurrency-limit.inventory-sync";

    /**
     * Merge the platform/servers/services contained in the specified inventory report into the server's inventory. Note
     * that the plugin container will use this method to send the results of both platform/server scans and service
     * scans. In the case of the former, the server will queue the inventory updates and require the JON administrator
     * to approve them before merging them into inventory.
     *
     * @param  inventoryReport a report containing updated inventory data
     *
     * @return response that contains information the plugin container will need in order to sync itself up with new
     *         data that the server had to create in order to merge the report into its inventory
     *
     * @throws InvalidInventoryReportException if the inventory report contains invalid data
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_INVENTORY_REPORT)
    @Timeout(1000L * 60 * 30)
    ResourceSyncInfo mergeInventoryReport(InventoryReport inventoryReport) throws InvalidInventoryReportException;

    /**
     * Merges a new availability report from the agent into the server. This updates the availability statuses of known
     * resources.
     *
     * @param  availabilityReport report containing updated availability statuses for a set of resources
     *
     * @return If <code>true</code>, this indicates everything seems OK - the server merged everything successfully and
     *         the server and agent seem to be in sync with each. If <code>false</code>, the server thinks something
     *         isn't right and it may be out of sync with the agent. When <code>false</code> is returned, the caller
     *         should send a <i>full</i> availability report the next time in order to ensure the server and agent are
     *         in sync. <code>true</code> should always be returned if the given availability report is already a full
     *         report.
     */
    // GH: Disabled temporarily (JBNADM-2385) @Asynchronous( guaranteedDelivery = true )
    @LimitedConcurrency(CONCURRENCY_LIMIT_AVAILABILITY_REPORT)
    boolean mergeAvailabilityReport(AvailabilityReport availabilityReport);

    /**
     * Returns the Resources with the given id's, optionally including all descendant Resources.
     *
     * @param resourceIds
     * @param includeDescendants
     * @return a tree of resources with the latest data
     */
    Set<Resource> getResources(Set<Integer> resourceIds, boolean includeDescendants);

    /**
     * Indicates that an error occurred on a resource.
     *
     * @param resourceError all information about the error that occurred
     */
    @Asynchronous(guaranteedDelivery = false)
    void setResourceError(ResourceError resourceError);

    /**
     * Clears errors of type {@link ResourceErrorType}.INVALID_PLUGIN_CONFIGURATION
     * @param resourceId id of the resource
     */
    @Asynchronous(guaranteedDelivery = true)
    void clearResourceConfigError(int resourceId);

    /**
     * Retrieve a set of inventory statuses for a resource and potentially its descendants.
     *
     * @param  rootResourceId a {@link Resource} id
     * @param  descendants    true if the resource's descendants should be included, or false if not
     *
     * @return a map of the resourceId to the inventory status
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_INVENTORY_SYNC)
    Map<Integer, InventoryStatus> getInventoryStatus(int rootResourceId, boolean descendants);

    /**
     * Merges the specified resource into inventory.
     *
     * @param  resource         the resource to be merged
     * @param  creatorSubjectId the {@link org.rhq.core.domain.auth.Subject} id of the JON user that requested the
     *                          addition of the resource
     *
     * @return a response containing the merged resource, as well as whether the resource already existed in inventory
     */
    MergeResourceResponse addResource(Resource resource, int creatorSubjectId);

    /**
     * Updates the version of the specified Resource in inventory, if it is indeed in inventory.
     *
     * @param resourceId the id of the Resource to be updated
     * @param version the new version
     *
     * @return true if the Resource was updated, or false if the Resource was not in inventory
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
    
    /**
     * Gives the server a chance to apply any necessary post-processing that's needed for newly committed resources
     * that have been successfully synchronized on the agent.
     *   
     * @param  resourceIds a collection of{@link Resource} ids that have been newly committed and successfully
     *                     synchronized on the agent
     *
     * @return the current list of measurement schedules that should be installed agent side for each resource contained
     *         within the passed set
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_INVENTORY_SYNC)
    Set<ResourceMeasurementScheduleRequest> postProcessNewlyCommittedResources(Set<Integer> resourceIds);
}