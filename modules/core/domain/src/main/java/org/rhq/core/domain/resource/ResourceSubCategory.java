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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class representing a sub category, where a sub category is meant to group similar Resource types together.
 */
@Deprecated
public class ResourceSubCategory implements Comparable<ResourceSubCategory>, Serializable {
    private static final long serialVersionUID = 1L;

    @Deprecated
    private int id;
    @Deprecated
    private String name;
    @Deprecated
    private String displayName;
    @Deprecated
    private String description;
    @Deprecated
    private Long ctime;
    @Deprecated
    private Long mtime;
    @Deprecated
    private List<ResourceSubCategory> childSubCategories = new ArrayList<ResourceSubCategory>();
    @Deprecated
    private ResourceSubCategory parentSubCategory;


    /* no-arg constructor required by EJB spec */
    @Deprecated
    public ResourceSubCategory() {
    }

    @Deprecated
    public ResourceSubCategory(String name) {
        assert name != null;

        // Initialize empty ordered lists...
        this.childSubCategories = new ArrayList<ResourceSubCategory>();
        this.name = name;
        this.mtime = this.ctime = System.currentTimeMillis();
    }

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
    }

    @Deprecated
    public int getId() {
        return this.id;
    }

    @Deprecated
    public void setId(int id) {
        this.id = id;
    }

    @Deprecated
    public String getName() {
        return this.name;
    }

    @Deprecated
    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public String getDisplayName() {
        return this.displayName;
    }

    @Deprecated
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Deprecated
    public String getDescription() {
        return this.description;
    }

    @Deprecated
    public void setDescription(String description) {
        this.description = description;
    }

    @Deprecated
    public long getCtime() {
        return this.ctime;
    }

    @Deprecated
    void onPersist() {
        this.mtime = this.ctime = System.currentTimeMillis();
    }

    @Deprecated
    public long getMtime() {
        return this.mtime;
    }

    @Deprecated
    void onUpdate() {
        this.mtime = System.currentTimeMillis();
    }

    @Deprecated
    public void update(ResourceSubCategory newSubCategory) {
        this.displayName = newSubCategory.getDisplayName();
        this.description = newSubCategory.getDescription();
    }

    @Deprecated
    public void removeChildSubCategory(ResourceSubCategory oldChildSubCategory) {
        oldChildSubCategory.parentSubCategory = null;
        this.childSubCategories.remove(oldChildSubCategory);
    }

    @Deprecated
    public void addChildSubCategory(ResourceSubCategory childSubCategory) {
        childSubCategory.setParentSubCategory(this);
        this.childSubCategories.add(childSubCategory);
    }

    @Deprecated
    public List<ResourceSubCategory> getChildSubCategories() {
        return childSubCategories;
    }

    @Deprecated
    public void setChildSubCategories(List<ResourceSubCategory> childSubCategories) {
        if (childSubCategories != null) {
            this.childSubCategories = childSubCategories;
        }
    }

    @Deprecated
    public void setParentSubCategory(ResourceSubCategory parentSubCategory) {
        this.parentSubCategory = parentSubCategory;
    }

    @Deprecated
    public ResourceSubCategory getParentSubCategory() {
        return parentSubCategory;
    }

    @Deprecated
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