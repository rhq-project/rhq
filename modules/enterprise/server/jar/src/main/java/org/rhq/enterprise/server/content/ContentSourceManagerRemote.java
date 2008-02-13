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
     * @see ContentSourceManagerLocal#deleteContentSource(Subject, int)
     */
    void deleteContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

    /**
     * @see ContentSourceManagerLocal#getAllContentSourceTypes()
     */
    Set<ContentSourceType> getAllContentSourceTypes();

    /**
     * @see ContentSourceManagerLocal#getAllContentSources(Subject, PageControl)
     */
    PageList<ContentSource> getAllContentSources(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ContentSourceManagerLocal#getContentSourceType(String)
     */
    ContentSourceType getContentSourceType(@WebParam(name = "name")
    String name);

    /**
     * @see ContentSourceManagerLocal#getContentSource(Subject, int)
     */
    ContentSource getContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

    /**
     * @see ContentSourceManagerLocal#getContentSourceByNameAndType(Subject, String, String)
     */
    ContentSource getContentSourceByNameAndType(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "name")
    String name, @WebParam(name = "typeName")
    String typeName);

    /**
     * @see ContentSourceManagerLocal#getAssociatedChannels(Subject, int, PageControl)
     */
    PageList<Channel> getAssociatedChannels(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ContentSourceManagerLocal#getContentSourceSyncResults(Subject, int, PageControl)
     */
    PageList<ContentSourceSyncResults> getContentSourceSyncResults(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ContentSourceManagerLocal#deleteContentSourceSyncResults(Subject, int[])
     */
    void deleteContentSourceSyncResults(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "id")
    int[] ids);

    /**
     * Create a {@link ContentSource} object from the values passed in. If there is no {@link ContentSourceType} with
     * the name given in <code>typeName</code>, then a runtime exception is thrown.
     *
     * @param  subject       the user that is making the request
     * @param  name
     * @param  description
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
     * @see ContentSourceManagerLocal#updateContentSource(Subject, ContentSource)
     */
    ContentSource updateContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSource")
    ContentSource contentSource);

    /**
     * @see ContentSourceManagerLocal#testContentSourceConnection(int)
     */
    boolean testContentSourceConnection(@WebParam(name = "contentSourceId")
    int contentSourceId);

    /**
     * @see ContentSourceManagerLocal#synchronizeAndLoadContentSource(Subject, int)
     */
    void synchronizeAndLoadContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);

    /**
     * @see ContentSourceManagerLocal#getPackageVersionsFromContentSource(Subject, int, PageControl)
     */
    PageList<PackageVersionContentSource> getPackageVersionsFromContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId, @WebParam(name = "pc")
    PageControl pc);

    /**
     * @see ContentSourceManagerLocal#getPackageVersionCountFromContentSource(Subject, int)
     */
    long getPackageVersionCountFromContentSource(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "contentSourceId")
    int contentSourceId);
}