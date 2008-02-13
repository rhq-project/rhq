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
package org.rhq.enterprise.server.content;

import java.util.Set;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.rhq.core.domain.auth.Subject;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
public interface ContentManagerRemote {
    /**
     * Deletes the specified package from the resource.
     *
     * @param user                the user who is requesting the delete
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    void deletePackages(@WebParam(name = "subject")
    Subject user, @WebParam(name = "resourceIds")
    Set<Integer> resourceIds, @WebParam(name = "installedPackageIds")
    Set<Integer> installedPackageIds);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param user              the user who is requesting the creation
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    void deployPackages(@WebParam(name = "subject")
    Subject user, @WebParam(name = "resourceIds")
    Set<Integer> resourceIds, @WebParam(name = "packageVersionIds")
    Set<Integer> packageVersionIds);
}