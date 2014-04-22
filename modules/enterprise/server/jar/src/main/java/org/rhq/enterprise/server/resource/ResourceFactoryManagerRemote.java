/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.resource;

import java.util.List;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.CreateDeletePolicy;
import org.rhq.core.domain.resource.CreateResourceHistory;
import org.rhq.core.domain.resource.DeleteResourceHistory;
import org.rhq.core.domain.resource.ResourceCreationDataType;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Public ResourceFactory API.
 */
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
     * @param timeout               number of milliseconds before the agent suffers a timeout when creating the resource. If null uses default.
     * @return                      the create resource history record
     */
    CreateResourceHistory createResource(Subject subject, int parentResourceId, int resourceTypeId,
        String resourceName, Configuration pluginConfiguration, Configuration resourceConfiguration, Integer timeout);

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
     * @param timeout                     number of milliseconds before the agent suffers a timeout when creating the resource. If null uses default.
     * @return                            the create resource history record
     */
    CreateResourceHistory createPackageBackedResource(Subject subject, int parentResourceId, int newResourceTypeId,
        String newResourceName, Configuration pluginConfiguration, String packageName, String packageVersion,
        Integer architectureId, Configuration deploymentTimeConfiguration, byte[] packageBits, Integer timeout);

    /**
     * Like {@link #createPackageBackedResource(org.rhq.core.domain.auth.Subject, int, int, String, org.rhq.core.domain.configuration.Configuration, String, String, Integer, org.rhq.core.domain.configuration.Configuration, byte[], Integer)}
     * except that this method takes a <code>temporaryContentHandle</code> as parameter instead of a byte array.
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
     * @param temporaryContentHandle      content handle of the package to create
     * @param timeout                     number of milliseconds before the agent suffers a timeout when creating the resource. If null uses default.
     * @return                            the create resource history record
     *
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#createTemporaryContentHandle()
     * @see org.rhq.enterprise.server.content.ContentManagerRemote#uploadContentFragment(String, byte[], int, int)
     * @see #createPackageBackedResource(org.rhq.core.domain.auth.Subject, int, int, String, org.rhq.core.domain.configuration.Configuration, String, String, Integer, org.rhq.core.domain.configuration.Configuration, byte[], Integer)
     *
     * @since 4.10
     */
    CreateResourceHistory createPackageBackedResourceViaContentHandle(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration, String packageName,
        String packageVersion, Integer architectureId, Configuration deploymentTimeConfiguration,
        String temporaryContentHandle, Integer timeout);

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
     * @param timeout                     number of milliseconds before the agent suffers a timeout when creating the resource. If null uses default.
     * @return                            the create resource history record
     */
    public CreateResourceHistory createPackageBackedResourceViaPackageVersion(Subject subject, int parentResourceId,
        int newResourceTypeId, String newResourceName, Configuration pluginConfiguration,
        Configuration deploymentTimeConfiguration, int packageVersionId, Integer timeout);

    /**
     * Deletes a physical resource from the agent machine. After this call, the resource will no longer be accessible
     * not only to JON, but in general. It is up to the plugin to determine how to complete the delete, but a deleted
     * resource will no longer be returned from resource discoveries.
     *
     * @param subject    user requesting the deletion. must have resource delete perm on the resource.
     * @param resourceId resource being deleted
     * @return           the delete resource history record
     */
    DeleteResourceHistory deleteResource(Subject subject, int resourceId);

    /**
     * Deletes physical resources from the agent machine. After this call, the resource will no longer be accessible
     * not only to JON, but in general. It is up to the plugin to determine how to complete the delete, but a deleted
     * resource will no longer be returned from resource discoveries.
     *
     * @param subject    user requesting the deletion. must have resource delete perm on the resources.
     * @param resourceIds the resources being deleted
     * @return not null
     */
    List<DeleteResourceHistory> deleteResources(Subject subject, int[] resourceIds);

    /**
     * Returns a pagination enabled list of requests for the creation of new child resources to the specified parent.
     * These requests may be completed or still in progress; it represents the history of creation attempts for this
     * parent resource.
     *
     * @param  subject          the user making the request
     * @param  parentResourceId resource to check for child resource creations
     * @param  beginDate        filter used to show only results occurring after this epoch millis parameter, nullable
     * @param  endDate          filter used to show only results occurring before this epoch millis parameter, nullable
     * @param  pageControl      control for pagination
     *
     * @return list of requests
     */
    PageList<CreateResourceHistory> findCreateChildResourceHistory(Subject subject, int parentResourceId,
        Long beginDate, Long endDate, PageControl pageControl);

    /**
     * Returns a pagination enabled list of requests to delete a child resource on the specified parent. These requests
     * may be complete or still in progress; it represents the history of all delete attempts of child resources to this
     * resource.
     *
     * @param  subject          the user making the request
     * @param  parentResourceId resource to check for deleted child resources
     * @param  beginDate        filter used to show only results occurring after this epoch millis parameter, nullable
     * @param  endDate           filter used to show only results occurring before this epoch millis parameter, nullable
     * @param  pageControl      control for pagination
     *
     * @return list of requests
     */
    PageList<DeleteResourceHistory> findDeleteChildResourceHistory(Subject subject, int parentResourceId,
        Long beginDate, Long endDate, PageControl pageControl);
}
