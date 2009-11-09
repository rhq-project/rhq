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
import org.rhq.enterprise.server.plugin.pc.content.RepoImportReport;

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

    /**
     * Functions similar to {@link RepoManagerRemote#createRepo(Subject, Repo)} except that it will ensure
     * the candidate bit on the repo parameter is correctly set.
     *
     * @param subject user creating the repo
     * @param repo    repo data to create
     * @return persisted repo (ID will be populated)
     * @throws RepoException if the repo contains invalid data
     */
    Repo createCandidateRepo(Subject subject, Repo repo) throws RepoException;

    /**
     * Handles a repo report from a content provider, adding and removing candidate repos as necessary into the
     * database.
     *
     * @param subject         user triggering the report processing
     * @param report          cannot be <code>null</code>
     * @param contentSourceId identifies the content source that
     * @param result          buffer used to store the results of dealing with the report
     */
    void processRepoImportReport(Subject subject, RepoImportReport report, int contentSourceId, StringBuilder result);

    /**
     * Changes the specified repos from being candidates in the system into full blown repositories,
     * allowing their packages to be syncced and resources to subscribe to them.
     *
     * @param subject user performing the import
     * @param repoIds the repos being imported; they must refer to repos in the database and must be flagged
     *                as candidates (i.e. an error will occur if an already imported repo is specified)
     * @throws RepoException if one or more of the repo IDs does not exist in the DB or is not a candidate
     */
    void importCandidateRepo(Subject subject, List<Integer> repoIds) throws RepoException;

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