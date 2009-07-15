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
package org.rhq.enterprise.server.resource.group;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.CreateException;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;
import org.rhq.enterprise.server.exception.UpdateException;

/**
 * @author Jay Shaughnessy 
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface ResourceGroupManagerRemote {

    //optional data for findResourceGroups and findResourceGroupComposites
    static public final String DATA_IMPLICIT_RESOURCES = "implicitResource";
    static public final String DATA_EXPLICIT_RESOURCES = "explicitResources";
    static public final String DATA_ROLES = "roles";

    @WebMethod
    void addResourcesToGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "resourceIds") int[] resourceIds) throws UpdateException;

    /**
     * 
     * @param subject
     * @param resourceGroup
     * @throws CreateException May wrap (ResourceGroupNotFoundException, ResourceGroupAlreadyExistsException)
     */
    @WebMethod
    ResourceGroup createResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceGroup") ResourceGroup resourceGroup) throws CreateException;

    /**
     * 
     * @param subject
     * @param groupId
     * @throws DeleteException Possible Causes (ResourceGroupNotFoundException, ResourceGroupDeleteException)
     */
    @WebMethod
    void deleteResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId) throws DeleteException;

    @WebMethod
    ResourceGroup getResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId) throws FetchException;

    /**
     * Get the ResourceGroup and Availability by id.
     *
     * @param  subject {@link Subject} of the calling user
     * @param  groupId id to search by
     *
     * @return ResourceGroupComposite composite object with the ResourceGroup and availability, as well as the count of
     *         resources in the group
     * @throws FetchException TODO
     */
    @WebMethod
    ResourceGroupComposite getResourceGroupComposite( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId) throws FetchException;

    @WebMethod
    PageList<ResourceGroup> findResourceGroupsForRole( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "pageControl") PageControl pc) throws FetchException;

    @WebMethod
    PageList<ResourceGroup> findResourceGroups( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceGroup criteria, //
        @WebParam(name = "pageControl") PageControl pc) throws FetchException;

    @WebMethod
    PageList<ResourceGroupComposite> findResourceGroupComposites( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceGroup criteria, //
        @WebParam(name = "pageControl") PageControl pc) throws FetchException;

    @WebMethod
    void removeResourcesFromGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "resourceIds") int[] resourceIds) throws UpdateException;

    @WebMethod
    void setRecursive( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "isRecursive") boolean isRecursive) throws UpdateException;

    /**
     * 
     * @param subject
     * @param newResourceGroup
     * @return updatedResourceGroup
     * @throws UpdateException Possible Causes (ResourceGroupAlreadyExistsException, ResourceGroupUpdateException)
     */
    @WebMethod
    ResourceGroup updateResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "newResourceGroup") ResourceGroup newResourceGroup) throws UpdateException;
}
