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
import java.util.List;
import java.util.Map;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSender;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing Bean for the roles sender alert sender plugin custom UI
 * @author Joseph Marques
 */
public class RolesBackingBean extends CustomAlertSenderBackingBean {

    private Map<String, String> available;
    private List<String> currentRoles;
    private static final String ROLE_ID = "roleId";

    @Override
    public void loadView() {
        // get available/all subjects
        List<Role> allRoles = LookupUtil.getRoleManager().findRoles(new PageControl());
        available = new HashMap<String, String>();
        for (Role role : allRoles) {
            String roleId = String.valueOf(role.getId());
            available.put(role.getName(), roleId);
        }

        // get current subjects
        String subjectString = alertParameters.getSimpleValue(ROLE_ID, "");
        currentRoles = AlertSender.unfence(subjectString, String.class);
    }

    @Override
    public void saveView() {
        String roleIds = AlertSender.fence(currentRoles);

        PropertySimple p = alertParameters.getSimple(ROLE_ID);
        if (p == null) {
            p = new PropertySimple(ROLE_ID, roleIds);
            alertParameters.put(p);
        } else {
            p.setStringValue(roleIds);
        }

        alertParameters = persistConfiguration(alertParameters);
    }

    public List<String> getCurrentRoles() {
        return currentRoles;
    }

    public void setCurrentRoles(List<String> currentRoles) {
        this.currentRoles = currentRoles;
    }

    public Map<String, String> getAvailableRolesMap() {
        return available;
    }
}
