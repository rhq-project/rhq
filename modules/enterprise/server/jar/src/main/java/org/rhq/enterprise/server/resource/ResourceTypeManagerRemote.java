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

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;

/**
 * ResourceType remote API.
 */
@Remote
public interface ResourceTypeManagerRemote {

    /**
     * Given a specific resource type ID, this will indicate if that type (and resources of that type)
     * are to be ignored or not. If the type is to be ignored (ignoreFlag == true), then all resources
     * that are already in inventory of that type will be uninventoried.
     *
     * @param subject user making the request
     * @param resourceTypeId the type to change
     * @param ignoreFlag true if the type (and resources of that type) are to be ignored.
     *
     * @since 4.7
     */
    void setResourceTypeIgnoreFlagAndUninventoryResources(Subject subject, int resourceTypeId, boolean ignoreFlag);

    /**
     * @param subject
     * @param resourceTypeId
     * @return the type
     * @throws ResourceTypeNotFoundException
     */
    ResourceType getResourceTypeById(Subject subject, int resourceTypeId) throws ResourceTypeNotFoundException;

    /**
     * @param subject
     * @param name
     * @param plugin
     * @return the resource type by name and plugin or null if the type is not found
     */
    ResourceType getResourceTypeByNameAndPlugin(Subject subject, String name, String plugin);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<ResourceType> findResourceTypesByCriteria(Subject subject, ResourceTypeCriteria criteria);
}