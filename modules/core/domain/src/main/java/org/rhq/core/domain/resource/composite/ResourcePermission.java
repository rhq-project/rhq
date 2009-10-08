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

import java.util.Set;
import java.io.Serializable;

import org.rhq.core.domain.authz.Permission;

/**
 * @author Joseph Marques
 * @author Greg Hinkle
 */
public class ResourcePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean measure;

    private boolean inventory;

    private boolean control;

    private boolean alert;

    private boolean configure;

    private boolean content;

    private boolean createChildResources;

    private boolean deleteResource;

    /**
     * All permissions
     */
    public ResourcePermission() {
        this(true, true, true, true, true, true, true, true);
    }

    public ResourcePermission(boolean measure, boolean inventory, boolean control, boolean alert, boolean configure,
        boolean content, boolean createChildResources, boolean deleteResource) {
        this.measure = measure;
        this.inventory = inventory;
        this.control = control;
        this.alert = alert;
        this.configure = configure;
        this.content = content;
        this.createChildResources = createChildResources;
        this.deleteResource = deleteResource;
    }

    public ResourcePermission(Set<Permission> permissions) {
        this(permissions.contains(Permission.MANAGE_MEASUREMENTS), permissions.contains(Permission.MODIFY_RESOURCE),
            permissions.contains(Permission.CONTROL), permissions.contains(Permission.MANAGE_ALERTS), permissions
                .contains(Permission.CONFIGURE), permissions.contains(Permission.MANAGE_CONTENT), permissions
                .contains(Permission.CREATE_CHILD_RESOURCES), permissions.contains(Permission.DELETE_RESOURCE));
    }

    public boolean isMeasure() {
        return measure;
    }

    public boolean isInventory() {
        return inventory;
    }

    public boolean isControl() {
        return control;
    }

    public boolean isAlert() {
        return alert;
    }

    public boolean isConfigure() {
        return configure;
    }

    public boolean isContent() {
        return content;
    }

    public boolean isCreateChildResources() {
        return createChildResources;
    }

    public boolean isDeleteResource() {
        return deleteResource;
    }

    @Override
    public String toString() {
        return "ResourcePermission=[" + //
            "measure: " + measure + ", " + //
            "inventory: " + inventory + ", " + //
            "control: " + control + ", " + //
            "alert: " + alert + ", " + // 
            "configure: " + configure + ", " + //
            "content: " + content + ", " + //
            "createChildResources: " + createChildResources + ", " + //
            "deleteResource: " + deleteResource + //
            "]";
    }
}