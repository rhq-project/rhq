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
package org.rhq.enterprise.server.plugins.alertRoles;

import java.util.HashMap;
import java.util.Map;

import java.util.Set;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
public class RolesBackingBean {

    private Map<String, String> rolesMap;
    private Set<String> currentRoles;

    public Set<String> getCurrentRoles() {
        return currentRoles;
    }

    public void setCurrentRoles(Set<String> currentRoles) {
        this.currentRoles = currentRoles;
    }

    public Map<String, String> getRolesMap() {
        if (this.rolesMap == null) {
            this.rolesMap = new HashMap<String, String>();

            RoleManagerLocal mgr = LookupUtil.getRoleManager();
            PageList<Role> roles = mgr.findRoles(new PageControl());

            for (Role role : roles) {
                this.rolesMap.put(role.getName(), role.getName());
            }
        }

        return this.rolesMap;
    }

    public String submit() {

        System.out.println("In Submit");

        System.out.println("Selected roles:  ");
        for (String role : currentRoles) {
            System.out.println(role);
        }

        // TODO: Customise this generated block

        return "ALERT_NOTIFICATIONS";
    }
}
