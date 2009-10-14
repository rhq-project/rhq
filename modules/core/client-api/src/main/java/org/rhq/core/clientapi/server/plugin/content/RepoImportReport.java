/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.core.clientapi.server.plugin.content;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Holds on to the repos and repo groups to be imported into the server through a {@link RepoSource}.
 *
 * @author Jason Dobies
 */
public class RepoImportReport {

    private List<RepoDetails> repos = new ArrayList<RepoDetails>();
    private List<RepoGroupDetails> repoGroups = new ArrayList<RepoGroupDetails>();

    public RepoImportReport() {
    }

    public List<RepoDetails> getRepos() {
        return repos;
    }

    public void addRepo(RepoDetails repo) {
        repos.add(repo);
    }

    public void addRepos(Collection<RepoDetails> repos) {
        repos.addAll(repos);
    }

    public List<RepoGroupDetails> getRepoGroups() {
        return repoGroups;
    }

    public void addRepoGroup(RepoGroupDetails repoGroup) {
        repoGroups.add(repoGroup);
    }

    public void addRepoGroups(Collection<RepoGroupDetails> repoGroups) {
        repoGroups.addAll(repoGroups);
    }
}
