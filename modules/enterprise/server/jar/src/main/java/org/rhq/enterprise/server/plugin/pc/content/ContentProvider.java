/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.plugin.pc.content;

import org.rhq.core.domain.configuration.Configuration;

/**
 * Interface used by the server to communicate with a content source. Content sources implement this interface as
 * well as any other source interfaces to further describe what functionality is provided by the content source.
 * <p/>
 * The class name of this interface's implementation is the class name specified in the server plugin descriptor.
 *
 * @author Jason Dobies
 * @author John Mazzitelli
 *
 * @see RepoSource
 * @see PackageSource
 */
public interface ContentProvider {

    /**
     * Initializes the adapter with the configuration values indicating how to connect to the external source system.
     *
     * @param  configuration user entered values describing how the content source should function.
     *
     * @throws Exception if the content source has an issue being configured.
     */
    void initialize(Configuration configuration) throws Exception;

    /**
     * Informs the adapter that it should shutdown.
     */
    void shutdown();

    /**
     * Tests if the content source can communicate with the external package source. This will be called after
     * {@link #initialize(Configuration)} to ensure the adapter is in a configured state to run the test.
     *
     * @throws Exception if the connection cannot be made to the external system.
     */
    void testConnection() throws Exception;

    /**
     * Fetch the Map of estimated 'weights' for the individual steps for syncing a ContentProvider.
     * 
     * Steps include:
     * 
     * Package Metadata
     * Package Bits
     * Distribution Metadata
     * Distribution Bits
     * Advisory (Errata) Metadata
     * Advisory Bits ?
     */
    SyncProgressWeight getSyncProgressWeight();
}