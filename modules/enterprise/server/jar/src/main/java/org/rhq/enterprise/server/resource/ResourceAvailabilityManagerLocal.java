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

import javax.ejb.Local;

import org.rhq.core.domain.measurement.ResourceAvailability;

/**
 * A manager that provides methods for manipulating and querying the cached current availability for resources.
 *
 * @author Joseph Marques
 */
@Local
public interface ResourceAvailabilityManagerLocal {

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