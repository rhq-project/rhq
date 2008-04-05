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
package org.rhq.core.domain.authz;

import java.util.EnumSet;

import org.rhq.core.domain.auth.Subject;

/**
 * An authorization permission is applied to {@link Role}s and related to {@link Subject}s that are members of those
 * Roles. There are two types of permissions - global and resource.
 *
 * @author Ian Springer
 * @author Joseph Marques
 * @author Greg Hinkle
 */
public enum Permission {
    /* ========== Global Permissions ========== */
    /**
     * can C/U/D users and roles (viewing is implied for everyone)
     */
    MANAGE_SECURITY(Target.GLOBAL),

    /**
     * can C/R/U/D all resources, groups and can import auto-discovered resources
     */
    MANAGE_INVENTORY(Target.GLOBAL),

    /**
     * can modify the JON Server configuration and perform any server-related functionality
     */
    MANAGE_SETTINGS(Target.GLOBAL),

    /* ========= Resource Permissions ========= */

    /**
     * can view this resource's config, metrics, and alerts
     */
    VIEW_RESOURCE(Target.RESOURCE),

    /**
     * can modify resource name, description, and plugin config (e.g. set principal/credentials JBoss plugin uses to access the server)
     */
    MODIFY_RESOURCE(Target.RESOURCE),

    /**
     * can delete this resource (which also implies deleting all its descendant resources)
     */
    DELETE_RESOURCE(Target.RESOURCE),

    /**
     * can manually create new child servers or services
     */
    CREATE_CHILD_RESOURCES(Target.RESOURCE),

    /**
     * can C/U/D alert definitions (this implies {@link #VIEW_RESOURCE}, {@link #MANAGE_MEASUREMENTS}, {@link #CONTROL})
     */
    MANAGE_ALERTS(Target.RESOURCE),

    /**
     * can C/U/D metric schedules (this implies {@link #VIEW_RESOURCE})
     */
    MANAGE_MEASUREMENTS(Target.RESOURCE),

    /**
     * can C/U/D content (package bits, software updates, etc) (this implies {@link #VIEW_RESOURCE})
     */
    MANAGE_CONTENT(Target.RESOURCE),

    /**
     * can invoke operations that only change the "running" state of the resource (e.g. start/stop)
     */
    CONTROL(Target.RESOURCE),

    /**
     * can C/U/D resource config (e.g. reconfiguring JBoss to listen for jnp on port 1199)
     */
    CONFIGURE(Target.RESOURCE);

    /**
     * The target that a permission applies to.
     */
    public enum Target {
        /** global permissions do not apply to specific resources in groups */
        GLOBAL,

        /** resource permissions apply only to the resources in the role's groups */
        RESOURCE
    }

    private Target target;

    Permission(Target target) {
        this.target = target;
    }

    /**
     * Returns the target that this permission applies to.
     *
     * @return the target of this permission
     */
    public Target getTarget() {
        return target;
    }

    public static final EnumSet<Permission> RESOURCE_ALL = EnumSet.noneOf(Permission.class);

    static {
        for (Permission permission : Permission.values()) {
            if (permission.getTarget() == Target.RESOURCE) {
                RESOURCE_ALL.add(permission);
            }
        }
    }
}