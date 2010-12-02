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

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.transfer.SubscribedRepo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface RepoManagerRemote {

    /**
     * Associates the package versions (identified by their IDs) to the given repo (also identified by its ID).
     *
     * @param subject           The logged in user's subject.
     * @param repoId         the ID of the repo
     * @param packageVersionIds the list of package version IDs to add to the repo
     */
    @WebMethod
    void addPackageVersionsToRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoId") int repoId, //
        @WebParam(name = "packageVersionIds") int[] packageVersionIds);

    /**
     * Creates a new {@link Repo}. Note that the created repo will not have any content sources assigned and no
     * resources will be subscribed. It is a virgin repo.
     *
     * @param subject The logged in user's subject.
     * @param repo a new repo object.
     *
     * @return the newly created repo
     * @throws RepoException if a repo already exists with the same name 
     */
    @WebMethod
    Repo createRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repo") Repo repo) //
        throws RepoException;

    /**
     * Deletes the indicated repo. If this deletion orphans package versions (that is, its originating resource or
     * content source has been deleted), this will also purge those orphaned package versions.
     *
     * @param subject The logged in user's subject.
     * @param repoId  identifies the repo to delete
     */
    @WebMethod
    void deleteRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoId") int repoId);

    /**
     * Returns the repo with the given id; throws an error if one does not exist at that id.
     *
     * @param subject user whose permissions will be checked for access to the repo
     * @param repoId  identifies the repo to be retrieved
     * @return details describing the repo
     */
    @WebMethod
    Repo getRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoId") int repoId);

    /**
     * Returns all repos that match the given criteria.
     *
     * @param subject  user making the query
     * @param criteria describes how the query should function; may not be <code>null</code>
     * @return any repos that match the given criteria; empty list if none match 
     */
    @WebMethod
    PageList<Repo> findReposByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") RepoCriteria criteria);

    /**
     * Returns all imported repos in the server.
     *
     * @param subject user making the request
     * @param pc      used for pagination
     * @return paged list
     */
    @WebMethod
    PageList<Repo> findRepos( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "pageControl") PageControl pc);

    /**
     * @param subject
     * @param criteria Caller must add a valid repoId via {@link PackageVersionCriteria#addFilterRepoId(Integer)}}
     * @return PackageVersions for the repo
     * @throws IllegalArgumentException for invalid repoId filter
     */
    @WebMethod
    PageList<PackageVersion> findPackageVersionsInRepoByCriteria( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") PackageVersionCriteria criteria);

    /**
     * Update an existing {@link Repo} object's basic fields, like name, description, etc. Note that the given <code>
     * repo</code>'s relationships will be ignored and not merged with the existing repo (e.g. is subscribed
     * resources will not be changed, regardless of what the given repo's subscribed resources set it).
     *
     * @param subject The logged in user's subject.
     * @param repo to be updated
     *
     * @return Repo that was updated
     */
    @WebMethod
    Repo updateRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repo") Repo repo) //
        throws RepoException;

    /**
     * Returns the set of package versions that can currently be accessed via the given repo.
     *
     * @param subject   The logged in user's subject.
     * @param repoId identifies the repo
     * @param filter    A repo filter.
     * @param pc        pagination controls
     *
     * @return the package versions that are available in the repo
     */
    @WebMethod
    PageList<PackageVersion> findPackageVersionsInRepo( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoId") int repoId, //
        @WebParam(name = "filter") String filter, //
        @WebParam(name = "pageControl") PageControl pc);

    /**
     * Gets all resources that are subscribed to the given repo.
     *
     * @param subject The logged in user's subject.
     * @param repoId
     * @param pc
     *
     * @return the list of subscribers
     */
    @WebMethod
    PageList<Resource> findSubscribedResources( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoId") int repoId, //
        @WebParam(name = "pageControl") PageControl pc);

    /**
     * Get a list of truncated Repo objects that represent the
     * subscriptions for the specified resource.
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource.
     * @return A list of repos.
     */
    @WebMethod
    List<SubscribedRepo> findSubscriptions( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId);

    /**
     * Subscribes the identified resource to the set of identified repos. Once complete, the resource will be able to
     * access all package content from all content sources that are assigned to the given repos.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param repoIds A list of repos to which the resource is subscribed.
     */
    @WebMethod
    void subscribeResourceToRepos( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "repoIds") int[] repoIds);

    /**
     * Unsubscribes the identified resource from the set of identified repos.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param repoIds A list of repos to which the resource is subscribed.
     */
    @WebMethod
    void unsubscribeResourceFromRepos( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceId") int resourceId, //
        @WebParam(name = "repoIds") int[] repoIds);

    @WebMethod
    int synchronizeRepos( //
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "repoIds") int[] repoIds) //
        throws Exception;
}