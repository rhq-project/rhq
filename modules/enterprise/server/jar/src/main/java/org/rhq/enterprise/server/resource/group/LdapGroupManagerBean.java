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

package org.rhq.enterprise.server.resource.group;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.server.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.RequiredPermission;
import org.rhq.enterprise.server.exception.LdapCommunicationException;
import org.rhq.enterprise.server.exception.LdapFilterException;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * This bean provides functionality to manipulate the ldap auth/authz functionality.
 * That is, adding/modifying/deleting ldap group/users and their
 * associated subjects and permissions are performed by this manager.
 * @author paji
 * @author Simeon Pinder
 */
@Stateless
public class LdapGroupManagerBean implements LdapGroupManagerLocal {

    private Log log = LogFactory.getLog(LdapGroupManagerBean.class);

    private static final String BASEDN_DELIMITER = ";";

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private SystemManagerLocal systemManager;

    public Set<Map<String, String>> findAvailableGroups() {
        Properties options = systemManager.getSystemConfiguration();
        String groupFilter = (String) options.get(RHQConstants.LDAPGroupFilter);
        String filter = String.format("(%s)", groupFilter);

        return buildGroup(options, filter);
    }

    public Set<String> findAvailableGroupsFor(String userName) {
        Properties options = systemManager.getSystemConfiguration();
        String groupFilter = (String) options.getProperty(RHQConstants.LDAPGroupFilter, "");
        String groupMember = (String) options.getProperty(RHQConstants.LDAPGroupMember, "");
        String userDN = getUserDN(options, userName);
        //TODO: spinder 4/21/10 put in error/debug logging messages for badly formatted filter combinations
        String filter = "";
        //form assumes examples where groupFilter is like 'objecclass=groupOfNames' and groupMember is 'member'
        // to produce ldaf filter like (&(objecclass=groupOfNames)(member=cn=Administrator,ou=People,dc=test,dc=com))
        filter = String.format("(&(%s)(%s=%s))", groupFilter, groupMember, userDN);

        Set<Map<String, String>> matched = buildGroup(options, filter);

        Set<String> ldapSet = new HashSet<String>();
        for (Map<String, String> match : matched) {
            ldapSet.add(match.get("id"));
        }
        return ldapSet;
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#addResourceGroupsToRole(Subject, int, int[])
     */
    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void addLdapGroupsToRole(Subject subject, int roleId, List<String> groupIds) {
        if ((groupIds != null) && (groupIds.size() > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to add resourceGroups to");
            }
            role.getLdapGroups().size(); // load them in

            for (String groupId : groupIds) {
                LdapGroup group = new LdapGroup();
                group.setName(groupId);
                if (role == null) {
                    throw new IllegalArgumentException("Tried to add ldapGroup[" + groupId + "] to role[" + roleId
                        + "], but resourceGroup was not found");
                }
                role.addLdapGroup(group);
            }
        }
    }

    /**
     * @see org.rhq.enterprise.server.authz.RoleManagerLocal#removeLdapGroupsFromRole(Subject, int, int[])
     */

    @RequiredPermission(Permission.MANAGE_SECURITY)
    public void removeLdapGroupsFromRole(Subject subject, int roleId, int[] groupIds) {
        if ((groupIds != null) && (groupIds.length > 0)) {
            Role role = entityManager.find(Role.class, roleId);
            if (role == null) {
                throw new IllegalArgumentException("Could not find role[" + roleId + "] to remove resourceGroups from");
            }
            role.getLdapGroups().size(); // load them in

            for (Integer groupId : groupIds) {
                LdapGroup doomedGroup = entityManager.find(LdapGroup.class, groupId);
                if (doomedGroup == null) {
                    throw new IllegalArgumentException("Tried to remove doomedGroup[" + groupId + "] from role["
                        + roleId + "], but doomedGroup was not found.");
                }
                role.removeLdapGroup(doomedGroup);
            }

            Query purgeQuery = entityManager.createNamedQuery(LdapGroup.DELETE_BY_ID);

            List<Integer> ids = new LinkedList<Integer>();
            for (int i : groupIds) {
                ids.add(i);
            }
            purgeQuery.setParameter("ids", ids);
            purgeQuery.executeUpdate();
        }
    }

    private List<Role> findRolesByLdapGroupNames(List<String> ldapGroupNames) {
        if (ldapGroupNames.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        Query query = entityManager.createNamedQuery(LdapGroup.FIND_BY_ROLES_GROUP_NAMES);
        query.setParameter("names", ldapGroupNames);
        return (List<Role>) query.getResultList();
    }

    public void assignRolesToLdapSubject(int subjectId, List<String> ldapGroupNames) {
        Subject sub = entityManager.find(Subject.class, subjectId);
        List<Role> roles = findRolesByLdapGroupNames(ldapGroupNames);
        sub.getRoles().clear();
        sub.getLdapRoles().clear();
        for (Role role : roles) {
            sub.addRole(role);
            sub.addLdapRole(role);
        }
    }

    public PageList<LdapGroup> findLdapGroupsByRole(int roleId, PageControl pageControl) {
        Role role = entityManager.find(Role.class, roleId);
        if (role == null) {
            throw new IllegalArgumentException("Could not find role[" + roleId + "] to lookup ldap Groups on");
        }
        return new PageList<LdapGroup>(role.getLdapGroups(), role.getLdapGroups().size(), pageControl);
    }

    public PageList<LdapGroup> findLdapGroups(PageControl pc) {

        pc.initDefaultOrderingField("g.name");

        String queryName = LdapGroup.QUERY_FIND_ALL;
        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        long count = (Long) queryCount.getSingleResult();
        //        List<Role> roles = query.getResultList();
        List<LdapGroup> groups = query.getResultList();
        return new PageList<LdapGroup>(groups, (int) count, pc);
    }

    private String getUserDN(Properties options, String userName) {
        // Load our LDAP specific properties
        Properties env = getProperties(options);

        // Load the BaseDN
        String baseDN = (String) options.get(RHQConstants.LDAPBaseDN);

        // Load the LoginProperty
        String loginProperty = (String) options.get(RHQConstants.LDAPLoginProperty);
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }
        // Load any information we may need to bind
        String bindDN = (String) options.get(RHQConstants.LDAPBindDN);
        String bindPW = (String) options.get(RHQConstants.LDAPBindPW);

        // Load any search filter
        String searchFilter = (String) options.get(RHQConstants.LDAPFilter);
        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        try {
            InitialLdapContext ctx = new InitialLdapContext(env, null);
            SearchControls searchControls = getSearchControls();

            // Add the search filter if specified.  This only allows for a single search filter.. i.e. foo=bar.
            String filter;
            if ((searchFilter != null) && (searchFilter.length() != 0)) {
                filter = "(&(" + loginProperty + "=" + userName + ")" + "(" + searchFilter + "))";
            } else {
                filter = "(" + loginProperty + "=" + userName + ")";
            }

            log.debug("Using LDAP filter=" + filter);

            // Loop through each configured base DN.  It may be useful
            // in the future to allow for a filter to be configured for
            // each BaseDN, but for now the filter will apply to all.
            String[] baseDNs = baseDN.split(BASEDN_DELIMITER);
            for (int x = 0; x < baseDNs.length; x++) {
                NamingEnumeration answer = ctx.search(baseDNs[x], filter, searchControls);
                if (!answer.hasMoreElements()) { //BZ:582471- ldap api bug change
                    log.debug("User " + userName + " not found for BaseDN " + baseDNs[x]);
                    // Nothing found for this DN, move to the next one if we have one.
                    continue;
                }

                // We use the first match
                SearchResult si = (SearchResult) answer.next();

                // Construct the UserDN
                String userDN = si.getName() + "," + baseDNs[x];
                return userDN;
            }

            // If we try all the BaseDN's and have not found a match, return false
            return "";
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @throws NamingException
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#validatePassword(java.lang.String,java.lang.String)
     */
    protected Set<Map<String, String>> buildGroup(Properties options, String filter) {
        Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
        // Load our LDAP specific properties
        Properties env = getProperties(options);

        // Load the BaseDN
        String baseDN = (String) options.get(RHQConstants.LDAPBaseDN);

        // Load the LoginProperty
        String loginProperty = (String) options.get(RHQConstants.LDAPLoginProperty);
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }
        // Load any information we may need to bind
        String bindDN = (String) options.get(RHQConstants.LDAPBindDN);
        String bindPW = (String) options.get(RHQConstants.LDAPBindPW);
        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }
        try {
            InitialLdapContext ctx = new InitialLdapContext(env, null);
            SearchControls searchControls = getSearchControls();
            /*String filter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=uid=" + userName
                + ",ou=People, dc=rhndev, dc=redhat, dc=com))";*/

            // Loop through each configured base DN.  It may be useful
            // in the future to allow for a filter to be configured for
            // each BaseDN, but for now the filter will apply to all.
            String[] baseDNs = baseDN.split(BASEDN_DELIMITER);

            for (int x = 0; x < baseDNs.length; x++) {
                NamingEnumeration answer = ctx.search(baseDNs[x], filter, searchControls);
                boolean ldapApiEnumerationBugEncountered = false;
                while ((!ldapApiEnumerationBugEncountered) && answer.hasMoreElements()) {//BZ:582471- ldap api bug change
                    // We use the first match
                    SearchResult si = null;
                    try {
                        si = (SearchResult) answer.next();
                    } catch (NullPointerException npe) {
                        ldapApiEnumerationBugEncountered = true;
                        break;
                    }
                    Map<String, String> entry = new HashMap<String, String>();
                    String name = (String) si.getAttributes().get("cn").get();
                    name = name.trim();
                    Attribute desc = si.getAttributes().get("description");
                    String description = desc != null ? (String) desc.get() : "";
                    description = description.trim();
                    entry.put("id", name);
                    entry.put("name", name);
                    entry.put("description", description);
                    ret.add(entry);
                }
            }
        } catch (NamingException e) {
            if (e instanceof InvalidSearchFilterException) {
                InvalidSearchFilterException fException = (InvalidSearchFilterException) e;
                String message = "The ldap group filter defined is invalid ";
                log.error(message, fException);
                throw new LdapFilterException(message + " " + fException.getMessage());
            }
            //TODO: check for ldap connection/unavailable/etc. exceptions.
            else {
                log.error("LDAP communication error: " + e.getMessage(), e);
                throw new LdapCommunicationException(e);
            }
        }

        return ret;
    }

    /**
     * Load a default set of properties to use when connecting to the LDAP server. If basic authentication is needed,
     * the caller must set Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS and Context.SECURITY_AUTHENTICATION
     * appropriately.
     *
     * @return properties that are to be used when connecting to LDAP server
     */
    private Properties getProperties(Properties options) {
        Properties env = new Properties(options);
        // Set our default factory name if one is not given
        String factoryName = env.getProperty(RHQConstants.LDAPFactory);
        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, factoryName);

        // Setup SSL if requested
        String protocol = env.getProperty(RHQConstants.LDAPProtocol);
        if ((protocol != null) && protocol.equals("ssl")) {
            String ldapSocketFactory = env.getProperty("java.naming.ldap.factory.socket");
            if (ldapSocketFactory == null) {
                env.put("java.naming.ldap.factory.socket", UntrustedSSLSocketFactory.class.getName());
            }
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        // Set the LDAP url
        String providerUrl = env.getProperty(RHQConstants.LDAPUrl);
        if (providerUrl == null) {
            providerUrl = "ldap://localhost:" + (((protocol != null) && protocol.equals("ssl")) ? "636" : "389");
        }

        env.setProperty(Context.PROVIDER_URL, providerUrl);

        // Follow referrals automatically
        env.setProperty(Context.REFERRAL, "ignore"); //BZ:582471- active directory query change

        return env;
    }

    /**
     * A simple method to construct a SearchControls object for use when doing LDAP searches. All of the defaults are
     * used, with the exception of the scope, which is set to SUBTREE rather than the default of ONE_LEVEL
     *
     * @return controls what is searched in LDAP
     */
    private SearchControls getSearchControls() {
        // Set the scope to subtree, default is one-level
        int scope = SearchControls.SUBTREE_SCOPE;

        // No limit on the time waiting for a response
        int timeLimit = 0;

        // No limit on the number of entries returned
        long countLimit = 0;

        // Attributes to return.
        String[] returnedAttributes = null;

        // Don't return the object
        boolean returnObject = false;

        // No dereferencing during the search
        boolean deference = false;

        SearchControls constraints = new SearchControls(scope, countLimit, timeLimit, returnedAttributes, returnObject,
            deference);
        return constraints;
    }
}
