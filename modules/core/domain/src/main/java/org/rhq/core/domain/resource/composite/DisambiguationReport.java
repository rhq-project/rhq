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

package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Contains information about disambiguation of a resource name.
 * 
 * @author Lukas Krejci
 */
public class DisambiguationReport<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private T original;
    private List<Resource> parents;
    private ResourceType resourceType;

    public static class ResourceType implements Serializable {

        private static final long serialVersionUID = 1L;
        private String name;
        private String plugin;
        private boolean singleton;
        
        /**
         * @param name
         * @param plugin
         * @param singleton
         */
        public ResourceType(String name, String plugin, boolean singleton) {
            super();
            this.name = name;
            this.plugin = plugin;
            this.singleton = singleton;
        }
        
        public String getName() {
            return name;
        }
        
        /**
         * @return the plugin that defines this type or null if such information
         * isn't needed to disambiguate this type.
         */
        public String getPlugin() {
            return plugin;
        }

        public boolean isSingleton() {
            return singleton;
        }
        
        public String toString() {
            return "ResourceType[name='" + name + "', plugin='" + plugin + "'" + "]";
        }
    }

    public static class Resource implements Serializable {

        private static final long serialVersionUID = 1L;
        private int id;
        private String name;
        private ResourceType type;
        
        /**
         * @param id
         * @param name
         * @param type
         */
        public Resource(int id, String name, ResourceType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        /**
         * @return the resource type to use to disambiguate the concrete resource or null
         * if no type disambiguation is needed.
         */
        public ResourceType getType() {
            return type;
        }
        
        public String toString() {
            return "Resource[id=" + id + ", name='" + name + "', type=" + type + "]";
        }
    }
    
    public DisambiguationReport(T original, List<Resource> parents, ResourceType resourceType) {
        this.original = original;
        this.parents = Collections.unmodifiableList(parents);
        this.resourceType = resourceType;
    }

    public T getOriginal() {
        return original;
    }
    
    /**
     * @return the list of parents to disambiguate the original. Empty if no disambiguation using
     * parents is needed.
     */
    public List<Resource> getParents() {
        return parents;
    }

    /**
     * @return the ResourceType of the resource represented by the {@link #getOriginal()}
     * or null if type disambiguation isn't needed.
     */
    public ResourceType getResourceType() {
        return resourceType;
    }
    
    public String toString() {
        return "DisambiguationReport(type=" + resourceType + ", parents=" + parents + ", original=" + original + ")";
    }
}