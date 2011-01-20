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

import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DistributionFile;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.content.RepoSyncResults;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.plugin.pc.content.AdvisorySyncReport;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetails;
import org.rhq.enterprise.server.plugin.pc.content.ContentProviderPackageDetailsKey;
import org.rhq.enterprise.server.plugin.pc.content.DistributionSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.PackageSyncReport;
import org.rhq.enterprise.server.plugin.pc.content.RepoDetails;

/**
 * Interface that provides access to the {@link ContentSource} objects deployed in the server, allowing the callers to
 * access data about and from remote content repositories.
 */
@Local
public interface ContentSourceManagerLocal {
    /**
     * This will look for any {@link PackageVersion}s that are "orphaned" (that is, is not related to any existing
     * content source or repo and is not installed anywhere) and will remove any orphans that it finds. This means it
     * will delete orphaned {@link PackageVersion} definitions and (if loaded) their {@link PackageBits}.
     *
     * @param subject user requesting the purge
     */
    void purgeOrphanedPackageVersions(Subject subject);

    /**
     * Deletes the identified content source. Any package versions that originated from this content source but are
     * still related to one or more repos will remain.
     *
     * @param subject An authenticated user making the request.
     * @param contentSourceId The id of the content source to be deleted.
     */
    void deleteContentSource(Subject subject, int contentSourceId);

    /**
     * Returns all {@link ContentSourceType} objects that are configured in the system.
     *
     * @return all content source types
     */
    Set<ContentSourceType> getAllContentSourceTypes();

    /**
     * Returns all {@link ContentSource} objects that are configured in the system but not presently
     * associated with the repo identified by repoId
     *
     * @param  subject   user asking to perform this
     * @param  repoId the identifier for the repo
     * @param  pc        pagination controls
     *
     * @return all content sources that are not presently associated with the repo identified by repoId
     */
    PageList<ContentSource> getAvailableContentSourcesForRepo(Subject subject, Integer repoId, PageControl pc);

    /**
     * Returns all {@link ContentSource} objects that are configured in the system.
     *
     * @param  subject user asking to perform this
     * @param  pc      pagination controls
     *
     * @return all content sources
     */
    PageList<ContentSource> getAllContentSources(Subject subject, PageControl pc);

    /**
     * Get a {@link ContentSourceType} by name. <code>null</code> will be returned if there is no content source type by
     * that name.
     *
     * @param  name the name of the {@link ContentSourceType} to be returned
     *
     * @return {@link ContentSourceType} found. <code>null</code> if none found
     */
    ContentSourceType getContentSourceType(String name);

    /**
     * Returns the {@link ContentSource} from its ID.
     *
     * @param  subject         user asking to perform this
     * @param  contentSourceId identifies the content source to return
     *
     * @return the content source object, <code>null</code> if the ID is invalid
     */
    ContentSource getContentSource(Subject subject, int contentSourceId);

    /**
     * Get a {@link ContentSource} by name and {@link ContentSourceType} name. <code>null</code> will be returned if
     * there is no content source with the given criteria.
     *
     * @param  subject  user asking to perform this
     * @param  name     the name of the {@link ContentSource} to be returned
     * @param  typeName the name of the {@link ContentSourceType}
     *
     * @return {@link ContentSource} found. <code>null</code> if none found
     */
    ContentSource getContentSourceByNameAndType(Subject subject, String name, String typeName);

    /**
     * Gets the list of imported repos that are associated with a given content source.
     *
     * @param  subject         user asking to perform this
     * @param  contentSourceId the id of a content source.
     * @param  pc              pagination controls
     *
     * @return list of associated repos
     */
    PageList<Repo> getAssociatedRepos(Subject subject, int contentSourceId, PageControl pc);

    /**
     * Gets the list of candidate repos that are associated with a given content source.
     *
     * @param  subject         user asking to perform this
     * @param  contentSourceId the id of a content source.
     * @param  pc              pagination controls
     *
     * @return list of candidate repos
     */
    PageList<Repo> getCandidateRepos(Subject subject, int contentSourceId, PageControl pc);

    /**
     * Allows the caller to page through a list of historical sync results for a content source.
     *
     * @param  subject user asking to perform this
     * @param  contentSourceId The id of a content source.
     * @param  pc pagination controls
     *
     * @return the list of results
     */
    PageList<ContentSourceSyncResults> getContentSourceSyncResults(Subject subject, int contentSourceId, PageControl pc);

