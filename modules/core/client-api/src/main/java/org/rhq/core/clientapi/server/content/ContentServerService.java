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
package org.rhq.core.clientapi.server.content;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.content.transfer.ContentDiscoveryReport;
import org.rhq.core.domain.content.transfer.DeployPackagesResponse;
import org.rhq.core.domain.content.transfer.RemovePackagesResponse;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Server-side interface for interacting with the server's content subsystem.
 */
public interface ContentServerService {
    /**
     * Concurrency control parameter to limit the number of content reports that are handled.
     */
    String CONCURRENCY_LIMIT_CONTENT_REPORT = "rhq.server.concurrency-limit.content-report";

    /**
     * Concurreny control setting to limit the number of packages that can be downloaded.
     */
    String CONCURRENCY_LIMIT_CONTENT_DOWNLOAD = "rhq.server.concurrency-limit.content-download";

    /**
     * Sends a set of newly discovered packages to the server. The collection of packagesrepresents the current set of
     * packages deployed on the specified resource. As such, entries may be either new packages or packages that have
     * been returned from a previous discovery. Any packages that were known for the resource that are not in this
     * collection of package are considered deleted from the resource.
     *
     * @param report report containing the current set of packages installed on the resource.
     */
    @Asynchronous(guaranteedDelivery = true)
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_REPORT)
    void mergeDiscoveredPackages(ContentDiscoveryReport report);

    /**
     * Informs the server that a previous request to deploy a package has completed.
     *
     * @param response indicates the original request and the result of executing it
     */
    @Asynchronous(guaranteedDelivery = true)
    void completeDeployPackageRequest(DeployPackagesResponse response);

    /**
     * Informs the server that a previous request to delete a package has completed.
     *
     * @param response indicates the original request and the result of executing it
     */
    @Asynchronous(guaranteedDelivery = true)
    void completeDeletePackageRequest(RemovePackagesResponse response);

    /**
     * Informs the server that a previous request to get a package's bits has completed.
     *
     * @param response      indicates the original request and the result of executing it
     * @param contentStream stream of the package bits being retrieved
     */
    @Asynchronous(guaranteedDelivery = true)
    void completeRetrievePackageBitsRequest(ContentServiceResponse response, InputStream contentStream);

    /**
     * Informs the server that a package installation (as indicated in the specified request ID) requires dependency
     * packages to be installed. The server will look in its package store to make sure the correct package versions can
     * be found. The server will take the necessary server-side actions to ensure the integrity of the initial request
     * (such as attaching the dependency packages to the initial request entity). In the case of a failure, the server
     * should not mark the request entity as a failure; the natural workflow of a failure response coming back to the
     * server from the agent will take care of that. This method should return the fully populated package descriptions.
     * If the package was not found in the package database, it will be omitted from the returned set.
     *
     * @param  requestId          refers back to the request ID of the package deployment for which these dependencies
     *                            were found
     * @param  dependencyPackages provides information on the dependency package (name, type, version, architecture);
     *
     * @return fully populated metadata on the packages known to the server for each of the specified keys; if the
     *         server does not know about a package, it will not be present in the returned set
     */
    Set<ResourcePackageDetails> loadDependencies(int requestId, Set<PackageDetailsKey> dependencyPackages);

    /**
     * Requests that the server download and stream the bits for the specified package. If the package cannot be found,
     * an exception will be thrown.
     *
     * @param  resourceId        identifies the resource to which the bits will be installed
     * @param  packageDetailsKey identifies the package to download
     * @param  outputStream      an output stream where the server should write the package contents. It is up to the
     *                           caller to prepare this output stream in order to write the package content to an
     *                           appropriate location.
     *
     * @return the number of bytes written to the output stream - this is the size of the package version that was
     *         downloaded
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    long downloadPackageBitsGivenResource(int resourceId, PackageDetailsKey packageDetailsKey, OutputStream outputStream);

    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    long downloadPackageBits(int resourceId, PackageDetailsKey packageDetailsKey, OutputStream outputStream);

    /**
     * Requests that the server download and stream the bits for the specified package. If the package cannot be found,
     * an exception will be thrown.
     *
     * @param  resourceId        identifies the resource to which the bits will be installed
     * @param  packageDetailsKey identifies the package to download
     * @param  outputStream      an output stream where the server should write the package contents. It is up to the
     *                           caller to prepare this output stream in order to write the package content to an
     *                           appropriate location.
     * @param  startByte         the first byte (inclusive) of the byte range to retrieve and output (bytes start at
     *                           index 0)
     * @param  endByte           the last byte (inclusive) of the byte range to retrieve and output (-1 means up to EOF)
     *                           (bytes start at index 0)
     *
     * @return the number of bytes written to the output stream - this is the size of the chunk downloaded
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    long downloadPackageBitsRangeGivenResource(int resourceId, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte);

    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    long downloadPackageBitsRange(int resourceId, PackageDetailsKey packageDetailsKey, OutputStream outputStream,
        long startByte, long endByte);

    /**
     * Requests all {@link PackageVersion#getMetadata() metadata} for all package versions that the given resource
     * component is subscribed to (see {@link Channel#getResources()}. The returned object has the metadata bytes that
     * are meaningful to the calling plugin component.
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
     */
    @LimitedConcurrency(CONCURRENCY_LIMIT_CONTENT_DOWNLOAD)
    PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(int resourceId, PageControl pc);

    /**
     * Gets the MD5 hash of the resource's "content subscription". If any changes were made to the channels this
     * resource is subscribed to, a changed MD5 will be returned.
     *
     * @param  resourceId identifies the resource requesting the MD5; if any change to any package version in any
     *                    resource's subscribed channels will be used when generating the MD5
     *
     * @return the MD5 of all package versions' metadata
     *
     * @see    #getPackageVersionMetadata(int, PageControl)
     */
    String getResourceSubscriptionMD5(int resourceId);

    /**
     * Requests the size, in bytes, of the identified package version.
     *
     * @param  resourceId        identifies the resource requesting the info
     * @param  packageDetailsKey identifies the package whose size is to be returned
     *
     * @return the size, in number of bytes, of the package version
     */
    long getPackageBitsLength(int resourceId, PackageDetailsKey packageDetailsKey);
}