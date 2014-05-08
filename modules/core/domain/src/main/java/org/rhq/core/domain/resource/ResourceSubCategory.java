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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Deprecated due to a simpler but more powerful subcategory design.
 * Please see https://bugzilla.redhat.com/show_bug.cgi?id=1069545
 *
 * This class is no longer in use because subcategories are now just an attribute
 * on {@link ResourceType}.
 */
@Deprecated
public class ResourceSubCategory implements Comparable<ResourceSubCategory>, Serializable {
    private static final long serialVersionUID = 2L;

    @Deprecated
    public static final String QUERY_FIND_BY_NAME_AND_PLUGIN = "";

    /* no-arg constructor required by EJB spec */
    @Deprecated
    public ResourceSubCategory() {
    }

    @Deprecated
    public ResourceSubCategory(String name) {
        assert name != null;
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
        return 0;
    }

    @Deprecated
    public void setId(int id) {
    }

    @Deprecated
    public String getName() {
        return null;
    }

    @Deprecated
    public void setName(String name) {
    }

    @Deprecated
    public String getDisplayName() {
        return null;
    }

    @Deprecated
    public void setDisplayName(String displayName) {
    }

    @Deprecated
    public String getDescription() {
        return null;
    }

    @Deprecated
    public void setDescription(String description) {
    }

    @Deprecated
    public long getCtime() {
        return 0;
    }

    @Deprecated
    public long getMtime() {
        return 0;
    }

    @Deprecated
    public ResourceType getResourceType() {
        return null;
    }

    @Deprecated
    public void setResourceType(ResourceType notused) {
    }

    @Deprecated
    public void update(ResourceSubCategory newSubCategory) {
    }

    @Deprecated
    public void removeChildSubCategory(ResourceSubCategory oldChildSubCategory) {
    }

    @Deprecated
    public void addChildSubCategory(ResourceSubCategory childSubCategory) {
    }

    @Deprecated
    public List<ResourceSubCategory> getChildSubCategories() {
        return null;
    }

    @Deprecated
    public void setChildSubCategories(List<ResourceSubCategory> childSubCategories) {
    }

    @Deprecated
    public void setParentSubCategory(ResourceSubCategory parentSubCategory) {
    }

    @Deprecated
    public ResourceSubCategory getParentSubCategory() {
        return null;
    }

    @Deprecated
    public int compareTo(ResourceSubCategory that) {
        return 0;
    }
}