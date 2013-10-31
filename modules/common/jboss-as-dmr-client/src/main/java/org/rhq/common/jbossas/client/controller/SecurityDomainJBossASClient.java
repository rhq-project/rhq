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

import java.util.List;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Provides convenience methods associated with security domain management.
 *
 * @author John Mazzitelli
 */
public class SecurityDomainJBossASClient extends JBossASClient {

    public static final String SUBSYSTEM_SECURITY = "security";
    public static final String SECURITY_DOMAIN = "security-domain";
    public static final String CACHE_TYPE = "cache-type";
    public static final String AUTHENTICATION = "authentication";
    public static final String LOGIN_MODULE  = "login-module";
    public static final String LOGIN_MODULES = "login-modules";
    public static final String CLASSIC = "classic";
    public static final String CODE = "code";
    public static final String FLAG = "flag";
    public static final String MODULE_OPTIONS = "module-options";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DS_JNDI_NAME = "dsJndiName";
    public static final String PRINCIPALS_QUERY = "principalsQuery";
    public static final String ROLES_QUERY = "rolesQuery";
    public static final String HASH_ALGORITHM = "hashAlgorithm";
    public static final String HASH_ENCODING = "hashEncoding";

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
     * Create a new security domain using the SecureIdentity authentication method.
     * This is used when you want to obfuscate a database password in the configuration.
     *
     * This is the version for as7.2+ (e.g. eap 6.1)
     *
     * @param securityDomainName the name of the new security domain
     * @param username the username associated with the security domain
     * @param password the value of the password to store in the configuration (e.g. the obfuscated password itself)
     *
     * @throws Exception if failed to create security domain
     */
    public void createNewSecureIdentitySecurityDomain72(String securityDomainName, String username, String password)
        throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        Address authAddr = addr.clone().add(AUTHENTICATION, CLASSIC);
        ModelNode addAuthNode = createRequest(ADD, authAddr);

        Address loginAddr = authAddr.clone().add("login-module","SecureIdentity");
        ModelNode loginModule = createRequest(ADD,loginAddr);

