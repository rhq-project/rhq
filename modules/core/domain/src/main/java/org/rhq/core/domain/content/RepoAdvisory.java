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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * This is the many-to-many entity that correlates a repo with an advisory that will fill the repo with
 * advisory updates. It is an explicit relationship mapping entity between {@link Repo} and {@link Advisory}.
 *
 * @author Pradeep Kilambi
 */
@Entity
@IdClass(RepoAdvisoryPK.class)
@NamedQueries( {
    @NamedQuery(name = RepoAdvisory.DELETE_BY_ADVISORY_ID, query = "DELETE RepoAdvisory rkt WHERE rkt.advisory.id = :advId"),
    @NamedQuery(name = RepoAdvisory.DELETE_BY_REPO_ID, query = "DELETE RepoAdvisory rkt WHERE rkt.repo.id = :repoId"),
    @NamedQuery(name = RepoAdvisory.QUERY_FIND_BY_REPO_ID, query = "SELECT rkt FROM RepoAdvisory rkt where rkt.repo.id = :repoId ") })
@Table(name = "RHQ_REPO_ADVISORY")
public class RepoAdvisory implements Serializable {

    public static final String DELETE_BY_ADVISORY_ID = "RepoAdvisory.deleteByAdvisoryId";
    public static final String DELETE_BY_REPO_ID = "RepoAdvisory.deleteByRepoId";
    public static final String QUERY_FIND_BY_REPO_ID = "RepoAdvisory.queryFindByRepoId";

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
    @JoinColumn(name = "ADVISORY_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Advisory advisory;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long last_modified;

    protected RepoAdvisory() {
    }

    public RepoAdvisory(Repo repo, Advisory advisory) {
        this.repo = repo;
        this.advisory = advisory;
    }

    public RepoAdvisoryPK getRepoAdvisoryPK() {
        return new RepoAdvisoryPK(repo, advisory);
    }

    public void setRepoAdvisoryPK(RepoAdvisoryPK pk) {
        this.repo = pk.getRepo();
        this.advisory = pk.getAdvisory();
    }

    public long getLastModified() {
        return last_modified;
    }

    public Repo getRepo() {
        return repo;
    }

    public void setRepo(Repo repo) {
        this.repo = repo;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public void setAdvisory(Advisory advisory) {
        this.advisory = advisory;
    }

    @PrePersist
    void onPersist() {
        this.last_modified = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RepoAdvisory: ");
        str.append("ctime=[").append(new Date(this.last_modified)).append("]");
        str.append(", Repo=[").append(this.repo).append("]");
        str.append(", Advisory=[").append(this.advisory).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((advisory == null) ? 0 : advisory.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoAdvisory))) {
            return false;
        }

        final RepoAdvisory other = (RepoAdvisory) obj;

        if (repo == null) {
            if (other.repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (advisory == null) {
            if (other.advisory != null) {
                return false;
            }
        } else if (!advisory.equals(other.advisory)) {
            return false;
        }

        return true;
    }
}
