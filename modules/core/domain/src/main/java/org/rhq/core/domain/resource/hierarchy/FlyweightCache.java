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

import java.util.HashMap;
import java.util.Map;

/**
 * A helper object to hold the cached instances of the flyweights.
 * The keys in the maps are ids, values are the objects themselves.
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
}
