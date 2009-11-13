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
 * Indicates a content provider has the capability to provide distributions into the server.
 * Distribution synchronization will be done through the calls defined in this interface.
 *
 * @author Pradeep Kilambi
 */
public interface DistributionSource {

    /**
     * Requests that this distribution source perform a synchronization with its external repository.
     *
     * @param repoName          repo name used to look up distributions
     * @param report           used to accumulate the collection of distros
     * @throws Exception if the source is unable to perform the synchronization, for instance if the external
     *                   source cannot be connected to.
     */
    void synchronizeDistribution(String repoName, DistributionSyncReport report,
                                 Collection<DistributionDetails> existingDistros)
        throws Exception;

    /**
     * Get an input stream for the specified distribution.
     *
     * @param location The location of the distribution.
     * @return An initialized input stream. <b>The caller is responsible for closing the stream.</b>
     * @throws Exception if failed to obtain the stream to the remote distribution data
     */
    InputStream getInputStream(String location) throws Exception;

}

