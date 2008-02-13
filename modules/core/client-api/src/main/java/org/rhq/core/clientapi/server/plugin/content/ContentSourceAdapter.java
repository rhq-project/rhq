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
package org.rhq.core.clientapi.server.plugin.content;

import java.io.InputStream;
import java.util.Collection;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.ContentSource;

/**
 * Interface used by the server to communicate with a content source. Content source types implement this interface,
 * with the implementing class name defined in the content source type definition.
 *
 * <p>The class name of this interface's implementation is the API class name specified in the server plugin
 * descriptor.</p>
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 */
public interface ContentSourceAdapter {
    /**
     * Initializes the adapter with the configuration values indicating how to connect to the external source system.
     *
     * @param  configuration user entered values describing how the content source should function.
     *
     * @throws Exception if the content source has an issue being configured.
     *
     * @see    ContentSource#getConfiguration()
     */
    void initialize(Configuration configuration) throws Exception;

    /**
     * Informs the adapter that it should shutdown.
     */
    void shutdown();

    /**
     * Tests if the content source adapter can communicate with the external package source. This will be called after
     * {@link #initialize(Configuration)} to ensure the adapter is in a configured state to run the test.
     *
     * @throws Exception if the connection cannot be made to the external system.
     */
    void testConnection() throws Exception;

    /**
     * Requests that the content source perform a synchronization with its external package source.
     *
     * @param  report           used to populate the packages diff information necessary to bring the server up to date
     *                          with the current state of the external package source.
     * @param  existingPackages collection of packages the server currently has in its inventory for this content
     *                          source; used when determining package diff information for the report.
     *
     * @throws Exception if the content source is unable to perform the synchronization, for instance if the external
     *                   source cannot be connected to.
     */
    void synchronizePackages(PackageSyncReport report, Collection<ContentSourcePackageDetails> existingPackages)
        throws Exception;

    /**
     * Get an input stream for the specified package.
     *
     * @param  location The location of the package. This is an adapter specific location that was originally set in the
     *                  PackageDetails object as part of the synch report.
     *
     * @return An initialized input stream. <b>The caller is responsible for closing the stream.</b>
     *
     * @throws Exception if failed to obtain the stream to the remote package data
     */
    InputStream getInputStream(String location) throws Exception;
}