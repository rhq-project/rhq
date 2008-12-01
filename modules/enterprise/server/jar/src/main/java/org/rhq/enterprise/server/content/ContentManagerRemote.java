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

import java.util.List;
import java.util.Set;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Architecture;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.enterprise.server.resource.ResourceTypeNotFoundException;

/**
 * @author Jay Shaughnessy
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
@Remote
public interface ContentManagerRemote {

    /**
     * Creates a new package version in the system. If the parent package (identified by the packageName parameter) does
     * not exist, it will be created. If a package version exists with the specified version ID, a new one will not be
     * created and the existing package version instance will be returned.
     *
     * @param  user           The logged in user's subject.
     * @param  packageName    parent package name; uniquely identifies the package under which this version goes
     * @param  packageTypeId  identifies the type of package in case the general package needs to be created
     * @param  version        identifies the version to be create
     * @param  architectureId architecture of the newly created package version
     *
     * @return newly created package version if one did not exist; existing package version that matches these data if
     *         one was found
     */
    @WebMethod
    PackageVersion createPackageVersion( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "packageName")
        String packageName, //
        @WebParam(name = "packageTypeId")
        int packageTypeId, //
        @WebParam(name = "version")
        String version, //
        @WebParam(name = "architectureId")
        int architectureId, //
        @WebParam(name = "packageBytes")
        byte[] packageBytes);

    /**
     * Deletes the specified package from the resource.
     *
     * @param  user             The logged in user's subject.
     * @param resourceId          identifies the resource from which the packages should be deleted
     * @param installedPackageIds identifies all of the packages to be deleted
     */
    @WebMethod
    void deletePackages( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "resourceId")
        int resourceId, //
        @WebParam(name = "installedPackages")
        Set<Integer> installedPackageIds, //
        @WebParam(name = "requestNotes")
        String requestNotes);

    /**
     * Deploys packages on the specified resources. Each installed package entry should be populated with the <code>
     * PackageVersion</code> being installed, along with the deployment configuration values if any. This method will
     * take care of populating the rest of the values in each installed package object.
     *
     * @param  user             The logged in user's subject.
     * @param resourceIds       identifies the resources against which the package will be deployed
     * @param packageVersionIds packageVersions we want to install
     */
    @WebMethod
    void deployPackages( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "resourceIds")
        Set<Integer> resourceIds, //
        @WebParam(name = "packageVersionIds")
        Set<Integer> packageVersionIds);

    /**
     * Returns all architectures known to the system.
     *
     * @param  user           The logged in user's subject.
     * @return list of all architectures in the database
     */
    @WebMethod
    List<Architecture> getArchitectures( //
        @WebParam(name = "user")
        Subject user);

    /**
     * This gets the package types that can be deployed to the given resource. It is a function of the resource
     * type of the resource.
     *
     * @param  user             The logged in user's subject.
     * @param  resourceTypeName The resource type in question
     *
     * @return The requested list of package types. Can be empty.
     */
    @WebMethod
    List<PackageType> getPackageTypes( //
        @WebParam(name = "user")
        Subject user, //
        @WebParam(name = "resourceTypeName")
        String resourceTypeName, //
        @WebParam(name = "pluginName")
        String pluginName) throws ResourceTypeNotFoundException;

}