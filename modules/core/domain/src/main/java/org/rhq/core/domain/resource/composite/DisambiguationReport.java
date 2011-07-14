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
import java.util.List;

/**
 * Contains information about disambiguation of a Resource name.
 * 
 * @author Lukas Krejci
 */
public class DisambiguationReport<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private T original;
    private List<Resource> parents;
    private Resource resource;

    public static class ResourceType implements Serializable {

        private static final long serialVersionUID = 1L;
        private String name;
        private String plugin;
        private boolean singleton;

        //no args
        public ResourceType() {
        }

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
        //no args
        public Resource() {
        }

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

    //GWT needs this
    protected DisambiguationReport() {
    }

    public DisambiguationReport(T original, List<Resource> parents, Resource resource) {
        this.original = original;
        //        this.parents = Collections.unmodifiableList(parents);
        //spinder: the returned type is not Serializable and causes GWT serialization errors.
        this.parents = parents;
        this.resource = resource;
    }

    /**
     * @return an object from the original result list that this disambiguation report represents.
     */
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
     * @return the resource that the {@link #getOriginal() original} represents.
     */
    public Resource getResource() {
        return resource;
    }
    
    /**
     * @return the ResourceType of the resource represented by the {@link #getOriginal()}
     * or null if type disambiguation isn't needed.
     * 
     * @deprecated use {@link #getResource()}.{@link Resource#getType() getType()}
     */
    @Deprecated
    public ResourceType getResourceType() {
        return resource.getType();
    }

    /**
     *
     * @return the Resource name
     * 
     * @deprecated use {@link #getResource()}.{@link Resource#getName() getName()}
     */
    @Deprecated
    public String getName() {
        return resource.getName();
    }

    /**
     *
     * @return the Resource id
     * 
     * @deprecated use {@link #getResource()}.{@link Resource#getId() getId()}
     */
    @Deprecated
    public int getId() {
        return resource.getId();
    }

    public String toString() {
        return "DisambiguationReport(resource=" + resource + ", parents=" + parents + ", original=" + original
            + ")";
    }
    
}