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
package org.rhq.enterprise.server.resource;

import java.util.List;

import javax.ejb.Local;
import javax.persistence.EntityManager;
import javax.persistence.PostPersist;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.discovery.InventoryReport;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.enterprise.server.discovery.DiscoveryBossBean;

/**
 * A manager that provides methods for manipulating and querying the cached current availability for resources.
 *
 * @author Joseph Marques
 */
@Local
public interface ResourceAvailabilityManagerLocal {

    /**
     * The first time an agent is started and its platform and top-level servers are discovered,
     * the {@link DiscoveryBossBean#mergeInventoryReport(InventoryReport)} will use the 
     * {@link EntityManager} to persist the resource.  A {@link PostPersist} hook exists on the
     * {@link Resource} entity to create a corresponding default {@link ResourceAvailability}
     * entity.  However, when a platform or top-level server is removed from inventory, the agent
     * might rediscover the resource so quickly that {@link InventoryReport} merges the resource
     * instead of persisting new ones, bypassing the {@link PostPersist} hook.  As a result, this
     * method should be called when resources are imported from the auto-discovery portlet (the
     * {@link InventoryStatus} is changed from NEW to COMMITTED, which will add the necessary
     * default {@link ResourceAvailability} objects to those resources missing them.
     * 
     * @param resourceIds a list of resource ids which should have default {@link ResourceAvailability}
     *                    objects created for them, only if the corresponding data doesn't already exist.
     */
    void insertNeededAvailabilityForImportedResources(List<Integer> resourceIds);

    /**
     * Returns the latest availability type for the given resource.
     * This tells you the currently known state of a resource - whether
     * it is UP or DOWN.
     * 
     * @param whoami the user asking for the data
     * @param resourceId the id of the resource
     * @return the latest availability type for the given resource, <code>null</code> if not known
     */
    AvailabilityType getLatestAvailabilityType(Subject whoami, int resourceId);

    /**
     * Returns the latest availability for the given Resource
     * 
     * @param resourceId the id of the resource
     * @return the latest availability for the given Resource
     */
    ResourceAvailability getLatestAvailability(int resourceId);

    /**
     * Marks all resources managed by the specified agent as down
     * 
     * @param agentId the id of the agent
     */
    void markResourcesDownForAgent(int agentId);
}