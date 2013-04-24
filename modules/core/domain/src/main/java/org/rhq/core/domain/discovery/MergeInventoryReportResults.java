 /*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.core.domain.discovery;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.rhq.core.domain.resource.ResourceType;

/**
 * The results of a merge inventory. The main data is the resource hierarchy found in the ResourceSyncInfo but
 * also provides optional information such as the resource types that were ignored when merging the
 * inventory report.
 *
 * @author John Mazzitelli
 */
public class MergeInventoryReportResults implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ResourceSyncInfo resourceSyncInfo;
    private final Collection<ResourceTypeFlyweight> ignoredResourceTypes;

    public MergeInventoryReportResults(ResourceSyncInfo rsi, Collection<ResourceType> ignoredResourceTypes) {
        resourceSyncInfo = rsi;

        if (ignoredResourceTypes == null || ignoredResourceTypes.isEmpty()) {
            this.ignoredResourceTypes = null;
        } else {
            this.ignoredResourceTypes = new ArrayList<ResourceTypeFlyweight>(ignoredResourceTypes.size());
            for (ResourceType ignoredType : ignoredResourceTypes) {
                this.ignoredResourceTypes.add(new ResourceTypeFlyweight(ignoredType));
            }
        }
    }

    public ResourceSyncInfo getResourceSyncInfo() {
        return resourceSyncInfo;
    }

    public Collection<ResourceTypeFlyweight> getIgnoredResourceTypes() {
        return ignoredResourceTypes;
    }

    /**
     * A small class encapsulating the primary key of a resource type.
     * Make this as small as possible to reduce over-the-wire size of this object.
     */
    public static class ResourceTypeFlyweight implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String plugin;
        private final String name;

        public ResourceTypeFlyweight(ResourceType rt) {
            this(rt.getPlugin(), rt.getName());
        }

        public ResourceTypeFlyweight(String plugin, String name) {
            if (plugin == null || name == null) {
                throw new NullPointerException("plugin and name must not be null");
            }
            this.plugin = plugin;
            this.name = name;
        }

        public String getPlugin() {
            return plugin;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + name.hashCode();
            result = 31 * result + plugin.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ResourceTypeFlyweight)) {
                return false;
            }
            ResourceTypeFlyweight other = (ResourceTypeFlyweight) obj;
            return (this.name.equals(other.name)) && (this.plugin.equals(other.plugin));
        }

    }
}