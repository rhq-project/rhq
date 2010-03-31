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

import java.util.Collection;

/**
 * Indicates a content source has the capability to fetch advisory information per repo into the server.
 * Advisory synchronization will be done through the calls defined in this interface.
 *
 * @author Pradeep Kilambi
 */
public interface AdvisorySource {

    /**
     * Requests that this advisory source perform a synchronization with its external repository.
     *
     * @param repoName          repo name used to look up advisory
     * @param report           used to accumulate the collection of advisory
     * @throws Exception if the source is unable to perform the synchronization, for instance if the external
     *                   source cannot be connected to.
     */
    void synchronizeAdvisory(String repoName, AdvisorySyncReport report, Collection<AdvisoryDetails> existingAdvisory)
        throws SyncException, InterruptedException;

}
