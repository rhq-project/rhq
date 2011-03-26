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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.jaxb.adapter.ResourceListAdapter;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * @author Asaf Shakarchi
 * @author Jay Shaughnessy 
 * @author Simeon Pinder
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ResourceManagerRemote {

    /**
     * Returns the availability of the resource with the specified id.
     * This performs a live check - a resource will be considered UNKNOWN if the agent
     * cannot be contacted for any reason.
     *
     * @param  subject The logged in user's subject.
     * @param  resourceId the id of a {@link Resource} in inventory.
     *
     * @return the resource availability - note that if the encapsulated availability type is <code>null</code>,
     *         the resource availability is UNKNOWN.
     *
     * @throws FetchException if the resource represented by the resourceId parameter does not exist, or if the
     *                        passed subject does not have permission to view this resource.
     */
    @WebMethod
    ResourceAvailability getLiveResourceAvailability( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Returns the Resource with the specified id.
     *
     * @param  subject The logged in user's subject.
     * @param  resourceId the id of a {@link Resource} in inventory.
     *
     * @return the resource
     * @throws FetchException if the resource represented by the resourceId parameter does not exist, or if the
     *                        passed subject does not have permission to view this resource.
     */
    @WebMethod
    Resource getResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

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
     * @throws FetchException on any issue. Wraps ResourceNotFoundException when necessary. 
     */
    @WebMethod
    @XmlJavaTypeAdapter(value = ResourceListAdapter.class)
    List<Resource> findResourceLineage( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Update resource's editable properties (name, description, location).
     * 
     * @param user the logged in user
     * @param resource the resource to update
     * @return the updated resource
     */
    @WebMethod
    Resource updateResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resource") Resource resource);

    /**
     * Removes these resources from inventory.  The resources may subsequently be rediscovered.  Note that for
     * each specified resource all children will also be removed, it it not necessary or recommended to
     * specify more than one resource in the same ancestry line.
     * 
     * @param subject The logged in user's subject.
     * @param resourceIds The resources to uninventory.
     */
    @WebMethod
    List<Integer> uninventoryResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds);

    @WebMethod
    PageList<Resource> findResourcesByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceCriteria criteria);

    @WebMethod
    PageList<Resource> findChildResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "pageControl") PageControl pageControl);

    @WebMethod
    Resource getParentResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

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
    @WebMethod
    Map<Integer, String> getResourcesAncestry( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") Integer[] resourceIds, //
        @WebParam(name = "format") ResourceAncestryFormat format);

}