    /**
     * Allow a user to purge content source sync results.
     *
     * @param subject  user asking to perform this
     * @param ids     the IDs of the {@link ContentSourceSyncResults} to delete
     */
    void deleteContentSourceSyncResults(Subject subject, int[] ids);

    /**
     * Create the specified content source.
     *
     * @param subject The user making the request.
     *
     * @param contentSource A content source to be created.
     *
     * @return The created content source.
     */
    ContentSource createContentSource(Subject subject, ContentSource contentSource) throws ContentSourceException;

    /**
     * Update an existing {@link ContentSource} object and restarts its underlying adapter. This also forces the adapter
     * to immediately sync with the remote repository. Note that this will only update the content source's basic fields
     * like name, description, etc. as well as its configuration. Specifically, it will not update the other
     * relationships like its repos. Use {@link #addContentSourcesToRepo(Subject, int, int[])} for things like
     * that.
     *
     * @param  subject       wanting to update the ContentSource
     * @param  contentSource to be updated
     * @param  syncNow if you wish to resync the ContentSource after updating
     *
     * @return the ContentSource that was updated
     */
    ContentSource updateContentSource(Subject subject, ContentSource contentSource, boolean syncNow)
        throws ContentSourceException;

    /**
     * Given a content source ID, this will test that the adapter responsible for pulling data from the content source's
     * remote repository can actually connect to that repository, and if not, throw an Exception.
     *
     * @param  contentSourceId the id of the content source on which to test the connection
     *
     * @throws Exception if the test failed
     */
    void testContentSourceConnection(int contentSourceId) throws Exception;

    /**
     * Requests that the identified content source be synchronized and if not lazy-loading to also download its
     * packages' bits. This ensures that the server maintains an accurate list of what is available on the content
     * source by seeing what was added, removed or updated since the last time the content source was synchronized. This
     * method is performed asynchronously - the calling thread will not block and will return immediately.
     *
     * @param  subject         the user asking to perform this
     * @param  contentSourceId identifies the content source to synchronize
     *
     * @throws Exception if failed to kick off the synchronize job
     */
    void synchronizeAndLoadContentSource(Subject subject, int contentSourceId);

    /**
     * Returns all the package versions that are served by the content source identified by the given ID.
     *
     * @param  subject         the user asking to perform this
     * @param  contentSourceId The id of a content source.
     * @param  pc pagination controls
     *
     * @return all package versions that the content source will be providing content for. The object returned also
     *         contains the location where those package versions are located in the content source
     */
    PageList<PackageVersionContentSource> getPackageVersionsFromContentSource(Subject subject, int contentSourceId,
        PageControl pc);

    /**
     * Returns all packages from the given repo. This call takes into account the content source to provide any
     * extra data on the package versions that exist for the particular mapping of content source and package,
     * such as the location attribute.
     *
     * @param subject         user retrieving the data
     * @param contentSourceId content source from which the packages are retrieved
     * @param repoId          repo from which the packages are retrieved
     *
     * @return all package versions that the content source will be providing content for. The object returned also
     *         contains the location where those package versions are located in the content source
     */
    List<PackageVersionContentSource> getPackageVersionsFromContentSourceForRepo(Subject subject, int contentSourceId,
        int repoId);

    /**
     * Returns count of PackageVersions associated with the given content source.
     *
     * @param  subject         caller requesting count
     * @param  contentSourceId to lookup
     *
     * @return count if any
     */
    long getPackageVersionCountFromContentSource(Subject subject, int contentSourceId);

    /**
     * Returns the length of the package version identified by its {@link PackageDetailsKey}. This method ensures that
     * the given resource is subscribed to a repo that contains the package version.
     *
     * @param  resourceId
     * @param  packageDetailsKey
     *
     * @return the length of the package version
     */
    long getPackageBitsLength(int resourceId, PackageDetailsKey packageDetailsKey);

    /////////////////////////////////////////////////////////////////////
    // The methods below should not be exposed to remote clients

    /**
     * Returns all the package versions that are served by all the content sources identified by the given IDs.
     *
     * @param  subject          the user asking to perform this
     * @param  contentSourceIds
     * @param  pc
     *
     * @return all package versions that the content sources will be providing content for. The object returned also
     *         contains the location where those package versions are located in the content source
     */
    PageList<PackageVersionContentSource> getPackageVersionsFromContentSources(Subject subject, int[] contentSourceIds,
        PageControl pc);

