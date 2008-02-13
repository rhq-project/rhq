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

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

@Local
public interface ChannelManagerLocal {
    /**
     * Deletes the identified channel. If this deletion orphans package versions (that is, its originating resource or
     * content source has been deleted), this will also purge those orphaned package versions.
     *
     * @param subject
     * @param channelId
     */
    void deleteChannel(Subject subject, int channelId);

    /**
     * Returns all {@link Channel} objects that are configured in the system.
     *
     * @param  subject user asking to perform this
     * @param  pc      pagination controls
     *
     * @return all channels sources
     */
    PageList<Channel> getAllChannels(Subject subject, PageControl pc);

    /**
     * Returns the {@link Channel} from its ID.
     *
     * @param  subject   user asking to perform this
     * @param  channelId identifies the channel to return
     *
     * @return the channel object, <code>null</code> if the ID is invalid
     */
    Channel getChannel(Subject subject, int channelId);

    /**
     * Gets all content sources that are associated with the given channel.
     *
     * @param  subject
     * @param  channelId
     * @param  pc
     *
     * @return the list of content sources
     */
    PageList<ContentSource> getAssociatedContentSources(Subject subject, int channelId, PageControl pc);

    /**
     * Gets all resources that are subscribed to the given channel.
     *
     * @param  subject
     * @param  channelId
     * @param  pc
     *
     * @return the list of subscribers
     */
    PageList<Resource> getSubscribedResources(Subject subject, int channelId, PageControl pc);
   
    /**
     * Gets all channels that are subscribed to by the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of subscriptions
     */
    PageList<Channel> getResourceSubscriptions(Subject subject, int resourceId, PageControl pc);
    
    /**
     * Gets all channels that aren't subscribed to for the given resource.
     *
     * @param  subject
     * @param  resourceId
     * @param  pc
     *
     * @return the list of available channels for the given resource
     */
    PageList<Channel> getAvailableResourceSubscriptions(Subject subject, int resourceId, PageControl pc);

    /**
     * Returns the set of package versions that can currently be accessed via the given channel.
     *
     * @param  subject   user asking to perform this
     * @param  channelId identifies the channel
     * @param  pc        pagination controls
     *
     * @return the package versions that are available in the channel
     */
    PageList<PackageVersion> getPackageVersionsInChannel(Subject subject, int channelId, PageControl pc);

    PageList<PackageVersion> getPackageVersionsInChannel(Subject subject, int channelId, String filter, PageControl pc);

    /**
     * Update an existing {@link Channel} object's basic fields, like name, description, etc. Note that the given <code>
     * channel</code>'s relationships will be ignored and not merged with the existing channel (e.g. is subscribed
     * resources will not be changed, regardless of what the given channel's subscribed resources set it). See methods
     * like {@link #addContentSourcesToChannel(Subject, int, int[])} to alter its relationships.
     *
     * @param  subject wanting to update the ContentSource
     * @param  channel to be updated
     *
     * @return Channel that was updated
     */
    Channel updateChannel(Subject subject, Channel channel);

    /**
     * Creates a new {@link Channel}. Note that the created channel will not have any content sources assigned and no
     * resources will be subscribed. It is a virgin channel.
     *
     * @param  subject the user asking to do the creation
     * @param  channel
     *
     * @return the newly created channel
     */
    Channel createChannel(Subject subject, Channel channel);

    /**
     * Adds the content sources (identified by their IDs) to the given channel (also identified by its ID). This will
     * associate all package versions that come from the content source to the channel.
     *
     * @param  subject          the user asking to perform this
     * @param  channelId        the ID of the channel to get the new content sources
     * @param  contentSourceIds the list of content source IDs to add to the channel
     *
     * @throws Exception if the channel or one of the content sources doesn't exist or if the addition failed
     */
    void addContentSourcesToChannel(Subject subject, int channelId, int[] contentSourceIds) throws Exception;

    /**
     * Removes the content sources (identified by their IDs) from the given channel (also identified by its ID). If one
     * of the content sources is already not a member of the channel, it is simply ignored (i.e. an exception will not
     * be thrown).
     *
     * @param  subject          the user asking to perform this
     * @param  channelId        the ID of the channel to remove the content sources
     * @param  contentSourceIds the list of content source IDs to remove from the channel
     *
     * @throws Exception if the channel or one of the content sources doesn't exist or if the removal failed
     */
    void removeContentSourcesFromChannel(Subject subject, int channelId, int[] contentSourceIds) throws Exception;

    /**
     * Subscribes the identified resource to the set of identified channels. Once complete, the resource will be able to
     * access all package content from all content sources that are assigned to the given channels.
     *
     * @param subject
     * @param resourceId
     * @param channelIds
     */
    void subscribeResourceToChannels(Subject subject, int resourceId, int[] channelIds);

    /**
     * Unsubscribes the identified resource from all of the identified channels. Once complete, the resource will no
     * longer be able to access any package content from the content sources that are assigned to the given channels.
     *
     * @param subject
     * @param resourceId
     * @param channelIds
     */
    void unsubscribeResourceFromChannels(Subject subject, int resourceId, int[] channelIds);

    /**
     * Returns count of {@link PackageVersion}s associated with the given channel.
     *
     * @param  subject   caller requesting count
     * @param  channelId of channel
     *
     * @return count if any
     */
    long getPackageVersionCountFromChannel(Subject subject, int channelId);
}