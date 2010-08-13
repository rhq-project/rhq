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
package org.rhq.enterprise.server.resource.group;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.jaxb.adapter.ResourceGroupAdapter;
import org.rhq.enterprise.server.system.ServerVersion;

/**
 * @author Jay Shaughnessy 
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ResourceGroupManagerRemote {

    @WebMethod
    void addResourcesToGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "resourceIds") int[] resourceIds);

    @WebMethod
    ResourceGroup createResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceGroup")//
        @XmlJavaTypeAdapter(ResourceGroupAdapter.class) ResourceGroup resourceGroup);

    @WebMethod
    void deleteResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId) throws ResourceGroupNotFoundException, ResourceGroupDeleteException;

    @WebMethod
    ResourceGroup getResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId);

    @WebMethod
    ResourceGroupComposite getResourceGroupComposite( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId);

    @WebMethod
    PageList<ResourceGroup> findResourceGroupsForRole( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "roleId") int roleId, //
        @WebParam(name = "pageControl") PageControl pc);

    @WebMethod
    void removeResourcesFromGroup(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "resourceIds") int[] resourceIds);

    @WebMethod
    void setRecursive( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "groupId") int groupId, //
        @WebParam(name = "isRecursive") boolean isRecursive);

    @WebMethod
    ResourceGroup updateResourceGroup( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "newResourceGroup") ResourceGroup newResourceGroup);

    @WebMethod
    PageList<ResourceGroup> findResourceGroupsByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceGroupCriteria criteria);

}
