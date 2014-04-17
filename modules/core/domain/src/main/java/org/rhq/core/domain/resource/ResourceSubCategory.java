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
package org.rhq.core.domain.resource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

/**
 * Class representing a sub category, where a sub category is meant to group similar Resource types together.
 */
@Entity
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_RESOURCE_SUBCAT_ID_SEQ", sequenceName = "RHQ_RESOURCE_SUBCAT_ID_SEQ")
@Table(name = "RHQ_RESOURCE_SUBCAT")
@NamedQueries({ @NamedQuery(name = ResourceSubCategory.FIND_BY_NAME_WITH_SUBCATEGORIES, query = "" //
    + "  SELECT rs " //
    + "    FROM ResourceSubCategory rs "
    + "    JOIN FETCH rs.childSubCategories" //
    + "    WHERE rs.name = :name") })
public class ResourceSubCategory implements Comparable<ResourceSubCategory>, Serializable {
    private static final long serialVersionUID = 1L;

    public static final String FIND_BY_NAME_WITH_SUBCATEGORIES = "ResourceSubCategory.findByNameWithSubCategories";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_RESOURCE_SUBCAT_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "DISPLAY_NAME")
    private String displayName;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "CTIME")
    private Long ctime;

    @Column(name = "MTIME")
    private Long mtime;

    @OneToMany(mappedBy = "parentSubCategory", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST,
        CascadeType.REMOVE })
    @OrderBy
    private List<ResourceSubCategory> childSubCategories = new ArrayList<ResourceSubCategory>();

    @JoinColumn(name = "PARENT_SUBCATEGORY_ID")
    @ManyToOne
    private ResourceSubCategory parentSubCategory;


    /* no-arg constructor required by EJB spec */
    public ResourceSubCategory() {
    }

    public ResourceSubCategory(String name) {
        assert name != null;

        // Initialize empty ordered lists...
        this.childSubCategories = new ArrayList<ResourceSubCategory>();

        this.name = name;

        this.mtime = this.ctime = System.currentTimeMillis();
    }

    /*@NotNull
    @Deprecated
    public Set<ResourceType> findTaggedResourceTypes() {
        return new HashSet<ResourceType>();
    }

    @Deprecated
    public ResourceType findParentResourceType() {
        return null;
    }

    @Deprecated
    public boolean isCreatable() {
        return false;
    }*/

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCtime() {
        return this.ctime;
    }

    @PrePersist
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    public long getMtime() {
        return this.mtime;
    }

    @PreUpdate
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    /**
     * Updates the contents of this definition with values from the specified new definition. The intention is for this
     * to be used as a merge between this attached instance and a detached instance. The name and resourceType will NOT
     * be updated as part of this call; they are used as identifiers and should already be the same if this merge is
     * being performed.
     *
     * @param newSubCategory contains new data to merge into this definition; cannot be <code>null</code>
     */
    public void update(@NotNull ResourceSubCategory newSubCategory) {
        this.displayName = newSubCategory.getDisplayName();
        this.description = newSubCategory.getDescription();
    }

    /**
     * Removes the given ResourceSubCategory as a child of this ResourceSubCategory
     */
    public void removeChildSubCategory(ResourceSubCategory oldChildSubCategory) {
        oldChildSubCategory.parentSubCategory = null;
        this.childSubCategories.remove(oldChildSubCategory);
    }

    /**
     * add a child ResourceSubCategory to this instance
     */
    public void addChildSubCategory(ResourceSubCategory childSubCategory) {
        childSubCategory.setParentSubCategory(this);
        this.childSubCategories.add(childSubCategory);
    }

    public List<ResourceSubCategory> getChildSubCategories() {
        return childSubCategories;
    }

    public void setChildSubCategories(List<ResourceSubCategory> childSubCategories) {
        if (childSubCategories != null) {
            this.childSubCategories = childSubCategories;
        }
    }

    public void setParentSubCategory(ResourceSubCategory parentSubCategory) {
        this.parentSubCategory = parentSubCategory;
    }

    /**
     * Returns this subcategory's parent subcategory, or null if this subcategory has no parent.
     *
     * @return this subcategory's parent subcategory, or null if this subcategory has no parent
     */
    public ResourceSubCategory getParentSubCategory() {
        return parentSubCategory;
    }

    public int compareTo(ResourceSubCategory that) {
        return this.name.compareTo(that.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResourceSubCategory)) {
            return false;
        }

        ResourceSubCategory that = (ResourceSubCategory) o;

        if (!name.equals(that.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public String toString() {
        return "ResourceSubCategory[id=" + id + ", name=" + name + "]";
    }

}