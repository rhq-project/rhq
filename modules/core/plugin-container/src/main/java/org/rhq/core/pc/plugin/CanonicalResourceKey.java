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
package org.rhq.core.pc.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;

/**
 * This object represents a canonical ID that can uniquely identify a resource
 * within a resource hierarchy.
 *
 * @author John Mazzitelli
 */
public class CanonicalResourceKey implements Serializable {

    private static final long serialVersionUID = 1L;

    private final KeyTypePlugin resourceKeyTypePlugin;
    private final List<KeyTypePlugin> ancestorKeyTypePlugins;

    public CanonicalResourceKey(Resource resource, Resource parent) throws PluginContainerException {
        if (resource == null) {
            throw new PluginContainerException("resource must not be null");
        }
        if (parent == null) {
            throw new PluginContainerException("parent must not be null");
        }

        this.resourceKeyTypePlugin = new KeyTypePlugin(resource.getResourceKey(), resource.getResourceType());

        this.ancestorKeyTypePlugins = new ArrayList<KeyTypePlugin>(5);
        while (parent != null) {
            KeyTypePlugin ktp = new KeyTypePlugin(parent.getResourceKey(), parent.getResourceType());
            this.ancestorKeyTypePlugins.add(ktp);
            parent = parent.getParentResource();
        }
    }

    public String getResourceKey() {
        return this.resourceKeyTypePlugin.key;
    }

    public String getResourceTypeName() {
        return this.resourceKeyTypePlugin.type;
    }

    public String getResourcePlugin() {
        return this.resourceKeyTypePlugin.plugin;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int ancestor = 0;
        for (KeyTypePlugin ktp : this.ancestorKeyTypePlugins) {
            builder.append("ancestor#").append(ancestor++).append("=").append(ktp).append(',');
        }
        builder.append("resource=").append(this.resourceKeyTypePlugin);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + this.resourceKeyTypePlugin.hashCode();
        for (KeyTypePlugin ktp : this.ancestorKeyTypePlugins) {
            result = 31 * result + ktp.hashCode();
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CanonicalResourceKey)) {
            return false;
        }

        CanonicalResourceKey other = (CanonicalResourceKey) obj;

        if (!this.resourceKeyTypePlugin.equals(other.resourceKeyTypePlugin)) {
            return false;
        }

        if (!this.ancestorKeyTypePlugins.equals(other.ancestorKeyTypePlugins)) {
            return false;
        }

        return true;
    }

    private static class KeyTypePlugin {
        public final String key;
        public final String type;
        public final String plugin;

        KeyTypePlugin(String key, ResourceType type) throws PluginContainerException {
            if (key == null) {
                throw new PluginContainerException("key must not be null");
            }
            if (type == null) {
                throw new PluginContainerException("type must not be null");
            }
            if (type.getName() == null) {
                throw new PluginContainerException("type name must not be null");
            }
            if (type.getPlugin() == null) {
                throw new PluginContainerException("plugin must not be null");
            }
            this.key = key.intern();
            this.type = type.getName().intern();
            this.plugin = type.getPlugin().intern();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("[key=").append(key).append(",type=").append(type).append(",plugin=").append(plugin).append(
                "]");
            return builder.toString();
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + key.hashCode();
            result = 31 * result + type.hashCode();
            result = 31 * result + plugin.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof KeyTypePlugin)) {
                return false;
            }
            KeyTypePlugin other = (KeyTypePlugin) obj;
            if (!this.key.equals(other.key)) {
                return false;
            }
            if (!this.plugin.equals(other.plugin)) {
                return false;
            }
            if (!this.type.equals(other.type)) {
                return false;
            }
            return true;
        }
    }
}
