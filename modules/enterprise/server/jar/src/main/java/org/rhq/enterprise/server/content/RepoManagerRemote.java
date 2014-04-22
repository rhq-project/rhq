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

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.transfer.SubscribedRepo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Public Repo API.
 */
@Remote
public interface RepoManagerRemote {

    /**
     * Associates the package versions (identified by their IDs) to the given repo (also identified by its ID).
     *
     * @param subject        The logged in user's subject.
     * @param repoId         the ID of the repo
     * @param packageVersionIds the list of package version IDs to add to the repo
     */
    void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds);

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
    Repo createRepo(Subject subject, Repo repo) throws RepoException;

    /**
     * Deletes the indicated repo. If this deletion orphans package versions (that is, its originating resource or
     * content source has been deleted), this will also purge those orphaned package versions.
     *
     * @param subject The logged in user's subject.
     * @param repoId  identifies the repo to delete
     */
    void deleteRepo(Subject subject, int repoId);

    /**
     * Returns the repo with the given id; throws an error if one does not exist at that id.
     *
     * @param subject user whose permissions will be checked for access to the repo
     * @param repoId  identifies the repo to be retrieved
     * @return details describing the repo
     */
    Repo getRepo(Subject subject, int repoId);

    /**
     * Returns all repos that match the given criteria.
     *
     * @param subject  user making the query
     * @param criteria describes how the query should function; may not be <code>null</code>
     * @return any repos that match the given criteria; empty list if none match
     */
    PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria);

    /**
     * Returns all imported repos in the server.
     *
     * @param subject user making the request
     * @param pc      used for pagination
     * @return paged list
     */
    PageList<Repo> findRepos(Subject subject, PageControl pc);

    /**
     * @param subject
     * @param criteria Caller must add a valid repoId via {@link PackageVersionCriteria#addFilterRepoId(Integer)}}
     * @return PackageVersions for the repo
     * @throws IllegalArgumentException for invalid repoId filter
     */
    PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria);

    /**
     * Returns the latest package version of the supplied package.
     * The latest version is determined using a comparator which is found using the following rules:
     * <ol>
     * <li>determine the comparator using the package type behavior if one is setup for the package type
     * <li>If no package behavior exists, use {@link PackageVersion#DEFAULT_COMPARATOR}
     * </ol>
     *
     * @param subject the authenticated user
     * @param packageId the id of the package to find the latest version for.
     * @param repoId the repo where to take the package versions of the package from
     * @return the package version or null
     */
    PackageVersion getLatestPackageVersion(Subject subject, int packageId, int repoId);

    /**
     * Update an existing {@link Repo} object's basic fields, like name, description, etc. Note that the given <code>
     * repo</code>'s relationships will be ignored and not merged with the existing repo (e.g. is subscribed
     * resources will not be changed, regardless of what the given repo's subscribed resources set it).
     *
     * @param subject The logged in user's subject.
     * @param repo to be updated
     *
     * @return Repo that was updated
     * @throws RepoException
     */
    Repo updateRepo(Subject subject, Repo repo) throws RepoException;

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
    PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc);

    /**
     * Deletes package versions from a repo if they are not referenced by
     * a content source.
     * <p>
     * The package versions themselves are not deleted until some content source or repository
     * is deleted at which point orphans detection is performed.
     *
     * @param subject
     * @param repoId
     * @param packageVersionIds
     * @return true if all the package versions were successfully deleted, false if some references exist.
     */
    boolean deletePackageVersionsFromRepo(Subject subject, int repoId, int[] packageVersionIds);

    /**
     * Gets all resources that are subscribed to the given repo.
     *
     * @param subject The logged in user's subject.
     * @param repoId
     * @param pc
     *
     * @return the list of subscribers
     */
    PageList<Resource> findSubscribedResources(Subject subject, int repoId, PageControl pc);

    /**
     * Get a list of truncated Repo objects that represent the
     * subscriptions for the specified resource.
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource.
     * @return A list of repos.
     */
    List<SubscribedRepo> findSubscriptions(Subject subject, int resourceId);

    /**
     * Subscribes the identified resource to the set of identified repos. Once complete, the resource will be able to
     * access all package content from all content sources that are assigned to the given repos.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param repoIds A list of repos to which the resource is subscribed.
     */
    void subscribeResourceToRepos(Subject subject, int resourceId, int[] repoIds);

    /**
     * Unsubscribes the identified resource from the set of identified repos.
     *
     * @param subject    The logged in user's subject.
     * @param resourceId The id of the resource to be subscribed.
     * @param repoIds A list of repos to which the resource is subscribed.
     */
    void unsubscribeResourceFromRepos(Subject subject, int resourceId, int[] repoIds);

    /**
     * @param subject
     * @param repoIds
     * @return number of synchronized repos
     * @throws Exception
     */
    int synchronizeRepos(Subject subject, int[] repoIds) throws Exception;

    /**
     * This method allows for downloading the bytes of an arbitrary package version. This call can be dangerous with
     * large packages because it will attempt to load the whole package in memory.
     *
     * @param subject
     * @param repoId
     * @param packageVersionId
     * @return the bytes of the package version
     * @since 4.5
     */
    byte[] getPackageVersionBytes(Subject subject, int repoId, int packageVersionId);
}