/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.domain.resource.hierarchy;

import java.io.Serializable;

import org.rhq.core.domain.resource.ResourceSubCategory;

/**
 * Flyweight representation of a resource sub category. Only id, name and parent are preserved.
 * 
 * @author Lukas Krejci
 */
public class ResourceSubCategoryFlyweight implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private ResourceSubCategoryFlyweight parentSubCategory;

    /**
     * @see #construct(int, String, Integer, FlyweightCache)
     * 
     * @param original
     * @param cache
     * @return
     */
    public static ResourceSubCategoryFlyweight construct(ResourceSubCategory original, FlyweightCache cache) {
        int id = original.getId();
        String name = original.getName();
        ResourceSubCategory parent = original.getParentSubCategory();
        Integer parentId = parent != null ? parent.getId() : null;
        String parentName = parent != null ? parent.getName() : null;
        
        return construct(id, name, parentId, parentName, cache);
    }

    /**
     * An existing sub category is first looked up in the cache. If there already is a flyweight
     * instance in the cache, its properties are updated with the provided values, otherwise a new instance
     * is put in the cache.
     * <p>
     * If parent sub category id is not null but a corresponding flyweight doesn't exist in the cache yet,
     * a new instance is put in the cache initialized with the parent id and name.
     * 
     * @param id
     * @param name
     * @param parentSubCategoryId
     * @param parentSubCategoryName
     * @param cache
     * @return
     */
    public static ResourceSubCategoryFlyweight construct(int id, String name, Integer parentSubCategoryId,
        String parentSubCategoryName, FlyweightCache cache) {
        ResourceSubCategoryFlyweight ret = cache.getSubCategories().get(id);

        if (ret == null) {
            ret = new ResourceSubCategoryFlyweight();
            cache.getSubCategories().put(id, ret);
        }

        ret.setId(id);
        ret.setName(name);

        if (parentSubCategoryId != null) {
            ResourceSubCategoryFlyweight parent = cache.getSubCategories().get(parentSubCategoryId);
            if (parent == null) {
                parent = construct(parentSubCategoryId, parentSubCategoryName, null, null, cache);
            }
            ret.setParentSubCategory(parent);
        } else {
            ret.setParentSubCategory(null);
        }

        return ret;
    }

    private ResourceSubCategoryFlyweight() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ResourceSubCategoryFlyweight getParentSubCategory() {
        return parentSubCategory;
    }

    public void setParentSubCategory(ResourceSubCategoryFlyweight parentSubCategory) {
        this.parentSubCategory = parentSubCategory;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ResourceSubCategoryFlyweight)) {
            return false;
        }

        ResourceSubCategoryFlyweight that = (ResourceSubCategoryFlyweight) o;

        if (!name.equals(that.getName())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = name.hashCode();

        return result;
    }    
}
