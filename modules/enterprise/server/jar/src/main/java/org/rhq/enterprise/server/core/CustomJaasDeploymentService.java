/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.enterprise.server.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.as.controller.client.ModelControllerClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient.LoginModuleRequest;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.util.obfuscation.Obfuscator;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.jaas.JDBCLoginModule;
import org.rhq.enterprise.server.core.jaas.JDBCPrincipalCheckLoginModule;
import org.rhq.enterprise.server.core.jaas.KeycloakLoginModule;
import org.rhq.enterprise.server.core.jaas.KeycloakLoginUtils;
import org.rhq.enterprise.server.core.jaas.LdapLoginModule;
import org.rhq.enterprise.server.core.service.ManagementService;
import org.rhq.enterprise.server.util.JMXUtil;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * Deploy the JAAS login modules that are configured. The JDBC login module is always deployed, however, the LDAP login
 * module is only deployed if LDAP is enabled in the RHQ configuration.
 */
@Singleton
@Startup
@LocalBean
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class CustomJaasDeploymentService implements CustomJaasDeploymentServiceMBean {

    private static final Log LOG = LogFactory.getLog(CustomJaasDeploymentService.class.getName());

    /**
     * @see org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean#installJaasModules()
     */
    public void installJaasModules() {
        try {
            LOG.info("Updating RHQ Server's JAAS login modules");
//            Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
//                LookupUtil.getSubjectManager().getOverlord());
//            updateJaasModules(systemConfig, false, false);
            upgradeRhqUserSecurityDomainIfNeeded();
        } catch (Exception e) {
            LOG.fatal("Error deploying JAAS login modules", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void upgradeRhqUserSecurityDomainIfNeeded() {
        try {
            Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord());

            String value = systemConfig.getProperty(SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName());
            boolean isLdapAuthenticationEnabled = (value != null) ? RHQConstants.LDAPJAASProvider.equals(value) : false;
            
            value = systemConfig.getProperty(SystemSetting.KEYCLOAK_URL.getInternalName());
            boolean isKeycloakAuthenticationEnabled = (value != null && !value.trim().isEmpty());

            if (isLdapAuthenticationEnabled || isKeycloakAuthenticationEnabled) {
                ModelControllerClient mcc = null;
                boolean ldapModulesPresent = isLdapAuthenticationEnabled;
                boolean keycloakModulesPresent = isKeycloakAuthenticationEnabled;
                try {
                    mcc = ManagementService.createClient();
                    final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);

                    if (isLdapAuthenticationEnabled) {
                        ldapModulesPresent = client.securityDomainHasLoginModule(RHQ_USER_SECURITY_DOMAIN,
                            "org.rhq.enterprise.server.core.jaas.LdapLoginModule");
                        if (!ldapModulesPresent) {
                            LOG.info("Updating RHQ Server's JAAS login modules with LDAP support");
                        }
                    }
                    if (isKeycloakAuthenticationEnabled) {
                        keycloakModulesPresent = client.securityDomainHasLoginModule(RHQ_USER_SECURITY_DOMAIN,
                            "org.rhq.enterprise.server.core.jaas.KeycloakLoginModule");
                        if (!keycloakModulesPresent) {
                            LOG.info("Updating RHQ Server's JAAS login modules with Keycloak support");
                        }

                    }
                    if (isLdapAuthenticationEnabled != ldapModulesPresent
                        || isKeycloakAuthenticationEnabled != keycloakModulesPresent) {
                        updateJaasModules(systemConfig, isLdapAuthenticationEnabled, isKeycloakAuthenticationEnabled);
                    }
                } finally {
                    MCCHelper.safeClose(mcc);
                }
            }
        } catch (Exception e) {
            LOG.fatal("Error deploying JAAS login modules", e);
            throw new RuntimeException(e);
        }

    }

    @PostConstruct
    private void init() {
        JMXUtil.registerMBean(this, OBJECT_NAME);
    }

    @PreDestroy
    private void destroy() {
        JMXUtil.unregisterMBeanQuietly(OBJECT_NAME);
    }

    /**
     * Will update the necessary JAAS login Modules.  The RHQ_USER_SECURITY_DOMAIN will be created, or recreated
     * if it already exists.  This allows us to add/remove ldap support as it is enabled or disabled.
     *
     * @param systemConfig System configuration to read the LDAP settings from
     * @throws Exception
     */
    private void updateJaasModules(Properties systemConfig, boolean installLdapModule, boolean installKeycloakModule)
        throws Exception {

        ModelControllerClient mcc = null;
        try {
            mcc = ManagementService.createClient();
            final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);

            if (client.isSecurityDomain(RHQ_USER_SECURITY_DOMAIN)) {
                LOG.info("Security domain [" + RHQ_USER_SECURITY_DOMAIN + "] already exists, it will be replaced.");
            }

            List<LoginModuleRequest> loginModules = new ArrayList<LoginModuleRequest>(4);

            // Always register the RHQ user JDBC login module, this checks the principal against the RHQ DB
            LoginModuleRequest jdbcLoginModule = new LoginModuleRequest(JDBCLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, getJdbcOptions(systemConfig));
            loginModules.add(jdbcLoginModule);

            if (installLdapModule) {
                // this is a "gatekeeper" that only allows us to go to LDAP if there is no principal in the DB
                LoginModuleRequest jdbcPrincipalCheckLoginModule = new LoginModuleRequest(
                    JDBCPrincipalCheckLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, getJdbcOptions(systemConfig));
                loginModules.add(jdbcPrincipalCheckLoginModule);

                // this is the LDAP module that checks the LDAP for auth
                Map<String, String> ldapModuleOptionProperties = getLdapOptions(systemConfig);
                try {
                    validateLdapOptions(ldapModuleOptionProperties);

                } catch (NamingException e) {
                    String descriptiveMessage = null;
                    if (e instanceof AuthenticationException) {
                        descriptiveMessage = "The LDAP integration cannot function because the LDAP Bind credentials"
                            + " for RHQ integration are incorrect. Contact the Administrator:" + e;

                    } else {
                        descriptiveMessage = "Problems encountered when communicating with LDAP server."
                            + " Contact the Administrator:" + e;
                    }
                    this.LOG.error(descriptiveMessage, e);
                }

                // Enable the login module even if the LDAP properties have issues
                LoginModuleRequest ldapLoginModule = new LoginModuleRequest(LdapLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, ldapModuleOptionProperties);
                loginModules.add(ldapLoginModule);
            }
            if (installKeycloakModule) {
//                if (!installLdapModule) {
//                    // this is a "gatekeeper" that only allows us to go to Keycloak if there is no principal in the DB
//                    // add this only if not added in previous step
//                    LoginModuleRequest jdbcPrincipalCheckLoginModule = new LoginModuleRequest(
//                        JDBCPrincipalCheckLoginModule.class.getName(),
//                        AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, getJdbcOptions(systemConfig));
//                    loginModules.add(jdbcPrincipalCheckLoginModule);
//                }

                // Enable the login module even if the LDAP properties have issues
                LoginModuleRequest keycloakLoginModule = new LoginModuleRequest(KeycloakLoginModule.class.getName(),
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE, getKeycloakOptions(systemConfig));
                loginModules.add(keycloakLoginModule);
            }

            client.createNewSecurityDomain(RHQ_USER_SECURITY_DOMAIN,
                loginModules.toArray(new LoginModuleRequest[loginModules.size()]));
            client.flushSecurityDomainCache("RHQRESTSecurityDomain");
            LOG.info("Security domain [" + RHQ_USER_SECURITY_DOMAIN + "] re-created with login modules " + loginModules);

        } catch (Exception e) {
            throw new Exception("Error registering RHQ JAAS modules", e);
        } finally {
            MCCHelper.safeClose(mcc);
        }
    }

    private Map<String, String> getJdbcOptions(Properties conf) {
        Map<String, String> configOptions = new HashMap<String, String>();

        // We always store passwords encoded.  Don't allow the end user to change this behavior.
        configOptions.put("hashAlgorithm", "MD5");
        configOptions.put("hashEncoding", "base64");

        return configOptions;
    }

    private Map<String, String> getLdapOptions(Properties conf) throws Exception {
        Map<String, String> configOptions = new HashMap<String, String>();

        configOptions.put(Context.INITIAL_CONTEXT_FACTORY, conf.getProperty(RHQConstants.LDAPFactory));
        configOptions.put(Context.PROVIDER_URL, conf.getProperty(RHQConstants.LDAPUrl));
        String value = conf.getProperty(SystemSetting.USE_SSL_FOR_LDAP.getInternalName());
        boolean ldapSsl = "ssl".equalsIgnoreCase(value);
        configOptions.put(Context.SECURITY_PROTOCOL, (ldapSsl) ? "ssl" : null);
        configOptions.put("LoginProperty", conf.getProperty(RHQConstants.LDAPLoginProperty));
        configOptions.put("Filter", conf.getProperty(RHQConstants.LDAPFilter));
        configOptions.put("GroupFilter", conf.getProperty(RHQConstants.LDAPGroupFilter));
        configOptions.put("GroupMemberFilter", conf.getProperty(RHQConstants.LDAPGroupMember));
        configOptions.put("BaseDN", conf.getProperty(RHQConstants.LDAPBaseDN));
        configOptions.put("BindDN", conf.getProperty(RHQConstants.LDAPBindDN));
        configOptions.put("BindPW", Obfuscator.encode(conf.getProperty(RHQConstants.LDAPBindPW)));
        boolean followReferralsBoolean = Boolean.valueOf(conf.getProperty(SystemSetting.LDAP_FOLLOW_REFERRALS.getInternalName(),
            "false"));
        configOptions.put(Context.REFERRAL, followReferralsBoolean ? "follow" : "ignore");

        return configOptions;
    }
    
    private Map<String, String> getKeycloakOptions(Properties conf) throws Exception {
        Map<String, String> configOptions = new HashMap<String, String>();
        configOptions.put(SystemSetting.KEYCLOAK_URL.getInternalName(), conf.getProperty(SystemSetting.KEYCLOAK_URL.getInternalName()));
        // todo: not hc
        //bindPW = Obfuscator.decode(bindPW);
        configOptions.put("client-id", KeycloakLoginUtils.APP_NAME);

        return configOptions;
    }


    private void validateLdapOptions(Map<String, String> options) throws NamingException {
        Properties env = new Properties();

        String factory = options.get(Context.INITIAL_CONTEXT_FACTORY);
        if (factory == null) {
            throw new NamingException("No initial context factory");
        }

        String url = options.get(Context.PROVIDER_URL);
        if (url == null) {
            throw new NamingException("Naming provider url not set");
        }

        String protocol = options.get(Context.SECURITY_PROTOCOL);
        if ("ssl".equals(protocol)) {
            String ldapSocketFactory = env.getProperty("java.naming.ldap.factory.socket");
            if (ldapSocketFactory == null) {
                env.put("java.naming.ldap.factory.socket", UntrustedSSLSocketFactory.class.getName());
            }
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        env.setProperty(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.setProperty(Context.PROVIDER_URL, url);

        // Load any information we may need to bind
        String bindDN = options.get("BindDN");
        String bindPW = options.get("BindPW");
        try {
            bindPW = Obfuscator.decode(bindPW);
        } catch (Exception e) {
            LOG.debug("Failed to decode bindPW, binding using undecoded value [" + bindPW + "]", e);
        }
        if ((bindDN != null) && (bindDN.length() != 0) && (bindPW != null) && (bindPW.length() != 0)) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        LOG.debug("Validating LDAP properties. Initializing context...");
        new InitialLdapContext(env, null).close();

        return;
    }
}
