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
package org.rhq.enterprise.gui.authorization;

import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class GlobalPermissionsUIBean {

    private boolean security;
    private boolean inventory;
    private boolean settings;
    private boolean isSuperuser;
    private boolean isDebugMode;
    private boolean isExperimental;

    public GlobalPermissionsUIBean() {
        Subject user = EnterpriseFacesContextUtility.getSubject();

        // if a subject ID is 0, it probably means this is a new LDAP user that needs to be registered
        if (user.getId() != 0) {
            Set<Permission> globalPermissions = LookupUtil.getAuthorizationManager().getExplicitGlobalPermissions(user);
            security = globalPermissions.contains(Permission.MANAGE_SECURITY);
            inventory = globalPermissions.contains(Permission.MANAGE_INVENTORY);
            settings = globalPermissions.contains(Permission.MANAGE_SETTINGS);
            isSuperuser = LookupUtil.getAuthorizationManager().isSystemSuperuser(user);
        }

        SystemManagerLocal systemManager = LookupUtil.getSystemManager();
        isDebugMode = systemManager.isDebugModeEnabled();
        isExperimental = systemManager.isExperimentalFeaturesEnabled();
    }

    public boolean isSuperuser() {
        return isSuperuser;
    }

    public boolean isDebugMode() {
        return isDebugMode;
    }

    public boolean isExperimental() {
        return isExperimental;
    }

    public boolean isSecurity() {
        return security;
    }

    public boolean isInventory() {
        return inventory;
    }

    public boolean isSettings() {
        return settings;
    }
}
