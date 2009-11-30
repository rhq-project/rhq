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

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.rhq.core.domain.common.Tag;

/**
 * This is the many-to-many entity that correlates a repo with a tag. 
 * It is an explicit relationship mapping entity between {@link Repo} 
 * and {@link Tag}.
 *
 * @author Sayli Karmarkar
 */

@Entity
@IdClass(RepoTagPK.class)
@Table(name = "RHQ_REPO_TAG_MAP")
public class RepoTag implements Serializable {

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
    @JoinColumn(name = "TAG_ID", referencedColumnName = "ID", nullable = false, insertable = false, updatable = false)
    private Tag tag;

    protected RepoTag() {
    }

    public RepoTag(Repo repo, Tag tag) {
        this.repo = repo;
        this.tag = tag;
    }

    public RepoTagPK getRepoTagPK() {
        return new RepoTagPK(repo, tag);
    }

    public void setRepoTagPK(RepoTagPK pk) {
        this.repo = pk.getRepo();
        this.tag = pk.getTag();
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("RepoTag: ");
        str.append(", rp=[").append(this.repo).append("]");
        str.append(", tg=[").append(this.tag).append("]");
        return str.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((repo == null) ? 0 : repo.hashCode());
        result = (31 * result) + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoTag))) {
            return false;
        }

        final RepoTag other = (RepoTag) obj;

        if (repo == null) {
            if (repo != null) {
                return false;
            }
        } else if (!repo.equals(other.repo)) {
            return false;
        }

        if (tag == null) {
            if (tag != null) {
                return false;
            }
        } else if (!tag.equals(other.tag)) {
            return false;
        }

        return true;
    }
}