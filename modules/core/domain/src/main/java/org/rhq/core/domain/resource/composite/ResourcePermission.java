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
package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.authz.Permission;

/**
 * A set of permissions that apply to a particular Resource or ResourceGroup (i.e. {@link Permission}s where
 * {@link Permission#getTarget()} is {@link Permission.Target#RESOURCE}
 *
 * @author Joseph Marques
 * @author Greg Hinkle
 */
public class ResourcePermission implements Serializable {

    private static final long serialVersionUID = 2L;

    private Set<Permission> permissions;

    /**
     * All permissions
     */
    public ResourcePermission() {
        this.permissions = Permission.RESOURCE_ALL;
    }

    public ResourcePermission(//
        boolean measure, //
        boolean inventory, //
        boolean control, //
        boolean alert, //
        boolean event, //
        boolean configureRead, //
        boolean configureWrite, //
        boolean content, //
        boolean createChildResources,//
        boolean deleteResource) {

        this.permissions = new HashSet<Permission>();

        if (measure) {
            this.permissions.add(Permission.MANAGE_MEASUREMENTS);
        }
        if (inventory) {
            this.permissions.add(Permission.MANAGE_INVENTORY);
        }
        if (control) {
            this.permissions.add(Permission.CONTROL);
        }
        if (alert) {
            this.permissions.add(Permission.MANAGE_ALERTS);
        }
        if (event) {
            this.permissions.add(Permission.MANAGE_EVENTS);
        }
        if (configureRead) {
            this.permissions.add(Permission.CONFIGURE_READ);
        }
        if (configureWrite) {
            this.permissions.add(Permission.CONFIGURE_WRITE);
        }
        if (content) {
            this.permissions.add(Permission.MANAGE_CONTENT);
        }
        if (createChildResources) {
            this.permissions.add(Permission.CREATE_CHILD_RESOURCES);
        }
        if (deleteResource) {
            this.permissions.add(Permission.DELETE_RESOURCE);
        }
    }

    public ResourcePermission(Set<Permission> permissions) {
        if (permissions instanceof EnumSet) {
            throw new IllegalArgumentException("EnumSet is not allowed due to GWT Serialization issues");
        }

        this.permissions = permissions;
    }

    public boolean isMeasure() {
        return this.permissions.contains(Permission.MANAGE_MEASUREMENTS);
    }

    public boolean isInventory() {
        return this.permissions.contains(Permission.MANAGE_INVENTORY);
    }

    public boolean isControl() {
        return this.permissions.contains(Permission.CONTROL);
    }

    public boolean isAlert() {
        return this.permissions.contains(Permission.MANAGE_ALERTS);
    }

    public boolean isEvent() {
        return this.permissions.contains(Permission.MANAGE_EVENTS);
    }

    public boolean isConfigureRead() {
        return this.permissions.contains(Permission.CONFIGURE_READ);
    }

    public boolean isConfigureWrite() {
        return this.permissions.contains(Permission.CONFIGURE_WRITE);
    }

    public boolean isContent() {
        return this.permissions.contains(Permission.MANAGE_CONTENT);
    }

    public boolean isCreateChildResources() {
        return this.permissions.contains(Permission.CREATE_CHILD_RESOURCES);
    }

    public boolean isDeleteResource() {
        return this.permissions.contains(Permission.DELETE_RESOURCE);
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    @Override
    public String toString() {
        return "ResourcePermission[" + this.permissions + "]";
    }
}