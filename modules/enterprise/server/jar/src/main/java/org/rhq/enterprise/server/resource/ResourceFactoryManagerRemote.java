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
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.enterprise.server.jaxb.adapter.ConfigurationAdapter;
import org.rhq.enterprise.server.system.ServerVersion;

/*
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ResourceFactoryManagerRemote {

    /**
     * Creates a new physical resource. The resource will be created as a child of the specified parent. In other words,
     * the resource component of the indicated parent will be used to create the new resource. This call should only be
     * made for resource types that are defined with a create/delete policy of {@link CreateDeletePolicy#BOTH} or
     * {@link CreateDeletePolicy#CREATE_ONLY}. If this call is made for a resource type that cannot be created based on
     * this policy, the plugin container will throw an exception. This call should only be made for resource types that
     * are defined with a creation data type of {@link ResourceCreationDataType#CONFIGURATION}. If this call is made for
     * a resource type that cannot be created via a configuration, the plugin container will throw an exception.
     *
     * @param subject               user requesting the creation
     * @param parentResourceId      parent resource under which the new resource should be created
     * @param resourceTypeId        type of resource to create
     * @param resourceName          name of the resource being created
     * @param pluginConfiguration   optional plugin configuration that may be needed in order to create the new resource
     * @param resourceConfiguration resource configuration for the new resource
     */
    @WebMethod
    CreateResourceHistory createResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "resourceTypeId") int resourceTypeId, //
        @WebParam(name = "resourceName") String resourceName, //        
        @WebParam(name = "pluginConfiguration") Configuration pluginConfiguration, //
        @WebParam(name = "resourceConfiguration") Configuration resourceConfiguration);

    /**
     * Creates a new physical resource. The resource will be created as a child of the specified parent. In other words,
     * the resource component of the indicated parent will be used to create the new resource. This call should only be
     * made for resource types that are defined with a create/delete policy of {@link CreateDeletePolicy#BOTH} or
     * {@link CreateDeletePolicy#CREATE_ONLY}. If this call is made for a resource type that cannot be created based on
     * this policy, the plugin container will throw an exception. This call should only be made for resource types that
     * are defined with a creation data type of {@link ResourceCreationDataType#CONTENT}. If this call is made for a
     * resource type that cannot be created via an package, the plugin container will throw an exception.
     *
     * @param subject                     user requesting the creation
     * @param parentResourceId            parent resource under which the new resource should be created
     * @param newResourceTypeId           identifies the type of resource being created
     * @param newResourceName             Ignored, pass null. This is determined from the package.
     * @param pluginConfiguration         optional plugin configuration that may be needed in order to create the new
     *                                    resource
     * @param packageName                 name of the package that will be created as a result of this resource create
     * @param packageVersion              The string version of the package. If null will be set to system timestamp (long)
     * @param architectureId              Id of the target architecture of the package, null indicates NoArch (any). 
     * @param deploymentTimeConfiguration dictates how the package will be deployed
     * @param packageBits                 content of the package to create
     */
    @WebMethod
    CreateResourceHistory createPackageBackedResource(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "newResourceTypeId") int newResourceTypeId, //
        @WebParam(name = "newResourceName") String newResourceName, //
        @WebParam(name = "pluginConfiguration")//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration pluginConfiguration, //
        @WebParam(name = "packageName") String packageName, //
        @WebParam(name = "packageVersion") String packageVersion, //
        @WebParam(name = "architectureId") Integer architectureId, //
        @WebParam(name = "deploymentTimeConfiguration")//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration deploymentTimeConfiguration, //
        @WebParam(name = "packageBits") byte[] packageBits);

    /**
     * Creates a new physical resource. The resource will be created as a child of the specified parent. In other words,
     * the resource component of the indicated parent will be used to create the new resource. This call should only be
     * made for resource types that are defined with a create/delete policy of {@link CreateDeletePolicy#BOTH} or
     * {@link CreateDeletePolicy#CREATE_ONLY}. If this call is made for a resource type that cannot be created based on
     * this policy, the plugin container will throw an exception. This call should only be made for resource types that
     * are defined with a creation data type of {@link ResourceCreationDataType#CONTENT}. If this call is made for a
     * resource type that cannot be created via an package, the plugin container will throw an exception.
     *
     * @param subject                     user requesting the creation
     * @param parentResourceId            parent resource under which the new resource should be created
     * @param newResourceTypeId           identifies the type of resource being created
     * @param newResourceName             Ignored, pass null. This is determined from the package.
     * @param pluginConfiguration         optional plugin configuration that may be needed in order to create the new
     *                                    resource
     * @param deploymentTimeConfiguration dictates how the package will be deployed
     * @param packageVersionId            An existing package version to back this resource
     */
    @WebMethod
    public CreateResourceHistory createPackageBackedResourceViaPackageVersion(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "parentResourceId") int parentResourceId, //
        @WebParam(name = "newResourceTypeId") int newResourceTypeId, //
        @WebParam(name = "newResourceName") String newResourceName, //
        @WebParam(name = "pluginConfiguration")//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration pluginConfiguration, //
        @WebParam(name = "deploymentTimeConfiguration")//
        @XmlJavaTypeAdapter(value = ConfigurationAdapter.class)//
        Configuration deploymentTimeConfiguration, //
        @WebParam(name = "packageVersionId") int packageVersionId);

    /**
     * Deletes a physical resource from the agent machine. After this call, the resource will no longer be accessible
     * not only to JON, but in general. It is up to the plugin to determine how to complete the delete, but a deleted
     * resource will no longer be returned from resource discoveries.
     *
     * @param subject    user requesting the creation
     * @param resourceId resource being deleted
     */
    @WebMethod
    DeleteResourceHistory deleteResource( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);
}
