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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * This is the many-to-many entity that correlates a repo with a repo group. 
 * It is an explicit relationship mapping entity between {@link Repo} 
 * and {@link RepoGroup}.
 *
 * @author Sayli Karmarkar
 */

@Entity
@IdClass(RepoRepoGroupPK.class)
@Table(name = "RHQ_REPO_REPO_GROUP_MAP")
public class RepoRepoGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    @ManyToOne
    @JoinColumn(name = "REPO_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Repo repo;

    @Id
    @ManyToOne
    @JoinColumn(name = "REPO_GROUP_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private RepoGroup repoGroup;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    protected RepoRepoGroup() {
    }

    public RepoRepoGroup(Repo repo, RepoGroup repoGroup) {
        this.repo = repo;
        this.repoGroup = repoGroup;
    }

    public RepoRepoGroupPK getRepoRepoGroupPK() {
        return new RepoRepoGroupPK(repo, repoGroup);
    }

    public void setRepoRepoGroupPK(RepoRepoGroupPK pk) {
        this.repo = pk.getRepo();
        this.repoGroup = pk.getRepoGroup();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the repo was first associated
     * with the repo group.
     */
    public long getCreatedTime() {
        return createdTime;
    }

    @PrePersist
    void onPersist() {
        this.createdTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RepoRepoGroup: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", rp=[").append(this.repo).append("]");
        str.append(", rg=[").append(this.repoGroup).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((repoGroup == null) ? 0 : repoGroup.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoRepoGroup))) {
            return false;
        }

        final RepoRepoGroup other = (RepoRepoGroup) obj;

        if (repo == null) {
            if (other.repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (repoGroup == null) {
            if (other.repoGroup != null) {
                return false;
            }
        } else if (!repoGroup.equals(other.repoGroup)) {
            return false;
        }

        return true;
    }
}