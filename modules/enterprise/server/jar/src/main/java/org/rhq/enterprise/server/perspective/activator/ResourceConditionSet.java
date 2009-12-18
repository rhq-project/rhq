/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.perspective.activator;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.regex.Pattern;

import org.rhq.core.domain.authz.Permission;

/**
 * An immutable set of conditions pertaining to a Resource:
 *
 * 1) the ResourceType the Resource must have (required)
 * 2) a set of Resource permissions the Resource must possess (optional)
 * 3) a set of traits with specific current values the Resource must possess (optional)
 *
 * @author Ian Springer
 */
public class ResourceConditionSet {
    static final long serialVersionUID = 1L;

    private String pluginName;
    private String resourceTypeName;
    private EnumSet<Permission> permissions;
    private Map<String, Pattern> traits;

    public ResourceConditionSet(String pluginName, String resourceTypeName, EnumSet<Permission> permissions,
                                Map<String, Pattern> traits) {
        this.pluginName = pluginName;
        this.resourceTypeName = resourceTypeName;
        this.permissions = permissions != null ? permissions : EnumSet.noneOf(Permission.class);
        this.traits = traits != null ? traits : Collections.<String, Pattern>emptyMap();
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getResourceTypeName() {
        return resourceTypeName;
    }

    public EnumSet<Permission> getPermissions() {
        return permissions;
    }

    public Map<String, Pattern> getTraits() {
        return traits;
    }
}
