/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
import java.util.Map;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.composite.ResourceAvailabilitySummary;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.PermissionException;

/**
 * @author Jay Shaughnessy
 * @author Simeon Pinder
 * @author Asaf Shakarchi
 */
@Remote
public interface ResourceManagerRemote {

    /**
     * Returns a summary object that provides information about a resource's
     * past availability history.
     *
     * @param subject
     * @param resourceId
     *
     * @return summary POJO
     */
    ResourceAvailabilitySummary getAvailabilitySummary(Subject subject, int resourceId);

    /**
     * Returns the availability of the resource with the specified id.
     * This performs a live check - a resource will be considered UNKNOWN if the agent
     * cannot be contacted for any reason.
     *
     * @param  subject The logged in user's subject.
     * @param  resourceId the id of a {@link Resource} in inventory.
     *
     * @return the resource availability - note that if the encapsulated availability type is <code>null</code>,
     *         the resource availability is UNKNOWN. As of RHQ 4.4 this does not return null but rather
     *         {@link AvailabilityType#UNKNOWN}.
     */
    ResourceAvailability getLiveResourceAvailability(Subject subject, int resourceId);

    /**
     * Returns the Resource with the specified id.
     *
     * @param  subject The logged in user's subject.
     * @param  resourceId the id of a {@link Resource} in inventory.
     *
     * @return the resource
     * @throws ResourceNotFoundException if the resource represented by the resourceId parameter does not exist.
     * @throws PermissionException if the user does not have view permission for the resource
     */
    Resource getResource(Subject subject, int resourceId) throws ResourceNotFoundException, PermissionException;

    /**
     * Returns the lineage of the Resource with the specified id. The lineage is represented as a List of Resources,
     * with the first item being the root of the Resource's ancestry (or the Resource itself if it is a root Resource
     * (i.e. a platform)) and the last item being the Resource itself. Since the lineage includes the Resource itself,
     * the returned List will always contain at least one item.
     *
     * @param  subject The logged in user's subject.
     * @param  resourceId the id of a {@link Resource} in inventory
     *
     * @return the lineage of the Resource with the specified id
     * @throws ResourceNotFoundException if the resource represented by the resourceId parameter does not exist.
     * @throws PermissionException if the user does not have view permission for a resource in the lineage
     */
    List<Resource> findResourceLineage(Subject subject, int resourceId);

    /**
     * Update resource's editable properties (name, description, location).
     *
     * @param subject the logged in user
     * @param resource the resource to update
     * @return the updated resource
     */
    Resource updateResource(Subject subject, Resource resource);

    /**
     * This will uninventory all resources managed by the given agent. Since this
     * also removes the platform resource, this will also remove the given agent as well.
     *
     * @param user the logged in user
     * @param doomedAgent the agent to be deleted and whose resources are to be uninventoried
     *
     * @since 4.7
     */
    void uninventoryAllResourcesByAgent(Subject user, Agent doomedAgent);

    /**
     * Given a specific resource type, this will uninventory all resources of that type.
     *
     * @param subject the logged in user
     * @param resourceTypeId identifies the type whose resources are to be uninventoried
     *
     * @since 4.7
     */
    void uninventoryResourcesOfResourceType(Subject subject, int resourceTypeId);

    /**
     * Removes these resources from inventory.  The resources may subsequently be rediscovered.  Note that for
     * each specified resource all children will also be removed, it it not necessary or recommended to
     * specify more than one resource in the same ancestry line.
     *
     * @param subject The logged in user's subject.
     * @param resourceIds The resources to uninventory.
     * @return the resource ids of the uninventoried resources
     */
    List<Integer> uninventoryResources(Subject subject, int[] resourceIds);

    /**
     * @param subject
     * @param criteria
     * @return not null
     */
    PageList<Resource> findResourcesByCriteria(Subject subject, ResourceCriteria criteria);

    /**
     * @param subject
     * @param resourceId
     * @param pageControl
     * @return not null
     */
    PageList<Resource> findChildResources(Subject subject, int resourceId, PageControl pageControl);

    /**
     * @param subject
     * @param resourceId
     * @return the parent resource or null if the resource has no parent
     * @throws ResourceNotFoundException  if the resource does not exist
     * @throws PermissionException if the user does not have view permission for a resource in the lineage
     */
    Resource getParentResource(Subject subject, int resourceId) throws ResourceNotFoundException, PermissionException;

    /**
     * Resource.ancestry is an encoded value that holds the resource's parental ancestry. It is not suitable for display.
     * This method can be used to get decoded and formatted ancestry values for a set of resources.  A typical usage
     * would a criteria resource fetch, and then a subsequent call to this method for ancestry display, potentially
     * for resource disambiguation purposes.
     *
     * @param subject
     * @param resourceIds
     * @param format
     * @return A Map of ResourceIds to FormattedAncestryStrings, one entry for each unique, valid, resourceId passed in.
     */
    Map<Integer, String> getResourcesAncestry(Subject subject, Integer[] resourceIds, ResourceAncestryFormat format);

    /**
     * Set these resources to {@link AvailabilityType#DISABLED}. While disabled resource availability reported
     * from the agent is ignored.  This is typically used for resources undergoing scheduled maintenance or
     * whose avail state should be disregarded for some period.
     * <br/><br/>
     * The calling user must possess {@link Permission#DELETE_RESOURCE} permission on all of the provided resources.
     * <br/><br/>
     * Resources already disabled are ignored.
     *
     * @param subject The logged in user's subject.
     * @param resourceIds The resources to disable.
     * @return the disabled resource ids, not null
     *
     * @see #enableResources(Subject, int[])
     */
    List<Integer> disableResources(Subject subject, int[] resourceIds);

    /**
     * Set these resources enabled. Resources already enabled are ignored. The availability will be set to UNKNOWN
     * until the agent reports a new, live, availability. The agent will be requested to check availability
     * for the enabled resources at its earliest convenience.
     * <br/><br/>
     * The calling user must possess {@link Permission#DELETE_RESOURCE} permission on all of the provided resources.
     *
     * @param subject The logged in user's subject.
     * @param resourceIds The resources to enable.
     * @return the enable resource ids, not null
     *
     * @see #disableResources(Subject, int[])
     */
    List<Integer> enableResources(Subject subject, int[] resourceIds);

}