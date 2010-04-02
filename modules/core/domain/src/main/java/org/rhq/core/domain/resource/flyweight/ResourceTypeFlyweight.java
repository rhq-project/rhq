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

package org.rhq.core.domain.resource.flyweight;

import java.io.Serializable;

import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A flyweight representation of the resource type intended for the display purposes. 
 * 
 * @author Lukas Krejci
 */
public class ResourceTypeFlyweight implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String plugin;
    private ResourceCategory category;
    private ResourceSubCategoryFlyweight subCategory;

    /**
     * @see #construct(int, String, String, ResourceCategory, Integer, FlyweightCache)
     * 
     * @param original the original resource type
     * @param cache the cache of flyweights
     * @return a fully initialized resource type flyweight
     */
    public static ResourceTypeFlyweight construct(ResourceType original, FlyweightCache cache) {
        int id = original.getId();
        String name = original.getName();
        String plugin = original.getPlugin();
        ResourceCategory category = original.getCategory();
        ResourceSubCategory subCategory = original.getSubCategory();

        return construct(id, name, plugin, category, subCategory != null ? subCategory.getId() : null, cache);
    }

    /**
     * Constructs a fully initialized resource type flyweight.
     * If a flyweight instance is found in the cache under the provided id, a new instance
     * is *NOT* created but rather the properties of that existing instance are updated with
     * the provided values.
     * <p>
     * The subcategory is supposed to exist in the cache. If it doesn't the subcategory of the
     * returned resource type flyweight is set to null.
     * 
     * @param id the resource type id
     * @param name the resource type name
     * @param plugin the resource type plugin
     * @param category the resource type category
     * @param subCategory the id of the resource type sub category or null
     * @param cache the flyweight cache
     * @return
     */
    public static ResourceTypeFlyweight construct(int id, String name, String plugin, ResourceCategory category,
        Integer subCategory, FlyweightCache cache) {

        ResourceTypeFlyweight ret = cache.getResourceTypes().get(id);
        
        if (ret == null) {
            ret = new ResourceTypeFlyweight();
            cache.getResourceTypes().put(id, ret);
        }
        
        ret.setId(id);
        ret.setName(name);
        ret.setPlugin(plugin);
        ret.setCategory(category);
        if (subCategory != null) {
            ret.setSubCategory(cache.getSubCategories().get(subCategory));
        } else {
            ret.setSubCategory(null);
        }
        
        return ret;
    }

    private ResourceTypeFlyweight() {

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

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public ResourceCategory getCategory() {
        return category;
    }

    public void setCategory(ResourceCategory resourceCategory) {
        this.category = resourceCategory;
    }

    public ResourceSubCategoryFlyweight getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(ResourceSubCategoryFlyweight subCategory) {
        this.subCategory = subCategory;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !(obj instanceof ResourceTypeFlyweight))
            return false;
        ResourceTypeFlyweight that = (ResourceTypeFlyweight) obj;
        if (!this.name.equals(that.name))
            return false;
        if (this.plugin != null ? !this.plugin.equals(that.plugin) : that.plugin != null)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        if (name != null && this.plugin != null) {
            result = this.name.hashCode();
            result = 31 * result + (this.plugin != null ? plugin.hashCode() : 0);
        } else {
            result = 31 * id;
        }
        return result;
    }    
}
