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

import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.security.SimpleGroup;
import org.jboss.security.auth.spi.UsernamePasswordLoginModule;
import org.keycloak.representations.AccessTokenResponse;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.enterprise.server.resource.group.LdapGroupManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * The login module for authN with Keycloak SSO server.
 *
 * @author Jirka Kremser
 */
public class KeycloakLoginModule extends UsernamePasswordLoginModule {
    private Log log = LogFactory.getLog(KeycloakLoginModule.class);

    LdapGroupManagerLocal ldapManager = LookupUtil.getLdapGroupManager();

    /**
     * Creates a new {@link KeycloakLoginModule} object.
     */
    public KeycloakLoginModule() {
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#getUsersPassword()
     */
    @Override
    protected String getUsersPassword() throws LoginException {
        return "";
    }

    /**
     * @see org.jboss.security.auth.spi.AbstractServerLoginModule#getRoleSets()
     */
    @Override
    protected Group[] getRoleSets() throws LoginException {
        SimpleGroup roles = new SimpleGroup("Roles");

        //roles.addMember( new SimplePrincipal( "some user" ) );
        Group[] roleSets = { roles };
        return roleSets;
    }

    /**
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#validatePassword(java.lang.String,java.lang.String)
     */
    @Override
    protected boolean validatePassword(String inputPassword, String expectedPassword) {
        // Load our LDAP specific properties
        Properties env = getProperties();
        
        String keycloakServerUrl = env.getProperty(SystemSetting.KEYCLOAK_URL.getInternalName());

        // Many LDAP servers allow bind's with an emtpy password. We will deny all requests with empty passwords
        if ((inputPassword == null) || inputPassword.equals("")) {
            if (log.isDebugEnabled()) {
                log.debug("Empty password, refusing login");
            }
            return false;
        }

        // Find the user that is calling us
        String username = getUsername();
        
        try {
            AccessTokenResponse response = KeycloakLoginUtils.getToken(username, inputPassword, keycloakServerUrl);
            return true;
        } catch (Exception e) {
            log.info("Failed to validate password for [" + username + "]: " + e.getMessage());
            return false;
        }
    }


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

        return env;
    }
}