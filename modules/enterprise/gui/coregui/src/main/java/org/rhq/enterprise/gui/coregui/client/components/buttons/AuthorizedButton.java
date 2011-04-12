/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.components.buttons;

import java.util.HashSet;
import java.util.Set;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.components.table.AuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableButton;

// TODO: This utility isCurrently unused. Candidate for removal. 

/**
 * This class allows for a standalone Button enablement based on global perm authorization.
 * 
 * For buttons embedded in Table views see {@link AuthorizedTableAction}.
 *  
 * @author Jay Shaughnessy
 */
public class AuthorizedButton extends LocatableButton {

    HashSet<Permission> globalPermissions = new HashSet<Permission>();

    Boolean isGlobalAuthorized;

    public AuthorizedButton(String locatorId, String title, Permission... permissions) {
        super(locatorId, title);

        if (permissions.length == 0) {
            throw new IllegalArgumentException("Must provide at least one Permission");
        }

        for (Permission p : permissions) {
            switch (p.getTarget()) {
            case GLOBAL:
                globalPermissions.add(p);
                break;
            case RESOURCE:
                throw new IllegalArgumentException("Does not support Resource permissions");
            }
        }

        setDisabled(true);

        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

            public void onPermissionsLoaded(Set<Permission> grantedPermissions) {
                for (Permission requiredPermission : globalPermissions) {
                    if (!grantedPermissions.contains(requiredPermission)) {
                        return;
                    }
                }
                setDisabled(false);
                markForRedraw();
            }
        });
    }
}
