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
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetails;
import org.rhq.core.clientapi.server.plugin.content.ContentSourcePackageDetailsKey;
import org.rhq.core.clientapi.server.plugin.content.PackageSyncReport;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Interface that provides access to the {@link ContentSource} objects deployed in the server, allowing the callers to
 * access data about and from remote content repositories.
 */
@Local
public interface ContentSourceManagerLocal {
    /**
     * This will look for any {@link PackageVersion}s that are "orphaned" (that is, is not related to any existing
     * content source or channel and is not installed anywhere) and will remove any orphans that it finds. This means it
     * will delete orphaned {@link PackageVersion} definitions and (if loaded) their {@link PackageBits}.
     *
     * @param subject user requesting the purge
     */
    void purgeOrphanedPackageVersions(Subject subject);

    /**
     * @see ContentSourceManagerRemote#deleteContentSource(Subject, int)
     */
    void deleteContentSource(Subject subject, int contentSourceId);

    /**
     * @see ContentSourceManagerRemote#getAllContentSourceTypes()
     */
    Set<ContentSourceType> getAllContentSourceTypes();

    /**
     * @see ContentSourceManagerRemote#getAllContentSources(Subject, PageControl)
     */
    PageList<ContentSource> getAllContentSources(Subject subject, PageControl pc);

    /**
     * @see ContentSourceManagerRemote#getContentSourceType(String)
     */
    ContentSourceType getContentSourceType(String name);

    /**
     * @see ContentSourceManagerRemote#getContentSource(Subject, int)
     */
    ContentSource getContentSource(Subject subject, int contentSourceId);

    /**
     * @see ContentSourceManagerRemote#getContentSourceByNameAndType(Subject, String, String)
     */
    ContentSource getContentSourceByNameAndType(Subject subject, String name, String typeName);

    /**
     * @see ContentSourceManagerRemote#getAssociatedChannels(Subject, int, PageControl)
     */
    PageList<Channel> getAssociatedChannels(Subject subject, int contentSourceId, PageControl pc);

    /**
     * @see ContentSourceManagerRemote#getContentSourceSyncResults(Subject, int, PageControl)
     */
    PageList<ContentSourceSyncResults> getContentSourceSyncResults(Subject subject, int contentSourceId, PageControl pc);

    /**
     * @see ContentSourceManagerRemote#deleteContentSourceSyncResults(Subject, int[])
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
    ContentSource createContentSource(Subject subject, ContentSource contentSource);

    /**
     * @see ContentSourceManagerRemote#updateContentSource(Subject, ContentSource)
     */
    ContentSource updateContentSource(Subject subject, ContentSource contentSource);

    /**
     * @see ContentSourceManagerRemote#testContentSourceConnection(int)
     */
    boolean testContentSourceConnection(int contentSourceId);

    /**
     * @see ContentSourceManagerRemote#synchronizeAndLoadContentSource(Subject, int)
     */
    void synchronizeAndLoadContentSource(Subject subject, int contentSourceId);

    /**
     * @see ContentSourceManagerRemote#getPackageVersionsFromContentSource(Subject, int, PageControl)
     */
    PageList<PackageVersionContentSource> getPackageVersionsFromContentSource(Subject subject, int contentSourceId,
        PageControl pc);

    /**
     * @see ContentSourceManagerRemote#getPackageVersionCountFromContentSource(Subject, int)
     */
    long getPackageVersionCountFromContentSource(Subject subject, int contentSourceId);

    /**
     * Returns the length of the package version identified by its {@link PackageDetailsKey}. This method ensures that
     * the given resource is subscribed to a channel that contains the package version.
     *
     * @param  resourceId
     * @param  packageDetailsKey
     *
     * @return teh length of the package version
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
    PageList<PackageVersionContentSource> getUnloadedPackageVersionsFromContentSource(Subject subject,
        int contentSourceId, PageControl pc);

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
    ContentSourceSyncResults mergeContentSourceSyncReport(ContentSource contentSource, PackageSyncReport report,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous, ContentSourceSyncResults syncResults);

    void _mergeContentSourceSyncReportUpdateChannel(int contentSourceId);

    ContentSourceSyncResults _mergeContentSourceSyncReportREMOVE(ContentSource contentSource, PackageSyncReport report,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress);

    ContentSourceSyncResults _mergeContentSourceSyncReportADD(ContentSource contentSource,
        Collection<ContentSourcePackageDetails> newPackages,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress, int addCount);

    ContentSourceSyncResults _mergeContentSourceSyncReportUPDATE(ContentSource contentSource, PackageSyncReport report,
        Map<ContentSourcePackageDetailsKey, PackageVersionContentSource> previous,
        ContentSourceSyncResults syncResults, StringBuilder progress);

    /**
     * Requests all {@link PackageVersion#getMetadata() metadata} for all package versions that the given resource
     * component is subscribed to (see {@link Channel#getResources()}. The returned object has the metadata bytes that
     * are meaningful to the calling plugin component.
     *
     * <p>Note that the returned object has the package version IDs that can be used to retrieve the actual content bits
     * of the package versions via a call to {@link #retrievePackageVersionBits(int, int)}.</p>
     *
     * <p>Callers should consider caching the returned metadata. Use {@link #getResourceSubscriptionMD5(int)} to get the
     * MD5 hashcode of the metadata for the resource to aid in determining when a cache of metadata is stale.</p>
     *
     * @param  resourceId identifies the resource requesting the data; all package versions in all the resource's
     *                    subscribed channels will be represented in the returned map
     * @param  pc         this method can potentially return a large set; this page control object allows the caller to
     *                    page through that large set, as opposed to requesting the entire set in one large chunk
     *
     * @return the list of all package versions' metadata
     *
     * @see    #getResourceSubscriptionMD5(int)
     */
    PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(int resourceId, PageControl pc);

    /**
     * Gets the MD5 hash which identifies a resource "content subscription". This MD5 hash will change when any channel
     * the resource is subscribed to has changed its contents (that is, if a package version was added/updated/removed
     * from it).
     *
     * @param  resourceId identifies the resource requesting the MD5; any change to any package version in any of the
     *                    resource's subscribed channels will determine the MD5
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
     *                           package version (due to the fact that it is not subscribed to a channel that is serving
     *                           that package version), an exception is thrown
     * @param  packageDetailsKey identifies the {@link PackageVersion} whose {@link PackageBits} are to be streamed
     * @param  outputStream      a stream that the caller prepared where this method will write the actual content
     *
     * @return the number of bytes written to the output stream
     */
    long outputPackageVersionBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream);

    long outputPackageVersionBits(int resourceTypeId, PackageDetailsKey packageDetailsKey, OutputStream outputStream);

    /**
     * Requests that the actual content data (the "bits") of the identified package version be streamed down to the
     * caller over the given output stream that the caller provides. This method will <b>not</b> be responsible for
     * closing the stream when its done, the caller needs to close it. This may be a time-consuming method call because
     * if the bits have not yet been loaded (i.e. the content source where the package version lives
     * {@link ContentSource#isLazyLoad() is lazy loading} then this may be the time when it is downloaded from the
     * remote repository.
     *
     * @param  resourceId        identifies the resource making the request; if this resource is not allowed to see the
     *                           package version (due to the fact that it is not subscribed to a channel that is serving
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

    long outputPackageVersionBitsRange(int resourceTypeId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte);
}