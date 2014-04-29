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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;

/**
 * The remote boss interface to the discovery subsystem.
 */
@Remote
public interface DiscoveryBossRemote {

    /**
     * Analogous to the GUI feature Import Resources in the auto discovery queue.
     * Note, to query for Resources with a specific InventoryStatus, see
     * {@link org.rhq.enterprise.server.resource.ResourceManagerRemote#findResourcesByCriteria}.
     *
     * @param subject
     * @param resourceIds
     */
    void importResources(Subject subject, int[] resourceIds);

    /**
     * Analogous to the GUI feature Ignore Resources in the auto discovery queue. This can also
     * ignore committed resources as well.
     * Note, to query for Resources with a specific InventoryStatus, see
     * {@link org.rhq.enterprise.server.resource.ResourceManagerRemote#findResourcesByCriteria}.
     *
     * @param subject
     * @param resourceIds
     */
    void ignoreResources(Subject subject, int[] resourceIds);

    /**
     * Analogous to the GUI feature Unignore Resources in the auto discovery queue.
     * Note, to query for Resources with a specific InventoryStatus, see
     * {@link org.rhq.enterprise.server.resource.ResourceManagerRemote#findResourcesByCriteria}.
     *
     * @param subject
     * @param resourceIds
     */
    void unignoreResources(Subject subject, int[] resourceIds);

    /**
     * This is used to specifically unignore previously ignored resources. This has the added feature
     * of immediately importing those newly unignored resources. This should only be used for unignoring
     * those resources that were previously committed but ignored.
     *
     * Note, to query for Resources with a specific InventoryStatus, see
     * {@link org.rhq.enterprise.server.resource.ResourceManagerRemote#findResourcesByCriteria}.
     *
     * @param subject
     * @param resourceIds
     *
     * @since 4.7
     */
    void unignoreAndImportResources(Subject subject, int[] resourceIds);

    /**
     * Manually Add the resource of the specified type to inventory using the specified plugin configuration (i.e.
     * connection properties). This will not only create a new resource, but it will also ensure the resource component
     * is activated (and thus connects to the managed resource).
     *
     * @param  subject             the user making the request
     * @param  resourceTypeId      the type of resource to be manually discovered
     * @param  parentResourceId    the id of the resource that will be the parent of the manually discovered resource
     * @param  pluginConfiguration the properties that should be used to connect to the underlying managed resource
     *
     * @return The resource. Note that the resource may have existed already if given the provided pluginConfiguration
     *         leads to a previously defined resource.
     *
     * @throws Exception if connecting to the underlying managed resource failed due to invalid plugin configuration or
     *                   if the manual discovery fails for any reason.
     */
    Resource manuallyAddResource(Subject subject, int resourceTypeId, int parentResourceId,
        Configuration pluginConfiguration) throws Exception;

}