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

import java.io.InputStream;
import java.util.Collection;

/**
 * Indicates a content source has the capability to provide packages into the server. Package synchronization will
 * be done through the calls defined in this interface. This interface should use the configuration settings
 * passed into the plugin through {@link ContentProvider#initialize(org.rhq.core.domain.configuration.Configuration)}
 *
 * @author Jason Dobies
 */
public interface PackageSource {

    /**
     * Requests that this package source perform a synchronization with its external package source.
     *
     * @param repoName         indicates the repo from which packages should be retrieved; may be ignored
     *                         if the package source does not support the concept of repos 
     * @param report           used to populate the packages diff information necessary to
     *                         bring the server up to date
     *                         with the current state of the external package source.
     * @param existingPackages collection of packages the server currently has in its inventory for this package
     *                         source; used when determining package diff information for the report.
     * @throws Exception if the package source is unable to perform the synchronization, for instance if the external
     *                   source cannot be connected to.
     */
    void synchronizePackages(String repoName, PackageSyncReport report,
        Collection<ContentProviderPackageDetails> existingPackages) throws SyncException, InterruptedException;

    /**
     * Get an input stream for the specified package.
     *
     * @param location The location of the package. This is an adapter specific location that was originally set in the
     *                 PackageDetails object as part of the synch report.
     * @return An initialized input stream. <b>The caller is responsible for closing the stream.</b>
     * @throws Exception if failed to obtain the stream to the remote package data
     */
    InputStream getInputStream(String location) throws Exception;
}
