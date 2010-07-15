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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * This is the many-to-many entity that correlates a repo with a content source that will fill the repo with
 * package versions. It is an explicit relationship mapping entity between {@link Repo} and {@link ContentSource}.
 *
 * @author John Mazzitelli
 */
@Entity
@IdClass(RepoContentSourcePK.class)
@NamedQueries( {
    @NamedQuery(name = RepoContentSource.DELETE_BY_CONTENT_SOURCE_ID, query = "DELETE RepoContentSource ccs WHERE ccs.contentSource.id = :contentSourceId"),
    @NamedQuery(name = RepoContentSource.DELETE_BY_REPO_ID, query = "DELETE RepoContentSource ccs WHERE ccs.repo.id = :repoId") })
@Table(name = "RHQ_REPO_CONTENT_SRC_MAP")
public class RepoContentSource implements Serializable {
    public static final String DELETE_BY_CONTENT_SOURCE_ID = "RepoContentSource.deleteByContentSourceId";
    public static final String DELETE_BY_REPO_ID = "RepoContentSource.deleteByRepoId";

    private static final long serialVersionUID = 1L;

    /*
     * http://opensource.atlassian.com/projects/hibernate/browse/EJB-286 Hibernate seems to want these mappings in the
     * @IdClass and ignore these here, even though the mappings should be here and no mappings should be needed in the
     * @IdClass.
     */
    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "REPO_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Repo repo;

    @Id
    //   @ManyToOne
    //   @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private ContentSource contentSource;

    @Column(name = "CTIME", nullable = false)
    private long createdTime;

    protected RepoContentSource() {
    }

    public RepoContentSource(Repo repo, ContentSource contentSource) {
        this.repo = repo;
        this.contentSource = contentSource;
    }

    public RepoContentSourcePK getRepoContentSourcePK() {
        return new RepoContentSourcePK(repo, contentSource);
    }

    public void setRepoContentSourcePK(RepoContentSourcePK pk) {
        this.repo = pk.getRepo();
        this.contentSource = pk.getContentSource();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the repo was first associated
     * with the content source.
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
        StringBuilder str = new StringBuilder("RepoContentSource: ");
        str.append("ctime=[").append(new Date(this.createdTime)).append("]");
        str.append(", ch=[").append(this.repo).append("]");
        str.append(", cs=[").append(this.contentSource).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((contentSource == null) ? 0 : contentSource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoContentSource))) {
            return false;
        }

        final RepoContentSource other = (RepoContentSource) obj;

        if (repo == null) {
            if (other.repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (contentSource == null) {
            if (other.contentSource != null) {
                return false;
            }
        } else if (!contentSource.equals(other.contentSource)) {
            return false;
        }

        return true;
    }
}