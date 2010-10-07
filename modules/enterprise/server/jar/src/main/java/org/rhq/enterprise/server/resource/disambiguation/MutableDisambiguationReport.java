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

package org.rhq.enterprise.server.resource.disambiguation;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.domain.resource.composite.DisambiguationReport;

/**
 * This is a helper class representing a disambiguation report during the process of disambiguation
 * where a writable access is needed to the report's properties. The resulting {@link DisambiguationReport}
 * is an immutable class.
 *  
 * @param <T> the type of the instances being disambiguated
 * 
 * @author Lukas Krejci
 */
public class MutableDisambiguationReport<T> {
    public static class ResourceType implements Cloneable {
        public int id;
        public String name;
        public String plugin;
        public boolean singleton;
        
        public DisambiguationReport.ResourceType getResourceType() {
            return new DisambiguationReport.ResourceType(name, plugin, singleton);
        }
        
        public String toString() {
            return "MutableResourceType[id=" + id + ", name='" + name + "', plugin='" + plugin + "', singleton=" + singleton + "]";
        }
        
        @Override
        public ResourceType clone() {
            ResourceType ret = new ResourceType();
            ret.id = id;
            ret.name = name;
            ret.plugin = plugin;
            ret.singleton = singleton;
            
            return ret;
        }
    }
    
    public static class Resource implements Cloneable {
        public int id;
        public String name;
        public MutableDisambiguationReport.ResourceType resourceType;
        
        public DisambiguationReport.Resource getResource() {
            return new DisambiguationReport.Resource(id, name, resourceType.getResourceType());
        }
        
        public String toString() {
            return "MutableResource[id=" + id + ", name='" + name + ", resourceType=" + resourceType + "]";
        }
        
        @Override
        public Resource clone() {
            Resource ret = new Resource();
            ret.id = id;
            ret.name = name;
            ret.resourceType = resourceType.clone();
            
            return ret;
        }
    }
    
    public T original;
    public MutableDisambiguationReport.Resource resource;
    public List<MutableDisambiguationReport.Resource> parents;
    
    public DisambiguationReport<T> getReport() {
        List<DisambiguationReport.Resource> realParents = new ArrayList<DisambiguationReport.Resource>();
        if (parents != null) {
            for(MutableDisambiguationReport.Resource p : parents) {
                realParents.add(p.getResource());
            }
        }
        return new DisambiguationReport<T>(original, realParents, resource.resourceType.getResourceType(), resource.name);
    } 
    
    @Override
    public String toString() {
        return "MutableDisambiguationReport[resource=" + resource + ", parents=" + parents + "]";
    }
}