        loginModule.get(CODE).set("SecureIdentity");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        addPossibleExpression(moduleOptions, USERNAME, username);
        addPossibleExpression(moduleOptions, PASSWORD, password);

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode, loginModule);

        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Given the name of an existing security domain that uses the SecureIdentity authentication method,
     * this updates that domain with the new credentials. Use this to change credentials if you don't
     * want to use expressions as the username or password entry (in some cases you can't, see the JIRA
     * https://issues.jboss.org/browse/AS7-5177 for more info).
     *
     * @param securityDomainName the name of the security domain whose credentials are to change
     * @param username the new username to be associated with the security domain
     * @param password the new value of the password to store in the configuration (e.g. the obfuscated password itself)
     *
     * @throws Exception if failed to update security domain
     */
    public void updateSecureIdentitySecurityDomainCredentials(String securityDomainName, String username,
        String password) throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName,
            AUTHENTICATION, CLASSIC);

        ModelNode loginModule = new ModelNode();
        loginModule.get(CODE).set("SecureIdentity");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        addPossibleExpression(moduleOptions, USERNAME, username);
        addPossibleExpression(moduleOptions, PASSWORD, password);

        // login modules attribute must be a list - we only have one item in it, the loginModule
        ModelNode loginModuleList = new ModelNode();
        loginModuleList.setEmptyList();
        loginModuleList.add(loginModule);

        final ModelNode op = createRequest(WRITE_ATTRIBUTE, addr);
        op.get(NAME).set(LOGIN_MODULES);
        op.get(VALUE).set(loginModuleList);

        ModelNode results = execute(op);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to update credentials for security domain ["
                + securityDomainName + "]");
        }

        return;
    }

    private void addPossibleExpression(ModelNode node, String name, String value) {
        if (value != null && value.contains("${")) {
            node.add(name, new ModelNode(ModelType.EXPRESSION).setExpression(value));
        } else {
            node.add(name, value);
        }
    }

    /**
     * Given the name of an existing security domain that uses the SecureIdentity authentication method,
     * this returns the module options for that security domain authentication method. This includes
     * the username and password of the domain.
     *
     * @param securityDomainName the name of the security domain whose module options are to be returned
     * @return the module options or null if the security domain doesn't exist
     * @throws Exception if the security domain could not be looked up
     */
    public ModelNode getSecureIdentitySecurityDomainModuleOptions(String securityDomainName) throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName,
            AUTHENTICATION, CLASSIC);

        ModelNode authResource = readResource(addr);
        List<ModelNode> loginModules = authResource.get(LOGIN_MODULES).asList();
        for (ModelNode loginModule : loginModules) {
            if ("SecureIdentity".equals(loginModule.get(CODE).asString())) {
                ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
                return moduleOptions;
            }
        }

        return null;
    }

    /**
     * Create a new security domain using the database server authentication method.
     * This is used when you want to directly authenticate against a db entry.
     * This is for AS 7.2+ (e.g. EAP 6.1) and works around https://issues.jboss.org/browse/AS7-6527
     *
     * @param securityDomainName the name of the new security domain
     * @param dsJndiName the jndi name for the datasource to query against
     * @param principalsQuery the SQL query for selecting password info for a principal
     * @param rolesQuery the SQL query for selecting role info for a principal
     * @param hashAlgorithm if null defaults to "MD5"
     * @param hashEncoding if null defaults to "base64"
     * @throws Exception if failed to create security domain
     */
    public void createNewDatabaseServerSecurityDomain72(String securityDomainName, String dsJndiName,
                                                        String principalsQuery, String rolesQuery, String hashAlgorithm,
                                                        String hashEncoding) throws Exception {

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        Address authAddr = addr.clone().add(AUTHENTICATION, CLASSIC);
        ModelNode addAuthNode = createRequest(ADD, authAddr);

        // Create the login module in a separate step
        Address loginAddr = authAddr.clone().add("login-module","Database"); // name = code
        ModelNode loginModule = createRequest(ADD,loginAddr ); //addAuthNode.get(LOGIN_MODULES);
        loginModule.get(CODE).set("Database");
        loginModule.get(FLAG).set("required");
        ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
        moduleOptions.setEmptyList();
        moduleOptions.add(DS_JNDI_NAME, dsJndiName);
        moduleOptions.add(PRINCIPALS_QUERY, principalsQuery);
        moduleOptions.add(ROLES_QUERY, rolesQuery);
        moduleOptions.add(HASH_ALGORITHM, (null == hashAlgorithm ? "MD5" : hashAlgorithm));
        moduleOptions.add(HASH_ENCODING, (null == hashEncoding ? "base64" : hashEncoding));

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode, loginModule);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Convenience method that removes a security domain by name. Useful when changing the characteristics of the
     * login modules.
     *
     * @param securityDomainName the name of the new security domain
     * @throws Exception if failed to remove the security domain
     */
    public void removeSecurityDomain(String securityDomainName) throws Exception {

        // If not there just return
        if (!isSecurityDomain(securityDomainName)) {
            return;
        }

        final Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);
        ModelNode removeSecurityDomainNode = createRequest(REMOVE, addr);

        final ModelNode results = execute(removeSecurityDomainNode);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to remove security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * Creates a new security domain including one or more login modules.
     * The security domain will be replaced if it exists.
     *
     * @param securityDomainName the name of the new security domain
     * @param loginModules an array of login modules to place in the security domain. They are ordered top-down in the
     * same index order of the array.
     * @throws Exception if failed to create security domain
     */
    public void createNewSecurityDomain(String securityDomainName, LoginModuleRequest... loginModules) throws Exception {
        //do not close the controller client here, we're using our own..
        CoreJBossASClient coreClient = new CoreJBossASClient(getModelControllerClient());
        String serverVersion = coreClient.getAppServerVersion();
        if (serverVersion.startsWith("7.2")) {
            createNewSecurityDomain72(securityDomainName, loginModules);
        }
        else {
            createNewSecurityDomain71(securityDomainName,loginModules);
        }

    }

    private void createNewSecurityDomain71(String securityDomainName, LoginModuleRequest... loginModules) throws Exception {

        if (isSecurityDomain(securityDomainName)) {
            removeSecurityDomain(securityDomainName);
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);

        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        ModelNode addAuthNode = createRequest(ADD, addr.clone().add(AUTHENTICATION, CLASSIC));
        ModelNode loginModulesNode = addAuthNode.get(LOGIN_MODULES);

        for (int i = 0, len = loginModules.length; i < len; ++i) {
            ModelNode loginModule = new ModelNode();
            loginModule.get(CODE).set(loginModules[i].getLoginModuleFQCN());
            loginModule.get(FLAG).set(loginModules[i].getFlagString());
            ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
            moduleOptions.setEmptyList();

            Map<String, String> moduleOptionProperties = loginModules[i].getModuleOptionProperties();
            if (null != moduleOptionProperties) {
                for (String key : moduleOptionProperties.keySet()) {
                    String value = moduleOptionProperties.get(key);
                    if (null != value) {
                        moduleOptions.add(key, value);
                    }
                }
            }

            loginModulesNode.add(loginModule);
        }

        ModelNode batch = createBatchRequest(addTopNode, addAuthNode);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    private void createNewSecurityDomain72(String securityDomainName, LoginModuleRequest... loginModules) throws Exception {

        if (isSecurityDomain(securityDomainName)) {
            removeSecurityDomain(securityDomainName);
        }

        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN, securityDomainName);

        ModelNode addTopNode = createRequest(ADD, addr);
        addTopNode.get(CACHE_TYPE).set("default");

        Address authAddr = addr.clone().add(AUTHENTICATION, CLASSIC);
        ModelNode addAuthNode = createRequest(ADD, authAddr);


        // We add each login module via its own :add request
        // Add the start we put the 2 "top nodes" so that it works with the varargs below
        ModelNode[] steps = new ModelNode[loginModules.length+2];
        steps[0] = addTopNode;
        steps[1] = addAuthNode;

        for (int i = 0; i < loginModules.length; i++) {
            LoginModuleRequest moduleRequest = loginModules[i];
            Address loginAddr = authAddr.clone().add("login-module", moduleRequest.getLoginModuleFQCN());

            ModelNode loginModule = createRequest(ADD, loginAddr);
            loginModule.get(CODE).set(moduleRequest.getLoginModuleFQCN());
            loginModule.get(FLAG).set(moduleRequest.getFlagString());
            ModelNode moduleOptions = loginModule.get(MODULE_OPTIONS);
            moduleOptions.setEmptyList();

            Map<String, String> moduleOptionProperties = moduleRequest.getModuleOptionProperties();
            if (null != moduleOptionProperties) {
                for (String key : moduleOptionProperties.keySet()) {
                    String value = moduleOptionProperties.get(key);
                    if (null != value) {
                        moduleOptions.add(key, value);
                    }
                }
            }

            steps[i+2]=loginModule;
        }

        ModelNode batch = createBatchRequest(steps);
        ModelNode results = execute(batch);
        if (!isSuccess(results)) {
            throw new FailureException(results, "Failed to create security domain [" + securityDomainName + "]");
        }

        return;
    }

    /**
     * send a :flush-cache operation to the passed security domain
     * @param domain simple name of the domain
     * @throws Exception
     */
    public void flushSecurityDomainCache(String domain) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN,domain);
        ModelNode request = createRequest("flush-cache",addr);
        ModelNode result = execute(request);
        if (!isSuccess(result)) {
            log.warn("Flushing " + domain + " failed - principals may be longer cached than expected");
        }
    }

    /**
     * Check if a certain login module is present inside the passed security domain
     * @param domainName Name of the security domain
     * @param moduleName Name of the Login module - wich usually is it FQCN
     * @return True if the module is present
     * @throws Exception
     */
    public boolean securityDomainHasLoginModule(String domainName, String moduleName) throws Exception {
        Address addr = Address.root().add(SUBSYSTEM, SUBSYSTEM_SECURITY, SECURITY_DOMAIN,domainName);
        addr.add(AUTHENTICATION,CLASSIC);
        addr.add(LOGIN_MODULE,moduleName);
        ModelNode request = createRequest("read-resource", addr);
        ModelNode response = execute(request);
        return isSuccess(response);
    }

    /** Immutable helper */
    public static class LoginModuleRequest {
        private AppConfigurationEntry entry;

        /**
         * @param loginModuleFQCN fully qualified class name to be set as the login-module "code".
         * @param flag constant, one of required|requisite|sufficient|optional
         * @param moduleOptionProperties map of propName->propValue mappings to to bet as module options
         */
        public LoginModuleRequest(String loginModuleFQCN, AppConfigurationEntry.LoginModuleControlFlag flag,
            Map<String, String> moduleOptionProperties) {

            this.entry = new AppConfigurationEntry(loginModuleFQCN, flag, moduleOptionProperties);
        }

        public String getLoginModuleFQCN() {
            return entry.getLoginModuleName();
        }

        public AppConfigurationEntry.LoginModuleControlFlag getFlag() {
            return entry.getControlFlag();
        }

        // deal with the fact that this dumb LoginModuleControlFlag class gives you no way of getting the
        // necessary string value.  Don't try to pick it out of the toString() value which seems sensitive to locale
        public String getFlagString() {
            if (LoginModuleControlFlag.SUFFICIENT.equals(entry.getControlFlag())) {
                return "sufficient";
            }
            if (LoginModuleControlFlag.REQUISITE.equals(entry.getControlFlag())) {
                return "requisite";
            }
            if (LoginModuleControlFlag.REQUIRED.equals(entry.getControlFlag())) {
                return "required";
            }

            // return the last possibility
            return "optional";
        }

        public Map<String, String> getModuleOptionProperties() {
            return (Map<String, String>) entry.getOptions();
        }

        @Override
        public String toString() {
            return "LoginModuleRequest [loginModuleFQCN=" + getLoginModuleFQCN() + ", flag=" + getFlag()
                + ", moduleOptionProperties=" + getModuleOptionProperties() + "]";
        }
    }
}
