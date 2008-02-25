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
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.ContentSourceSyncResults;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.core.domain.content.DownloadMode;
import org.rhq.core.domain.content.PackageVersionContentSource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * The remote interface to the Content Source Manager. This is mainly to provide easy to use methods for our web
 * clients.
 *
 * @author Mike McCune
 */
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
public interface ContentSourceManagerRemote {
    /**
     * Deletes the identified content source. Any package versions that originated from this content source but are
     * still related to one or more channels will remain.
     *
     * @param subject An authenticated user making the request.
     * @param contentSourceId The id of the content source to be deleted.
     */
    void deleteContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

    /**
     * Returns all {@link ContentSourceType} objects that are configured in the system.
     *
     * @return all content source types
     */
    Set<ContentSourceType> getAllContentSourceTypes();

    /**
     * Returns all {@link ContentSource} objects that are configured in the system.
     *
     * @param  subject user asking to perform this
     * @param  pc      pagination controls
     *
     * @return all content sources
     */
    PageList<ContentSource> getAllContentSources(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "pc")
    PageControl pc);

    /**
     * Get a {@link ContentSourceType} by name. <code>null</code> will be returned if there is no content source type by
     * that name.
     *
     * @param  name the name of the {@link ContentSourceType} to be returned
     *
     * @return {@link ContentSourceType} found. <code>null</code> if none found
     */
    ContentSourceType getContentSourceType(@WebParam(name = "name")
    String name);

    /**
     * Returns the {@link ContentSource} from its ID.
     *
     * @param  subject         user asking to perform this
     * @param  contentSourceId identifies the content source to return
     *
     * @return the content source object, <code>null</code> if the ID is invalid
     */
    ContentSource getContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

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
    ContentSource getContentSourceByNameAndType(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "name")
    String name, @WebParam(name = "typeName")
    String typeName);

    /**
     * Gets the list of channels that are associated with a given content source.
     *
     * @param  subject user asking to perform this
     * @param  contentSourceId The id of a content source.
     * @param  pc pagination controls
     *
     * @return list of associated channels
     */
    PageList<Channel> getAssociatedChannels(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * Allows the caller to page through a list of historical sync results for a content source.
     *
     * @param  subject user asking to perform this
     * @param  contentSourceId The id of a content source.
     * @param  pc pagination controls
     *
     * @return the list of results
     */
    PageList<ContentSourceSyncResults> getContentSourceSyncResults(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * Allow a user to purge content source sync results.
     *
     * @param subject  user asking to perform this
     * @param ids     the IDs of the {@link ContentSourceSyncResults} to delete
     */
    void deleteContentSourceSyncResults(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "id")
    int[] ids);

    /**
     * Create a {@link ContentSource} object from the values passed in. If there is no {@link ContentSourceType} with
     * the name given in <code>typeName</code>, then a runtime exception is thrown.
     *
     * @param  subject       the user that is making the request
     * @param  name the name of the content source
     * @param  description A description for the content source.
     * @param  typeName {@link ContentSourceType} name
     * @param  configuration the configuration settings needed to connect and use the content source
     * @param  lazyLoad      if <code>true</code> the content bits from this content source will only be loaded on demand
     *                       otherwise, all bits will be downloaded as soon as possible
     * @param  downloadMode  determines where package bits are stored (and even if they are to be stored at all)
     *
     * @return the newly created content source
     */
    ContentSource createContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "name")
    String name, @WebParam(name = "description")
    String description, @WebParam(name = "typeName")
    String typeName, @WebParam(name = "configuration")
    Configuration configuration, @WebParam(name = "lazyLoad")
    boolean lazyLoad, @WebParam(name = "downloadMode")
    DownloadMode downloadMode);

    /**
     * Update an existing {@link ContentSource} object and restarts its underlying adapter. This also forces the adapter
     * to immediately sync with the remote repository. Note that this will only update the content source's basic fields
     * like name, description, etc. as well as its configuration. Specifically, it will not update the other
     * relationships like its channels. Use {@link #addContentSourcesToChannel(Subject, int, int[])} for things like
     * that.
     *
     * @param  subject       wanting to update the ContentSource
     * @param  contentSource to be updated
     *
     * @return the ContentSource that was updated
     */
    ContentSource updateContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSource")
    ContentSource contentSource);

    /**
     * Given a content source ID, this will test that the adapter responsible for pulling data from the content source's
     * remote repository can actually connect to that repository.
     *
     * @param  contentSourceId The id of the content source on which to test the connection.
     *
     * @return <code>true</code> if the remote content souce can be reached
     */
    boolean testContentSourceConnection(@WebParam(name = "contentSourceId")
    int contentSourceId);

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
    void synchronizeAndLoadContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

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
    PageList<PackageVersionContentSource> getPackageVersionsFromContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * Returns count of PackageVersions associated with the given content source.
     *
     * @param  subject         caller requesting count
     * @param  contentSourceId to lookup
     *
     * @return count if any
     */
    long getPackageVersionCountFromContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);
}