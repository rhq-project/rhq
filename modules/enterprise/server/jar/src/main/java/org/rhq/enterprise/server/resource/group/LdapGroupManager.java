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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.system.SystemManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * @author paji
 *
 */
public class LdapGroupManager {
    private static final LdapGroupManager INSTANCE = new LdapGroupManager();
    private static final String BASEDN_DELIMITER = ";";
    private Log log = LogFactory.getLog(LdapGroupManager.class);

    private LdapGroupManager() {
    }

    public static LdapGroupManager getInstance() {
        return INSTANCE;
    }

    public Set<Map<String, String>> findAvailableGroups() {
        SystemManagerLocal manager = LookupUtil.getSystemManager();
        manager.getSystemConfiguration();
        Set<Map<String, String>> ldapSet = new HashSet<Map<String, String>>();
        String[] names = { "bar", "foo" };

        for (String name : names) {
            Map<String, String> group = new HashMap<String, String>();
            group.put("id", name);
            group.put("name", name);
            group.put("description", name);
            ldapSet.add(group);
        }
        return ldapSet;
    }

    public Set<Map<String, String>> findAvailableGroupsFor(String userName) {
        SystemManagerLocal manager = LookupUtil.getSystemManager();
        manager.getSystemConfiguration();
        Set<Map<String, String>> ldapSet = new HashSet<Map<String, String>>();
        String[] names = { "bar", "foo" };

        for (String name : names) {
            Map<String, String> group = new HashMap<String, String>();
            group.put("id", name);
            group.put("name", name);
            group.put("description", name);
            ldapSet.add(group);
        }
        return ldapSet;
    }

    /*
     * 
    {BindDN=uid=shaggy,ou=People, dc=rhndev, dc=redhat, dc=com, 
    java.naming.factory.initial=com.sun.jndi.ldap.LdapCtxFactory, jboss.security.security_domain=JON, 
    LoginProperty=uid, BaseDN=dc=rhndev,dc=redhat,dc=com, java.naming.provider.url=ldap://fjs-0-16.rhndev.redhat.com, 
    java.naming.security.protocol=, BindPW=dog8code}
     */
    protected boolean test() throws Exception {

        // Load our LDAP specific properties
        Properties env = null;// getProperties();

        // Load the BaseDN
        String baseDN = "dc=rhndev,dc=redhat,dc=com";

        // Load the LoginProperty
        String loginProperty = "uid";

        // Load any search filter

        // Find the user that is calling us
        String userName = "sdoo";

        // Load any information we may need to bind
        String bindDN = "uid=shaggy,ou=People, dc=rhndev, dc=redhat, dc=com";
        String bindPW = "dog8code";

        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }
        InitialLdapContext ctx = new InitialLdapContext(env, null);
        SearchControls searchControls = getSearchControls();

        // Add the search filter if specified.  This only allows for a single search filter.. i.e. foo=bar.
        String filter;
        /*            if ((searchFilter != null) && (searchFilter.length() != 0)) {
                    filter = "(&(" + loginProperty + "=" + userName + ")" + "(" + searchFilter + "))";
                } else {
                    filter = "(" + loginProperty + "=" + userName + ")";
                }
                */
        //filter = "(" + loginProperty + "=" + userName + ")";
        filter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=uid=" + userName
            + ",ou=People, dc=rhndev, dc=redhat, dc=com))";

        // Loop through each configured base DN.  It may be useful
        // in the future to allow for a filter to be configured for
        // each BaseDN, but for now the filter will apply to all.
        String[] baseDNs = baseDN.split(BASEDN_DELIMITER);
        log.info(Arrays.asList(baseDNs));
        for (int x = 0; x < baseDNs.length; x++) {
            NamingEnumeration answer = ctx.search(baseDNs[x], filter, searchControls);
            log.info(answer.hasMore());
            while (answer.hasMore()) {
                // We use the first match
                SearchResult si = (SearchResult) answer.next();
                log.info(si);

                /*                    
                                // Construct the UserDN
                                String userDN = si.getName() + "," + baseDNs[x];
                                print (userDN);
                                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
                                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, "dog8code");
                                ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                                ctx.reconnect(null);*/
            }

        }

        // If we try all the BaseDN's and have not found a match, return false
        return false;
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#validatePassword(java.lang.String,java.lang.String)
     */
    protected void buildGroup(Properties options, String userName) {
        // Load our LDAP specific properties
        Properties env = getProperties(options);

        // Load the BaseDN
        String baseDN = (String) options.get(RHQConstants.LDAPBaseDN);
        if (baseDN == null) {
            // If the BaseDN is not specified, log an error and refuse the login attempt
            log.info("BaseDN is not set, refusing login");
        }

        // Load the LoginProperty
        String loginProperty = (String) options.get(RHQConstants.LDAPLoginProperty);
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }

        String groupFilter = (String) options.get("groupFilter");
        String groupMember = (String) options.get("groupMember");

        // Load any information we may need to bind
        String bindDN = (String) options.get("BindDN");
        String bindPW = (String) options.get("BindPW");
        if (bindDN != null) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        try {
            InitialLdapContext ctx = new InitialLdapContext(env, null);
            SearchControls searchControls = getSearchControls();
            String filter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=uid=" + userName
                + ",ou=People, dc=rhndev, dc=redhat, dc=com))";
            // Load any search filter
            String searchFilter = (String) options.get("Filter");
            // Add the search filter if specified.  This only allows for a single search filter.. i.e. foo=bar.
            if ((searchFilter != null) && (searchFilter.length() != 0)) {
                filter = "(&(" + loginProperty + "=" + userName + ")" + "(" + searchFilter + "))";
            } else {
                filter = "(" + loginProperty + "=" + userName + ")";
            }

            // Loop through each configured base DN.  It may be useful
            // in the future to allow for a filter to be configured for
            // each BaseDN, but for now the filter will apply to all.
            String[] baseDNs = baseDN.split(BASEDN_DELIMITER);
            for (int x = 0; x < baseDNs.length; x++) {
                NamingEnumeration answer = ctx.search(baseDNs[x], filter, searchControls);
                if (!answer.hasMore()) {
                    log.debug("User " + userName + " not found for BaseDN " + baseDNs[x]);

                    // Nothing found for this DN, move to the next one if we have one.
                    continue;
                }

                // We use the first match
                SearchResult si = (SearchResult) answer.next();

            }

        } catch (Exception e) {
            log.info("Failed to validate password: " + e.getMessage());
        }
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
        String factoryName = env.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (factoryName == null) {
            env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        }

        // Setup SSL if requested
        String protocol = env.getProperty(Context.SECURITY_PROTOCOL);
        if ((protocol != null) && protocol.equals("ssl")) {
            String ldapSocketFactory = env.getProperty("java.naming.ldap.factory.socket");
            if (ldapSocketFactory == null) {
                env.put("java.naming.ldap.factory.socket", UntrustedSSLSocketFactory.class.getName());
            }
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        // Set the LDAP url
        String providerUrl = env.getProperty(Context.PROVIDER_URL);
        if (providerUrl == null) {
            providerUrl = "ldap://localhost:" + (((protocol != null) && protocol.equals("ssl")) ? "636" : "389");
        }

        env.setProperty(Context.PROVIDER_URL, providerUrl);

        // Follow referrals automatically
        env.setProperty(Context.REFERRAL, "follow");

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
