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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.annotations.Create;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.util.PageControl;
import org.rhq.enterprise.server.authz.RoleManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing Bean for the roles sender alert sender plugin custom UI
 * @author Heiko W. Rupp
 */
@SuppressWarnings("unused")
public class RolesBackingBean extends CustomAlertSenderBackingBean {

    private final Log log = LogFactory.getLog(RolesBackingBean.class);

    private List<Role> allRoles;

    private Map<String, String> rolesMap;
    private List<String> currentRoles;
    private List<String> rolesToRemove;
    private static final String ROLE_ID = "roleId";
    private boolean isDebug = false;

    @Create
    public void init() {

        if (log.isDebugEnabled())
            isDebug = true;

        getAllRoles();

        getSelectableRolesMap();
        fillRolesFromAlertParameters();
    }

    private void getAllRoles() {
        RoleManagerLocal mgr = LookupUtil.getRoleManager();
        allRoles = mgr.findRoles(new PageControl());
    }

    private void fillRolesFromAlertParameters() {
        String rolesString = alertParameters.getSimpleValue(ROLE_ID,"");
        String[] roles = rolesString.split(",");
        if (roles.length==0)
            return;

        if (currentRoles==null)
            currentRoles = new ArrayList<String>();
        currentRoles.addAll(Arrays.asList(roles));
    }

    public List<String> getCurrentRoles() {
        if (currentRoles==null)
            fillRolesFromAlertParameters();
        return currentRoles;
    }

    public void setCurrentRoles(List<String> currentRoles) {
        this.currentRoles = currentRoles;
    }

    public List<String> getRolesToRemove() {
        return rolesToRemove;
    }

    public void setRolesToRemove(List<String> rolesToRemove) {
        this.rolesToRemove = rolesToRemove;
    }

    public Map<String, String> getSelectableRolesMap() {

        if (rolesMap == null) {
            rolesMap = new HashMap<String, String>();

            if (allRoles==null)
                getAllRoles();

            if (currentRoles==null)
                fillRolesFromAlertParameters();

            for (Role role : allRoles) {
                String roleId = String.valueOf(role.getId());
                if (currentRoles==null || !currentRoles.contains(roleId))
                    rolesMap.put(role.getName(), roleId);
            }
        }
        return this.rolesMap;
    }

    public Map<String, String> getCurrentRolesMap() {

        Map<String,String> ret = new HashMap<String, String>();
        if (currentRoles==null)
            return ret;

        for (Role role:allRoles) {
            String roleId = String.valueOf(role.getId());
            if (currentRoles.contains(roleId))
                ret.put(role.getName(), roleId);
        }
        return ret;

    }

    public String addRoles() {

        if (isDebug)
            log.debug("Selected roles:  " + currentRoles );
        if (currentRoles.isEmpty())
            return "ALERT_NOTIFICATION";

        String roles="";
        for (String role : currentRoles) {
            roles += role;
            roles += ",";
        }
        if (roles.endsWith(","))
                roles = roles.substring(0,roles.length()-1);

        PropertySimple p = alertParameters.getSimple(ROLE_ID);
        if (p==null) {
                p = new PropertySimple(ROLE_ID,roles);
                alertParameters.put(p);
        }
        else
            p.setStringValue(roles);

        alertParameters = persistConfiguration(alertParameters);

        fillRolesFromAlertParameters();

        return "ALERT_NOTIFICATIONS";
    }

    public String removeRoles() {
        if (isDebug)
            log.debug("In remove roles, " + rolesToRemove);

        String roles ="";
        List<String> resulting = new ArrayList<String>(currentRoles);
        resulting.removeAll(rolesToRemove);

        for (String subject : resulting) {
            roles += subject;
            roles += ",";
        }

        if (roles.endsWith(","))
            roles = roles.substring(0, roles.length()-1);

        PropertySimple p = alertParameters.getSimple(ROLE_ID);
        if (p==null) {
            if (!resulting.isEmpty()) {
                p = new PropertySimple(ROLE_ID, roles);
                alertParameters.put(p);
            }
        }
        else
            p.setStringValue(roles);

        alertParameters = persistConfiguration(alertParameters);

        currentRoles = resulting;

        fillRolesFromAlertParameters();

        return "ALERT_NOTIFICATIONS";
    }
}
