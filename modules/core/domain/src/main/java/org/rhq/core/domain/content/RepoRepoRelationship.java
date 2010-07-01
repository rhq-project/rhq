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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * This is the many-to-many entity that correlates a repo with a repo relationship. 
 * It is an explicit relationship mapping entity between {@link Repo} 
 * and {@link RepoRelationship}.
 *
 * @author Sayli Karmarkar
 */

@Entity
@NamedQueries({
    @NamedQuery(name = RepoRepoRelationship.DELETE_BY_REPO_ID, query = "DELETE RepoRepoRelationship rrr WHERE rrr.repo.id = :repoId")
})
@IdClass(RepoRepoRelationshipPK.class)
@Table(name = "RHQ_REPO_REPO_RELATION_MAP")
public class RepoRepoRelationship implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DELETE_BY_REPO_ID = "RepoRepoRelationship.deleteByRepoId";

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
    @JoinColumn(name = "REPO_RELATION_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private RepoRelationship repoRelationship;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    protected RepoRepoRelationship() {
    }

    public RepoRepoRelationship(Repo repo, RepoRelationship repoRelationship) {
        this.repo = repo;
        this.repoRelationship = repoRelationship;
    }

    public RepoRepoRelationshipPK getRepoRepoRelationshipPK() {
        return new RepoRepoRelationshipPK(repo, repoRelationship);
    }

    public void setRepoRepoRelationshipPK(RepoRepoRelationshipPK pk) {
        this.repo = pk.getRepo();
        this.repoRelationship = pk.getRepoRelationship();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the repo was first associated
     * with the repo relationship.
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
        StringBuilder str = new StringBuilder("RepoRepoRelationship: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", rp=[").append(this.repo).append("]");
        str.append(", rr=[").append(this.repoRelationship).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((repoRelationship == null) ? 0 : repoRelationship.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoRepoRelationship))) {
            return false;
        }

        final RepoRepoRelationship other = (RepoRepoRelationship) obj;

        if (repo == null) {
            if (other.repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (repoRelationship == null) {
            if (other.repoRelationship != null) {
                return false;
            }
        } else if (!repoRelationship.equals(other.repoRelationship)) {
            return false;
        }

        return true;
    }
}