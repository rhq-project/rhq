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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.allen_sauer.gwt.log.client.Log;

import org.rhq.core.domain.auth.Subject;
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

    public void updateLdapGroupAssignmentsForSubject(Subject subject) {
        try {
            //BZ-580127: only do group authz check if one or both of group filter fields is set
            //      Properties options = systemManager.getSystemConfiguration();
            String groupFilter = LookupUtil.getSystemManager().getSystemConfiguration().getProperty(
                RHQConstants.LDAPGroupFilter, "");
            String groupMember = LookupUtil.getSystemManager().getSystemConfiguration().getProperty(
                RHQConstants.LDAPGroupMember, "");
            if ((groupFilter.trim().length() > 0) || (groupMember.trim().length() > 0)) {
                String provider = LookupUtil.getSystemManager().getSystemConfiguration().getProperty(
                    RHQConstants.JAASProvider);
                if (RHQConstants.LDAPJAASProvider.equals(provider)) {
                    List<String> groupNames = new ArrayList<String>(ldapManager.findAvailableGroupsFor(subject
                        .getName()));
                    ldapManager.assignRolesToLdapSubject(subject.getId(), groupNames);
                }
            }
            //          try { //defend against ldap communication runtime difficulties.
            //          } catch (EJBException ejx) {
            //              //this is the exception type thrown now that we use SLSB.Local methods
            //              // mine out other exceptions
            //              Exception cause = ejx.getCausedByException();
            //              if (cause == null) {
            //                  ActionMessages actionMessages = new ActionMessages();
            //                  actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("errors.cam.general"));
            //                  saveErrors(request, actionMessages);
            //              } else {
            //                  if (cause instanceof LdapFilterException) {
            //                      ActionMessages actionMessages = new ActionMessages();
            //                      actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
            //                          "admin.role.LdapGroupFilterMessage"));
            //                      saveErrors(request, actionMessages);
            //                  } else if (cause instanceof LdapCommunicationException) {
            //                      ActionMessages actionMessages = new ActionMessages();
            //                      SystemManagerLocal manager = LookupUtil.getSystemManager();
            //                      options = manager.getSystemConfiguration();
            //                      String providerUrl = options.getProperty(RHQConstants.LDAPUrl, "(unavailable)");
            //                      actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
            //                          "admin.role.LdapCommunicationMessage", providerUrl));
            //                      saveErrors(request, actionMessages);
            //                  }
            //              }
            //          } catch (LdapFilterException lce) {
            //              ActionMessages actionMessages = new ActionMessages();
            //              actionMessages.add(ActionMessages.GLOBAL_MESSAGE,
            //                  new ActionMessage("admin.role.LdapGroupFilterMessage"));
            //              saveErrors(request, actionMessages);
            //          } catch (LdapCommunicationException lce) {
            //              ActionMessages actionMessages = new ActionMessages();
            //              String providerUrl = options.getProperty(RHQConstants.LDAPUrl, "(unavailable)");
            //              actionMessages.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
            //                  "admin.role.LdapCommunicationMessage", providerUrl));
            //              saveErrors(request, actionMessages);
            //          }
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    @Override
    public Set<Map<String, String>> findLdapGroupsAssignedToRole(int roleId) {
        try {
            PageList<LdapGroup> allAssignedLdapGroups = ldapManager.findLdapGroupsByRole(roleId, PageControl
                .getUnlimitedInstance());
            Set<Map<String, String>> ldapGroups = new HashSet<Map<String, String>>();

            for (LdapGroup group : allAssignedLdapGroups) {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("name", group.getName());
                map.put("id", group.getName());
                map.put("description", group.getDescription());
                ldapGroups.add(map);
            }

            return SerialUtility.prepare(ldapGroups, "findLdapGroupsAssignedToRole");
        } catch (Exception e) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(e));
        }
    }

    /** Does a series of LDAP checks and for case insensitive ldap matching accounts will return new Subject with session id. 
     *  i) needs registration(user exists in ldap but not yet in RHQ)
     *  ii) if LDAP authentication is enabled. All authentication is piped through this method.
     *    
     * 
     */
    @Override
    public Subject processSubjectForLdap(Subject currentSubject, String password, boolean ldapRegistration) {
        try {
            currentSubject = subjectManager.processSubjectForLdap(currentSubject, password, ldapRegistration);

            return SerialUtility.prepare(currentSubject, "processSubjectForLdap");
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