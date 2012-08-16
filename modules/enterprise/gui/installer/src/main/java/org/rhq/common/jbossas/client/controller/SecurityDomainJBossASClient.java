/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.common.jbossas.client.controller;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * Provides convienence methods associated with security domain management.
 * 
 * @author John Mazzitelli
 */
public class SecurityDomainJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_SECURITY = "security";
    public static final String SECURITY_DOMAIN = "security-domain";
    public static final String CACHE_TYPE = "cache-type";
    public static final String AUTHENTICATION = "authentication";
    public static final String LOGIN_MODULES = "login-modules";
    public static final String CLASSIC = "classic";
    public static final String CODE = "code";
    public static final String FLAG = "flag";
    public static final String MODULE_OPTIONS = "module-options";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public SecurityDomainJBossASClient(ModelControllerClient client) {
        super(client);
    }

    /**
     * Checks to see if there is already a security domain with the given name.
     *
     * @param securityDomainName the name to check
     * @return true if there is a security domain with the given name already in existence
     */
    public boolean isSecurityDomain(String securityDomainName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY);
        String haystack = SECURITY_DOMAIN;
        return null != findNodeInList(addr, haystack, securityDomainName);
    }

    /**
     * Convienence method that builds a request which can create a new security-domain
     * using the SecureIdentity authentication method. This is used when you want
     * to obfuscate a database password in the configuration. 
     *
     * @param securityDomainName the name of the new security domain
     * @param username the username associated with the security domain
     * @param password the value of the password to store in the configuration (e.g. the obfuscated password itself)
     * 
     * @throws Exception if failed to create security domain
     */
    public void createNewSecureIdentitySecurityDomainRequest(String securityDomainName, String username, String password)
        throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        ModelNode addAuthNode = createRequest(ADD, addr.clone().add(AUTHENTICATION, CLASSIC));
        ModelNode loginModulesNode = addAuthNode.get(LOGIN_MODULES);
        ModelNode loginModule = new ModelNode();
        loginModule.get(CODE).set("SecureIdentity");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        // TODO: we really want to use addExpression (e.g. ${rhq.server.database.user-name})
        // for username and password so rhq-server.properties can be used to set these.
        // However, AS7.1 doesn't support this yet - see https://issues.jboss.org/browse/AS7-5177
        moduleOptions.add(USERNAME, username);
        moduleOptions.add(PASSWORD, password);
        loginModulesNode.add(loginModule);

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }
}
