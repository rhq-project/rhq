/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.server.core.jaas;

import java.security.acl.Group;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;

import org.rhq.core.util.obfuscation.Obfuscator;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * A login module for authenticating against an LDAP directory server using JNDI, based on configuration properties.<br/
 * LDAP module options:
 *
 * <pre>
 * java.naming.factory.initial
 *   This should be set to the fully qualified class name of the initial
 *   context factory.  Defaults to com.sun.jndi.ldap.LdapCtxFactory
 *
 * java.naming.provider.url
 *   The full url to the LDAP server.  Defaults to ldap://localhost.  Port
 *   389 is used unless java.naming.security.protocol is set to ssl.  In
 *   that case port 636 is used.
 *
 * java.naming.security.protocol
 *   Set this to 'ssl' to enable secure communications.  If the
 *   java.naming.provider.url is not set, it will be initialized with
 *   port 636.
 *
 * LoginProperty
 *   The LDAP property that contains the user name.  Defaults to cn.  If
 *   multiple matches are found, the first entry found is used.
 *
 * Filter
 *   Any additional filters to apply when doing the LDAP search.  Useful
 *   if you only want to authenticate against a group of users that have
 *   a given LDAP property set.  (CAMUser=true for example)
 *
 * BaseDN
 *   The base of the LDAP tree we are authenticating against.  For example:
 *   o=Covalent Technologies,c=US.  Multiple LDAP bases can be used by
 *   separating each DN by ';'
 *
 * BindDN
 *   The BindDN to use if the LDAP server does not support anonymous searches.
 *
 * BindPW
 *   The password to use if the LDAP server does not support anonymous searches
 * </pre>
 */

public class KeycloakLoginModule extends UsernamePasswordLoginModule {
    private Log log = LogFactory.getLog(KeycloakLoginModule.class);

    LdapGroupManagerLocal ldapManager = LookupUtil.getLdapGroupManager();

    // The delimiter to use when specifying multiple BaseDN's.
    private static final String BASEDN_DELIMITER = ";";

    /**
     * Creates a new {@link KeycloakLoginModule} object.
     */
    public KeycloakLoginModule() {
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#getUsersPassword()
     */
    protected String getUsersPassword() throws LoginException {
        return "";
    }

    /**
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    protected Group[] getRoleSets() throws LoginException {
        SimpleGroup roles = new SimpleGroup("Roles");

        //roles.addMember( new SimplePrincipal( "some user" ) );
        Group[] roleSets = { roles };
        return roleSets;
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#validatePassword(java.lang.String,java.lang.String)
     */
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        // Load our LDAP specific properties
        Properties env = getProperties();

        // Load the BaseDN
        String baseDN = (String) options.get("BaseDN");
        if (baseDN == null) {
            // If the BaseDN is not specified, log an error and refuse the login attempt
            log.info("BaseDN is not set, refusing login");
            return false;
        }

        // Many LDAP servers allow bind's with an emtpy password. We will deny all requests with empty passwords
        if ((inputPassword == null) || inputPassword.equals("")) {
            log.debug("Empty password, refusing login");
            return false;
        }

        // Load the LoginProperty
        String loginProperty = (String) options.get("LoginProperty");
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }

        // Load any search filter
        String searchFilter = (String) options.get("Filter");

        // Find the user that is calling us
        String userName = getUsername();

        // Load any information we may need to bind
        String bindDN = (String) options.get("BindDN");
        String bindPW = (String) options.get("BindPW");
        try {
            bindPW = Obfuscator.decode(bindPW);
        } catch (Exception e) {
            log.debug("Failed to decode bindPW, validating using undecoded value [" + bindPW + "]", e);
        }
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
                boolean ldapApiNpeFound = false;
                if (!answer.hasMoreElements()) {//BZ:582471- ldap api bug
                    log.debug("User " + userName + " not found for BaseDN " + baseDNs[x]);

                    // Nothing found for this DN, move to the next one if we have one.
                    continue;
                }

                // We use the first match
                SearchResult si = (SearchResult) answer.next();

                // Construct the UserDN
                String userDN = null;

                try {
                    userDN = si.getNameInNamespace();
                } catch (UnsupportedOperationException use) {
                    userDN = new CompositeName(si.getName()).get(0);
                    if (si.isRelative()) {
                        userDN += "," + baseDNs[x];
                    }
                }

                log.debug("Using LDAP userDN=" + userDN);

                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, inputPassword);
                ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");

                //if successful then verified that user and pw are valid ldap credentials
                ctx.reconnect(null);

                return true;
            }

            // If we try all the BaseDN's and have not found a match, return false
            return false;
        } catch (Exception e) {
            log.info("Failed to validate password for [" + userName + "]: " + e.getMessage());
            return false;
        }
    }

    /**
     * Load a default set of properties to use when connecting to the LDAP server. If basic authentication is needed,
     * the caller must set Context.SECURITY_PRINCIPAL, Context.SECURITY_CREDENTIALS and Context.SECURITY_AUTHENTICATION
     * appropriately.
     *
     * @return properties that are to be used when connecting to LDAP server
     */
    private Properties getProperties() {
        Properties env = new Properties();

        // Map all user options into into our environment
        Iterator iter = options.entrySet().iterator();
        while (iter.hasNext()) {
            Entry entry = (Entry) iter.next();
            if ((entry.getKey() != null) && (entry.getValue() != null)) {
                env.put(entry.getKey(), entry.getValue());
            }
        }

        // Set our default factory name if one is not given
        String factoryName = env.getProperty(Context.INITIAL_CONTEXT_FACTORY);
        if (factoryName == null) {
            env.setProperty(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        }

        // Setup SSL if requested
        String protocol = env.getProperty(Context.SECURITY_PROTOCOL);
        if ("ssl".equals(protocol)) {
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
        env.setProperty(Context.REFERRAL, "ignore");//BZ:582471- active directory query change

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