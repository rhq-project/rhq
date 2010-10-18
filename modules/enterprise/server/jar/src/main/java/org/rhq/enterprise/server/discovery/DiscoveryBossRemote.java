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
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * The remote boss interface to the discovery subsystem.
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface DiscoveryBossRemote {

    /**
     * Analogous to the GUI feature Import Resources in the auto discovery queue. Note, to query for
     * Resources with a specific InnventryStatus see {@link ResourceManagerRemote.findResourcesByCriteria(ResourceCriteria)}
     * 
     * @param subject
     * @param resourceIds
     */
    void importResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds);

    /**
     * Analogous to the GUI feature Ignore Resources in the auto discovery queue. Note, to query for
     * Resources with a specific InnventryStatus see {@link ResourceManagerRemote.findResourcesByCriteria(ResourceCriteria)}
     * 
     * @param subject
     * @param resourceIds
     */
    void ignoreResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds);

    /**
     * Analogous to the GUI feature Unignore Resources in the auto discovery queue. Note, to query for
     * Resources with a specific InnventryStatus see {@link ResourceManagerRemote.findResourcesByCriteria(ResourceCriteria)}
     * 
     * @param subject
     * @param resourceIds
     */
    void unignoreResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceIds") int[] resourceIds);

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
    @NotNull
    Resource manuallyAddResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceTypeId") int resourceTypeId, //        
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "pluginConfiguration") Configuration pluginConfiguration) //
        throws Exception;

}