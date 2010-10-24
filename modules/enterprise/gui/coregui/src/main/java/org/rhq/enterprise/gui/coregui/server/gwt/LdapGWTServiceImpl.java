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
package org.rhq.enterprise.gui.coregui.server.gwt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;

import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.LdapGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
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

    @Override
    public Set<Map<String, String>> findAvailableGroups() {

        try {
            Set<Map<String, String>> results = ldapManager.findAvailableGroups();
            return SerialUtility.prepare(results, "findAvailableGroups");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public void addLdapGroupsToRole(int roleId, List<String> groupIds) {
        try {
            ldapManager.addLdapGroupsToRole(subjectManager.getOverlord(), roleId, groupIds);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }

    }

    public void setLdapGroupsForRole(int roleId, List<String> groupIds) {
        try {
            //clean out existing roles as this defines the new list of roles
            PageList<LdapGroup> existing = ldapManager.findLdapGroupsByRole(roleId, PageControl.getUnlimitedInstance());
            Log.trace("Removing " + existing.getTotalSize() + " groups from role '" + roleId + "'.");
            int[] groupIndices = new int[existing.size()];
            int indx = 0;
            for (LdapGroup lg : existing) {
                groupIndices[indx++] = lg.getId();
            }
            Log.trace("Removing " + groupIndices.length + " LDAP Groups." + groupIndices);
            ldapManager.removeLdapGroupsFromRole(subjectManager.getOverlord(), roleId, groupIndices);
            PageList<LdapGroup> nowGroups = ldapManager
                .findLdapGroupsByRole(roleId, PageControl.getUnlimitedInstance());

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
            Log.trace("Adding " + validGroupIds.size() + " ldap groups to role[" + roleId + "].");
            ldapManager.addLdapGroupsToRole(subjectManager.getOverlord(), roleId, groupIds);
            nowGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl.getUnlimitedInstance());
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public PageList<LdapGroup> findLdapGroupsAssignedToRole(int roleId) {
        try {
            PageList<LdapGroup> allAssignedLdapGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl
                .getUnlimitedInstance());
            return SerialUtility.prepare(allAssignedLdapGroups, "findLdapGroupsAssignedToRole");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /**Light call to determine ldap configuration status.
     * 
     */
    @Override
    public Boolean checkLdapConfiguredStatus() {
        Boolean ldapEnabled = false;
        try {
            String provider = systemManager.getSystemConfiguration().getProperty(RHQConstants.JAASProvider);
            ldapEnabled = ((provider != null) && provider.equals(RHQConstants.LDAPJAASProvider));
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return ldapEnabled;
    }

    /**Returns all LDAP details for a given user, using the configured ldap details of server.
     * 
     */
    @Override
    public Map<String, String> getLdapDetailsFor(String user) {
        Map<String, String> ldapDetails = new HashMap<String, String>();
        try {
            ldapDetails = ldapManager.findLdapUserDetails(user);
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
        return ldapDetails;
    }
}