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
 * package versions. It is an explicit relationship mapping entity between {@link Repo} and {@link Distribution}.
 *
 * @author Pradeep Kilambi
 */
@Entity
@IdClass(RepoDistributionPK.class)
@NamedQueries( {
    @NamedQuery(name = RepoDistribution.DELETE_BY_KICKSTART_TREE_ID, query = "DELETE RepoDistribution rkt WHERE rkt.kstree.id = :KsTreeId"),
    @NamedQuery(name = RepoDistribution.DELETE_BY_REPO_ID, query = "DELETE RepoDistribution rkt WHERE rkt.repo.id = :repoId") })
@Table(name = "RHQ_REPO_KS_MAP_KEY")
public class RepoDistribution implements Serializable {
    public static final String DELETE_BY_KICKSTART_TREE_ID = "RepoDistribution.deleteByKickstartTreeId";
    public static final String DELETE_BY_REPO_ID = "RepoDistribution.deleteByRepoId";

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
    //   @JoinColumn(name = "KICKSTART_TREE_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Distribution kstree;

    @Column(name = "LAST_MODIFIED", nullable = false)
    private long last_modified;

    protected RepoDistribution() {
    }

    public RepoDistribution(Repo repo, Distribution kstree) {
        this.repo = repo;
        this.kstree = kstree;
    }

    public RepoDistributionPK getRepoKickstartTreePK() {
        return new RepoDistributionPK(repo, kstree);
    }

    public void setRepoKickstartTreePK(RepoDistributionPK pk) {
        this.repo = pk.getRepo();
        this.kstree = pk.getKickstartTree();
    }

    /**
     * This is the epoch time when this mapping was first created; in other words, when the repo was first associated
     * with the content source.
     */
    public long getLastModified() {
        return last_modified;
    }

    @PrePersist
    void onPersist() {
        this.last_modified = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RepoKickstartTree: ");
        str.append("ctime=[").append(new Date(this.last_modified)).append("]");
        str.append(", ch=[").append(this.repo).append("]");
        str.append(", cs=[").append(this.kstree).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((kstree == null) ? 0 : kstree.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoDistribution))) {
            return false;
        }

        final RepoDistribution other = (RepoDistribution) obj;

        if (repo == null) {
            if (repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (kstree == null) {
            if (kstree != null) {
                return false;
            }
        } else if (!kstree.equals(other.kstree)) {
            return false;
        }

        return true;
    }
}
