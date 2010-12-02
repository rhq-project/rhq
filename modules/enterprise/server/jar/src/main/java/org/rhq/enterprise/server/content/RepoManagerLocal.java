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
import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.Distribution;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoGroup;
import org.rhq.core.domain.content.RepoGroupType;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.content.composite.RepoComposite;
import org.rhq.core.domain.content.transfer.SubscribedRepo;
import org.rhq.core.domain.criteria.PackageVersionCriteria;
import org.rhq.core.domain.criteria.RepoCriteria;
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
     * Get the overall sync status of this Repository.  This is a summation of all the syncs.
     *
     * There is a weight to the status since this returns the most 'relevant' status:
     *
     * 1) ContentSourceSyncStatus.FAILURE
     * 2) ContentSourceSyncStatus.INPROGRESS
     * 3) ContentSourceSyncStatus.SUCCESS
     *

     * @param subject caller
     * @param repoId to calc status for
     * @return String summary of the status of this Repository
     */
    String calculateSyncStatus(Subject subject, int repoId);

    /**
     */
    void addContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception;

    /**
     * Associates content sources with the given repo. Unlike {@link #addContentSourcesToRepo(Subject, int, int[])},
     * no further operations will be performed, such as any initial synchronization or initialization.
     * <p/>
     * This should only be used for test purposes.
     *
     * @param subject          may not be <code>null</code>
     * @param repoId           must refer to a valid repo in the system
     * @param contentSourceIds may not be <code>null</code>
     * @throws Exception if there is an error making the association
     */
    void simpleAddContentSourcesToRepo(Subject subject, int repoId, int[] contentSourceIds) throws Exception;

    /**
     */
    void removeContentSourcesFromRepo(Subject subject, int repoId, int[] contentSourceIds) throws RepoException;

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
     * Removes candidate repos whose only content source is the indicated content source.
     *
     * @param subject         user performing the delete
     * @param contentSourceId identifies the content source
     */
    void deleteCandidatesWithOnlyContentSource(Subject subject, int contentSourceId);

    /**
     * Handles a repo report from a content source, adding and removing candidate repos as necessary into the
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
     * Creates a new {@link RepoGroup} in the server.
     *
     * @param subject   represents the user creating the group
     * @param repoGroup group data to create
     * @return group instance populated after persisting
     * @throws RepoException if a repo group already exists with this name
     */
    RepoGroup createRepoGroup(Subject subject, RepoGroup repoGroup) throws RepoException;

    /**
     * Deletes the indicated repo group.
     *
     * @param subject     user deleting the group
     * @param repoGroupId identifies the group being deleted
     */
    void deleteRepoGroup(Subject subject, int repoGroupId);

    /**
     * @see RepoManagerRemote#getRepo(Subject, int)
     */
    Repo getRepo(Subject subject, int repoId);

    /**
     * Returns the repo group with the given id; throws an error if one does not exist at that id.
     *
     * @param subject     user whose permissions will be checked for access to the repo
     * @param repoGroupId identifies the repo group to be retrieved
     * @return details describing the repo group
     */
    RepoGroup getRepoGroup(Subject subject, int repoGroupId);

    /**
     * Returns the repo group type with the given name.
     *
     * @param subject user whose permissions will be checked for access to the group type
     * @param name    identifies the repo group type
     * @return details of the group type; <code>null</code> if no group is found with the name
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

    /**
     * @see RepoManagerRemote#findAssociatedDistributions(Subject, int, PageControl)
     */
    PageList<Distribution> findAssociatedDistributions(Subject subject, int repoid, PageControl pc);

    /**
     * @see RepoManagerRemote#findAssociatedAdvisory(Subject, int, PageControl)
     */
    PageList<Advisory> findAssociatedAdvisory(Subject subject, int repoid, PageControl pc);

    /**
     * Schedules jobs to synchronize the content associated with the repoIds passed in.
     *
     * @param repoIds to synchronize; may not be <code>null</code>
     * @return count of the number of repositories synced.
     * @throws Exception if there is an error connecting with the plugin container
     */
    int synchronizeRepos(Subject subject, int[] repoIds) throws Exception;

    /**
     * Performs the actual synchronization of the given repos.
     *
     * @param subject user performing the sync
     * @param repoIds identifies all repos to be syncced
     * @return number of repos successfully syncced
     * @throws Exception if any errors occur
     */
    int internalSynchronizeRepos(Subject subject, Integer[] repoIds) throws InterruptedException;

    /**
     * Cancel any running sync job for the given repo
     *
     * @param repoId you want to cancel the sync for
     * @return boolean if it was cancelled or not
     */
    void cancelSync(Subject subject, int repoId) throws ContentException;

    /**
     * Creates a new sync results object. Note that this will return <code>null</code> if the given results object has a
     * status of INPROGRESS but there is already a sync results object that is still INPROGRESS and has been in that
     * state for less than 24 hours. Use this to prohibit the system from synchronizing on the same content source
     * concurrently.
     *
     * @param  results the results that should be persisted
     *
     * @return the persisted object, or <code>null</code> if another sync is currently inprogress.
     */
    RepoSyncResults persistRepoSyncResults(RepoSyncResults results);

    /**
     * Updates an existing sync results object. Do not use this method to create a new sync results object - use
     * {@link #persistContentRepoSyncResults(RepoSyncResults)} for that.
     *
     * @param  results the existing results that should be or merged
     *
     * @return the merged object
     */
    RepoSyncResults mergeRepoSyncResults(RepoSyncResults results);

    /**
     * Allows the caller to page through a list of historical sync results for a content source.
     *
     * @param  subject user asking to perform this
     * @param  contentSourceId The id of a content source.
     * @param  pc pagination controls
     *
     * @return the list of results
     */
    PageList<RepoSyncResults> getRepoSyncResults(Subject subject, int repoId, PageControl pc);

    /**
     * Returns the full sync results object.
     *
     * @param  resultsId the ID of the object to return
     *
     * @return the full sync results
     */
    RepoSyncResults getRepoSyncResults(int resultsId);

    /**
     * Get the most recent RepoSyncResults for this Repo
     * @param subject caller
     * @param repoId to fetch most recent sync results for
     * @return RepoSyncResults if found, null if not
     */
    RepoSyncResults getMostRecentSyncResults(Subject subject, int repoId);

    /**
     * Gets all repos that are subscribed to by the given resource.
     * @param subject
     * @param resourceId
     * @return
     */
    List<SubscribedRepo> findSubscriptions(Subject subject, int resourceId);
}