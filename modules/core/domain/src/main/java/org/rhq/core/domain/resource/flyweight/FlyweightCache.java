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

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceSubCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * A helper object to hold the cached instances of the flyweights.
 * The keys in the maps are ids, values are the objects themselves.
 * <p>
 * The <code>construct*</code> methods are provided to correctly initialize
 * instances of the flyweight types in the cache instance from a minimal set
 * of data.
 *
 * @author Lukas Krejci
 */
public class FlyweightCache {

    private Map<Integer, ResourceFlyweight> resources = new HashMap<Integer, ResourceFlyweight>();
    private Map<Integer, ResourceTypeFlyweight> resourceTypes = new HashMap<Integer, ResourceTypeFlyweight>();
    private Map<Integer, ResourceSubCategoryFlyweight> subCategories = new HashMap<Integer, ResourceSubCategoryFlyweight>();

    public Map<Integer, ResourceFlyweight> getResources() {
        return resources;
    }

    public Map<Integer, ResourceTypeFlyweight> getResourceTypes() {
        return resourceTypes;
    }

    public Map<Integer, ResourceSubCategoryFlyweight> getSubCategories() {
        return subCategories;
    }

    /**
     * @see #constructResource(int, String, String, String, Integer, int, AvailabilityType)
     *
     * @param original the resource
     * @return the initialized resource flyweight
     */
    public ResourceFlyweight constructResource(Resource original) {
        int id = original.getId();
        String name = original.getName();
        String uuid = original.getUuid();
        String resourceKey = original.getResourceKey();
        Resource parent = original.getParentResource();
        ResourceType type = original.getResourceType();
        ResourceAvailability avail = original.getCurrentAvailability();

        return constructResource(id, name, uuid, resourceKey, parent != null ? parent.getId() : null, type.getId(),
            avail != null ? avail.getAvailabilityType() : AvailabilityType.UNKNOWN);
    }

    /**
     * Constructs a fully initialized instance of the resource flyweight.
     * The resource type, sub-category and parent are looked up
     * in this cache instance. If not found, new instances are created and added
     * to this cache.
     * <p>
     * Note that if the parentId is not null and not found in this cache, a new flyweight
     * is created for the parent, initialized only with the id.
     * <p>
     * The type is supposed to exist in this cache already. If it doesn't, no type is assigned
     * to the returned resource flyweight.
     * <p>
     * If a corresponding flyweight for the provided resource id is already found in this cache,
     * it is refreshed with the data provided to this call.
     *
     * @param id the resource id
     * @param name the resource name
     * @param uuid the resource uuid
     * @param resourceKey the resource key
     * @param parentId the id of the parent resource
     * @param typeId the id of the resource type
     * @param currentAvailability the availability of the resource
     * @return the initialized resource flyweight
     */
    public ResourceFlyweight constructResource(int id, String name, String uuid, String resourceKey, Integer parentId,
        int typeId, AvailabilityType currentAvailability) {

        ResourceFlyweight ret = getResources().get(id);
        if (ret == null) {
            ret = new ResourceFlyweight();
            getResources().put(id, ret);
        }

        ret.setId(id);
        ret.setName(name);
        ret.setUuid(uuid);
        ret.setResourceKey(resourceKey);
        ret.setCurrentAvailability(new ResourceAvailabilityFlyweight(ret, currentAvailability));

        if (parentId != null) {
            ResourceFlyweight parent = getResources().get(parentId);
            if (parent == null) {
                parent = constructResource(parentId, null, null, null, null, -1, null);
            }
            parent.getChildResources().add(ret);
            ret.setParentResource(parent);
        } else {
            ResourceFlyweight previousParent = ret.getParentResource();
            if (previousParent != null) {
                previousParent.getChildResources().remove(ret);
            }
            ret.setParentResource(null);
        }

        ret.setResourceType(getResourceTypes().get(typeId));

        return ret;
    }

    /**
     * @see #constructSubCategory(int, String, Integer, String)
     *
     * @param original
     * @return a fully initialized resource sub category flyweight
     */
    @Deprecated
    public ResourceSubCategoryFlyweight constructSubCategory(ResourceSubCategory original) {
        return null;
    }

    /**
     * An existing sub category is first looked up in this cache. If there already is a flyweight
     * instance in this cache, its properties are updated with the provided values, otherwise a new instance
     * is put in this cache.
     * <p>
     * If parent sub category id is not null but a corresponding flyweight doesn't exist in this cache yet,
     * a new instance is put in this cache initialized with the parent id and name.
     *
     * @param id
     * @param name
     * @param parentSubCategoryId
     * @param parentSubCategoryName
     * @return a fully initialized resource sub category flyweight
     */
    public ResourceSubCategoryFlyweight constructSubCategory(int id, String name, Integer parentSubCategoryId,
        String parentSubCategoryName) {
        ResourceSubCategoryFlyweight ret = getSubCategories().get(id);

        if (ret == null) {
            ret = new ResourceSubCategoryFlyweight();
            getSubCategories().put(id, ret);
        }

        ret.setId(id);
        ret.setName(name);

        if (parentSubCategoryId != null) {
            ResourceSubCategoryFlyweight parent = getSubCategories().get(parentSubCategoryId);
            if (parent == null) {
                parent = constructSubCategory(parentSubCategoryId, parentSubCategoryName, null, null);
            }
            ret.setParentSubCategory(parent);
        } else {
            ret.setParentSubCategory(null);
        }

        return ret;
    }

    /**
     * @see #constructResourceType(int, String, String, boolean, ResourceCategory, Integer)
     *
     * @param original the original resource type
     * @return a fully initialized resource type flyweight
     */
    public ResourceTypeFlyweight constructResourceType(ResourceType original) {
        int id = original.getId();
        String name = original.getName();
        String plugin = original.getPlugin();
        boolean singleton = original.isSingleton();
        ResourceCategory category = original.getCategory();
        String subCategory = original.getSubCategory();

        return constructResourceType(id, name, plugin, singleton, category, subCategory);
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
     * @param singleton true if the resource type is a singleton
     * @param category the resource type category
     * @param subCategory the id of the resource type sub category or null
     * @return the resource type flyweight
     */
    public ResourceTypeFlyweight constructResourceType(int id, String name, String plugin, boolean singleton,
        ResourceCategory category, String subCategory) {

        ResourceTypeFlyweight ret = getResourceTypes().get(id);

        if (ret == null) {
            ret = new ResourceTypeFlyweight();
            getResourceTypes().put(id, ret);
        }

        ret.setId(id);
        ret.setName(name);
        ret.setPlugin(plugin);
        ret.setSingleton(singleton);
        ret.setCategory(category);
        ret.setSubCategory(subCategory);

        return ret;
    }
}
