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

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Channel;
import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
public interface ChannelManagerRemote {
    /**
     * @see ChannelManagerLocal#deleteChannel(Subject, int)
     */
    void deleteChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId);

    /**
     * @see ChannelManagerLocal#getAllChannels(Subject, PageControl)
     */
    PageList<Channel> getAllChannels(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ChannelManagerLocal#getChannel(Subject, int)
     */
    Channel getChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId);

    /**
     * @see ChannelManagerLocal#getAssociatedContentSources(Subject, int, PageControl)
     */
    PageList<ContentSource> getAssociatedContentSources(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ChannelManagerLocal#getSubscribedResources(Subject, int, PageControl)
     */
    PageList<Resource> getSubscribedResources(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ChannelManagerLocal#getPackageVersionsInChannel(Subject, int, String, PageControl)
     */
    PageList<PackageVersion> getPackageVersionsInChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId, @WebParam(name = "filter")
    String filter, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ChannelManagerLocal#updateChannel(Subject, Channel)
     */
    Channel updateChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channel")
    Channel channel);

    /**
     * @see ChannelManagerLocal#createChannel(Subject, Channel)
     */
    Channel createChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channel")
    Channel channel);

    /**
     * @see ChannelManagerLocal#addContentSourcesToChannel(Subject, int, int[])
     */
    void addContentSourcesToChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId, @WebParam(name = "contentSourceIds")
    int[] contentSourceIds) throws Exception;

    /**
     * @see ChannelManagerLocal#removeContentSourcesFromChannel(Subject, int, int[])
     */
    void removeContentSourcesFromChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "channelId")
    int channelId, @WebParam(name = "contentSourceIds")
    int[] contentSourceIds) throws Exception;

    /**
     * @see ChannelManagerLocal#subscribeResourceToChannels(Subject, int, int[])
     */
    void subscribeResourceToChannels(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "resourceId")
    int resourceId, @WebParam(name = "channelIds")
    int[] channelIds);

    /**
     * @see ChannelManagerLocal#unsubscribeResourceFromChannels(Subject, int, int[])
     */
    void unsubscribeResourceFromChannels(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "resourceId")
    int resourceId, @WebParam(name = "channelIds")
    int[] channelIds);

    /**
     * @see ChannelManagerLocal#getPackageVersionCountFromChannel(Subject, int)
     */
    long getPackageVersionCountFromChannel(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "filter")
    String filter, @WebParam(name = "channelId")
    int channelId);
}