    /**
     * Returns all the package versions that are served by the content source identified by the given ID but whose
     * {@link PackageVersion#getPackageBits() package bits} have not been loaded yet.
     *
     * @param  subject         the user asking to perform this
     * @param  contentSourceId
     * @param  pc
     *
     * @return all unloaded package versions that the content source will be providing content for. The object returned
     *         also contains the location where those package versions are located in the content source
     */
    PageList<PackageVersionContentSource> getUnloadedPackageVersionsFromContentSourceInRepo(Subject subject,
        int contentSourceId, int repoId, PageControl pc);

    /**
     * This will download all the distribution bits associated with a specific content source.
     *
     * @param subject
     * @param contentSource
     */
    void downloadDistributionBits(Subject subject, ContentSource contentSource);
    
    /**
     * This will download the actual package bits for that package version from that content source's remote repository.
     * 
     * @param resourceId
     * @param packageDetailsKey
     * @param packageVersionId
     * @return
     */
    boolean downloadPackageBits(int resourceId, PackageDetailsKey packageDetailsKey);
        
    
    /**
     * Given a {@link PackageVersionContentSource} which contains the ID of a content source, an ID of a package
     * version, and the location of that package version on the remote content source repo, this will download the
     * actual package bits for that package version from that content source's remote repository.
     *
     * <p>An exception will be thrown if the package bits could not be loaded.</p>
     *
     * <p>This method is potentially a long running operation. Its transaction timeout should be extended
     * appropriately.</p>
     *
     * <p>If the content source where the package version is found is flagged to {@link DownloadMode#NEVER NEVER}
     * download package bits, this will immediately return <code>null</code>.</p>
     *
     * @param  subject the user asking to perform this
     * @param  pvcs
     *
     * @return information about the package bits that were downloaded - note that this will NOT have the actual bits
     *         inside it - we will not load the package bits in memory for obvious reasons. This will be <code>
     *         null</code> if the content source is configured to never actually download package bits.
     */
    PackageBits downloadPackageBits(Subject subject, PackageVersionContentSource pvcs);

    /**
     * Requests that the identified content source be synchronized. This ensures that the server maintains an accurate
     * list of what is available on the content source by seeing what was added, removed or updated since the last time
     * the content source was synchronized.
     *
     * <p>Do <b>not</b> call this method unless you know what you are doing. It is potentially a long running process
     * and will block the calling thread. In addition, this method must <b>never</b> be called from inside a
     * transaction, because it can be long running. You probably want to call
     * {@link #synchronizeAndLoadContentSource(int)}.</p>
     *
     * @param  contentSourceId identifies the content source to synchronize
     *
     * @return <code>true</code> if the synchronization is complete; <code>false</code> if there is already a
     *         synchronization already in progress and this call did nothing and aborted.
     *
     * @throws Exception if failed to synchronize
     */
    boolean internalSynchronizeContentSource(int contentSourceId) throws Exception;

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
    ContentSourceSyncResults persistContentSourceSyncResults(ContentSourceSyncResults results);

    /**
     * Updates an existing sync results object. Do not use this method to create a new sync results object - use
     * {@link #persistContentSourceSyncResults(ContentSourceSyncResults)} for that.
     *
     * @param  results the existing results that should be or merged
     *
     * @return the merged object
     */
    ContentSourceSyncResults mergeContentSourceSyncResults(ContentSourceSyncResults results);

    /**
     * Returns the full sync results object.
     *
     * @param  resultsId the ID of the object to return
     *
     * @return the full sync results
     */
    ContentSourceSyncResults getContentSourceSyncResults(int resultsId);

    /**
     * Updates the server with the results of a repo import from a content source.
     *
     * @param repos list of repo data received from the content source; should not be <code>null</code>
     */
    void mergeRepoImportResults(List<RepoDetails> repos);

    /**
     * After a sync has happened, this is responsible for persisting the results.
     *
     * @param  contentSource content source that was just sync'ed
     * @param  report        information on what the current inventory should look like
     * @param  previous      information from the previous inventory, before the sync happened
     * @param  syncResults   sync results object that should be updated to track this method's progress
     *
     * @return the updated syncResults that includes more summary information in the results string that indicates what
     *         was done
     */
    RepoSyncResults mergePackageSyncReport(ContentSource contentSource, Repo repo, PackageSyncReport report,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults);

    /**
     * After a sync has happened, this is responsible for persisting the results.
     *
     * @param  contentSource content source that was just sync'ed
     * @param  report        information on what the current inventory should look like
     * @param  syncResults   sync results object that should be updated to track this method's progress
     *
     * @return the updated syncResults that includes more summary information in the results string that indicates what
     *         was done
     */
    RepoSyncResults mergeDistributionSyncReport(ContentSource contentSource, DistributionSyncReport report,
        RepoSyncResults syncResults);

