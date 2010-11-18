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
package org.rhq.core.domain.authz;

import java.util.HashSet;

import org.rhq.core.domain.auth.Subject;

/**
 * An authorization permission is applied to {@link Role}s and related to {@link Subject}s that are members of those
 * Roles. There are two types of permissions - {@link Target#GLOBAL global} and {@link Target#RESOURCE Resource}.
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
    MANAGE_SECURITY(Target.GLOBAL, "Manage Security", "can C/U/D users and roles (viewing is implied for everyone)"), // 0

    /**
     * can C/R/U/D all resources, groups and can import auto-discovered resources
     */
    MANAGE_INVENTORY(Target.GLOBAL, "Manage Inventory",
        "can C/R/U/D all resources, groups and can import auto-discovered resources"), // 1

    /**
     * can modify the JON Server configuration and perform any server-related functionality
     */
    MANAGE_SETTINGS(Target.GLOBAL, "Manage Settings",
        "can modify the JON Server configuration and perform any server-related functionality"), // 2

    /* ========= Resource Permissions ========= */

    /**
     * can view (but not C/U/D) all aspects of this Resource except its configuration ({@link #CONFIGURE_READ} is
     * required to view that); this permission is implied just by having a Resource or Group in one's assigned Roles
     */
    VIEW_RESOURCE(
        Target.RESOURCE,
        "View Resource",
        "can view (but not C/U/D) all aspects of this Resource except its configuration (CONFIGURE_READ is"
            + "required to view that); this permission is implied just by having a Resource or Group in one's assigned Roles"), // 3

    /**
     * can modify resource name, description, and plugin config (e.g. set principal/credentials jboss-as plugin uses to
     * access the managed JBossAS instance)
     */
    MODIFY_RESOURCE(Target.RESOURCE, "Modify Resource",
        "can modify resource name, description, and plugin config (e.g. set principal/credentials jboss-as plugin uses "
            + "to access the managed JBossAS instance)"), // 4

    /**
     * can delete this resource (which also implies deleting all its descendant resources)
     */
    DELETE_RESOURCE(Target.RESOURCE, "Delete Resource",
        "can delete this resource (which also implies deleting all its descendant resources)"), // 5

    /**
     * can manually create new child servers or services
     */
    CREATE_CHILD_RESOURCES(Target.RESOURCE, "Create Child Resource",
        "can manually create new child servers or services"), // 6

    /**
     * can C/U/D alert definitions (this implies {@link #MANAGE_MEASUREMENTS} and {@link #CONTROL})
     */
    MANAGE_ALERTS(Target.RESOURCE, "Manage Alerts",
        "can C/U/D alert definitions (this implies MANAGE_MEASUREMENTS and CONTROL)"), // 7

    /**
     * can C/U/D metric schedules
     */
    MANAGE_MEASUREMENTS(Target.RESOURCE, "Manage Measurements", "can C/U/D metric schedules"), // 8

    /**
     * can C/U/D content (package bits, software updates, etc.)
     */
    MANAGE_CONTENT(Target.RESOURCE, "Manage Content", "can C/U/D content (package bits, software updates, etc.)"), // 9

    /**
     * can invoke operations and delete operation history items
     */
    CONTROL(Target.RESOURCE, "Execute Operations", "can invoke operations and delete operation history items"), // 10

    /**
     * can C/U/D resource config (e.g. reconfiguring JBoss to listen for jnp on port 1199);
     * having this permission implies having {@link #CONFIGURE_READ}
     */
    CONFIGURE_WRITE(Target.RESOURCE, "Update Configuration",
        "can C/U/D resource config (e.g. reconfiguring JBoss to listen for jnp on port 1199); "
            + "having this permission implies having CONFIGURE_READ"), // 11

    /**
     * can C/U/D provisioning bundles
     */
    // NOTE: This is a GLOBAL permission, but is defined down here so as to maintain the ordinal indexes of the other
    //       pre-existing permissions.
    MANAGE_BUNDLE(Target.GLOBAL, "Manage Bundles", "can C/U/D provisioning bundles"), // 12

    /**
     * can view Resource configuration, but can not necessarily C/U/D unless {@link #CONFIGURE_WRITE} is also possessed
     */
    CONFIGURE_READ(Target.RESOURCE, "View Configuration",
        "can view Resource configuration, but can not necessarily C/U/D unless CONFIGURE_WRITE is also possessed"), // 13

    /**
     * can C/U/D events
     * (in the future, will also C/U/D event definitions)
     */
    MANAGE_EVENTS(Target.RESOURCE, "Manage Events", "can C/U/D events") // 14

    ;

    /**
     * the target to which the permission applies
     */
    public enum Target {
        /** global permissions do not apply to specific resources in groups */
        GLOBAL,

        /** resource permissions apply only to the resources in the role's groups */
        RESOURCE
    }

    private Target target;

    /**
     * a brief display name for the permission (TODO: i18n)
     */
    private String displayName;

    /**
     * a one or two sentence description of the permission (TODO: i18n)
     */
    private String description;

    Permission(Target target, String displayName, String description) {
        this.target = target;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns the target to which the permission applies
     *
     * @return the target to which the permission applies
     */
    public Target getTarget() {
        return target;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static final HashSet<Permission> GLOBAL_ALL = new HashSet<Permission>();
    public static final HashSet<Permission> RESOURCE_ALL = new HashSet<Permission>();
    static {
        for (Permission permission : Permission.values()) {
            switch (permission.getTarget()) {
                case GLOBAL:
                    GLOBAL_ALL.add(permission);
                    break;
                case RESOURCE:
                    RESOURCE_ALL.add(permission);
                    break;
            }
        }
    }

}