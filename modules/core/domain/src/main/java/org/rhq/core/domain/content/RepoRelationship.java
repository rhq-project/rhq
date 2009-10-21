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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * A RepoRelationship represents relationship with a {@link Repo} 
 * having a (@link RepoRelationshipType) which can represent parent/child, 
 * clone relationship etc.
 *
 * @author Sayli Karmarkar
 */

@Entity
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_REPO_RELATION_ID_SEQ")
@Table(name = "RHQ_REPO_RELATION")
public class RepoRelationship implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "RELATED_REPO_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private Repo relatedRepo;

    @JoinColumn(name = "REPO_RELATION_TYPE_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne
    private RepoRelationshipType repoRelationshipType;

    @OneToMany(mappedBy = "repoRelationship", fetch = FetchType.LAZY)
    private Set<RepoRepoRelationship> repoRepoRelationships;

    // Constructor ----------------------------------------

    public RepoRelationship() {
        // for JPA use
    }

    // Public  --------------------------------------------

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    public Repo getRelatedRepo() {
        return relatedRepo;
    }

    public void setRelatedRepo(Repo relatedRepo) {
        this.relatedRepo = relatedRepo;
    }
    
    
    /**
     * Describes the type of this repoRelationship.
     */
    public RepoRelationshipType getRepoRelationshipType() {
        return repoRelationshipType;
    }

    public void setRepoRelationshipType(RepoRelationshipType repoRelationshipType) {
        this.repoRelationshipType = repoRelationshipType;
    }
    

    /**
     * Returns the explicit mapping entities.
     *
     * @return the mapping entities
     *
     * @see    #getRepoRelationship()
     */
    public Set<RepoRepoRelationship> getRepoRepoRelationships() {
        return repoRepoRelationships;
    }

    /**
     * The repos that this repo relationship is associated with.
     *
     * <p>The returned set is not backed by this entity - if you want to alter the set of associated repos, use
     * {@link #getRepoRepoRelationships()} or {@link #addRepo(Repo)}, {@link #removeRepo(Repo)}.</p>
     */
    public Set<Repo> getRelatedRepos() {
        HashSet<Repo> repos = new HashSet<Repo>();

        if (repoRepoRelationships != null) {
            for (RepoRepoRelationship rrr : repoRepoRelationships) {
                repos.add(rrr.getRepoRepoRelationshipPK().getRepo());
            }
        }

        return repos;
    }

    /**
     * Directly assign a repo to this repo relationship.
     *
     * @param  repo
     *
     * @return the mapping that was added
     */
    public RepoRepoRelationship addRepo(Repo repo) {
        if (this.repoRepoRelationships == null) {
            this.repoRepoRelationships = new HashSet<RepoRepoRelationship>();
        }

        RepoRepoRelationship mapping = new RepoRepoRelationship(repo, this);
        this.repoRepoRelationships.add(mapping);
        //repo.addRepoRelationship(this);
        return mapping;
    }

    /**
     * Removes the repo from this repo relationship, if it exists. If it does exist, the mapping that was removed is
     * returned; if the given repo did not did not belong to this repo relationship, <code>null</code> is
     * returned.
     *
     * @param  repo the repo to remove from this repo relationship
     *
     * @return the mapping that was removed or <code>null</code> if the repo did not belong to this repo relationship
     */
    public RepoRepoRelationship removeRepo(Repo repo) {
        if ((this.repoRepoRelationships == null) || (repo == null)) {
            return null;
        }

        RepoRepoRelationship doomed = null;

        for (RepoRepoRelationship rrr : this.repoRepoRelationships) {
            if (repo.equals(rrr.getRepoRepoRelationshipPK().getRepo())) {
                doomed = rrr;
                //repo.removeRepoRelationship(this);
                break;
            }
        }

        if (doomed != null) {
            this.repoRepoRelationships.remove(doomed);
        }

        return doomed;
    }
    
    
    @Override
    public int hashCode() {
        int result = 1;
        result = (31 * result) + ((relatedRepo == null) ? 0 : relatedRepo.hashCode());
        result = (31 * result) + ((repoRelationshipType == null) ? 0 : repoRelationshipType.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "RepoRelationship: id=[" + this.id + "]" + "related repo=[" + 
        this.relatedRepo.getName() + "]" + "relation=[" + this.getRepoRelationshipType().getName() 
        + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoRelationship))) {
            return false;
        }

        final RepoRelationship other = (RepoRelationship) obj;

        if (relatedRepo.getName() == null) {
            if (other.relatedRepo.getName() != null) {
                return false;
            }
        } else if (!relatedRepo.getName().equals(other.relatedRepo.getName())) {
            return false;
        }
        
        if (repoRelationshipType.getName() == null) {
            if (other.repoRelationshipType.getName() != null) {
                return false;
            }
        } else if (!repoRelationshipType.getName().equals(other.relatedRepo.getName())) {
            return false;
        }

        return true;
    }
}
