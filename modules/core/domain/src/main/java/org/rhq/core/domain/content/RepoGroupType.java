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
 * Defines a type of (@link RepoGroup) e.g. Channel families in RHN map to a type of 
 * (@link RepoGroup). 
 * @author Sayli Karmarkar
 */

public class RepoGroupType implements Serializable {

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

    //@OneToMany(mappedBy = "repoGroupType", fetch = FetchType.LAZY)
    private Set<RepoGroup> repoGroups;

    // Constructor ----------------------------------------

    public RepoGroupType() {
        // for JPA use
    }

    public RepoGroupType(String name) {
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
     * Programmatic name of the repo group type.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Free text description of this repo group type.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Repo groups of this type.
     */
    public Set<RepoGroup> getRepoGroups() {
        return repoGroups;
    }

    public void addRepoGroup(RepoGroup repoGroup) {
        if (this.repoGroups == null) {
            this.repoGroups = new HashSet<RepoGroup>();
        }

        this.repoGroups.add(repoGroup);
        repoGroup.setRepoGroupType(this);
    }

    public void setRepoGroups(Set<RepoGroup> repoGroups) {
        this.repoGroups = repoGroups;
    }

    @Override
    public String toString() {
        return "RepoGroupType: name=[" + this.name + "]";
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

        if ((obj == null) || (!(obj instanceof RepoGroupType))) {
            return false;
        }

        final RepoGroupType other = (RepoGroupType) obj;

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