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
package org.rhq.core.domain.resource.composite;

import java.util.Set;
import org.rhq.core.domain.authz.Permission;

/**
 * @author Greg Hinkle
 */
public class ResourcePermission {
    private boolean measure;

    private boolean inventory;

    private boolean control;

    private boolean alert;

    private boolean configure;

    private boolean software;

    /**
     * All permissions
     */
    public ResourcePermission() {
        this(true, true, true, true, true, true);
    }

    public ResourcePermission(boolean measure, boolean inventory, boolean control, boolean alert, boolean configure,
        boolean software) {
        this.measure = measure;
        this.inventory = inventory;
        this.control = control;
        this.alert = alert;
        this.configure = configure;
        this.software = software;
    }

    public ResourcePermission(Set<Permission> permissions) {
        this(permissions.contains(Permission.MANAGE_MEASUREMENTS), permissions.contains(Permission.MODIFY_RESOURCE),
            permissions.contains(Permission.CONTROL), permissions.contains(Permission.MANAGE_ALERTS), permissions
                .contains(Permission.CONFIGURE), permissions.contains(Permission.MANAGE_CONTENT));
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

    public boolean isSoftware() {
        return software;
    }

    @Override
    public String toString() {
        return "ResourcePermission=[" + "measure: " + measure + ", inventory: " + inventory + ", control: " + control
            + ", alert: " + alert + ", configure: " + configure + ", software: " + software + "]";
    }
}