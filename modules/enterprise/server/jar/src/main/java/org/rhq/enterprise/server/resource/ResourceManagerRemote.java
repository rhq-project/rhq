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

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourceInstallCount;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.exception.DeleteException;
import org.rhq.enterprise.server.exception.FetchException;

/**
 * @author Asaf Shakarchi
 * @author Jay Shaughnessy 
 * @author Simeon Pinder
 */

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface ResourceManagerRemote {

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // These methods and constants also live in the local interface, update both locations when making a change!
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!    

    static public final String DATA_AGENT = "agent";
    static public final String DATA_AVAILABILITY = "availability";
    static public final String DATA_CURRENT_AVAILABILITY = "currentAvailability";
    static public final String DATA_EXPLICIT_GROUPS = "explicitGroups";
    static public final String DATA_IMPLICIT_GROUPS = "implicitGroups";
    static public final String DATA_PARENT_RESOURCE = "parentResource";
    static public final String DATA_PLUGIN_CONFIGURATION = "pluginConfiguration";
    static public final String DATA_PRODUCT_VERSION = "productVersion";
    static public final String DATA_RESOURCE_CONFIGURATION = "resourceConfiguration";
    static public final String DATA_RESOURCE_ERRORS = "resourceErrors";
    static public final String DATA_RESOURCE_TYPE = "resourceType";

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
        @WebParam(name = "resourceId") int resourceId) throws FetchException;

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
    List<Resource> findResourceLineage( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId) throws FetchException;

    /**
     * This find service can be used to find resources based on various criteria and return various data.
     *
     * @param subject The logged in user's subject.
     * @param criteria {@link Resource}, can be null
     * <pre>
     * If provided the Resource object can specify various search criteria as specified below.
     *   Resource.id : exact match
     *   Resource.description : case insensitive substring match   
     *   Resource.inventoryStatus : exact match
     *   Resource.name : case insensitive substring match   
     *   Resource.parentResource.id : exact match
     *   Resource.resourceKey : case insensitive substring match   
     *   Resource.resourceType.id : exact match
     * All other fields are ignored:
     * </pre>
     * @param pc {@link PageControl}
     * <pre>
     * If provided PageControl specifies page size, requested page, sorting, and optional data.
     * 
     * Supported OptionalData
     *   To specify optional data call pc.setOptionalData() and supply one of more of the DATA_* constants
     *   defined in this interface.
     * 
     * Supported Sorting:
     *   ?? This needs to be defined ??
     *    
     * </pre>
     * @return
     * @throws FetchException
     */
    @WebMethod
    PageList<Resource> findResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") Resource criteria, //
        @WebParam(name = "pageControl") PageControl pc) throws FetchException;

    /**
     * This find service can be used to find child resources for the specified resource,
     * based on various criteria and return various data.
     *
     * @param subject The logged in user's subject.
     * @param resourceId the id of a {@link Resource} in inventory. 
     * @param criteria {@link Resource}, can be null
     * <pre>
     * If provided the Resource object can specify various search criteria as specified below.
     * These criteria are applied to the children resources!
     *   Resource.id : exact match
     *   Resource.description : case insensitive substring match   
     *   Resource.inventoryStatus : exact match
     *   Resource.name : case insensitive substring match   
     *   Resource.resourceKey : case insensitive substring match   
     *   Resource.resourceType.id : exact match
     * All other fields are ignored:
     * </pre>
     * @param pc {@link PageControl}
     * <pre>
     * If provided PageControl specifies page size, requested page, sorting, and optional data.
     * 
     * Supported OptionalData
     *   To specify optional data call pc.setOptionalData() and supply one of more of the DATA_* constants
     *   defined in this interface.
     * 
     * Supported Sorting:
     *   ?? This needs to be defined ??
     *   
     * </pre>
     * @return
     * @throws FetchException
     */
    @WebMethod
    PageList<Resource> findResourceChildren( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "criteria") Resource criteria, //
        @WebParam(name = "pageControl") PageControl pc) throws FetchException;

    /**
     * Removes these resources from inventory.  The resources may subsequently be rediscovered.  Note that for
     * each specified resource all children will also be removed, it it not necessary or recommended to
     * specify more than one resource in the same ancestry line.
     * 
     * @param subject The logged in user's subject.
     * @param resourceIds The resources to uninventory.
     * @throws DeleteException
     */
    @WebMethod
    void uninventoryResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds) throws DeleteException;

    // THIS WILL BE REMOVED
    @WebMethod
    PageList<ResourceComposite> findResourceComposites( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "category") ResourceCategory category, //
        @WebParam(name = "typeName") String typeName, //
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "searchString") String searchString, //
        @WebParam(name = "pageControl") PageControl pageControl);

    List<ResourceInstallCount> findResourceInstallCounts(Subject subject, boolean groupByVersions);
}