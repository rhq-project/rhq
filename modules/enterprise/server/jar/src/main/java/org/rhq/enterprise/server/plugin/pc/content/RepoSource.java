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

/**
 * Indicates a content source has the capability to provide repos into the server. Repo synchronization will be
 * done through calls defined in this interface. This interface should use the configuration settings
 * passed into the plugin through {@link ContentProvider#initialize(org.rhq.core.domain.configuration.Configuration)}
 *
 * @author Jason Dobies
 */
public interface RepoSource {

    /**
     * Requests the content source return a list of all repos it wants to ensure exist in the server. This call
     * should return the current state of the repos as known to the content source, not a diff of what has
     * changed from the previous call to it. The server will resolve the differences.
     *
     * @return contains a list of all groups and repos to be imported into the system
     * @throws Exception if there is an error retrieving the repos that should be reported to the user
     */
    RepoImportReport importRepos() throws Exception;

}
