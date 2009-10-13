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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A RepoGroup represents a set of related {@link Repo}s. Repos can be tied together
 *  by different (@link RepoGroupType)s.
 *
 * @author Sayli Karmarkar
 */

public class RepoGroup implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    //@Column(name = "ID", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    //@Id
    private int id;

    //@Column(name = "NAME", nullable = false)
    private String name;

    //@Column(name = "DESCRIPTION", nullable = true)
    private String description;

    //@JoinColumn(name = "REPO_GROUP_TYPE_ID", referencedColumnName = "ID", nullable = false)
    //@ManyToOne
    private RepoGroupType repoGroupType;

    //@OneToMany(mappedBy = "contentSource", fetch = FetchType.LAZY)
    private Set<RepoRepoGroup> repoRepoGroups;

    // Constructor ----------------------------------------

    public RepoGroup() {
        // for JPA use
    }

    public RepoGroup(String name) {
        this.name = name;
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Programmatic name of the repoGroup.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * User specified description of the repoGroup.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Describes the capabilities of this repoGroup.
     */
    public RepoGroupType getRepoGroupType() {
        return repoGroupType;
    }

    public void setRepoGroupType(RepoGroupType repoGroupType) {
        this.repoGroupType = repoGroupType;
    }

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepoGroup()
     */
    public Set<RepoRepoGroup> getRepoRepoGroups() {
        return repoRepoGroups;
    }

    /**
     * The repos that this repo group is associated with.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated repos, use
     * {@link #getRepoRepoGroups()} or {@link #addRepo(Repo)}, {@link #removeRepo(Repo)}.</p>
     */
    public Set<Repo> getRepos() {
        HashSet<Repo> repos = new HashSet<Repo>();

        if (repoRepoGroups != null) {
            for (RepoRepoGroup ccs : repoRepoGroups) {
                repos.add(ccs.getRepoRepoGroupPK().getRepo());
            }
        }

        return repos;
    }

    /**
     * Directly assign a repo to this repogroup.
     *
     * @param  repo
     *
     * @return the mapping that was added
     */
    public RepoRepoGroup addRepo(Repo repo) {
        if (this.repoRepoGroups == null) {
            this.repoRepoGroups = new HashSet<RepoRepoGroup>();
        }

        RepoRepoGroup mapping = new RepoRepoGroup(repo, this);
        this.repoRepoGroups.add(mapping);
        repo.addRepoGroup(this);
        return mapping;
    }

    /**
     * Removes the repo from this repogroup, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given repo did not did not belong to this repo group, <code>null</code> is
     * returned.
     *
     * @param  repo the repo to remove from this repo group
     *
     * @return the mapping that was removed or <code>null</code> if the repo did not belong to this repo group
     */
    public RepoRepoGroup removeRepo(Repo repo) {
        if ((this.repoRepoGroups == null) || (repo == null)) {
            return null;
        }

        RepoRepoGroup doomed = null;

        for (RepoRepoGroup rrg : this.repoRepoGroups) {
            if (repo.equals(rrg.getRepoRepoGroupPK().getRepo())) {
                doomed = rrg;
                repo.removeRepoGroup(this);
                break;
            }
        }

        if (doomed != null) {
            this.repoRepoGroups.remove(doomed);
        }

        return doomed;
    }

    @Override
    public String toString() {
        return "RepoGroup: id=[" + this.id + "], name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoGroup))) {
            return false;
        }

        final RepoGroup other = (RepoGroup) obj;

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }
}
