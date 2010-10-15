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
import org.rhq.core.domain.criteria.SubjectCriteria;
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
    public Subject checkSubjectForLdapAuth(Subject currentSubject, String user, String password) {
        try {
            Subject newSubject = null;
            Log.trace("Subject being checked for ldapAuthentication is :" + currentSubject);

            boolean needsRegistrationOrCaseIncorrectOnAccountName = false;

            //null checks.
            if ((currentSubject != null) && (user != null) && (password != null)) {
                if (currentSubject.getId() == 0) {
                    // Subject with a ID of 0 means the subject wasn't in the database but the login succeeded.
                    // This means the login method detected the LDAP authenticated user and gave us a dummy subject.
                    // Set the needs-registration flag so we can eventually steer the user to the LDAP registration workflow.
                    needsRegistrationOrCaseIncorrectOnAccountName = true;
                }

                Log.trace("Subject has id of :" + currentSubject.getId() + "and requires Registration:"
                    + needsRegistrationOrCaseIncorrectOnAccountName);

                // figure out if the user has a principal
                String provider = LookupUtil.getSystemManager().getSystemConfiguration().getProperty(
                    RHQConstants.JAASProvider);
                boolean ldapEnabled = ((provider != null) && provider.equals(RHQConstants.LDAPJAASProvider));

                Log.trace("LDAP Authentication has been enabled :" + ldapEnabled);
                boolean hasPrincipal = false;

                if (ldapEnabled) {
                    // when we allow for LDAP authentication, we may still have users logging in with JDBC.
                    // The only way we can distinguish these users is by checking to see if they have an
                    // entry in the principals table.  If they do, then we know we use JDBC authentication
                    // for that user.  If they do not, then we must be using LDAP to authenticate that user.
                    //                    hasPrincipal = subjectManager.isUserWithPrincipal(currentSubject.getName());
                    hasPrincipal = subjectManager.isUserWithPrincipal(user);
                    Log.trace("Subject '" + user + "' hasPrincipal :" + hasPrincipal);

                    if (!hasPrincipal && needsRegistrationOrCaseIncorrectOnAccountName) {
                        //for the case when they're already registered but entering a case sensitive different name
                        //BZ-586435: insert case insensitivity for usernames with ldap auth
                        // locate first matching subject and attach.
                        SubjectCriteria subjectCriteria = new SubjectCriteria();
                        subjectCriteria.setCaseSensitive(false);
                        subjectCriteria.setStrict(true);
                        subjectCriteria.addFilterName(user);
                        subjectCriteria.fetchRoles(true);
                        subjectCriteria.fetchConfiguration(true);
                        PageList<Subject> subjectsLocated = LookupUtil.getSubjectManager().findSubjectsByCriteria(
                            LookupUtil.getSubjectManager().getOverlord(), subjectCriteria);
                        Log.trace("Subjects located with name '" + user + "' and found:" + subjectsLocated.size());

                        //if subject variants located then take the first one with a principal otherwise do nothing
                        //To defend against the case where they create an account with the same name but not 
                        //case as an rhq sysadmin or higher perms, then make them relogin with same creds entered.
                        if (!subjectsLocated.isEmpty()) {//then case insensitive username matches found. Try to use instead.
                            Subject ldapSubject = subjectsLocated.get(0);
                            String msg = "Located existing ldap account with different case for ["
                                + ldapSubject.getName() + "]. "
                                + "Attempting to authenticate with that account instead.";
                            Log.info(msg);
                            Log.trace("Attempting to log back in with credentials passed in.");
                            newSubject = subjectManager.login(user, password);
                            Log.trace("Logged in as [" + ldapSubject.getName() + "] with session id ["
                                + newSubject.getSessionId() + "]");
                            needsRegistrationOrCaseIncorrectOnAccountName = false;
                        }
                    }

                } else {
                    // with regular JDBC authentication, we are guaranteed to have a principal
                    hasPrincipal = true;
                }
            } else {
                Log.debug("The Subject and user/password cannot be null to proceed.");
            }
            return SerialUtility.prepare(newSubject, "checkSubjectForLdapAuth");
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