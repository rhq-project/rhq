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

package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.HashSet;
import java.util.Set;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;

/**
 * This class allows for TableAction (ie. Button) enablement based on row selection and global perm authorization.
 * 
 * For Resource Perm authorization see {@link ResourceAuthorizedTableAction}.
 * 
 * @author Jay Shaughnessy
 */
public abstract class AuthorizedTableAction extends AbstractTableAction {

    Table<?> table;

    HashSet<Permission> globalPermissions = new HashSet<Permission>();

    Boolean isGlobalAuthorized;

    protected AuthorizedTableAction(Table<?> table, Permission... permissions) {
        this(table, TableActionEnablement.ALWAYS, permissions);
    }

    protected AuthorizedTableAction(Table<?> table, TableActionEnablement enablement, Permission... permissions) {
        super(enablement);

        this.table = table;

        for (Permission p : permissions) {
            switch (p.getTarget()) {
            case GLOBAL:
                globalPermissions.add(p);
                break;
            case RESOURCE:
                throw new IllegalArgumentException("Does not support Resource permissions");
            }
        }

        if (globalPermissions.isEmpty()) {
            isGlobalAuthorized = Boolean.TRUE;
        }
    }

    @Override
    public boolean isEnabled(ListGridRecord[] selection) {
        if (!super.isEnabled(selection)) {
            return false;
        }

        if (null == isGlobalAuthorized) {
            new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {

                public void onPermissionsLoaded(Set<Permission> grantedPermissions) {
                    for (Permission requiredPermission : globalPermissions) {
                        if (!grantedPermissions.contains(requiredPermission)) {
                            return;
                        }
                    }
                    isGlobalAuthorized = true;
                    table.refreshTableInfo();
                }
            });

            return false;
        }

        return isGlobalAuthorized;
    }
}