    /**
     * After a sync has happened, this is responsible for persisting the results.
     *
     * @param  contentSource content source that was just sync'ed
     * @param  report        information on what the current inventory should look like
     * @param  syncResults   sync results object that should be updated to track this method's progress
     *
     * @return the updated syncResults that includes more summary information in the results string that indicates what
     *         was done
     */
    RepoSyncResults mergeAdvisorySyncReport(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults);

    void _mergePackageSyncReportUpdateRepo(int contentSourceId);

    RepoSyncResults _mergePackageSyncReportREMOVE(ContentSource contentSource, Repo repo, PackageSyncReport report,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults,
        StringBuilder progress);

    RepoSyncResults _mergePackageSyncReportADD(ContentSource contentSource, Repo repo,
        Collection<ContentProviderPackageDetails> newPackages,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults,
        StringBuilder progress, int addCount);

    RepoSyncResults _mergePackageSyncReportUPDATE(ContentSource contentSource, PackageSyncReport report,
        Map<ContentProviderPackageDetailsKey, PackageVersionContentSource> previous, RepoSyncResults syncResults,
        StringBuilder progress);

    RepoSyncResults _mergeDistributionSyncReportREMOVE(ContentSource contentSource, DistributionSyncReport report,
        RepoSyncResults syncResults, StringBuilder progress);

    RepoSyncResults _mergeDistributionSyncReportADD(ContentSource contentSource, DistributionSyncReport report,
        RepoSyncResults syncResults, StringBuilder progress);

    RepoSyncResults _mergeAdvisorySyncReportADD(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults, StringBuilder progress);

    RepoSyncResults _mergeAdvisorySyncReportREMOVE(ContentSource contentSource, AdvisorySyncReport report,
        RepoSyncResults syncResults, StringBuilder progress);

    /**
     * Requests all {@link PackageVersion#getMetadata() metadata} for all package versions that the given resource
     * component is subscribed to (see {@link Repo#getResources()}. The returned object has the metadata bytes that
     * are meaningful to the calling plugin component.
     *
     * <p>Note that the returned object has the package version IDs that can be used to retrieve the actual content bits
     * of the package versions via a call to {@link #retrievePackageVersionBits(int, int)}.</p>
     *
     * <p>Callers should consider caching the returned metadata. Use {@link #getResourceSubscriptionMD5(int)} to get the
     * MD5 hashcode of the metadata for the resource to aid in determining when a cache of metadata is stale.</p>
     *
     * @param  resourceId identifies the resource requesting the data; all package versions in all the resource's
     *                    subscribed repos will be represented in the returned map
     * @param  pc         this method can potentially return a large set; this page control object allows the caller to
     *                    page through that large set, as opposed to requesting the entire set in one large chunk
     *
     * @return the list of all package versions' metadata
     *
     * @see    #getResourceSubscriptionMD5(int)
     */
    PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(int resourceId, PageControl pc);

    /**
     * Gets the MD5 hash which identifies a resource "content subscription". This MD5 hash will change when any repo
     * the resource is subscribed to has changed its contents (that is, if a package version was added/updated/removed
     * from it).
     *
     * @param  resourceId identifies the resource requesting the MD5; any change to any package version in any of the
     *                    resource's subscribed repos will determine the MD5
     *
     * @return the MD5
     *
     * @see    #getPackageVersionMetadata(int, PageControl)
     */
    String getResourceSubscriptionMD5(int resourceId);

