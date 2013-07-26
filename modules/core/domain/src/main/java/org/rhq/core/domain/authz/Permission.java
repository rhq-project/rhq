/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
import java.util.Set;

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
    MANAGE_SECURITY(Target.GLOBAL), // 0

    /**
     * can C/R/U/D all resources, groups and can import auto-discovered resources
     */
    MANAGE_INVENTORY(Target.GLOBAL), // 1

    /**
     * can modify the RHQ Server configuration and perform any server-related functionality
     */
    MANAGE_SETTINGS(Target.GLOBAL), // 2

    /* ========= Resource Permissions ========= */

    /**
     * can view (but not C/U/D) all aspects of this Resource except its configuration ({@link #CONFIGURE_READ} is
     * required to view that); this permission is implied just by having a Resource or Group in one's assigned Roles
     */
    VIEW_RESOURCE(Target.RESOURCE), // 3

    /**
     * can modify resource name, description, and plugin config (e.g. set principal/credentials jboss-as plugin uses to
     * access the managed JBossAS instance)
     */
    MODIFY_RESOURCE(Target.RESOURCE), // 4

    /**
     * can delete this resource (which also implies deleting all its descendant resources)
     */
    DELETE_RESOURCE(Target.RESOURCE), // 5

    /**
     * can manually create new child servers or services
     */
    CREATE_CHILD_RESOURCES(Target.RESOURCE), // 6

    /**
     * can C/U/D alert definitions (this implies {@link #MANAGE_MEASUREMENTS} and {@link #CONTROL})
     */
    MANAGE_ALERTS(Target.RESOURCE), // 7

    /**
     * can C/U/D metric schedules
     */
    MANAGE_MEASUREMENTS(Target.RESOURCE), // 8

    /**
     * can C/U/D content (package bits, software updates, etc.)
     */
    MANAGE_CONTENT(Target.RESOURCE), // 9

    /**
     * can invoke operations and delete operation history items
     */
    CONTROL(Target.RESOURCE), // 10

    /**
     * can C/U/D resource config (e.g. reconfiguring JBoss to listen for jnp on port 1199);
     * having this permission implies having {@link #CONFIGURE_READ}
     */
    CONFIGURE_WRITE(Target.RESOURCE), // 11

    /**
     * can C/U/D provisioning bundles
     */
    // NOTE: This is a GLOBAL permission, but is defined down here so as to maintain the ordinal indexes of the other
    //       pre-existing permissions.
    MANAGE_BUNDLE(Target.GLOBAL), // 12

    /**
     * can view Resource configuration, but can not necessarily C/U/D unless {@link #CONFIGURE_WRITE} is also possessed
     */
    CONFIGURE_READ(Target.RESOURCE), // 13

    /**
     * can C/U/D events
     * (in the future, will also C/U/D event definitions)
     */
    MANAGE_EVENTS(Target.RESOURCE), // 14

    /**
     * Can C/U/D repositories and content sources
     */
    // NOTE: This is a GLOBAL permission but defined here to maintain the ordinal indexes
    MANAGE_REPOSITORIES(Target.GLOBAL), // 15

    /**
     * Can C/U/D drift related entities
     */
    MANAGE_DRIFT(Target.RESOURCE), // 16

    /**
     * Can view other RHQ users, except for their assigned roles
     */
    // NOTE: This is a GLOBAL permission but defined here to maintain the ordinal indexes
    VIEW_USERS(Target.GLOBAL), // 17

    /**
     * Can CRUD BundleGroups
     */
    MANAGE_BUNDLE_GROUPS(Target.GLOBAL), // 18

    /**
     * Can create Bundle [Versions]s
     * Can assign to viewable bundle groups 
     * Can create global Bundle [Versions] if holding Global.VIEW_BUNDLES
     */
    CREATE_BUNDLES(Target.GLOBAL), // 19

    /**
     * Can delete viewable bundle [Versions]s
     * Can unassign from viewable bundle groups 
     * Can delete global bundles if holding Global.VIEW_BUNDLES
     */
    DELETE_BUNDLES(Target.GLOBAL), // 20

    /**
     * Can view any bundle, including global bundles
     */
    VIEW_BUNDLES(Target.GLOBAL), // 21

    /**
     * Can deploy any viewable bundle version to any viewable (deployable, compatible) resource group
     */
    DEPLOY_BUNDLES(Target.GLOBAL), // 22

    /**
     * Can assign viewable bundles to the bundle group
     * - this can be a copy from another viewable bundle group
     * - this can be a global bundle if holding Global.VIEW_BUNDLES
     */
    ASSIGN_BUNDLES_TO_GROUP(Target.BUNDLE), // 23

    /**
     * Can unassign bundles from the bundle group
     * - the bundle is not deleted and becomes a global bundle if assigned to no other bundle group
     */
    UNASSIGN_BUNDLES_FROM_GROUP(Target.BUNDLE), // 24

    /**
     * Can create bundle [version]s for this bundle group
     */
    CREATE_BUNDLES_IN_GROUP(Target.BUNDLE), // 25

    /**
     * Can delete bundle [version]s from the bundle group
     */
    DELETE_BUNDLES_FROM_GROUP(Target.BUNDLE), // 26

    /**
     * Implied - Can view bundles in the bundle group
     */
    VIEW_BUNDLES_IN_GROUP(Target.BUNDLE) // 27

    ;

    /**
     * the target to which the permission applies
     */
    public enum Target {
        /** global permissions do not apply to specific resources or bundles  */
        GLOBAL,

        /** resource permissions apply only to the resources in the role's resource groups */
        RESOURCE,

        /** bundle permissions apply only to the bundles in the role's bundle groups */
        BUNDLE
    }

    private Target target;

    Permission(Target target) {
        this.target = target;
    }

    /**
     * Returns the target to which the permission applies
     *
     * @return the target to which the permission applies
     */
    public Target getTarget() {
        return target;
    }

    public static final Set<Permission> GLOBAL_ALL = new HashSet<Permission>();
    public static final Set<Permission> RESOURCE_ALL = new HashSet<Permission>();
    public static final Set<Permission> BUNDLE_ALL = new HashSet<Permission>();
    static {
        for (Permission permission : Permission.values()) {
            switch (permission.getTarget()) {
            case GLOBAL:
                GLOBAL_ALL.add(permission);
                if (permission.name().contains("BUNDLE")) {
                    BUNDLE_ALL.add(permission);
                }
                break;
            case RESOURCE:
                RESOURCE_ALL.add(permission);
                break;
            case BUNDLE:
                BUNDLE_ALL.add(permission);
                break;
            }
        }
    }

}