/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.coregui.server.gwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.gwt.LdapGWTService;
import org.rhq.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Simeon Pinder
 */
public class LdapGWTServiceImpl extends AbstractGWTServiceImpl implements LdapGWTService {

    private static final long serialVersionUID = 1L;

    private LdapGroupManagerLocal ldapManager = LookupUtil.getLdapGroupManager();
    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
    private SystemManagerLocal systemManager = LookupUtil.getSystemManager();
    private AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();

    private final Log log = LogFactory.getLog(LdapGWTServiceImpl.class);

    @Override
    public Set<Map<String, String>> findAvailableGroups() throws RuntimeException {
        try {
            //add permissions check
            Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(getSessionSubject());
            Boolean accessGranted = globalPermissions.contains(Permission.MANAGE_SECURITY);

            Set<Map<String, String>> results = null;
            if (accessGranted) {
                results = ldapManager.findAvailableGroups();
            } else {
                String message = "User '" + getSessionSubject().getName()
                    + "' does not have sufficient permissions to query available LDAP groups.";
                log.debug(message);
                throw new PermissionException(message);
            }
            return SerialUtility.prepare(results, "findAvailableGroups");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Set<Map<String, String>> findAvailableGroupsStatus() throws RuntimeException {
        try {
            //add permissions check
            Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(getSessionSubject());
            Boolean accessGranted = globalPermissions.contains(Permission.MANAGE_SECURITY);

            Set<Map<String, String>> results = null;
            if (accessGranted) {
                results = ldapManager.findAvailableGroupsStatus();
            } else {
                String message = "User '" + getSessionSubject().getName()
                    + "' does not have sufficient permissions to query the status of available LDAP groups request.";
                log.debug(message);
                throw new PermissionException(message);
            }
            return SerialUtility.prepare(results, "findAvailableGroups");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public void setLdapGroupsForRole(int roleId, List<String> groupIds) throws RuntimeException {
        try {
            //add permissions check
            Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(getSessionSubject());
            Boolean accessGranted = globalPermissions.contains(Permission.MANAGE_SECURITY);

            if (accessGranted) {
                //clean out existing roles as this defines the new list of roles
                PageList<LdapGroup> existing = ldapManager.findLdapGroupsByRole(roleId, PageControl
                    .getUnlimitedInstance());
                log.trace("Removing " + existing.getTotalSize() + " groups from role '" + roleId + "'.");
                int[] groupIndices = new int[existing.size()];
                int indx = 0;
                for (LdapGroup lg : existing) {
                    groupIndices[indx++] = lg.getId();
                }
                log.trace("Removing " + groupIndices.length + " LDAP Groups." + groupIndices);
                ldapManager.removeLdapGroupsFromRole(subjectManager.getOverlord(), roleId, groupIndices);
                PageList<LdapGroup> nowGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl
                    .getUnlimitedInstance());

                //from among all available groups, if group name matches then add it to the list.
                ArrayList<String> validGroupIds = new ArrayList<String>();
                Set<Map<String, String>> allAvailableLdapGroups = ldapManager.findAvailableGroups();
                for (String group : groupIds) {
                    for (Map<String, String> map : allAvailableLdapGroups) {
                        if (map.get("name").equals(group)) {
                            validGroupIds.add(group);
                        }
                    }
                }
                log.trace("Adding " + validGroupIds.size() + " ldap groups to role[" + roleId + "].");
                ldapManager.addLdapGroupsToRole(subjectManager.getOverlord(), roleId, groupIds);
                nowGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl.getUnlimitedInstance());
            } else {
                String message = "User '" + getSessionSubject().getName()
                    + "' does not have sufficient permissions to modify LDAP group assignments for roles.";
                log.debug(message);
                throw new PermissionException(message);
            }

        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    public PageList<LdapGroup> findLdapGroupsAssignedToRole(int roleId) throws RuntimeException {
        try {
            //add permissions check
            Set<Permission> globalPermissions = authorizationManager.getExplicitGlobalPermissions(getSessionSubject());
            Boolean accessGranted = globalPermissions.contains(Permission.MANAGE_SECURITY);

            PageList<LdapGroup> allAssignedLdapGroups = null;
            if (accessGranted) {
                allAssignedLdapGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl.getUnlimitedInstance());
            } else {
                String message = "User '" + getSessionSubject().getName()
                    + "' does not have permissions to query LDAP group by role.";
                log.debug(message);
                throw new PermissionException(message);
            }
            return SerialUtility.prepare(allAssignedLdapGroups, "findLdapGroupsAssignedToRole");
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    /**
     * Returns true if LDAP authentication is enabled, or false otherwise.
     */
    public Boolean checkLdapConfiguredStatus() throws RuntimeException {
        try {
            SystemSettings systemSettings = systemManager.getUnmaskedSystemSettings(true);
            String value = systemSettings.get(SystemSetting.LDAP_BASED_JAAS_PROVIDER);
            boolean result = (value != null) ? Boolean.valueOf(value) : false;
            return result;
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    /**
     * Returns all LDAP details for a given user, using the configured ldap details of server.
     */
    public Map<String, String> getLdapDetailsFor(String user) throws RuntimeException {
        try {
            return ldapManager.findLdapUserDetails(user);
        } catch (Throwable t) {
            throw getExceptionToThrowToClient(t);
        }
    }

    @Override
    public Boolean checkLdapServerRequiresAttention() throws RuntimeException {
        boolean requiresAttention = false;
        if (checkLdapConfiguredStatus()) {//ldap configured
            try {
                requiresAttention = ldapManager.ldapServerRequiresAttention();
            } catch (Throwable t) {
                throw getExceptionToThrowToClient(t);
            }
        }
        return Boolean.valueOf(requiresAttention);
    }
}
