/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.as.controller.client.ModelControllerClient;

import org.rhq.common.jbossas.client.controller.SecurityDomainJBossASClient;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.jaas.JDBCLoginModule;
import org.rhq.enterprise.server.core.jaas.JDBCPrincipalCheckLoginModule;
import org.rhq.enterprise.server.core.jaas.LdapLoginModule;
import org.rhq.enterprise.server.core.service.ManagementService;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/**
 * Deploy the JAAS login modules that are configured. The JDBC login module is always deployed, however, the LDAP login
 * module is only deployed if LDAP is enabled in the RHQ configuration.
 */
public class CustomJaasDeploymentService implements CustomJaasDeploymentServiceMBean, MBeanRegistration {

    private Log log = LogFactory.getLog(CustomJaasDeploymentService.class.getName());
    private MBeanServer mbeanServer = null;

    /**
     * Constructor for {@link CustomJaasDeploymentService}.
     */
    public CustomJaasDeploymentService() {
    }

    /**
     * @see org.rhq.enterprise.server.core.CustomJaasDeploymentServiceMBean#installJaasModules()
     */
    public void installJaasModules() {
        try {
            log.info("Installing RHQ Server's JAAS login modules");
            Properties systemConfig = LookupUtil.getSystemManager().getSystemConfiguration(
                LookupUtil.getSubjectManager().getOverlord());
            registerJaasModules(systemConfig);
        } catch (Exception e) {
            log.fatal("Error deploying JAAS login modules", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer,javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
        this.mbeanServer = server;
        return name;
    }

    /**
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean registrationDone) {
    }

    /**
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() {
    }

    /**
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
    }

    private void registerJaasModules(Properties systemConfig) throws Exception {

        ModelControllerClient mcc = null;
        try {
            mcc = ManagementService.getClient();
            final SecurityDomainJBossASClient client = new SecurityDomainJBossASClient(mcc);
            final String securityDomain = RHQ_USER_SECURITY_DOMAIN;

            if (client.isSecurityDomain(securityDomain)) {
                log.info("Security domain [" + securityDomain + "] already exists, skipping the creation request");
                return;
            }

            // Always register the RHQ user JDBC login module, this checks the pricipal against the RHQ DB
            Map<String, String> moduleOptionProperties = getJdbcOptions(systemConfig);
            String code = JDBCLoginModule.class.getName();

            client.createNewCustomSecurityDomainRequest(securityDomain, code, moduleOptionProperties);
            log.info("Security domain [" + securityDomain + "] created");
            log.info("Security domain login module [" + securityDomain + ":" + code + "] created");

            // Optionally register two more login modules for LDAP support. The first ensures
            // we don't have a DB principal (if we do then the JDBC login module is sufficient.
            // The second performs the actual LDAP authorization.
            String value = systemConfig.getProperty(SystemSetting.LDAP_BASED_JAAS_PROVIDER.getInternalName());
            boolean isLdapAuthenticationEnabled = (value != null) ? RHQConstants.LDAPJAASProvider.equals(value) : false;

            if (isLdapAuthenticationEnabled) {
                // this is a "gatekeeper" that only allows us to go to LDAP if there is no principal in the DB
                moduleOptionProperties = getJdbcOptions(systemConfig);
                code = JDBCPrincipalCheckLoginModule.class.getName();

                client.createNewCustomSecurityDomainRequest(securityDomain, code, moduleOptionProperties);
                log.info("Security domain login module [" + securityDomain + ":" + code + "] created");

                // this is the LDAP module that checks the LDAP for auth
                moduleOptionProperties = getLdapOptions(systemConfig);
                try {
                    validateLdapOptions(moduleOptionProperties);

                } catch (NamingException e) {
                    String descriptiveMessage = null;
                    if (e instanceof AuthenticationException) {
                        descriptiveMessage = "The LDAP integration cannot function because the LDAP Bind credentials"
                            + " for RHQ integration are incorrect. Contact the Administrator:" + e;

                    } else {
                        descriptiveMessage = "Problems encountered when communicating with LDAP server."
                            + " Contact the Administrator:" + e;
                    }
                    this.log.error(descriptiveMessage, e);
                }

                // Enable the login module even if the LDAP properties have issues
                code = LdapLoginModule.class.getName();

                client.createNewCustomSecurityDomainRequest(securityDomain, code, moduleOptionProperties);
                log.info("Security domain login module [" + securityDomain + ":" + code + "] created");
            }
        } catch (Exception e) {
            throw new Exception("Error registering RHQ JAAS modules", e);
        } finally {
            safeClose(mcc);
        }
    }

    private static void safeClose(final ModelControllerClient mcc) {
        if (null != mcc) {
            try {
                mcc.close();
            } catch (Exception e) {
            }
        }
    }

    private Map<String, String> getJdbcOptions(Properties conf) {
        Map<String, String> configOptions = new HashMap<String, String>();

        // We always store passwords encoded.  Don't allow the end user to change this behavior.
        configOptions.put("hashAlgorithm", "MD5");
        configOptions.put("hashEncoding", "base64");

        return configOptions;
    }

    private Map<String, String> getLdapOptions(Properties conf) {
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
        configOptions.put("BindPW", conf.getProperty(RHQConstants.LDAPBindPW));

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
        if ((bindDN != null) && (bindDN.length() != 0) && (bindPW != null) && (bindPW.length() != 0)) {
            env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
            env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
            env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        log.debug("Validating LDAP properties. Initializing context...");
        new InitialLdapContext(env, null).close();

        return;
    }
}