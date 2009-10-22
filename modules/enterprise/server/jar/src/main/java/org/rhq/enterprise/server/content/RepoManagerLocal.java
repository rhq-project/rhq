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

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.criteria.RepoCriteria;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

@Local
public interface RepoManagerLocal {

    /**
     * @see RepoManagerRemote#findRepos(Subject, PageControl)
     */
    PageList<Repo> findRepos(Subject subject, PageControl pc);

    /**
     */
    PageList<ContentSource> findAssociatedContentSources(Subject subject, int repoId, PageControl pc);

    /**
     * Gets all repos that are subscribed to by the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of subscriptions
     */
    PageList<RepoComposite> findResourceSubscriptions(Subject subject, int resourceId, PageControl pc);

    /**
     * Gets all repos that aren't subscribed to for the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of available repos for the given resource
     */
    PageList<RepoComposite> findAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc);

    /**
     * Gets all repos that are subscribed to by the given resource.
     *
     * @param  resourceId
     *
     * @return the list of subscriptions
     */
    List<RepoComposite> findResourceSubscriptions(int resourceId);

    /**
     * Gets all repos that aren't subscribed to for the given resource.
     *
     * @param  resourceId
     *
     * @return the list of available repos for the given resource
     */
    List<RepoComposite> findAvailableResourceSubscriptions(int resourceId);

    /**
     * Returns the set of package versions that can currently be accessed via the given repo.
     *
     * @param  subject   user asking to perform this
     * @param  repoId identifies the repo
     * @param  pc        pagination controls
     *
     * @return the package versions that are available in the repo
     */
    PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, PageControl pc);

    /**
     * @see RepoManagerRemote#findPackageVersionsInRepo(Subject, int, String, PageControl)
     */
    PageList<PackageVersion> findPackageVersionsInRepo(Subject subject, int repoId, String filter, PageControl pc);

    /**
     */
    void addContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception;

    /**
     */
    void removeContentSourcesFromRepo(Subject subject, int repoId, int[] contentSourceIds)
        throws RepoException;

    /**
     */
    long getPackageVersionCountFromRepo(Subject subject, int repoId);

    /**
     * Creates a relationship between two repos. The relationship will be marked as being the specified
     * type. For relationships where the order matters, think of the <code>repoId</code> as being the source
     * of the relationship and <code>relatedRepoId</code> as being the destination or target of it.
     *
     * @param subject              user making the relationship
     * @param repoId               must reference a valid repo in the system the user has permissions to access
     * @param relatedRepoId        must reference a valid repo in the system the user has permissions to access
     * @param relationshipTypeName must identify an existing relationship in the database
     */
    void addRepoRelationship(Subject subject, int repoId, int relatedRepoId, String relationshipTypeName);

    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //
    // The following are shared with the Remote Interface
    //
    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    /**
     * @see RepoManagerRemote#addPackageVersionsToRepo(Subject, int, int[])
     */
    void addPackageVersionsToRepo(Subject subject, int repoId, int[] packageVersionIds);

    /**
     * @see RepoManagerRemote#createRepo(Subject, Repo)
     */
    Repo createRepo(Subject subject, Repo repo) throws RepoException;

    /**
     * @see RepoManagerRemote#deleteRepo(Subject, int)
     */
    void deleteRepo(Subject subject, int repoId);

    /**
     * @see RepoManagerRemote#createRepoGroup(Subject, RepoGroup)
     */
    RepoGroup createRepoGroup(Subject subject, RepoGroup repoGroup) throws RepoException;

    /**
     * @see RepoManagerRemote#deleteRepoGroup(Subject, int)
     */
    void deleteRepoGroup(Subject subject, int repoGroupId);

    /**
     * @see RepoManagerRemote#getRepo(Subject, int)
     */
    Repo getRepo(Subject subject, int repoId);

    /**
     * @see RepoManagerRemote#getRepoGroup(Subject, int)
     */
    RepoGroup getRepoGroup(Subject subject, int repoGroupId);

    /**
     * @see RepoManagerRemote#getRepoGroupTypeByName(Subject, String)
     */
    RepoGroupType getRepoGroupTypeByName(Subject subject, String name);

    /**
     * Returns all repos that match the given name. The returned list should only be of size 0 or 1.
     *
     * @param name name of the repo to match
     * @return list of matching repos; presumably of size 0 or 1
     */
    List<Repo> getRepoByName(String name);

    /**
     * Returns the repo group with the given name if it exists.
     *
     * @param name name of the repo group to match
     * @return repo group with the given name; <code>null</code> if one does not
     */
    RepoGroup getRepoGroupByName(String name);

    /**
     * @see RepoManagerRemote#findPackageVersionsInRepoByCriteria(Subject, PackageVersionCriteria)
     */
    PageList<Repo> findReposByCriteria(Subject subject, RepoCriteria criteria);

    /**
     * @see RepoManagerRemote#findPackageVersionsInRepo(Subject, int, String, PageControl) 
     */
    PageList<PackageVersion> findPackageVersionsInRepoByCriteria(Subject subject, PackageVersionCriteria criteria);

    /**
     * @see RepoManagerRemote#subscribeResourceToRepos(Subject, int, int[])
     */
    void subscribeResourceToRepos(Subject subject, int resourceId, int[] repoIds);

    /**
     * @see RepoManagerRemote#unsubscribeResourceFromRepos(Subject, int, int[])
     */
    void unsubscribeResourceFromRepos(Subject subject, int resourceId, int[] repoIds);

    /**
     * @see RepoManagerRemote#findSubscribedResources(Subject, int, PageControl)
     */
    PageList<Resource> findSubscribedResources(Subject subject, int repoId, PageControl pc);

    /**
     * @see RepoManagerRemote#updateRepo(Subject, Repo)
     */
    Repo updateRepo(Subject subject, Repo repo) throws RepoException;

}