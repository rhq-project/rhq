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
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

/**
 * Defines a type of (@link RepoRelationship) e.g. Clone, Parent-child are types of 
 * (@link RepoRelationship). 
 * @author Sayli Karmarkar
 */

@Entity
@NamedQueries( {
    @NamedQuery(name = RepoRelationshipType.QUERY_FIND_BY_NAME, query = "SELECT r FROM RepoRelationshipType r WHERE r.name = :name")
})

@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_REPO_RELATION_TYPE_ID_SEQ")
@Table(name = "RHQ_REPO_RELATION_TYPE")
public class RepoRelationshipType implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String QUERY_FIND_BY_NAME = "RepoRelationshipType.findByName";

    // Attributes  --------------------------------------------

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @OneToMany(mappedBy = "repoRelationshipType", fetch = FetchType.LAZY)
    private Set<RepoRelationship> repoRelationships;

    // Constructor ----------------------------------------

    public RepoRelationshipType() {
        // for JPA use
    }

    public RepoRelationshipType(String name) {
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
     * Programmatic name of the repo relationship type.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Free text description of this repo relationship type.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Repo relationships of this type.
     */
    public Set<RepoRelationship> getRepoRelationships() {
        return repoRelationships;
    }

    public void addRepoRelationship(RepoRelationship repoRelationship) {
        if (this.repoRelationships == null) {
            this.repoRelationships = new HashSet<RepoRelationship>();
        }

        this.repoRelationships.add(repoRelationship);
        repoRelationship.setRepoRelationshipType(this);
    }

    public void setRepoRelationships(Set<RepoRelationship> repoRelationships) {
        this.repoRelationships = repoRelationships;
    }

    @Override
    public String toString() {
        return "RepoRelationshipType: name=[" + this.name + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RepoRelationshipType))) {
            return false;
        }

        final RepoRelationshipType other = (RepoRelationshipType) obj;

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