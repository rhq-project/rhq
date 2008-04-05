package org.rhq.enterprise.gui.authorization;

import java.util.Set;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.LookupUtil;

public class GlobalPermissionsUIBean {

    private boolean security;
    private boolean inventory;
    private boolean settings;

    public GlobalPermissionsUIBean() {
        Subject user = EnterpriseFacesContextUtility.getSubject();
        Set<Permission> globalPermissions = LookupUtil.getAuthorizationManager().getExplicitGlobalPermissions(user);

        security = globalPermissions.contains(Permission.MANAGE_SECURITY);
        inventory = globalPermissions.contains(Permission.MANAGE_INVENTORY);
        settings = globalPermissions.contains(Permission.MANAGE_SETTINGS);
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
