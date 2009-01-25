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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.core.jaas.JDBCLoginModule;
import org.rhq.enterprise.server.core.jaas.LdapLoginModule;
import org.rhq.enterprise.server.core.jaas.TempSessionLoginModule;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Deploy the JAAS login modules that are configured. JDBC login module is always deployed, however, the LDAP login
 * module is only deployed if LDAP is enabled in the RHQ configuration.
 */
public class CustomJaasDeploymentService implements CustomJaasDeploymentServiceMBean, MBeanRegistration {
    private static final String AUTH_METHOD = "addAppConfig";
    private static final String AUTH_OBJECTNAME = "jboss.security:service=XMLLoginConfig";

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
            Properties conf = LookupUtil.getSystemManager().getSystemConfiguration();
            registerJaasModules(conf);
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

    private void registerJaasModules(Properties conf) throws Exception {
        List<AppConfigurationEntry> configEntries = new ArrayList<AppConfigurationEntry>();
        AppConfigurationEntry ace;
        Map<String, String> configOptions;

        try {
            configOptions = getJdbcOptions(conf);
            ace = new AppConfigurationEntry(JDBCLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, configOptions);

            // We always add the JDBC provider to the auth config
            this.log.info("Enabling RHQ JDBC JAAS Provider");
            configEntries.add(ace);

            // to support the need for authenticating temporary session passwords, add a login module that can do that
            // we set an empty set of config options, but if we need to, we can store the config items
            // in the RHQ_config_props table, which would allow the GUI to modify them (just in case we want to add that capability)
            // the "conf" Properties value has all RHQ_config_props values, so we have that now
            // for now, there are no config properties we need in this login module, so just create an empty map
            ace = new AppConfigurationEntry(TempSessionLoginModule.class.getName(),
                AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, new HashMap<String, String>());
            this.log.info("Enabled the temporary session login module");
            configEntries.add(ace);

            String provider = conf.getProperty(RHQConstants.JAASProvider);

            if ((provider != null) && provider.equals(RHQConstants.LDAPJAASProvider)) {
                configOptions = getLdapOptions(conf);
                try {
                    validateLdapOptions(configOptions);
                    ace = new AppConfigurationEntry(LdapLoginModule.class.getName(),
                        AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT, configOptions);
                    this.log.info("Enabling RHQ LDAP JAAS Provider");
                    configEntries.add(ace);
                } catch (NamingException e) {
                    this.log.info("Disabling RHQ LDAP JAAS Provider: " + e.getMessage(), e);
                }
            }

            AppConfigurationEntry[] config = configEntries.toArray(new AppConfigurationEntry[0]);

            ObjectName objName = new ObjectName(AUTH_OBJECTNAME);
            Object obj = mbeanServer.invoke(objName, AUTH_METHOD, new Object[] { SECURITY_DOMAIN_NAME, config },
                new String[] { "java.lang.String", config.getClass().getName() });
        } catch (Exception e) {
            throw new Exception("Error registering RHQ JAAS Modules", e);
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
        configOptions.put(Context.SECURITY_PROTOCOL, conf.getProperty(RHQConstants.LDAPProtocol));
        configOptions.put("LoginProperty", conf.getProperty(RHQConstants.LDAPLoginProperty));
        configOptions.put("Filter", conf.getProperty(RHQConstants.LDAPFilter));
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
        if ((protocol != null) && protocol.equals("ssl")) {
            env.put("java.naming.ldap.factory.socket",
                "org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory");
            env.put(Context.SECURITY_PROTOCOL, protocol);
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

        log.debug("Validating LDAP with environment=" + env);
        new InitialLdapContext(env, null);

        return;
    }
}