    /**
     * Requests that the actual content data (the "bits") of the identified package version be streamed down to the
     * caller over the given output stream that the caller provides. This method will <b>not</b> be responsible for
     * closing the stream when its done, the caller needs to close it. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * <p>This is the same as calling
     * {@link #outputPackageVersionBitsRangeGivenResource(int, PackageDetailsKey, OutputStream, long, long)} with the start/end bytes
     * of 0 and -1 respectively.</p>
     *
     * @param  resourceId        identifies the resource making the request; if this resource is not allowed to see the
     *                           package version (due to the fact that it is not subscribed to a repo that is serving
     *                           that package version), an exception is thrown
     * @param  packageDetailsKey identifies the {@link PackageVersion} whose {@link PackageBits} are to be streamed
     * @param  outputStream      a stream that the caller prepared where this method will write the actual content
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageVersionBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream);

    /**
     * Requests that the actual content data (the "bits") of the identified package version be streamed down to the
     * caller over the given output stream that the caller provides. This method will <b>not</b> be responsible for
     * closing the stream when its done, the caller needs to close it. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * @param  resourceId        identifies the resource making the request; if this resource is not allowed to see the
     *                           package version (due to the fact that it is not subscribed to a repo that is serving
     *                           that package version), an exception is thrown
     * @param  packageDetailsKey identifies the {@link PackageVersion} whose {@link PackageBits} are to be streamed
     * @param  outputStream      a stream that the caller prepared where this method will write the actual content
     * @param  startByte         the first byte (inclusive) of the byte range to retrieve and output (bytes start at
     *                           index 0)
     * @param  endByte           the last byte (inclusive) of the byte range to retrieve and output (-1 means up to EOF)
     *                           (bytes start at index 0)
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageVersionBitsRangeGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte);

    /**
     * Requests the bits of a package being used to create a child resource be stream down to the caller over
     * the given output stream. This method will <b>not</b> take care of closing the stream when it is finished;
     * it is the caller's responsibility. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * @param parentResourceId  identifies the parent resource under which the new resource is being created
     * @param resourceTypeName  type of child resource being created
     * @param packageDetailsKey package being used to create the child resource
     * @param outputStream      an output stream where the server should write the package contents. It is up to the
     *                          caller to prepare this output stream in order to write the package content to an
     *                          appropriate location.
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageBitsForChildResource(int parentResourceId, String resourceTypeName,
        PackageDetailsKey packageDetailsKey, OutputStream outputStream);

    /**
     * Requests the bits of a package be streamed down to the caller over the given output stream. 
     * This method will <b>not</b> take care of closing the stream when it is finished;
     * it is the caller's responsibility. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * @param packageVersion    packageVersion to fetch 
     * @param outputStream      an output stream where the server should write the package contents. It is up to the
     *                          caller to prepare this output stream in order to write the package content to an
     *                          appropriate location.
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageVersionBits(PackageVersion packageVersion, OutputStream outputStream);

    /**
     * Requests a range of bits from a package.  This range of bits will be streamed down to the caller over the given 
     * output stream.  This method will <b>not</b> take care of closing the stream when it is finished;
     * it is the caller's responsibility. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * @param packageVersion    packageVersion to fetch 
     * @param outputStream      an output stream where the server should write the package contents. It is up to the
     *                          caller to prepare this output stream in order to write the package content to an
     *                          appropriate location.
     * @param startByte         start index
     * @param endByte           end index
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageVersionBits(PackageVersion packageVersion, OutputStream outputStream, long startByte, long endByte);

    /**
     * Requests the bits of a distribution file be streamed down to the caller over the given output stream.
     * This method will <b>not</b> take care of closing the stream when it is finished;
     * it is the caller's responsibility.
     *
     * @param distFile          distribution file to fetch
     * @param outputStream      an output stream where the server should write the package contents. It is up to the
     *                          caller to prepare this output stream in order to write the package content to an
     *                          appropriate location.
     *
     * @return the number of bytes written to the output stream
     */
    long outputDistributionFileBits(DistributionFile distFile, OutputStream outputStream);

    /**
     * Adds the specified content source to the database but does not attempt to create or start
     * the server-side plugin provider implementation associated with it.
     * <p/>
     * This should only be used for test purposes.
     *
     * @param subject       may not be <code>null</code>
     * @param contentSource may not be <code>null</code>
     * @return instance after being persisted; will contain a populated ID value
     * @throws ContentSourceException if the content source cannot be created, such as if the data in
     *                                the given object are not valid
     */
    ContentSource simpleCreateContentSource(Subject subject, ContentSource contentSource) throws ContentSourceException;
    
    /**
     * Returns the latest package version of the supplied package as deemed by the supplied comparator.
     * The supplied comparator is taken as an override to the default one to use.
     * The default comparator is determined using the following algorithm:
     * <ol>
     * <li>Find the first content provider defining the package that defines a non-null comparator and return that
     * <li>If no content provider provides explicit comparator, use {@link PackageVersion#DEFAULT_COMPARATOR}
     * </ol>
     * 
     * @param packageId the id of the package to find the latest version for.
     * @param versionComparator if left null, the comparator to use is determined by the rules above
     * @return
     */
    PackageVersion getLatestPackageVersion(int packageId, Comparator<PackageVersion> versionComparator);    
} 
